package org.example.corepayorderservice.order.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.application.OrderService;
import org.example.corepayorderservice.order.application.command.CancelOrderCommand;
import org.example.corepayorderservice.order.application.command.CompleteOrderCommand;
import org.example.corepayorderservice.order.application.command.RefundOrderCommand;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentFailedEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCompletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderResultConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    // [보상 트랜잭션] 실패 이벤트 수신
    @KafkaListener(topics = "payment-failed-topic", groupId = "order-group")
    public void consumeOrderCancel(String message) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
            CancelOrderCommand command = CancelOrderCommand.builder()
                    .id(event.orderId())
                    .reason(event.reason())
                    .build();

            orderService.cancelOrder(command);
        } catch (JsonProcessingException e) {
            log.error("보상 트랜잭션 메시지 파싱 에러", e);
        }
    }

    // [결제 완료] 최종 성공 이벤트 수신
    @KafkaListener(topics = "payment-completed-topic", groupId = "order-group")
    public void consumeOrderComplete(String message) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
            CompleteOrderCommand command = CompleteOrderCommand.builder().id(event.orderId()).build();
            orderService.completeOrder(command);
        } catch (JsonProcessingException e) {
            log.error("성공 메시지 파싱 에러", e);
        }
    }

    // [환불 완료] 환불(취소) 이벤트 수신
    @KafkaListener(topics = "payment-cancel-topic", groupId = "order-group")
    public void consumeOrderRefund(String message) {
        try {
            PaymentCancelEvent event = objectMapper.readValue(message, PaymentCancelEvent.class);
            RefundOrderCommand command = RefundOrderCommand.builder().id(event.orderId()).build();
            orderService.refundOrder(command);
        } catch (JsonProcessingException e) {
            log.error("환불 메시지 파싱 에러", e);
        }
    }
}