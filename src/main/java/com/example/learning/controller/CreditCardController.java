package com.example.learning.controller;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.entity.CreditCardEntity;
import com.example.learning.service.CreditCardService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/credit-card")
@AllArgsConstructor
@CrossOrigin
public class CreditCardController {
    private final CreditCardService creditCardService;

    @PostMapping
    public IdNameResponse createDefaultCreditCard() {
        return creditCardService.createDefaultCreditCard();
    }

    @GetMapping
    public CreditCardEntity getCreditCardById(@RequestParam("id") Long id) {
        return creditCardService.getCreditCard(id);
    }

    @PutMapping
    public void testMultiThreadIncreaseMoney(@RequestParam Long creditCardId) {
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> creditCardService.increaseMoneyWithCreditCard(creditCardId, 1)),
                CompletableFuture.runAsync(() -> creditCardService.increaseMoneyWithCreditCard(creditCardId, 2)),
                CompletableFuture.runAsync(() -> creditCardService.increaseMoneyWithCreditCard(creditCardId, 3)),
                CompletableFuture.runAsync(() -> creditCardService.increaseMoneyWithCreditCard(creditCardId, 4)),
                CompletableFuture.runAsync(() -> creditCardService.increaseMoneyWithCreditCard(creditCardId, 5)),
                CompletableFuture.runAsync(() -> creditCardService.increaseMoneyWithCreditCard(creditCardId, 6)),
                CompletableFuture.runAsync(() -> creditCardService.increaseMoneyWithCreditCard(creditCardId, 7)),
                CompletableFuture.runAsync(() -> creditCardService.increaseMoneyWithCreditCard(creditCardId, 8)),
                CompletableFuture.runAsync(() -> creditCardService.increaseMoneyWithCreditCard(creditCardId, 9))
        );
    }
}
