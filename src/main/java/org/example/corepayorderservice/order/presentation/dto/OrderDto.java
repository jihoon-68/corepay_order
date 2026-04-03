package org.example.corepayorderservice.order.presentation.dto;

import lombok.Builder;
import org.example.corepayorderservice.order.domain.Order;
import org.example.corepayorderservice.order.domain.OrderState;

import java.time.LocalDateTime;

@Builder
public record OrderDto(
        Long id,
        Long userId,
        Long productId,
        Integer orderPrice,
        Integer amount,
        OrderState state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderDto from(Order order){
        return OrderDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .productId(order.getProductId())
                .orderPrice(order.getOrderPrice())
                .amount(order.getAmount())
                .state(order.getState())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
