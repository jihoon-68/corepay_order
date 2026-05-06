package org.example.corepayorderservice.order.application.command;

import lombok.Builder;
import java.util.List;

@Builder
public record CreatedOrderCommand(
        Long userId,
        List<OrderItemCommand> items
) {
    public record OrderItemCommand(Long productId, Integer amount) {}
}