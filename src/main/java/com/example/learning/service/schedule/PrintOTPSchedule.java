//package com.example.learning.service.schedule;
//
//import com.warrenstrange.googleauth.GoogleAuthenticator;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//public class PrintOTPSchedule { // TOTP
//    private final String SEED = "HLPI5NZM4E5HY5R5";
//    @Scheduled(fixedRate = 5000)
//    public void printMessage() {
//        GoogleAuthenticator gAuth = new GoogleAuthenticator();
//        int otp = gAuth.getTotpPassword(SEED);
//        System.out.println("key: " + SEED + " with OTP: " + otp);
//    }
//}
