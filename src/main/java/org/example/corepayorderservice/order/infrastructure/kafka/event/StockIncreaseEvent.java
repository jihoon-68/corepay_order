package org.example.corepayorderservice.order.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record StockIncreaseEvent(
        Long orderId,
        Long productId,
        Integer amount
) {
}
