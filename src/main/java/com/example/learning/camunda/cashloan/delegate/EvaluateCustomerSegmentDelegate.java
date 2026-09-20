package com.example.learning.camunda.cashloan.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Gắn trên send_task.evaluate_customer_segment ("Phân loại segment").
 * Đây là Send Task kiểu camunda:class (JavaDelegate) chứ không phải External Task, nên
 * engine chạy class này NGAY LẬP TỨC khi token tới đây (nhờ asyncBefore=true nên có job
 * chạy trong background thread, không nằm trên thread HTTP request gốc). Class chỉ có
 * nhiệm vụ "bắn request" đi hệ thống ngoài (module chấm segment), KHÔNG trả kết quả ngay.
 * Kết quả thật sự sẽ tới sau, qua message EVALUATE_CUSTOMER_SEGMENT_RESULT, tương ứng
 * receive_task.evaluate_customer_segment ngay phía sau.
 */
public class EvaluateCustomerSegmentDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(EvaluateCustomerSegmentDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("[{}] EvaluateCustomerSegmentDelegate: đã gửi yêu cầu phân loại segment, đang chờ message EVALUATE_CUSTOMER_SEGMENT_RESULT",
                execution.getProcessInstanceId());
        execution.setVariableLocal("evaluateCustomerSegmentRequestedAt", Instant.now().toString());
    }
}
