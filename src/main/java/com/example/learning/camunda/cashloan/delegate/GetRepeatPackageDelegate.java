package com.example.learning.camunda.cashloan.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Gắn trên send_task.get_repeat_package ("Lấy gói vay lại" - dùng cho khách upsell/vay
 * lại). Chỉ chạy khi gateway.need_get_repeated_package trả về true. Bắn yêu cầu đi, chờ
 * kết quả qua message GET_REPEAT_PACKAGE_RESULT (receive_task.get_repeat_package). Kết
 * quả set biến getRepeatPackageSuccess.
 */
public class GetRepeatPackageDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(GetRepeatPackageDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("[{}] GetRepeatPackageDelegate: đã gửi yêu cầu lấy gói vay lại, đang chờ message GET_REPEAT_PACKAGE_RESULT",
                execution.getProcessInstanceId());
        execution.setVariableLocal("getRepeatPackageRequestedAt", Instant.now().toString());
    }
}
