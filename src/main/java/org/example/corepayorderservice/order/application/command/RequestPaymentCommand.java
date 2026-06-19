package org.example.corepayorderservice.order.application.command;

import lombok.Builder;

@Builder
public record RequestPaymentCommand(
        Long OrderId,
        Long userId
) {
}
