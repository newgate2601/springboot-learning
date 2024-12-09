package com.example.learning.controller;

import com.example.learning.dto.VapidPublicKeyResponse;
import com.example.learning.service.PushNotificationService;
import lombok.AllArgsConstructor;
import nl.martijndwars.webpush.PushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import nl.martijndwars.webpush.Subscription;

@RestController
@RequestMapping("/api/webpush")
@CrossOrigin
public class WebPushController {
    private final PushNotificationService pushNotificationService;
    private final String publicKey;

    public WebPushController(PushNotificationService pushNotificationService,
                             @Value("${vapid.public.key}") String publicKey) {
        this.pushNotificationService = pushNotificationService;
        this.publicKey = publicKey;
    }

    @GetMapping("/vapid-public-key")
    public VapidPublicKeyResponse getVapidPublicKey() {
        return VapidPublicKeyResponse.builder()
                .vapidPublicKey(publicKey)
                .build();
    }

    @PostMapping("/subscribe")
    public Subscription subscribe(@RequestBody Subscription subcription) {
        pushNotificationService.subscribe(subcription);
        return subcription;
    }
}
