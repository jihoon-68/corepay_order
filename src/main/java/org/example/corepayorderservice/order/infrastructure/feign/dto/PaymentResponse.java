package org.example.corepayorderservice.order.infrastructure.feign.dto;

public record PaymentResponse(
        boolean isSuccess,
        String failReason
) {
}
