package org.example.corepayorderservice.order.application;

import org.example.corepayorderservice.order.presentation.dto.OrderCreatReq;
import org.example.corepayorderservice.order.presentation.dto.OrderUpdateStateReq;
import org.example.corepayorderservice.order.presentation.dto.OrderDto;

import java.util.List;

public interface OrderService {

    OrderDto creat(OrderCreatReq req);
    void updateState(OrderUpdateStateReq req);
    void cancelOrder(Long id,String reason);
    void completeOrder(Long id);
    void refundOrder(Long id);
    OrderDto get(Long id);
    List<OrderDto> getList();
    void delete(Long id);
}
