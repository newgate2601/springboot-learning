package com.example.learning.camunda.cashloan.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Dùng chung cho CẢ BA chỗ sàng lọc khách hàng trong process:
 *  - send_task.precheck_1                  (sàng lọc lần 1, ngay sau phân loại segment)
 *  - send_task.precheck_2_no_standardized  (sàng lọc lần 2, nhánh chưa chuẩn hóa hồ sơ)
 *  - send_task.precheck_2_standardized     (sàng lọc lần 2, nhánh đã chuẩn hóa hồ sơ)
 * BPMN không truyền input param nào để phân biệt 3 chỗ này (properties panel trống,
 * chỉ có camunda:class + asyncBefore), nên cách nhận biết đang chạy ở nhánh nào là đọc
 * execution.getCurrentActivityId() ngay trong lúc execute() — activity id lúc đó chính là
 * "send_task.precheck_1" / "..._no_standardized" / "..._standardized". Cả 3 chỗ đều chờ
 * chung một message PRECHECK_RESULT để nhận kết quả (Message_38bu3k5), vì message chỉ cần
 * đúng processInstanceId là correlate được, không quan trọng đang đứng ở receive task nào.
 */
public class PreCheckDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PreCheckDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        log.info("[{}] PreCheckDelegate: đã gửi yêu cầu sàng lọc khách hàng tại bước '{}', đang chờ message PRECHECK_RESULT",
                execution.getProcessInstanceId(), activityId);
        execution.setVariableLocal("precheckRequestedAt", Instant.now().toString());
        execution.setVariableLocal("precheckRequestedAtStep", activityId);
    }
}
