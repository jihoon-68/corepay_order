package org.example.corepayorderservice.order.infrastructure.feign;

import org.example.corepayorderservice.order.infrastructure.feign.dto.StockReserveRequest;
import org.example.corepayorderservice.order.infrastructure.feign.dto.StockReserveResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service", url = "${services.product.url}")
public interface ProductStockClient {

    @PostMapping("/api/products/stock/reserve")
    StockReserveResponse reserveStock(@RequestBody StockReserveRequest request);
}