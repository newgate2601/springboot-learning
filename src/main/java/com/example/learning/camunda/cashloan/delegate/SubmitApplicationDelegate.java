package com.example.learning.camunda.cashloan.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Gắn trên messageEventDefinition của intermediateThrowEvent "submit_application".
 * Camunda gọi class này ngay khi token đi tới event đó (đồng bộ, cùng thread với request
 * đang chạy engine) để bắn hồ sơ vay sang hệ thống thẩm định/lending core.
 */
public class SubmitApplicationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SubmitApplicationDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("[{}] SubmitApplicationDelegate: gửi hồ sơ vay đi submit application (đang giả lập, chưa gọi hệ thống thật)",
                execution.getProcessInstanceId());
        execution.setVariableLocal("submitApplicationAt", Instant.now().toString());
    }
}
