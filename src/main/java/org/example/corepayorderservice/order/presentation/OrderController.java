package org.example.corepayorderservice.order.presentation;

import lombok.RequiredArgsConstructor;
import org.example.corepayorderservice.order.application.CancelReason;
import org.example.corepayorderservice.order.application.command.CancelOrderCommand;
import org.example.corepayorderservice.order.application.command.CreatedOrderCommand;
import org.example.corepayorderservice.order.application.command.UpdateStateOrderCommand;
import org.example.corepayorderservice.order.presentation.dto.*;
import org.example.corepayorderservice.order.application.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 💡 프론트엔드에서 결제창 띄우기 직전에 호출하는 아주 중요한 API!
    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(
            @RequestHeader("X-User-Id") Long userId, // 🛡️ 1. 게이트웨이가 인증한 100% 신뢰할 수 있는 유저 ID
            @RequestBody OrderCreatReq req) {

        // Req -> Command 변환 (List 매핑)
        List<CreatedOrderCommand.OrderItemCommand> itemCommands = req.items().stream()
                .map(item -> new CreatedOrderCommand.OrderItemCommand(item.productId(), item.amount()))
                .toList();

        CreatedOrderCommand command = CreatedOrderCommand.builder()
                .userId(userId) // 🛡️ 2. 클라이언트가 보낸 req.userId()가 아니라, 안전한 헤더의 userId를 주입!
                .items(itemCommands)
                .build();

        return ResponseEntity.ok(orderService.create(command));
    }
    @PatchMapping("/cancel")
    public ResponseEntity<Void> cancelOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody OrderCancelReq req){
        CancelOrderCommand command = CancelOrderCommand.builder().
                id(req.id()).
                userId(userId).
                reason(CancelReason.valueOf(req.reason()))
                .build();
        return ResponseEntity.ok().build();
    }

    // 💡 단건 조회도 내 주문인지 확인하려면 userId가 필요할 수 있습니다.
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        // (필요 시) 서비스 레이어에서 이 주문이 해당 userId의 주문이 맞는지 검증하는 로직 추가
        return ResponseEntity.ok(orderService.get(id));
    }

    // 💡 전체 목록이 아니라 "내 주문 목록"만 가져오도록 헤더를 받습니다.
    @GetMapping
    public ResponseEntity<Page<OrderDto>> getOrderList(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        // 서비스 레이어의 메서드도 getList(userId) 처럼 변경해서 내 것만 조회하도록 하면 완벽합니다.
        return ResponseEntity.ok(orderService.getList(pageable));
    }

    @PatchMapping("/{id}/state")
    public ResponseEntity<Void> updateOrderState(@RequestBody OrderUpdateStateReq req) {
        // 상태 변경은 보통 결제 서버(Payment) 콜백이나 관리자가 호출하므로 로직 유지
        UpdateStateOrderCommand command = UpdateStateOrderCommand.builder()
                .id(req.id())
                .state(req.state())
                .build();

        orderService.updateState(command);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}