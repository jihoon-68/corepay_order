package org.example.corepayorderservice.order.infrastructure.kafka;

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
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentRefundEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.example.corepaycommon.log.KafkaMdcHelper;


@Slf4j
@Component
@RequiredArgsConstructor
public class OrderResultConsumer {

    private final OrderService orderService;
    private final KafkaMdcHelper kafkaMdcHelper;

    @KafkaListener(topics = "payment-failed-topic", groupId = "order-group")
    public void consumePaymentFailed(@Payload String message, @Header(value = "X-Trace-Id", required = false) String traceId) {
        kafkaMdcHelper.processEventWithMdc(traceId, message, PaymentFailedEvent.class, event -> {
            CancelOrderCommand command = CancelOrderCommand.builder()
                    .id(event.orderId())
                    .reason(event.reason())
                    .build();
            orderService.cancelOrder(command);
        });
    }

    // [결제 완료] 최종 성공 이벤트 수신
    @KafkaListener(topics = "payment-completed-topic", groupId = "order-group")
    public void consumePaymentComplete(@Payload String message, @Header(value = "X-Trace-Id", required = false) String traceId) {
        kafkaMdcHelper.processEventWithMdc(traceId, message, PaymentCompletedEvent.class, event -> {
            CompleteOrderCommand command = CompleteOrderCommand.builder().id(event.orderId()).build();
            orderService.completeOrder(command);
        });
    }

    // [환불 완료] 환불(취소) 이벤트 수신
    @KafkaListener(topics = "payment-refund-topic", groupId = "order-group")
    public void consumePaymentRefund(@Payload String message, @Header(value = "X-Trace-Id", required = false) String traceId) {
        kafkaMdcHelper.processEventWithMdc(traceId, message, PaymentRefundEvent.class, event -> {
            RefundOrderCommand command = RefundOrderCommand.builder().id(event.orderId()).build();
            orderService.refundOrder(command);
        });
    }

    // [환불 완료] 재품 -> 오더 취소 이벤트 수신
    @KafkaListener(topics = "order-cancel-topic", groupId = "order-group")
    public void consumeOrderFailed(@Payload String message, @Header(value = "X-Trace-Id", required = false) String traceId) {
        kafkaMdcHelper.processEventWithMdc(traceId, message, PaymentCancelEvent.class, event -> {
            CancelOrderCommand command = CancelOrderCommand.builder().id(event.orderId()).build();
            orderService.cancelOrder(command);
        });
    }

}