package org.example.corepayorderservice.order.infrastructure.kafka.event;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderCancelledEvent (
        Long orderId,
        List<OrderItemDto> items,
        Boolean paymentConfirmed
){
    public static OrderCancelledEvent ofConfirmed(Long orderId, List<OrderItemDto> items) {
        return OrderCancelledEvent.builder()
                .orderId(orderId)
                .items(items)
                .paymentConfirmed(true)
                .build();
    }

    // 결제 실패 / 선점만 된 취소 → Redis만 복구
    public static OrderCancelledEvent ofFailed(Long orderId, List<OrderItemDto> items) {
        return OrderCancelledEvent.builder()
                .orderId(orderId)
                .items(items)
                .paymentConfirmed(false)
                .build();
    }
}
