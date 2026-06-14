package org.example.corepayorderservice.order.infrastructure.feign.dto;

import java.util.List;

public record StockReserveRequest(
        Long orderId,
        List<Item> items
) {
    public record Item(Long productId, int amount) {}
}
