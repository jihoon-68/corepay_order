package org.example.corepayorderservice.order.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.domain.Order;
import org.example.corepayorderservice.order.infrastructure.db.OrderRepository;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentRefundEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundDltConsumer {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    /**
     * payment-refund-topic 재시도 3회 모두 실패 시 DLQ 수신
     * → 주문 상태를 REFUND_FAILED 로 강제 마킹
     * → 슬랙/알림 (TODO)
     */
    @Transactional
    @KafkaListener(topics = "payment-refund-topic.DLT", groupId = "order-group-dlt")
    public void consumePaymentRefundDlt(@Payload String message) {
        log.error("[DLQ 수신] payment-refund 재시도 전부 실패. message={}", message);

        try {
            PaymentRefundEvent event = objectMapper.readValue(message, PaymentRefundEvent.class);

            Order order = orderRepository.findById(event.orderId()).orElse(null);

            if (order == null) {
                log.error("[DLQ 처리 실패] 주문 없음. orderId={}", event.orderId());
                return;
            }

            // 환불 실패 상태로 강제 마킹 → 운영팀 수동 처리 대상
            order.refundFailed();
            orderRepository.save(order);

            // TODO: alertService.sendAlert("[환불 실패] orderId=" + event.orderId() + " 수동 처리 필요");
            log.error("[DLQ 알림 필요] orderId={} 수동 환불 처리 요망", event.orderId());

        } catch (Exception e) {
            // DLT Consumer 자체 실패 → 로그만 남기고 넘김 (무한루프 방지)
            log.error("[DLQ 처리 중 예외] message={}, error={}", message, e.getMessage());
        }
    }
}