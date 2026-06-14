package org.example.corepayorderservice.order.domain;


public enum OrderState {
    PENDING_STOCK,
    STOCK_RESERVED,
    STOCK_FAILED,
    PAYMENT_REQUESTED,
    COMPLETED,
    CANCELLED,
    REFUNDED,
    EXPIRED
}
