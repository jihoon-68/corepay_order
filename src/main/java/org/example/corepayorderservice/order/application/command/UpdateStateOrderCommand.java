package org.example.corepayorderservice.order.application.command;

import lombok.Builder;
import org.example.corepayorderservice.order.domain.OrderState;
@Builder
public record UpdateStateOrderCommand(
        Long id,
        OrderState state
) {
}
