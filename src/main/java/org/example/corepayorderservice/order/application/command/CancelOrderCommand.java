package org.example.corepayorderservice.order.application.command;

import lombok.Builder;
import org.example.corepayorderservice.order.application.CancelReason;

@Builder
public record CancelOrderCommand(
        Long id,
        Long userId,
        CancelReason reason
) {
}
