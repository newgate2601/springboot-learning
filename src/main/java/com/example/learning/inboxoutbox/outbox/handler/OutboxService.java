package com.example.learning.inboxoutbox.outbox.handler;

public interface OutboxService {
  void persistOutbox(String aggregateId, String messageType, Object payload);
}
