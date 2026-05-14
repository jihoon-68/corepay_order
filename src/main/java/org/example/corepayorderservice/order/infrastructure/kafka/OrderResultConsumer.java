package org.example.corepayorderservice.order.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.application.OrderService;
import org.example.corepayorderservice.order.application.command.CancelOrderCommand;
import org.example.corepayorderservice.order.application.command.CompleteOrderCommand;
import org.example.corepayorderservice.order.application.command.RefundOrderCommand;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentFailedEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCompletedEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentRefundEvent;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderResultConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @KafkaListener(topics = "payment-failed-topic", groupId = "order-group")
    public void consumePaymentFailed(@Payload String message, @Header(value = "X-Trace-Id", required = false) String traceId) {
        processEventWithMdc(traceId, message, PaymentFailedEvent.class, event -> {
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
        processEventWithMdc(traceId, message, PaymentCompletedEvent.class, event -> {
            CompleteOrderCommand command = CompleteOrderCommand.builder().id(event.orderId()).build();
            orderService.completeOrder(command);
        });
    }

    // [환불 완료] 환불(취소) 이벤트 수신
    @KafkaListener(topics = "payment-refund-topic", groupId = "order-group")
    public void consumePaymentRefund(@Payload String message, @Header(value = "X-Trace-Id", required = false) String traceId) {
        processEventWithMdc(traceId, message, PaymentRefundEvent.class, event -> {
            RefundOrderCommand command = RefundOrderCommand.builder().id(event.orderId()).build();
            orderService.refundOrder(command);
        });
    }

    // [환불 완료] 재품 -> 오더 취소 이벤트 수신
    @KafkaListener(topics = "order-cancel-topic", groupId = "order-group")
    public void consumeOrderFailed(@Payload String message, @Header(value = "X-Trace-Id", required = false) String traceId) {
        processEventWithMdc(traceId, message, PaymentCancelEvent.class, event -> {
            CancelOrderCommand command = CancelOrderCommand.builder().id(event.orderId()).build();
            orderService.cancelOrder(command);
        });
    }

    // MDC 설정, JSON 파싱, 예외 처리, MDC 초기화를 한 곳
    private <T> void processEventWithMdc(String traceId, String message, Class<T> eventType, Consumer<T> eventProcessor) {
        try {
            // 1. MDC 세팅
            if (traceId != null) {
                MDC.put("traceId", traceId);
            }

            // 2. 공통 JSON 파싱 로직
            T event = objectMapper.readValue(message, eventType);

            // 3. 각 리스너가 전달한 비즈니스 로직(람다 함수) 실행
            eventProcessor.accept(event);

        } catch (JsonProcessingException e) {
            log.error("[{}] 카프카 메시지 파싱 에러 - 메시지: {}", eventType.getSimpleName(), message, e);
        } catch (Exception e) {
            log.error("[{}] 이벤트 처리 중 예상치 못한 에러 발생", eventType.getSimpleName(), e);
        } finally {
            // 4. 안전하게 MDC 클리어
            MDC.clear();
        }
    }
}