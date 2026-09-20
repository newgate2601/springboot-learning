package com.example.learning.camunda.cashloan.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Gắn trên send_task.scoring ("Chấm điểm"). Chỉ chạy khi khách KHÔNG phải VTP_OFF_NET
 * (gateway.check_is_vtp_off_net = No). Bắn yêu cầu chấm điểm tín dụng đi, chờ kết quả
 * qua message SCORING_RESULT (receive_task.scoring), rồi mới đi tiếp sang
 * send_task.decision_making.
 */
public class GetScoringDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(GetScoringDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("[{}] GetScoringDelegate: đã gửi yêu cầu chấm điểm, đang chờ message SCORING_RESULT",
                execution.getProcessInstanceId());
        execution.setVariableLocal("scoringRequestedAt", Instant.now().toString());
    }
}
