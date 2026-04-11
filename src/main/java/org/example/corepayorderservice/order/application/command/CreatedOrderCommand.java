package org.example.corepayorderservice.order.application.command;

import lombok.Builder;

@Builder
public record CreatedOrderCommand(
        Long userId,
        Long productId,
        Integer amount
) {
}
