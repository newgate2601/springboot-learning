package com.example.learning.controller;

import com.example.learning.dto.TransferRestRequest;
import com.example.learning.dto.TransferRestResponse;
import com.example.learning.security.PartnerSecurityService;
import com.example.learning.transfer.PartnerTransferService;
import com.example.learning.transfer.TransferCommand;
import com.example.learning.transfer.TransferResult;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // REST controller: Spring sẽ nhận HTTP request và trả JSON response.
@RequestMapping("/api/v1/partner-transfer") // Base path của API: POST /api/v1/partner-transfer.
@AllArgsConstructor // Lombok tạo constructor để inject các field final bên dưới.
public class TransferRestController {
    private static final Logger log = LoggerFactory.getLogger(TransferRestController.class); // Logger để nhìn flow REST trong console.

    private final PartnerTransferService transferService; // Service xử lý nghiệp vụ chuyển tiền dùng chung REST/SOAP.
    private final PartnerSecurityService securityService; // Service kiểm tra client, timestamp và chữ ký request.

    @PostMapping // Method này xử lý HTTP POST vào base path ở trên.
    public TransferRestResponse transfer(
            @RequestHeader("X-Client-Id") String clientId, // REST truyền client id qua HTTP header.
            @RequestHeader("X-Timestamp") String timestamp, // Timestamp giúp server reject request quá cũ để giảm replay attack.
            @RequestHeader("X-Signature") String signature, // Chữ ký HMAC do client tính và gửi lên.
            @RequestBody TransferRestRequest request // JSON body được Spring parse thành object Java.
    ) {
        log.info("[REST] Nhận request chuyển tiền: requestId={}, from={}, to={}, amount={}, currency={}",
                request.getRequestId(), request.getFromAccount(), request.getToAccount(), request.getAmount(), request.getCurrency());

        // Canonical payload là chuỗi chuẩn mà client và server cùng dùng để ký/verify.
        // Demo dạng ký: requestId:amount:currency, ví dụ REQ-001:500000:VND.
        String canonicalPayload = securityService.canonicalPayload(
                request.getRequestId(),
                request.getAmount().toPlainString(),
                request.getCurrency()
        );
        log.info("[REST] Canonical payload dùng để verify signature: {}", canonicalPayload);

        // Nếu client sai, timestamp quá hạn, hoặc signature sai thì service sẽ ném SecurityException.
        securityService.validate(clientId, timestamp, signature, canonicalPayload);
        log.info("[REST] Security hợp lệ, bắt đầu gọi business service");

        // Sau khi request hợp lệ, map REST DTO -> command nội bộ -> gọi service -> map kết quả về REST response.
        TransferRestResponse response = toResponse(transferService.transfer(toCommand(request)));
        log.info("[REST] Trả response: transactionId={}, status={}, fee={}",
                response.getTransactionId(), response.getStatus(), response.getFee());
        return response;
    }

    // Controller không đưa thẳng DTO vào service để service không bị phụ thuộc giao thức REST.
    private TransferCommand toCommand(TransferRestRequest request) {
        TransferCommand command = new TransferCommand();
        command.setRequestId(request.getRequestId());
        command.setFromAccount(request.getFromAccount());
        command.setToAccount(request.getToAccount());
        command.setAmount(request.getAmount());
        command.setCurrency(request.getCurrency());
        return command;
    }

    // TransferResult là kết quả nghiệp vụ chung; response này là hình dạng JSON riêng của REST API.
    private TransferRestResponse toResponse(TransferResult result) {
        TransferRestResponse response = new TransferRestResponse();
        response.setTransactionId(result.getTransactionId());
        response.setRequestId(result.getRequestId());
        response.setStatus(result.getStatus());
        response.setAmount(result.getAmount());
        response.setFee(result.getFee());
        response.setCurrency(result.getCurrency());
        response.setMessage(result.getMessage());
        return response;
    }
}
