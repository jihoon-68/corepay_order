package org.example.corepayorderservice.order.application;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.application.command.*;
import org.example.corepayorderservice.order.domain.OrderLineItem;
import org.example.corepayorderservice.order.infrastructure.feign.PaymentFeignClient;
import org.example.corepayorderservice.order.infrastructure.feign.ProductStockClient;
import org.example.corepayorderservice.order.infrastructure.feign.dto.PaymentRequest;
import org.example.corepayorderservice.order.infrastructure.feign.dto.PaymentResponse;
import org.example.corepayorderservice.order.infrastructure.feign.dto.StockReserveRequest;
import org.example.corepayorderservice.order.infrastructure.feign.dto.StockReserveResponse;
import org.example.corepayorderservice.order.infrastructure.kafka.event.*;
import org.example.corepayorderservice.order.presentation.dto.OrderCreateResponse;
import org.example.corepayorderservice.order.presentation.dto.OrderDto;
import org.example.corepayorderservice.order.infrastructure.db.OrderRepository;
import org.example.corepayorderservice.order.domain.Order;
import org.example.corepayorderservice.order.presentation.dto.PaymentResultDto;
import org.example.corepayorderservice.product.application.ProductSnapshotService;
import org.example.corepayorderservice.product.presentation.ProductSnapshotDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BasicOrderService implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductSnapshotService productSnapshotService;
    private final ApplicationEventPublisher publisher;
    private final ProductStockClient productStockClient;
    private final PaymentFeignClient paymentFeignClient;

    @Override
    @Transactional
    public OrderCreateResponse creat(CreatedOrderCommand command) {
        Map<Long, ProductSnapshotDto> productMap = fetchProductMap(command);
        Order order = buildOrder(command, productMap);
        orderRepository.save(order);
        return processStockReservation(order, command);
    }

    @Override
    @Transactional
    public PaymentResultDto requestPayment(Long orderId) {
        Order order = findOrderById(orderId);
        order.requestPayment(); // STOCK_RESERVED 검증 → PAYMENT_REQUESTED

        try {
            PaymentResponse response = paymentFeignClient.pay(
                    new PaymentRequest(orderId, order.getUserId(), order.getOrderPrice())
            );

            if (response.isSuccess()) {
                return processPaymentSuccess(order);
            } else {
                return processPaymentFailed(order, response.failReason());
            }

        } catch (FeignException.GatewayTimeout | FeignException.ServiceUnavailable e) {
            // 타임아웃: PAYMENT_REQUESTED 유지 → 스케줄러가 EXPIRED 처리
            log.error("[결제 타임아웃] orderId={} → PAYMENT_REQUESTED 상태 유지", orderId, e);
            throw new RuntimeException("결제 결과를 확인할 수 없습니다. 잠시 후 다시 확인해주세요.", e);
        }
    }

    @Override
    @Transactional
    public void cancelOrder(CancelOrderCommand command) {
        Order order = findOrderById(command.id());
        order.cancel();
        log.info("[주문 취소 완료] 주문 ID: {}, 사유: {}", command.id(), command.reason());
        publishPaymentCancelEvent(order, command);
    }

    @Override
    @Transactional
    public void refundOrder(RefundOrderCommand command) {
        Order order = findOrderById(command.id());
        order.refund();
        log.info("[환불 완료] 주문 ID: {}", command.id());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto get(Long id) {
        return OrderDto.from(findOrderById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getList() {
        return orderRepository.findAll().stream()
                .map(OrderDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        orderRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateState(UpdateStateOrderCommand command) {
        // 배송 전 배달지 변경 등 추가 예정
    }

    // ── private 헬퍼 ──────────────────────────────

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("취소할 주문을 찾을 수 없습니다."));
    }

    private Map<Long, ProductSnapshotDto> fetchProductMap(CreatedOrderCommand command) {
        List<Long> productIds = command.items().stream()
                .map(CreatedOrderCommand.OrderItemCommand::productId)
                .toList();
        return productSnapshotService.getProductInfos(productIds);
    }

    private Order buildOrder(CreatedOrderCommand command, Map<Long, ProductSnapshotDto> productMap) {
        Order order = Order.builder()
                .userId(command.userId())
                .build();

        int totalAmount = 0;
        for (CreatedOrderCommand.OrderItemCommand itemCommand : command.items()) {
            ProductSnapshotDto product = productMap.get(itemCommand.productId());
            int discountedPrice = calcDiscountedPrice(product);
            totalAmount += discountedPrice * itemCommand.amount();
            order.addLineItem(OrderLineItem.builder()
                    .productId(itemCommand.productId())
                    .price(discountedPrice)
                    .amount(itemCommand.amount())
                    .build());
        }

        order.updateOrderPrice(totalAmount);
        return order;
    }

    private OrderCreateResponse processStockReservation(Order order, CreatedOrderCommand command) {
        StockReserveResponse result = requestStockReserve(order, command);
        return result.success()
                ? handleReserveSuccess(order, command)
                : handleReserveFailed(order, result);
    }

    private StockReserveResponse requestStockReserve(Order order, CreatedOrderCommand command) {
        StockReserveRequest request = new StockReserveRequest(
                order.getId(),
                command.items().stream()
                        .map(i -> new StockReserveRequest.Item(i.productId(), i.amount()))
                        .toList()
        );

        try {
            return productStockClient.reserveStock(request);
        } catch (FeignException e) {
            log.error("[재고 선점 통신 오류] 주문 ID: {}, status: {}, message: {}",
                    order.getId(), e.status(), e.getMessage());
            throw new RuntimeException("재고 서버와 통신 중 오류가 발생했습니다.", e);
        }
    }

    private OrderCreateResponse handleReserveSuccess(Order order, CreatedOrderCommand command) {
        order.reserveStock();
        log.info("[재고 선점 성공] 주문 ID: {}", order.getId());
        return OrderCreateResponse.reserved(order.getId());
    }

    private OrderCreateResponse handleReserveFailed(Order order, StockReserveResponse result) {
        order.failStock();
        log.warn("[재고 선점 실패] 주문 ID: {}, 품절 상품: {}",
                order.getId(), result.outOfStockProductIds());
        return OrderCreateResponse.failed(order.getId(), result.outOfStockProductIds());
    }


    private PaymentResultDto processPaymentSuccess(Order order){
        order.completePayment(); // PAYMENT_REQUESTED → COMPLETED
        publishStockConfirmEvent(order); // Kafka: Product DB 재고 확정
        log.info("[결제 성공] orderId={}", order.getId());
        return PaymentResultDto.success(order.getId());
    }

    private void publishStockConfirmEvent(Order order) {
        List<OrderItemDto> items = order.getOrderLineItems().stream()
                .map(item -> OrderItemDto.from(item.getProductId(), item.getAmount()))
                .toList();
        publisher.publishEvent(
                StockConfirmEvent.builder()
                        .orderId(order.getId())
                        .items(items)
                        .build()
        );
    }

    private PaymentResultDto processPaymentFailed(Order order, String failReason){
        order.failPayment(); // PAYMENT_REQUESTED → CANCELLED
        publishOrderCancelledEvent(order); // Kafka: Product Redis 재고 복구
        log.warn("[결제 실패] orderId={}, reason={}", order.getId(), failReason);
        return PaymentResultDto.fail(order.getId(), failReason);
    }

    private void publishOrderCancelledEvent(Order order) {
        List<OrderItemDto> items = order.getOrderLineItems().stream()
                .map(item -> OrderItemDto.from(item.getProductId(), item.getAmount()))
                .toList();
        publisher.publishEvent(
                OrderCancelledEvent.builder()
                        .orderId(order.getId())
                        .items(items)
                        .build()
        );
    }


    private int calcDiscountedPrice(ProductSnapshotDto product) {
        return product.price() - (product.price() * product.discount() / 100);
    }

    private void publishPaymentCancelEvent(Order order, CancelOrderCommand command) {
        publisher.publishEvent(PaymentCancelEvent.builder()
                .orderId(order.getId())
                .reason(command.reason())
                .build());
    }
}