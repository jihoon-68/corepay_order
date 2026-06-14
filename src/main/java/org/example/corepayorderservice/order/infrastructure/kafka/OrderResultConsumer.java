package org.example.corepayorderservice.order.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaycommon.log.KafkaMdcHelper;
import org.example.corepayorderservice.order.application.OrderService;
import org.example.corepayorderservice.order.application.command.RefundOrderCommand;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentRefundEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class OrderResultConsumer {

    private final OrderService orderService;
    private final KafkaMdcHelper kafkaMdcHelper;

    // [주문 취소] 환불(취소) 이벤트 수신
    @KafkaListener(topics = "payment-refund-topic", groupId = "order-group", concurrency = "2")
    public void consumePaymentRefund(@Payload String message, @Header(value = "X-Trace-Id", required = false) String traceId) {
        kafkaMdcHelper.processEventWithMdc(traceId, message, PaymentRefundEvent.class, event -> {
            RefundOrderCommand command = RefundOrderCommand.builder().id(event.orderId()).build();
            orderService.refundOrder(command);
        });
    }


}