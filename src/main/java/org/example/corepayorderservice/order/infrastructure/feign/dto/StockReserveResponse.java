package org.example.corepayorderservice.order.infrastructure.feign.dto;

import java.util.List;

public record StockReserveResponse(
        boolean success,
        List<Long> outOfStockProductIds  // 선점 성공 시 빈 리스트
) {}
