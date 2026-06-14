package org.example.corepayorderservice.order.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.infrastructure.kafka.OrderEventProducer;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderCancelledEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.StockConfirmEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderEventProducer producer;

    // 결제 성공 → DB 재고 확정 Kafka 발행
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockConfirm(StockConfirmEvent event) {
        log.info("[이벤트 수신] StockConfirmEvent orderId={}", event.orderId());
        producer.sendStockConfirm(event);
    }

    // 결제 실패 → Redis 재고 복구 Kafka 발행
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("[이벤트 수신] OrderCancelledEvent orderId={}", event.orderId());
        producer.sendOrderCancelled(event);
    }

    // 사용자 취소 → 결제 취소 Kafka 발행
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCancel(PaymentCancelEvent event) {
        log.info("[이벤트 수신] PaymentCancelEvent orderId={}", event.orderId());
        producer.sendPaymentCancel(event);
    }

}
