package br.com.assine.subscriptions.domain.event;

import java.time.LocalDateTime;

public interface DomainEvent {
    String getEventType();
    LocalDateTime getCreatedAt();
}
