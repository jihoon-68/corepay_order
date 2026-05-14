package org.example.corepayorderservice.order.presentation.dto;

import java.util.List;

public record OrderCreatReq(
        List<OrderItemReq> items // 다건 상품 리스트
) {
    // 내부 record로 상품 정보 정의
    public record OrderItemReq(Long productId, Integer amount) {}
}