package com.example.learning.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service // Đăng ký bean để controller/endpoint có thể inject.
public class PartnerSecurityService {
    private static final Logger log = LoggerFactory.getLogger(PartnerSecurityService.class); // Logger để theo dõi bước verify security.

    private static final String SHARED_SECRET = "demo-partner-secret"; // Secret demo dùng chung giữa server và partner.
    private static final Duration TIMESTAMP_WINDOW = Duration.ofDays(365L * 100); // Demo cho newbie: request hợp lệ nếu timestamp lệch tối đa 100 năm.

    // Validate request security cho cả REST và SOAP.
    public void validate(String clientId, String timestamp, String signature, String canonicalPayload) {
        log.info("[SECURITY] Bắt đầu verify request: clientId={}, timestamp={}, canonicalPayload={}",
                clientId, timestamp, canonicalPayload);

        // Demo chỉ chấp nhận một partner. Thực tế sẽ lookup client trong DB/config.
        if (!"partner-mobile".equals(clientId)) {
            log.warn("[SECURITY] Reject request vì clientId không hợp lệ: {}", clientId);
            throw new SecurityException("Unknown partner client");
        }
        log.info("[SECURITY] Client hợp lệ");

        Instant requestTime = Instant.parse(timestamp); // Timestamp phải theo ISO-8601, ví dụ 2026-05-09T03:10:00Z.
        Duration age = Duration.between(requestTime, Instant.now()).abs(); // Tính độ lệch giữa giờ request và giờ server.
        log.info("[SECURITY] Độ lệch timestamp là {} giây, giới hạn demo là {} ngày",
                age.toSeconds(), TIMESTAMP_WINDOW.toDays());
        if (age.compareTo(TIMESTAMP_WINDOW) > 0) {
            log.warn("[SECURITY] Reject request vì timestamp vượt quá giới hạn 100 năm");
            throw new SecurityException("Expired partner request. Demo timestamp window is 100 years.");
        }

        // Server tự tính lại chữ ký từ các thành phần đã thống nhất với client.
        String expected = hmac(clientId + ":" + timestamp + ":" + canonicalPayload);
        if (!constantTimeEquals(expected, signature)) {
            log.warn("[SECURITY] Reject request vì signature sai. expected={}, actual={}", expected, signature);
            throw new SecurityException("Invalid partner signature");
        }
        log.info("[SECURITY] Signature hợp lệ");
    }

    // Chuỗi này cần ổn định tuyệt đối giữa client và server; đổi format là signature sẽ sai.
    public String canonicalPayload(String requestId, String amount, String currency) {
        return requestId + ":" + amount + ":" + currency;
    }

    // Hàm này đại diện cho phía partner: dùng cùng công thức để tạo signature gửi lên server SOAP/REST.
    public String sign(String clientId, String timestamp, String canonicalPayload) {
        return hmac(clientId + ":" + timestamp + ":" + canonicalPayload);
    }

    // Tạo chữ ký HMAC SHA-256 và encode Base64 để gửi được qua HTTP/SOAP text.
    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); // Chọn thuật toán ký.
            mac.init(new SecretKeySpec(SHARED_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")); // Nạp secret key.
            return Base64.getEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))); // Ký input và encode Base64.
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot calculate partner signature", ex);
        }
    }

    // So sánh constant-time để giảm việc lộ thông tin qua thời gian so sánh signature.
    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8); // Chuyển chữ ký server tính được sang byte.
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8); // Chuyển chữ ký client gửi sang byte.
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int result = 0; // Nếu có bất kỳ byte nào khác, result sẽ khác 0.
        for (int i = 0; i < leftBytes.length; i++) {
            result |= leftBytes[i] ^ rightBytes[i]; // XOR từng byte nhưng không return sớm.
        }
        return result == 0;
    }
}
