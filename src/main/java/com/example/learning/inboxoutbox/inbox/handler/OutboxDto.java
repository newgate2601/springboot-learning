package com.example.learning.inboxoutbox.inbox.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OutboxDto {
    private String id; // unique message id for idempotent handle

    private String aggregateId; // order id, return order id

    private String messageType; // create order, return order

    private String payload;

    private String status; // PENDING or PROCESSED

    private String destination;

    private LocalDateTime createdAt;
}
