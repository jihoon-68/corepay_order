package org.example.corepayorderservice.order.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.application.command.*;
import org.example.corepayorderservice.order.domain.OrderLineItem;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderItemDto;
import org.example.corepayorderservice.order.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepayorderservice.order.infrastructure.kafka.event.StockIncreaseEvent;
import org.example.corepayorderservice.order.presentation.dto.OrderDto;
import org.example.corepayorderservice.order.infrastructure.db.OrderRepository;
import org.example.corepayorderservice.order.domain.Order;
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

    @Override
    @Transactional
    public OrderDto creat(CreatedOrderCommand command) {
        Map<Long, ProductSnapshotDto> productMap = fetchProductMap(command);
        Order order = buildOrder(command, productMap);
        orderRepository.save(order);
        publishOrderCreatedEvent(order, command);
        return OrderDto.from(order);
    }

    @Override
    @Transactional
    public void cancelOrder(CancelOrderCommand command) {
        Order order = findOrderById(command.id());
        order.cancel();
        log.info("[주문 취소 완료] 주문 ID: {}, 사유: {}", command.id(), command.reason());

        if (command.reason().isNeedStockRestore()) {
            publishStockIncreaseEvent(order);
        }
        publishPaymentCancelEvent(order, command);
    }

    @Override
    @Transactional
    public void completeOrder(CompleteOrderCommand command) {
        Order order = findOrderById(command.id());
        order.complete();
        log.info("[주문 최종 완료] 주문 ID: {}", command.id());
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

    private int calcDiscountedPrice(ProductSnapshotDto product) {
        return product.price() - (product.price() * product.discount() / 100);
    }

    private void publishOrderCreatedEvent(Order order, CreatedOrderCommand command) {
        List<OrderItemDto> eventItems = command.items().stream()
                .map(item -> OrderItemDto.from(item.productId(), item.amount()))
                .toList();
        publisher.publishEvent(OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalPrice(order.getOrderPrice())
                .items(eventItems)
                .build());
    }

    private void publishStockIncreaseEvent(Order order) {
        List<OrderItemDto> eventItems = order.getOrderLineItems().stream()
                .map(item -> OrderItemDto.from(item.getProductId(), item.getAmount()))
                .toList();
        publisher.publishEvent(StockIncreaseEvent.builder()
                .orderId(order.getId())
                .items(eventItems)
                .build());
    }

    private void publishPaymentCancelEvent(Order order, CancelOrderCommand command) {
        publisher.publishEvent(PaymentCancelEvent.builder()
                .orderId(order.getId())
                .reason(command.reason())
                .build());
    }
}