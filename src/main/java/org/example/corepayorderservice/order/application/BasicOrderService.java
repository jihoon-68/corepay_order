package org.example.corepayorderservice.order.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayorderservice.order.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepayorderservice.order.presentation.dto.OrderCreatReq;
import org.example.corepayorderservice.order.presentation.dto.OrderUpdateStateReq;
import org.example.corepayorderservice.order.presentation.dto.OrderDto;
import org.example.corepayorderservice.order.infrastructure.db.OrderRepository;
import org.example.corepayorderservice.order.domain.Order;
import org.example.corepayorderservice.order.infrastructure.kafka.OrderEventProducer;
import org.example.corepayorderservice.product.application.ProductSnapshotService;
import org.example.corepayorderservice.product.presentation.ProductSnapshotDto;
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
    private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional
    public OrderDto creat(OrderCreatReq req) {
        //상품 확인 및 재고 차감
        ProductSnapshotDto product = productSnapshotService.getProductInfo(req.productId());

        //최종 결제 금액 계산
        int discountedPrice = product.price() - (product.price() * product.discount() / 100);
        int totalAmount = discountedPrice * req.amount();

        //주문(Order) 생성 (READY)
        Order order = Order.builder()
                .userId(req.userId())
                .productId(req.productId())
                .orderPrice(totalAmount)
                .amount(req.amount())
                .build();
        orderRepository.save(order);

        //결제(Payment) 대기열 같이 생성 (READY)
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .productId(order.getProductId())
                .totalPrice(totalAmount)
                .amount(req.amount())
                .build();

        orderEventProducer.sendOrderCreated(event);

        return OrderDto.from(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("취소할 주문을 찾을 수 없습니다. ID: " + orderId));

        order.cancel();
        log.error(" [주문 취소 완료] 주문 ID: {}, 사유: {}", orderId, reason);
    }

    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("완료할 주문을 찾을 수 없습니다. ID: " + orderId));

        order.complete();
        log.info(" [주문 최종 완료] 주문 ID: {}", orderId);
    }

    @Transactional
    public void refundOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("환불할 주문을 찾을 수 없습니다. ID: " + orderId));

        order.refund();
        log.info(" [환불 완료] 주문 ID: {}의 상태가 REFUNDED로 변경되었습니다.", orderId);
    }

    @Override
    @Transactional
    public void updateState(OrderUpdateStateReq req) {
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
