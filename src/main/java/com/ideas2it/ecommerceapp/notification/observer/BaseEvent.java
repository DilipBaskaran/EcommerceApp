package com.ideas2it.ecommerceapp.notification.observer;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * Base class for all system events.
 */
@Getter
public abstract class BaseEvent {
    private final String eventId;
    private final LocalDateTime timestamp;
    private final String eventType;

    public BaseEvent(String eventType) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.eventType = eventType;
    }

    /**
     * Gets a description of the event.
     *
     * @return A human-readable description of the event
     */
    public abstract String getDescription();
}
