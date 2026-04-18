package org.example.corepayorderservice.order.infrastructure.kafka.event;

import lombok.Builder;
import org.example.corepayorderservice.order.application.CancelReason;

@Builder
public record PaymentFailedEvent(
        Long orderId,
        CancelReason reason
) {
}
