package com.example.learning.service;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.entity.CreditCardEntity;
import com.example.learning.redis.redisson.RedissonLockService;
import com.example.learning.repository.CreditCardRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@AllArgsConstructor
public class CreditCardService {
    private final CreditCardRepository creditCardRepository;
    private final RedissonLockService redissonLockService;
    private final RedissonClient redissonClient;

    private static final Integer INCREASE_MONEY = 1;

    @Transactional
    public IdNameResponse createDefaultCreditCard() {
        CreditCardEntity creditCardEntity = creditCardRepository.save(
                CreditCardEntity.builder()
                        .money(0)
                        .build()
        );
        return IdNameResponse.builder()
                .id(creditCardEntity.getId())
                .name(creditCardEntity.getMoney().toString())
                .build();
    }

    @Transactional
    public void increaseMoneyWithCreditCard(Long creditCardId, Integer threadId) {
        RLock lock = redissonClient.getLock(creditCardId.toString());
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(10, 60, TimeUnit.SECONDS);
            if (isLocked) {
                log.error("Get lock of thread {}", threadId);
                CreditCardEntity creditCard = getCreditCard(creditCardId);
                creditCard.setMoney(creditCard.getMoney() + INCREASE_MONEY);
                log.error("Money of credit card with id = " + creditCardId + ": after add 1: " + creditCard.getMoney());
                creditCardRepository.save(creditCard);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.error("Release lock of thread {}", threadId);
                        lock.unlock();
                    }
                });
            }
        } catch (InterruptedException e) {
            log.error("Interrupted thread {}", threadId);
            Thread.currentThread().interrupt();
        }
    }

//    @Transactional
//    public void increaseMoneyWithCreditCard(Long creditCardId, Integer threadId) {
//        RLock lock = redissonClient.getLock(creditCardId.toString());
//        boolean isLocked = false;
//        try {
//            isLocked = lock.tryLock(10, 60, TimeUnit.SECONDS);
//            if (isLocked) {
//                log.error("Get lock of thread {}", threadId);
//                CreditCardEntity creditCard = getCreditCard(creditCardId);
//                creditCard.setMoney(creditCard.getMoney() + INCREASE_MONEY);
//                log.error("Money of credit card with id = " + creditCardId + ": after add 1: " + creditCard.getMoney());
//                creditCardRepository.save(creditCard);
//            }
//        } catch (InterruptedException e) {
//            log.error("Interrupted thread {}", threadId);
//            Thread.currentThread().interrupt();
//        }
//        finally {
//            if (isLocked) {
//                log.error("Release lock of thread {}", threadId);
//                lock.unlock();
//            }
//        }
//    }

//    @Transactional
//    public void increaseMoneyWithCreditCard(Long creditCardId, Integer threadId) {
//        boolean locked = false;
//        try {
//            locked = redissonLockService.tryLock(creditCardId.toString(), threadId);
//            if (locked) {
//                CreditCardEntity creditCard = getCreditCard(creditCardId);
//                creditCard.setMoney(creditCard.getMoney() + INCREASE_MONEY);
//                log.error("Money of credit card with id = " + creditCardId + ": after add 1: " + creditCard.getMoney());
//                creditCardRepository.save(creditCard);
//            }
//        } finally {
//            redissonLockService.unlock(creditCardId.toString(), threadId);
//        }
//    }

    public CreditCardEntity getCreditCard(Long creditCardId) {
        return creditCardRepository.findById(creditCardId).orElse(null);
    }
}
