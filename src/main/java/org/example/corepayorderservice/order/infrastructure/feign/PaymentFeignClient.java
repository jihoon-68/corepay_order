package org.example.corepayorderservice.order.infrastructure.feign;

import org.example.corepayorderservice.order.infrastructure.feign.dto.PaymentRequest;
import org.example.corepayorderservice.order.infrastructure.feign.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${services.payment.url}")
public interface PaymentFeignClient {

    @PostMapping("/api/payments/pay")
    PaymentResponse pay(@RequestBody PaymentRequest request);

}
