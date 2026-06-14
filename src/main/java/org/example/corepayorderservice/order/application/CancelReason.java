package org.example.corepayorderservice.order.application;

import lombok.Getter;

@Getter
public enum CancelReason {
    OUT_OF_STOCK,

    PRODUCT_NOT_FOUND,

    CUSTOMER_CANCEL,

    PAYMENT_FAILED,

    RESERVATION_EXPIRED;

}
