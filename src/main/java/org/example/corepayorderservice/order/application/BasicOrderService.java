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
        Order order = Order.builder()
                .userId(command.userId())
                .build();

        int totalAmount = 0;

        // 2. 다건 상품 순회 처리
        for (CreatedOrderCommand.OrderItemCommand itemCommand : command.items()) {
            // 스냅샷 정보 조회 (Product 서비스 데이터 복제본)
            ProductSnapshotDto product = productSnapshotService.getProductInfo(itemCommand.productId());

            // 할인가 적용 단가 계산
            int discountedPrice = product.price() - (product.price() * product.discount() / 100);
            int itemTotalPrice = discountedPrice * itemCommand.amount();
            totalAmount += itemTotalPrice;

            // 주문 상세 엔티티 생성 및 Order에 추가
            OrderLineItem lineItem = OrderLineItem.builder()
                    .productId(itemCommand.productId())
                    .price(discountedPrice) // 단가 저장
                    .amount(itemCommand.amount())
                    .build();

            order.addLineItem(lineItem); // 양방향 매핑 설정
        }

        // 3. 계산된 총액 세팅 후 DB 저장
        // CascadeType.ALL 덕분에 OrderLineItem 데이터들도 한 번에 자동 INSERT 됩니다.
        order.updateOrderPrice(totalAmount);
        orderRepository.save(order);

        //결제(Payment) 대기열 같이 생성 (READY)
        List<OrderItemDto> eventItems = command.items().stream()
                .map(item -> OrderItemDto.from(item.productId(), item.amount()))
                .toList();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalPrice(totalAmount)
                .items(eventItems)
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

        if(command.reason().isNeedStockRestore()){
            // 재품 재고 복구 이벤트 발행
            List<OrderItemDto> eventItems = order.getOrderLineItems().stream()
                    .map(item -> OrderItemDto.from(item.getProductId(), item.getAmount()))
                    .toList();

            StockIncreaseEvent event = StockIncreaseEvent.builder()
                    .orderId(order.getId())
                    .items(eventItems)
                    .build();

            publisher.publishEvent(event);
        }
        // 어떤 사유든 주문이 취소되면 결제 서버도 상태를 정리해야 하므로 결제 취소 이벤트 발행
        PaymentCancelEvent event = PaymentCancelEvent.builder()
                .orderId(order.getId())
                .reason(command.reason())
                .build();
        publisher.publishEvent(event);

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
