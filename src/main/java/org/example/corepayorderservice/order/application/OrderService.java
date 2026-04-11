package org.example.corepayorderservice.order.application;

import org.example.corepayorderservice.order.application.command.*;
import org.example.corepayorderservice.order.presentation.dto.OrderUpdateStateReq;
import org.example.corepayorderservice.order.presentation.dto.OrderDto;

import java.util.List;

public interface OrderService {

    OrderDto creat(CreatedOrderCommand command);
    void updateState(UpdateStateOrderCommand command);
    void cancelOrder(CancelOrderCommand command);
    void completeOrder(CompleteOrderCommand command);
    void refundOrder(RefundOrderCommand command);
    OrderDto get(Long id);
    List<OrderDto> getList();
    void delete(Long id);
}
