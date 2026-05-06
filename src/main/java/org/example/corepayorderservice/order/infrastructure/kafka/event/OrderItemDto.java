package org.example.corepayorderservice.order.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record OrderItemDto(
        Long productId,
        Integer amount
) {
    public static OrderItemDto from(Long productId, Integer amount){
        return OrderItemDto.builder().productId(productId).amount(amount).build();
    }
}