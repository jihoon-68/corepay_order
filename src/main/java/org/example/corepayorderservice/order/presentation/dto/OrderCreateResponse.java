package org.example.corepayorderservice.order.presentation.dto;

import java.util.List;

public record OrderCreateResponse(
        Long orderId,
        String status,              // "STOCK_RESERVED" or "STOCK_FAILED"
        List<Long> outOfStockItems  // 품절 상품 ID 목록 (선점 성공 시 빈 리스트)
) {
    public static OrderCreateResponse reserved(Long orderId) {
        return new OrderCreateResponse(orderId, "STOCK_RESERVED", List.of());
    }

    public static OrderCreateResponse failed(Long orderId, List<Long> outOfStockItems) {
        return new OrderCreateResponse(orderId, "STOCK_FAILED", outOfStockItems);
    }
}