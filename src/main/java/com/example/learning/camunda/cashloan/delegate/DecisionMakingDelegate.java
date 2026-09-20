package com.example.learning.camunda.cashloan.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Gắn trên send_task.decision_making ("Ra quyết định"). Bắn yêu cầu đi hệ thống ra quyết
 * định cuối (dựa trên điểm scoring), chờ kết quả qua message DECISION_MAKING_RESULT
 * (receive_task.decision_making). Kết quả set biến isDecisionMakingPassed; nếu pass thì
 * đi submit_application, nếu không thì rẽ qua gateway.check_reoffer để xem có được offer
 * lại (đổi số tiền/kỳ hạn) hay không, trước khi kết thúc ở decision_making_fail.
 */
public class DecisionMakingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(DecisionMakingDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("[{}] DecisionMakingDelegate: đã gửi yêu cầu ra quyết định, đang chờ message DECISION_MAKING_RESULT",
                execution.getProcessInstanceId());
        execution.setVariableLocal("decisionMakingRequestedAt", Instant.now().toString());
    }
}
