package org.example.corepayorderservice.order.application;

import org.example.corepayorderservice.order.application.command.*;
import org.example.corepayorderservice.order.presentation.dto.OrderCreateResponse;
import org.example.corepayorderservice.order.presentation.dto.OrderDto;
import org.example.corepayorderservice.order.presentation.dto.PaymentResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderCreateResponse create(CreatedOrderCommand command);
    PaymentResultDto requestPayment(RequestPaymentCommand command);
    void updateState(UpdateStateOrderCommand command);
    void cancelOrder(CancelOrderCommand command);
    void refundOrder(RefundOrderCommand command);
    OrderDto get(Long id);
    Page<OrderDto> getList(Pageable pageable);
    void delete(Long id);
}
