package org.example.corepayorderservice.order.infrastructure.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.corepayorderservice.order.application.CancelReason;
import org.example.corepayorderservice.order.application.OrderService;
import org.example.corepayorderservice.order.application.command.CancelOrderCommand;
import org.example.corepayorderservice.order.application.command.CompleteOrderCommand;
import org.example.corepayorderservice.order.application.command.RefundOrderCommand;
import org.example.corepayorderservice.order.infrastructure.kafka.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        ports = {9092},
        topics = {
                "payment-failed-topic",
                "payment-completed-topic",
                "payment-refund-topic",
                "order-cancel-topic"
        }
)
class OrderResultConsumerTest {

    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired ObjectMapper objectMapper;
    @MockBean  OrderService orderService;

    // ── payment-refund-topic ──────────────────────────────────────

    @Nested
    @DisplayName("payment-refund-topic 수신")
    class PaymentRefund {

        @Test
        @DisplayName("환불 완료 수신 시 refundOrder가 올바른 orderId로 호출된다")
        void consumePaymentRefund_callsRefundOrder() throws Exception {
            PaymentRefundEvent event = PaymentRefundEvent.builder()
                    .orderId(4L)
                    .build();

            kafkaTemplate.send("payment-refund-topic", objectMapper.writeValueAsString(event));

            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                ArgumentCaptor<RefundOrderCommand> captor =
                        ArgumentCaptor.forClass(RefundOrderCommand.class);
                then(orderService).should(atLeastOnce()).refundOrder(captor.capture());
                assertThat(captor.getValue().id()).isEqualTo(4L);
            });
        }
    }


}