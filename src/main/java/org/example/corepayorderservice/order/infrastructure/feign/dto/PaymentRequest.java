package org.example.corepayorderservice.order.infrastructure.feign.dto;

public record PaymentRequest(
        Long orderId,
        Long userId,
        int totalPrice
) {}
