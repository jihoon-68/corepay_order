package org.example.corepayorderservice.order.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaycommon.outbox.OutboxEvent;
import org.example.corepaycommon.outbox.OutboxRepository;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderCancelledEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.StockConfirmEvent;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // 결제 성공 → DB 재고 확정 Kafka 발행
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockConfirm(StockConfirmEvent event) throws JsonProcessingException {
        log.info("[이벤트 수신] StockConfirmEvent orderId={}", event.orderId());
        saveOutbox("stock-confirm-topic",event);

    }

    // 결제 실패 → Redis 재고 복구 Kafka 발행
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("[이벤트 수신] OrderCancelledEvent orderId={}", event.orderId());
        saveOutbox("order-cancelled-topic",event);
    }

    // 사용자 취소 → 결제 취소 Kafka 발행
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCancel(PaymentCancelEvent event) {
        log.info("[이벤트 수신] PaymentCancelEvent orderId={}", event.orderId());
        saveOutbox("payment-cancel-topic",event);
    }

    private void saveOutbox(String topic, Object event){
        try {
            String messagePayload = objectMapper.writeValueAsString(event);

            String traceId = MDC.get("traceId");

            outboxRepository.save(OutboxEvent.builder()
                    .topic(topic)
                    .payload(messagePayload)
                    .traceId(traceId != null ? traceId : "UNKNOWN-TRACE")
                    .build()
            );
            log.info("[Outbox 저장 완료] 토픽: {}", topic);
        }catch (JsonProcessingException e){
            log.error("Outbox 메시지 직렬화 에러. 토픽: {}", topic, e);
        }

    }

}
