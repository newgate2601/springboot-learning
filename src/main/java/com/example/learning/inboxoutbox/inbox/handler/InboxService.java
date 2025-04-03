package com.example.learning.inboxoutbox.inbox.handler;

public interface InboxService {
  void persistInbox(String aggregateId, String messageType, Object payload);
}
