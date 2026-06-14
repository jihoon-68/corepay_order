package org.example.corepayorderservice.order.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder().userId(1L).build();
    }

    @Test
    @DisplayName("주문 생성 시 초기 상태는 PENDING_STOCK")
    void initialState() {
        assertThat(order.getState()).isEqualTo(OrderState.PENDING_STOCK);
    }

    @Test
    @DisplayName("정상 흐름: PENDING → RESERVED → PAYMENT_REQUESTED → COMPLETED → REFUNDED")
    void happyPath() {
        order.reserveStock();
        assertThat(order.getState()).isEqualTo(OrderState.STOCK_RESERVED);

        order.requestPayment();
        assertThat(order.getState()).isEqualTo(OrderState.PAYMENT_REQUESTED);

        order.completePayment();
        assertThat(order.getState()).isEqualTo(OrderState.COMPLETED);

        order.refund();
        assertThat(order.getState()).isEqualTo(OrderState.REFUNDED);
    }

    @Test
    @DisplayName("재고 부족 흐름: PENDING → STOCK_FAILED")
    void stockFailPath() {
        order.failStock();
        assertThat(order.getState()).isEqualTo(OrderState.STOCK_FAILED);
    }

    @Test
    @DisplayName("결제 실패 흐름: RESERVED → PAYMENT_REQUESTED → CANCELLED")
    void paymentFailPath() {
        order.reserveStock();
        order.requestPayment();
        order.failPayment();
        assertThat(order.getState()).isEqualTo(OrderState.CANCELLED);
    }

    @Test
    @DisplayName("만료 흐름: RESERVED → EXPIRED")
    void expirePath() {
        order.reserveStock();
        order.expire();
        assertThat(order.getState()).isEqualTo(OrderState.EXPIRED);
    }

    @Test
    @DisplayName("잘못된 상태 전이 시 IllegalStateException 발생")
    void invalidStateTransition() {
        // PENDING_STOCK 상태에서 결제 요청 불가
        assertThatThrownBy(order::requestPayment)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("cancel()은 어떤 상태에서도 CANCELLED 전이")
    void cancelAnyState() {
        order.reserveStock();
        order.cancel();
        assertThat(order.getState()).isEqualTo(OrderState.CANCELLED);
    }

    @Test
    @DisplayName("라인아이템 추가 및 총액 업데이트")
    void addLineItemAndPrice() {
        order.addLineItem(OrderLineItem.builder()
                .productId(1L).price(5000).amount(2).build());
        order.updateOrderPrice(10000);

        assertThat(order.getOrderLineItems()).hasSize(1);
        assertThat(order.getOrderPrice()).isEqualTo(10000);
    }
}