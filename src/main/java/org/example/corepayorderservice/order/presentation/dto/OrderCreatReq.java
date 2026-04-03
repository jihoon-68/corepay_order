package org.example.corepayorderservice.order.presentation.dto;

public record OrderCreatReq(
        Long userId,
        Long productId,
        Integer amount
) {
}
