package org.example.corepayorderservice.order.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        Long productId,
        Integer totalPrice,
        Integer amount
) {
}
