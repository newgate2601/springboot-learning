package com.example.learning.camunda.cashloan.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Gắn trên send_task.offer_package ("Phân gói hồ sơ"). Bắn yêu cầu tính toán gói vay phù
 * hợp cho khách, chờ kết quả trả về qua message OFFER_PACKAGE_RESULT
 * (receive_task.offer_package). Kết quả set biến packageOfferSuccess, gateway
 * gateway.offer_package_success đọc biến này để quyết định đi tiếp hay kết thúc ở
 * offer_package_fail.
 */
public class PackageOfferDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PackageOfferDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        log.info("[{}] PackageOfferDelegate: đã gửi yêu cầu phân gói hồ sơ, đang chờ message OFFER_PACKAGE_RESULT",
                execution.getProcessInstanceId());
        execution.setVariableLocal("offerPackageRequestedAt", Instant.now().toString());
    }
}
