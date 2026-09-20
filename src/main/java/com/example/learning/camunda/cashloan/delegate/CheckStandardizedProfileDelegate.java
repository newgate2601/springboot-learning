package com.example.learning.camunda.cashloan.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Gắn trên send_task.check_standardized_profile ("Check CHHS" - check hồ sơ đã chuẩn hóa
 * hay chưa). Bắn yêu cầu đi, chờ kết quả trả về qua message CHECK_STANDARDIZED_PROFILE_RESULT
 * (receive_task.check_standardized_profile). Kết quả set trong output của message này quyết
 * định 2 gateway phía sau: checkStandardizedProfileSuccess (check có chạy được không) và
 * useStandardizedEkycFlow (nếu chạy được, hồ sơ có thuộc diện chuẩn hóa hay không).
 */
public class CheckStandardizedProfileDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CheckStandardizedProfileDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("[{}] CheckStandardizedProfileDelegate: đã gửi yêu cầu check hồ sơ chuẩn hóa, đang chờ message CHECK_STANDARDIZED_PROFILE_RESULT",
                execution.getProcessInstanceId());
        execution.setVariableLocal("checkStandardizedProfileRequestedAt", Instant.now().toString());
    }
}
