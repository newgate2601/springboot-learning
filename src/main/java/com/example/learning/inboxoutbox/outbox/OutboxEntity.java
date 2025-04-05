package com.example.learning.inboxoutbox.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_outbox")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboxEntity {
  @Id
  private String id; // unique message id for idempotent handle

  private String aggregateId; // order id, return order id

  private String messageType; // create order, return order

  private String payload;

  private String status; // PENDING or PROCESSED 
  
  private String destination;

  private LocalDateTime createdAt;
}
