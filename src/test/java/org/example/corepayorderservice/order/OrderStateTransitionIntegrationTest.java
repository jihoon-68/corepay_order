package org.example.corepayorderservice.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.corepayorderservice.order.application.CancelReason;
import org.example.corepayorderservice.order.domain.Order;
import org.example.corepayorderservice.order.domain.OrderState;
import org.example.corepayorderservice.order.infrastructure.db.OrderRepository;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentFailedEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, ports = {9092})
public class OrderStateTransitionIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private Long orderId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        Order readyOrder = Order.builder()
                .userId(1L)
                .productId(100L)
                .orderPrice(50000)
                .amount(1)
                .build();
        // 초기 상태가 READY인지 명시적 세팅 (구현에 따라 다를 수 있음)
        orderRepository.save(readyOrder);
        orderId = readyOrder.getId();

        // 카프카 컨슈머 준비 대기 (결제 서버에서 배운 필수 로직)
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
    }

    @Test
    @DisplayName("결제 완료 이벤트를 수신하면, 해당 주문의 상태가 COMPLETED로 변경된다.")
    void consumePaymentCompletedEvent_UpdatesOrderState() throws Exception {

        // Given: 결제 서버가 보낼 가짜 이벤트 JSON 생성
        PaymentCompletedEvent event = PaymentCompletedEvent.builder().orderId(orderId).build();
        String message = objectMapper.writeValueAsString(event);

        // When: 테스트 코드가 결제 서버인 척 카프카에 완료 메시지를 발송합니다.
        kafkaTemplate.send("payment-completed-topic", message);

        // Then: DB를 업데이트할 때까지 Awaitility로 기다리며 검증합니다.
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(updatedOrder.getState()).isEqualTo(OrderState.COMPLETED);
        });
    }

    @Test
    @DisplayName("결제 실패 이벤트를 수신하면, 해당 주문의 상태가 CANCELED로 변경된다.")
    void consumePaymentCancelOrderEvent_UpdatesOrderState() throws Exception {

        // Given: 결제 서버가 보낼 가짜 이벤트 JSON 생성
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .orderId(orderId)
                .reason(CancelReason.PAYMENT_FAILED)
                .build();

        String message = objectMapper.writeValueAsString(event);

        // When: 테스트 코드가 결제 서버인 척 카프카에 완료 메시지를 발송합니다.
        kafkaTemplate.send("payment-failed-topic", message);

        // Then: DB를 업데이트할 때까지 Awaitility로 기다리며 검증합니다.
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(updatedOrder.getState()).isEqualTo(OrderState.CANCELED);
        });
    }

    @Test
    @DisplayName("결제 환불 이벤트를 수신하면, 해당 주문의 상태가 REFUNDED로 변경된다.")
    void consumePaymentRefundOrderEvent_UpdatesOrderState() throws Exception {

        // Given: 결제 서버가 보낼 가짜 이벤트 JSON 생성
        PaymentCancelEvent event = PaymentCancelEvent.builder().orderId(orderId).build();
        String message = objectMapper.writeValueAsString(event);

        // When: 테스트 코드가 결제 서버인 척 카프카에 완료 메시지를 발송합니다.
        kafkaTemplate.send("payment-refund-topic", message);

        // Then: DB를 업데이트할 때까지 Awaitility로 기다리며 검증합니다.
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(updatedOrder.getState()).isEqualTo(OrderState.REFUNDED);
        });
    }
}