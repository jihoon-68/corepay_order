package org.example.corepayorderservice.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.corepayorderservice.order.application.CancelReason;
import org.example.corepayorderservice.order.domain.Order;
import org.example.corepayorderservice.order.domain.OrderLineItem;
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

        // 1. 주문 마스터 생성
        Order readyOrder = Order.builder()
                .userId(1L)
                .build();
        readyOrder.updateOrderPrice(50000);

        // 2. 주문 상세 생성
        OrderLineItem item = OrderLineItem.builder()
                .productId(100L)
                .price(50000)
                .amount(1)
                .build();

        // 3. 연관관계 맵핑 (Cascade.ALL에 의해 함께 저장됨)
        readyOrder.addLineItem(item);

        orderRepository.save(readyOrder);
        orderId = readyOrder.getId();

        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
    }

    @Test
    @DisplayName("결제 완료 이벤트를 수신하면, 해당 주문의 상태가 COMPLETED로 변경된다.")
    void consumePaymentCompletedEvent_UpdatesOrderState() throws Exception {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder().orderId(orderId).build();
        String message = objectMapper.writeValueAsString(event);

        kafkaTemplate.send("payment-completed-topic", message);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(updatedOrder.getState()).isEqualTo(OrderState.COMPLETED);
        });
    }

    @Test
    @DisplayName("결제 실패 이벤트를 수신하면, 해당 주문의 상태가 CANCELED로 변경된다.")
    void consumePaymentCancelOrderEvent_UpdatesOrderState() throws Exception {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .orderId(orderId)
                .reason(CancelReason.PAYMENT_FAILED)
                .build();
        String message = objectMapper.writeValueAsString(event);

        kafkaTemplate.send("payment-failed-topic", message);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(updatedOrder.getState()).isEqualTo(OrderState.CANCELED);
        });
    }

    @Test
    @DisplayName("결제 환불 이벤트를 수신하면, 해당 주문의 상태가 REFUNDED로 변경된다.")
    void consumePaymentRefundOrderEvent_UpdatesOrderState() throws Exception {
        PaymentCancelEvent event = PaymentCancelEvent.builder().orderId(orderId).build();
        String message = objectMapper.writeValueAsString(event);

        kafkaTemplate.send("payment-refund-topic", message);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(updatedOrder.getState()).isEqualTo(OrderState.REFUNDED);
        });
    }
}