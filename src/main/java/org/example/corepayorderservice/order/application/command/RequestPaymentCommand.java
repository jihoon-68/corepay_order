package org.example.corepayorderservice.order.application.command;

import lombok.Builder;

@Builder
public record RequestPaymentCommand(
        Long orderId,
        Long userId
) {
}
