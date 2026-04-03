package org.example.corepayorderservice.order.presentation.dto;

import org.example.corepayorderservice.order.domain.OrderState;

public record OrderUpdateStateReq(
        Long id,
        OrderState state
) {
}
