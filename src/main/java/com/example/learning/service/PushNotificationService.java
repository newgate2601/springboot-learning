package com.example.learning.service;

import jakarta.annotation.PostConstruct;
import lombok.Value;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

// https://chatgpt.com/c/674ec9a2-1030-8010-8ed3-24d9f5c3d6ac
// https://chatgpt.com/c/674ec10f-f870-8010-870f-7106b6bd4e5c
@Service
public class PushNotificationService {
    @Value("${vapid.public.key}")
    private String publicKey;
    private String privateKey;
    private String subject;
    private PushService pushService;
    private final Map<String, Subscription> endpointToSubscription = new HashMap<>();

    @PostConstruct
    private void init() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        this.publicKey =
        pushService = new PushService(publicKey, privateKey, subject);
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void sendNotification(Subscription subscription, String messageJson) {
        try {
            pushService.send(new Notification(subscription, messageJson));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JoseException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void subscribe(Subscription subscription) {
        System.out.println("Subscribed to " + subscription.endpoint);
        endpointToSubscription.put(subscription.endpoint, subscription);
    }

    public void unsubscribe(Subscription subscription) {
        System.out.println("Unsubscribed " + subscription.endpoint + " auth:" + subscription.keys.auth);
        endpointToSubscription.remove(subscription.endpoint);
    }

    @Scheduled(fixedRate = 5000) // Send notifications every 5 seconds
    public void sendScheduledNotifications() {
        String messageJson = "{\"title\": \"Scheduled Notification\", \"body\": \"This is a scheduled message.\"}";
        for (Subscription subscription : endpointToSubscription.values()) {
            try {
                sendNotification(subscription, messageJson);
                System.out.println("Notification sent to " + subscription.endpoint);
            } catch (Exception e) {
                System.err.println("Failed to send notification to " + subscription.endpoint + ": " + e.getMessage());
            }
        }
    }
}
