package com.example.learning.camunda;

import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CashloanDemo {
    public static final String LAB_PROCESS_KEY = "CAKE_FEATURE_LAB";
    private static final long EXTERNAL_TASK_LOCK_MS = 30000;
    private static final Logger log = LoggerFactory.getLogger(CashloanDemo.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final ExternalTaskService externalTaskService;
    private final HistoryService historyService;
    private final String workerId = "cashloan-demo-" + UUID.randomUUID();

    public CashloanDemo(RuntimeService runtimeService, TaskService taskService,
                         ExternalTaskService externalTaskService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.externalTaskService = externalTaskService;
        this.historyService = historyService;
    }

    // Không còn deployOnStartup()/RestTemplate gọi /deployment/create nữa.
    // camunda-bpm-spring-boot-starter tự deploy processes/cashloan-feature-lab.bpmn
    // khi context khởi động, theo camunda.bpm.deployment-resource-pattern (application.yaml).

    public String startLab(Boolean segmentSuccess, Boolean precheckPassed, String segmentType) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("segmentSuccess", segmentSuccess == null || segmentSuccess);
        variables.put("precheckPassed", precheckPassed == null || precheckPassed);
        variables.put("segmentType", segmentType == null || segmentType.isEmpty() ? "VTP_ON_NET" : segmentType);
        String id = runtimeService.startProcessInstanceByKey(LAB_PROCESS_KEY, variables).getId();
        log.info("Started {}: processInstanceId={}, inputs={}", LAB_PROCESS_KEY, id, variables);
        return id;
    }

    public Map<String, Object> status(String id) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(id).list();
        List<String> visited = new ArrayList<>();
        String outcome = null;
        for (HistoricActivityInstance activity : activities) {
            visited.add(activity.getActivityId());
            if (activity.getActivityType() != null && activity.getActivityType().endsWith("EndEvent") &&
                    ("lab_rejected_segment".equals(activity.getActivityId()) || "lab_rejected_precheck".equals(activity.getActivityId()) ||
                     "lab_skip_scoring".equals(activity.getActivityId()) || "lab_ready_for_scoring".equals(activity.getActivityId()))) {
                outcome = activity.getActivityId();
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", id);
        result.put("outcome", outcome == null ? "RUNNING" : outcome);
        result.put("visitedActivities", visited);
        return result;
    }

    public Map<String, Object> labStatus(String id) {
        Map<String, Object> result = status(id);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(id).list();
        if (!tasks.isEmpty()) {
            result.put("waitingTaskId", tasks.getFirst().getId());
            result.put("waitingTaskName", tasks.getFirst().getName());
        }
        List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery().processInstanceId(id).list();
        if (!externalTasks.isEmpty()) {
            result.put("waitingExternalTaskId", externalTasks.getFirst().getId());
            result.put("waitingExternalTaskTopic", externalTasks.getFirst().getTopicName());
        }
        List<HistoricVariableInstance> history = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(id).list();
        Map<String, Object> variables = new LinkedHashMap<>();
        for (HistoricVariableInstance item : history) variables.put(item.getName(), item.getValue());
        result.put("variables", variables);
        return result;
    }

    public void completeLabReview(String id) {
        Task task = taskService.createTaskQuery().processInstanceId(id).taskDefinitionKey("lab_manual_review").singleResult();
        if (task == null) throw new IllegalStateException("No review task for process " + id);
        taskService.complete(task.getId());
        log.info("[{}] Completed lab manual review", id);
    }

    public void processLabSegment(String processInstanceId) {
        ExternalTask task = lockExternalTask(processInstanceId, "lab-segment");
        Map<String, Object> input = runtimeService.getVariablesLocal(task.getExecutionId());
        boolean success = Boolean.TRUE.equals(input.getOrDefault("requestSegmentSuccess", true));
        String segmentType = String.valueOf(input.getOrDefault("requestType", "VTP_ON_NET"));
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("isSegmentEvaluatedSuccess", success);
        output.put("segmentType", segmentType);
        externalTaskService.complete(task.getId(), workerId, output);
        log.info("[{}] Processed lab-segment: success={}, segmentType={}", processInstanceId, success, segmentType);
    }

    public void processLabPrecheck(String processInstanceId) {
        ExternalTask task = lockExternalTask(processInstanceId, "lab-precheck");
        Map<String, Object> input = runtimeService.getVariablesLocal(task.getExecutionId());
        boolean passed = Boolean.TRUE.equals(input.getOrDefault("requestPrecheck", true));
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("isPrecheckPassed", passed);
        externalTaskService.complete(task.getId(), workerId, output);
        log.info("[{}] Processed lab-precheck: passed={}", processInstanceId, passed);
    }

    private ExternalTask lockExternalTask(String processInstanceId, String topic) {
        List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery()
                .processInstanceId(processInstanceId).topicName(topic).list();
        if (tasks.isEmpty()) {
            throw new IllegalStateException("No external task " + topic + " for process " + processInstanceId);
        }
        if (tasks.size() > 1) {
            throw new IllegalStateException("Multiple external tasks " + topic + " for process " + processInstanceId);
        }
        ExternalTask task = tasks.get(0);
        externalTaskService.lock(task.getId(), workerId, EXTERNAL_TASK_LOCK_MS);
        return task;
    }

    public void correlateLabSegment(String id) {
        runtimeService.createMessageCorrelation("LabSegmentResult")
                .processInstanceId(id)
                .correlate();
        log.info("[{}] Correlated LabSegmentResult", id);
    }
}
