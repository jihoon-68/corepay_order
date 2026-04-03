package org.example.corepayorderservice.order.infrastructure.kafka.event;

public record PaymentCompletedEvent(
        Long orderId,
        String status
) {
}
