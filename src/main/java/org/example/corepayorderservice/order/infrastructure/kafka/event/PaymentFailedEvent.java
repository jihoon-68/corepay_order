package org.example.corepayorderservice.order.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record PaymentFailedEvent(
        Long orderId,
        String reason
) {
}
