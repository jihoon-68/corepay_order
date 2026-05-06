package org.example.corepayorderservice.order.infrastructure.kafka.event;

import lombok.Builder;

import java.util.List;

@Builder
public record StockIncreaseEvent(
        Long orderId,
        List<OrderItemDto> items
) {
}
