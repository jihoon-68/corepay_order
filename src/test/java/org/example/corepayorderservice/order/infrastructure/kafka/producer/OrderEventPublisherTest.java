package org.example.corepayorderservice.order.infrastructure.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.corepayorderservice.order.application.CancelReason;
import org.example.corepayorderservice.order.infrastructure.kafka.OrderEventProducer;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderCancelledEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderItemDto;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.StockConfirmEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @InjectMocks
    private OrderEventProducer producer;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ObjectMapper는 실제 구현체 주입
    @org.junit.jupiter.api.BeforeEach
    void injectMapper() throws Exception {
        var field = OrderEventProducer.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(producer, objectMapper);
    }

    @SuppressWarnings("unchecked")
    private Message<String> captureMessage() {
        ArgumentCaptor<Message<String>> captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(captor.capture());
        return captor.getValue();
    }

    // ── sendPaymentCancel ─────────────────────────────────

    @Test
    @DisplayName("결제 취소 이벤트 → payment-cancel-topic 발행")
    void sendPaymentCancel_topic() {
        PaymentCancelEvent event = PaymentCancelEvent.builder()
                .orderId(1L).reason(CancelReason.CUSTOMER_CANCEL).build();

        producer.sendPaymentCancel(event);

        Message<String> msg = captureMessage();
        assertThat(msg.getHeaders().get("kafka_topic")).isEqualTo("payment-cancel-topic");
    }

    @Test
    @DisplayName("결제 취소 이벤트 페이로드에 orderId 포함")
    void sendPaymentCancel_payload() throws Exception {
        PaymentCancelEvent event = PaymentCancelEvent.builder()
                .orderId(1L).reason(CancelReason.CUSTOMER_CANCEL).build();

        producer.sendPaymentCancel(event);

        String payload = (String) captureMessage().getPayload();
        assertThat(payload).contains("\"orderId\":1");
    }

    // ── sendStockConfirm ──────────────────────────────────

    @Test
    @DisplayName("재고 확정 이벤트 → stock-confirm-topic 발행")
    void sendStockConfirm_topic() {
        StockConfirmEvent event = StockConfirmEvent.builder()
                .orderId(2L)
                .items(List.of(OrderItemDto.from(10L, 3)))
                .build();

        producer.sendStockConfirm(event);

        Message<String> msg = captureMessage();
        assertThat(msg.getHeaders().get("kafka_topic")).isEqualTo("stock-confirm-topic");
    }

    @Test
    @DisplayName("재고 확정 이벤트 페이로드에 items 포함")
    void sendStockConfirm_payload() throws Exception {
        StockConfirmEvent event = StockConfirmEvent.builder()
                .orderId(2L)
                .items(List.of(OrderItemDto.from(10L, 3)))
                .build();

        producer.sendStockConfirm(event);

        String payload = (String) captureMessage().getPayload();
        assertThat(payload).contains("\"orderId\":2");
        assertThat(payload).contains("\"productId\":10");
    }

    // ── sendOrderCancelled ────────────────────────────────

    @Test
    @DisplayName("주문 취소 이벤트 → order-cancelled-topic 발행")
    void sendOrderCancelled_topic() {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(3L)
                .items(List.of(OrderItemDto.from(20L, 1)))
                .build();

        producer.sendOrderCancelled(event);

        Message<String> msg = captureMessage();
        assertThat(msg.getHeaders().get("kafka_topic")).isEqualTo("order-cancelled-topic");
    }

    @Test
    @DisplayName("주문 취소 이벤트 페이로드에 orderId 포함")
    void sendOrderCancelled_payload() {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(3L)
                .items(List.of(OrderItemDto.from(20L, 1)))
                .build();

        producer.sendOrderCancelled(event);

        String payload = (String) captureMessage().getPayload();
        assertThat(payload).contains("\"orderId\":3");
    }

    // ── MDC traceId 헤더 ──────────────────────────────────

    @Test
    @DisplayName("MDC traceId 존재 시 X-Trace-Id 헤더에 포함")
    void traceId_fromMdc() {
        MDC.put("traceId", "test-trace-123");
        try {
            producer.sendPaymentCancel(PaymentCancelEvent.builder()
                    .orderId(1L).reason(CancelReason.CUSTOMER_CANCEL).build());

            Message<String> msg = captureMessage();
            assertThat(msg.getHeaders().get("X-Trace-Id")).isEqualTo("test-trace-123");
        } finally {
            MDC.clear();
        }
    }

    @Test
    @DisplayName("MDC traceId 없을 시 X-Trace-Id 헤더에 UNKNOWN-TRACE 설정")
    void traceId_unknown() {
        MDC.clear();
        producer.sendPaymentCancel(PaymentCancelEvent.builder()
                .orderId(1L).reason(CancelReason.CUSTOMER_CANCEL).build());

        Message<String> msg = captureMessage();
        assertThat(msg.getHeaders().get("X-Trace-Id")).isEqualTo("UNKNOWN-TRACE");
    }
}