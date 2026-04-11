package org.example.corepayorderservice.order.application.command;

import lombok.Builder;

@Builder
public record RefundOrderCommand(
        Long id
) {
}
