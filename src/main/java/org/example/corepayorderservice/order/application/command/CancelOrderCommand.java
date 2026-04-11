package org.example.corepayorderservice.order.application.command;

import lombok.Builder;

@Builder
public record CancelOrderCommand(
        Long id,
        String reason
) {
}
