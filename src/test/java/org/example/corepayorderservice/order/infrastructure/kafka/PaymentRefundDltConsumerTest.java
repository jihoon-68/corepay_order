package org.example.corepayorderservice.order.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.corepayorderservice.order.domain.Order;
import org.example.corepayorderservice.order.domain.OrderState;
import org.example.corepayorderservice.order.infrastructure.db.OrderRepository;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentRefundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRefundDltConsumerTest {

    @InjectMocks private PaymentRefundDltConsumer dltConsumer;
    @Mock private OrderRepository orderRepository;
    @Mock private ObjectMapper objectMapper;

    // 정상 처리
    @Test
    @DisplayName("DLQ 수신 — 주문 존재: REFUND_FAILED 상태로 강제 마킹")
    void consumeDlt_orderExists_markedAsRefundFailed() throws Exception {
        PaymentRefundEvent event = PaymentRefundEvent.builder()
                .orderId(1L)
                .build();

        given(objectMapper.readValue(anyString(), eq(PaymentRefundEvent.class)))
                .willReturn(event);

        Order order = buildCompletedOrder(1L);
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(orderRepository.save(any(Order.class))).willReturn(order);

        dltConsumer.consumePaymentRefundDlt("{}");

        assertThat(order.getState()).isEqualTo(OrderState.REFUND_FAILED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("DLQ 수신 — 주문 없음: save 미호출, 예외 없음")
    void consumeDlt_orderNotFound_noSaveNoException() throws Exception {
        PaymentRefundEvent event = PaymentRefundEvent.builder()
                .orderId(999L)
                .build();

        given(objectMapper.readValue(anyString(), eq(PaymentRefundEvent.class)))
                .willReturn(event);
        given(orderRepository.findById(999L)).willReturn(Optional.empty());

        dltConsumer.consumePaymentRefundDlt("{}");

        verify(orderRepository, never()).save(any());
    }

    // 예외 방어
    @Test
    @DisplayName("DLQ 수신 — 역직렬화 실패: 예외 삼킴, 무한루프 방지")
    void consumeDlt_deserializationFails_noException() throws Exception {
        given(objectMapper.readValue(anyString(), eq(PaymentRefundEvent.class)))
                .willThrow(new RuntimeException("역직렬화 실패"));

        dltConsumer.consumePaymentRefundDlt("invalid-json");

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("DLQ 수신 — save 중 예외 발생: 예외 삼킴, 무한루프 방지")
    void consumeDlt_saveFails_noException() throws Exception {
        PaymentRefundEvent event = PaymentRefundEvent.builder()
                .orderId(1L)
                .build();

        given(objectMapper.readValue(anyString(), eq(PaymentRefundEvent.class)))
                .willReturn(event);

        Order order = buildCompletedOrder(1L);
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(orderRepository.save(any())).willThrow(new RuntimeException("DB 오류"));

        // 예외가 밖으로 나오면 안 됨
        dltConsumer.consumePaymentRefundDlt("{}");
    }

    // 헬퍼
    private Order buildCompletedOrder(Long orderId) {
        Order order = Order.builder().userId(10L).build();
        ReflectionTestUtils.setField(order, "id", orderId);

        // COMPLETED 상태로 세팅 (refund() 호출 가능한 상태)
        ReflectionTestUtils.setField(order, "state", OrderState.COMPLETED);
        return order;
    }
}