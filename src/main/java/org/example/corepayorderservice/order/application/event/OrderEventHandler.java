package org.example.corepayorderservice.order.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.infrastructure.kafka.OrderEventProducer;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.StockIncreaseEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderEventProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void OrderCreatedEvent(OrderCreatedEvent event){
        log.info("========[오더 생성 이벤트 수신 받음]========");
        producer.sendOrderCreated(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void StockIncreaseEvent(StockIncreaseEvent event){
        log.info("========[재고 복구 이벤트 수신 받음]========");
        producer.sendStockIncrease(event);
    }

}
