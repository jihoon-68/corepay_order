package org.example.corepayorderservice.order.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.application.command.*;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepayorderservice.order.presentation.dto.OrderUpdateStateReq;
import org.example.corepayorderservice.order.presentation.dto.OrderDto;
import org.example.corepayorderservice.order.infrastructure.db.OrderRepository;
import org.example.corepayorderservice.order.domain.Order;
import org.example.corepayorderservice.order.infrastructure.kafka.OrderEventProducer;
import org.example.corepayorderservice.product.application.ProductSnapshotService;
import org.example.corepayorderservice.product.presentation.ProductSnapshotDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        //상품 확인 및 재고 차감
        ProductSnapshotDto product = productSnapshotService.getProductInfo(command.productId());

        //최종 결제 금액 계산
        int discountedPrice = product.price() - (product.price() * product.discount() / 100);
        int totalAmount = discountedPrice * command.amount();

        //주문(Order) 생성 (READY)
        Order order = Order.builder()
                .userId(command.userId())
                .productId(command.productId())
                .orderPrice(totalAmount)
                .amount(command.amount())
                .build();
        orderRepository.save(order);

        //결제(Payment) 대기열 같이 생성 (READY)
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .productId(order.getProductId())
                .totalPrice(totalAmount)
                .amount(command.amount())
                .build();

        publisher.publishEvent(event);

        return OrderDto.from(order);
    }

    @Override
    @Transactional
    public void cancelOrder(CancelOrderCommand command) {
        Order order = orderRepository.findById(command.id())
                .orElseThrow(() -> new RuntimeException("취소할 주문을 찾을 수 없습니다. ID: " + command.id()));

        order.cancel();
        log.error(" [주문 취소 완료] 주문 ID: {}, 사유: {}", command.id(),command.reason());
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void completeOrder(CompleteOrderCommand command) {
        Order order = orderRepository.findById(command.id())
                .orElseThrow(() -> new RuntimeException("완료할 주문을 찾을 수 없습니다. ID: " + command.id()));

        order.complete();
        log.info(" [주문 최종 완료] 주문 ID: {}", command.id());
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void refundOrder(RefundOrderCommand command) {
        Order order = orderRepository.findById(command.id())
                .orElseThrow(() -> new RuntimeException("환불할 주문을 찾을 수 없습니다. ID: " + command.id()));

        order.refund();
        log.info(" [환불 완료] 주문 ID: {}의 상태가 REFUNDED로 변경되었습니다.", command.id());
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void updateState(UpdateStateOrderCommand command) {
        //배달지, 언락처, 받는이 등 추가시 배송전에 변경가능
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto get(Long id) {
        return OrderDto.from(orderRepository.findById(id).orElseThrow());
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
        //소프트 삭제? 하드삭제?
        orderRepository.deleteById(id);
    }
}
