package com.example.learning.integration.wssec; // Package client WS-Security chuẩn.

import org.apache.wss4j.common.ext.WSPasswordCallback; // Callback object của WSS4J để hỏi password private key.

import javax.security.auth.callback.Callback; // Callback chuẩn Java.
import javax.security.auth.callback.CallbackHandler; // Interface xử lý callback.
import javax.security.auth.callback.UnsupportedCallbackException; // Exception khi callback không hỗ trợ.
import java.io.IOException; // Exception IO.

public class PartnerKeystorePasswordCallback implements CallbackHandler { // WSS4J gọi class này để lấy password private key.
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException { // Method xử lý danh sách callback.
        for (Callback callback : callbacks) { // Duyệt từng callback.
            if (callback instanceof WSPasswordCallback passwordCallback) { // Nếu là callback hỏi password của WSS4J.
                passwordCallback.setPassword("changeit"); // Trả password demo của private key alias partner.
                continue; // Xử lý callback tiếp theo.
            }
            throw new UnsupportedCallbackException(callback); // Callback lạ thì báo không hỗ trợ.
        }
    }
}
