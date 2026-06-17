package org.example.corepayorderservice.order.domain;


import lombok.Getter;

@Getter
public enum OrderState {
    PENDING_STOCK,
    STOCK_RESERVED,
    STOCK_FAILED,
    PAYMENT_REQUESTED,
    COMPLETED,
    CANCELLED,
    REFUNDED,
    REFUND_FAILED,
    EXPIRED
}
