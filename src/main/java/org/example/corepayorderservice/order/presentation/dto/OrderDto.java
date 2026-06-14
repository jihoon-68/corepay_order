package org.example.corepayorderservice.order.presentation.dto;

import lombok.Builder;
import org.example.corepayorderservice.order.domain.Order;
import org.example.corepayorderservice.order.domain.OrderState;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderItemDto;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderDto(
        Long id,
        Long userId,
        Integer orderPrice,
        String state,
        List<OrderItemDto> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderDto from(Order order){
        List<OrderItemDto> eventItems = order.getOrderLineItems().stream()
                .map(item -> OrderItemDto.from(item.getProductId(), item.getAmount()))
                .toList();

        return OrderDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderPrice(order.getOrderPrice())
                .state(order.getState().name())
                .items(eventItems)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
