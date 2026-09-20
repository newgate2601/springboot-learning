package com.example.learning.camunda.cashloan.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Gắn trên send_task.underwriting ("Thẩm định hồ sơ"). Đây là bước gần cuối, chạy sau khi
 * khách ký hợp đồng (cust.sign-contract). Bắn yêu cầu thẩm định cuối cùng đi bên cho vay
 * (lender), chờ kết quả qua message UNDERWRITING_RESULT (receive_task.underwriting). Kết
 * quả set biến underwritingPassed; nếu pass thì hồ sơ chuyển trạng thái "Chờ giải ngân"
 * (pending_disbursement) — đây là end event thành công của toàn bộ process.
 */
public class UnderwritingApplicationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingApplicationDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("[{}] UnderwritingApplicationDelegate: đã gửi yêu cầu thẩm định hồ sơ, đang chờ message UNDERWRITING_RESULT",
                execution.getProcessInstanceId());
        execution.setVariableLocal("underwritingRequestedAt", Instant.now().toString());
    }
}
