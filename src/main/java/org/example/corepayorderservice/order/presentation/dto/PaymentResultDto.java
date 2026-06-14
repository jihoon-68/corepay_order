package org.example.corepayorderservice.order.presentation.dto;

public record PaymentResultDto(
        Long orderId,
        boolean success,
        String failReason
) {
    public static PaymentResultDto success(Long orderId) {
        return new PaymentResultDto(orderId, true, null);
    }

    public static PaymentResultDto fail(Long orderId, String failReason) {
        return new PaymentResultDto(orderId, false, failReason);
    }
}