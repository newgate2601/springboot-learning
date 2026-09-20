package com.example.learning.camunda.cashloan;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.EventSubscription;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service điều khiển process CAKE_CASHLOAN (BPMN: processes/cake-cashloan.bpmn).
 *
 * Khác hẳn CashloanDemo (lab cũ, dùng External Task): process này dùng camunda:class
 * (JavaDelegate) cho các Send Task — 9 delegate trong package
 * com.example.learning.camunda.cashloan.delegate. Các delegate đó chạy TỰ ĐỘNG, đồng bộ,
 * ngay khi engine đi tới activity — không cần Spring Boot gọi API nào để "kích" chúng chạy
 * cả (khác hẳn External Task phải lock rồi complete thủ công).
 *
 * Việc duy nhất Spring Boot cần chủ động làm là:
 *   1) Start process (POST /start)
 *   2) Trả kết quả về cho process bằng cách correlate message (khi process đang đứng ở một
 *      Receive Task / message catch), hoặc complete User Task (khi process đang chờ người
 *      dùng nhập liệu / thao tác trên Tasklist).
 *
 * Process này có tới ~24 message định nghĩa và 7 User Task khác nhau (ekyc, ký hợp đồng,
 * nhập thông tin khách hàng, reoffer,...), nên thay vì viết 20-30 API riêng biệt cho từng
 * message/task, service này để API tổng quát: truyền đúng tên message hoặc đúng
 * taskDefinitionKey là dùng được cho bất kỳ điểm chờ nào trong toàn bộ luồng.
 */
@Service
public class CakeCashloanService {

    public static final String PROCESS_KEY = "CAKE_CASHLOAN";

    private static final Logger log = LoggerFactory.getLogger(CakeCashloanService.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    public CakeCashloanService(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    /**
     * Tạo process instance mới. variables ở đây chính là những biến process đọc ngay từ
     * đầu, ví dụ segmentType, isVtpOffNet,... (tùy hồ sơ demo bạn muốn dựng).
     */
    public String start(Map<String, Object> variables) {
        Map<String, Object> vars = variables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(variables);
        String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY, vars).getId();
        log.info("Started {}: processInstanceId={}, inputs={}", PROCESS_KEY, id, vars);
        return id;
    }

    /**
     * Trạng thái hiện tại của process: đã đi qua activity nào, đang chờ User Task nào,
     * đang chờ message nào (Receive Task / message catch), và toàn bộ biến hiện có.
     */
    public Map<String, Object> status(String processInstanceId) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();
        List<String> visited = new ArrayList<>();
        String outcome = null;
        for (HistoricActivityInstance activity : activities) {
            visited.add(activity.getActivityId());
            if (activity.getActivityType() != null && activity.getActivityType().endsWith("EndEvent")) {
                outcome = activity.getActivityId();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", processInstanceId);
        result.put("outcome", outcome == null ? "RUNNING" : outcome);
        result.put("visitedActivities", visited);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        List<Map<String, Object>> waitingTasks = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("taskId", task.getId());
            t.put("taskDefinitionKey", task.getTaskDefinitionKey());
            t.put("name", task.getName());
            waitingTasks.add(t);
        }
        result.put("waitingTasks", waitingTasks);

        List<EventSubscription> subs = runtimeService.createEventSubscriptionQuery()
                .processInstanceId(processInstanceId)
                .eventType("message")
                .list();
        List<String> waitingMessages = new ArrayList<>();
        for (EventSubscription sub : subs) {
            waitingMessages.add(sub.getEventName());
        }
        result.put("waitingMessages", waitingMessages);

        List<HistoricVariableInstance> history = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();
        Map<String, Object> variables = new LinkedHashMap<>();
        for (HistoricVariableInstance item : history) {
            variables.put(item.getName(), item.getValue());
        }
        result.put("variables", variables);

        return result;
    }

    /**
     * Hoàn thành một User Task (ví dụ: cust.input.application.form, cust.sign-contract,
     * cust.ekyc,...). taskDefinitionKey lấy từ field "waitingTasks[].taskDefinitionKey"
     * trong response của status(). variables truyền vào là dữ liệu người dùng "nhập" ở
     * bước đó (ví dụ số tiền vay, kỳ hạn,...).
     */
    public void completeTask(String processInstanceId, String taskDefinitionKey, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .singleResult();
        if (task == null) {
            throw new IllegalStateException("Không có task '" + taskDefinitionKey + "' đang chờ ở process " + processInstanceId
                    + ". Gọi GET /{id} để xem waitingTasks hiện tại.");
        }
        taskService.complete(task.getId(), variables == null ? Collections.emptyMap() : variables);
        log.info("[{}] Completed user task {}", processInstanceId, taskDefinitionKey);
    }

    /**
     * Gửi kết quả cho process bằng cách correlate một message (dùng cho mọi Receive Task /
     * message catch trong process, ví dụ EVALUATE_CUSTOMER_SEGMENT_RESULT, PRECHECK_RESULT,
     * SCORING_RESULT, UNDERWRITING_RESULT,...). messageName lấy từ field "waitingMessages"
     * trong response của status(). variables chính là output mà "hệ thống ngoài" (mà ở đây
     * ta đang giả lập) trả về, ví dụ isSegmentEvaluatedSuccess=true.
     */
    public void correlateMessage(String processInstanceId, String messageName, Map<String, Object> variables) {
        runtimeService.createMessageCorrelation(messageName)
                .processInstanceId(processInstanceId)
                .setVariables(variables == null ? Collections.emptyMap() : variables)
                .correlate();
        log.info("[{}] Correlated message {} với variables={}", processInstanceId, messageName, variables);
    }
}
