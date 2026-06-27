package org.example.corepayorderservice.order.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaycommon.outbox.OutboxEvent;
import org.example.corepaycommon.outbox.OutboxEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final OutboxEventPublisher outboxEventPublisher;

    public void publishImmediately(OutboxEvent outboxEvent) {
        if (outboxEvent == null) return;
        try {
            outboxEventPublisher.publish(outboxEvent);
        } catch (Exception e) {
            log.warn("[Outbox 즉시 발행 실패] 스케줄러가 재시도 예정. topic={}", outboxEvent.getTopic());
        }
    }
}
