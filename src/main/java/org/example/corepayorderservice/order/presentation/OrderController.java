package org.example.corepayorderservice.order.presentation;

import lombok.RequiredArgsConstructor;
import org.example.corepayorderservice.order.presentation.dto.OrderCreatReq;
import org.example.corepayorderservice.order.presentation.dto.OrderUpdateStateReq;
import org.example.corepayorderservice.order.application.OrderService;
import org.example.corepayorderservice.order.presentation.dto.OrderDto;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderCreatReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.creat(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.get(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getOrderList() {
        return ResponseEntity.ok(orderService.getList());
    }

    @PatchMapping("/{id}/state")
    public ResponseEntity<Void> updateOrderState(@RequestBody OrderUpdateStateReq req) {
        // req 객체에 id가 이미 포함되어 있다면 서비스 레이어 파라미터를 살짝 맞춰주면 돼!
        orderService.updateState(req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
