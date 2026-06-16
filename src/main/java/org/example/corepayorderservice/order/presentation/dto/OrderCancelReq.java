package org.example.corepayorderservice.order.presentation.dto;

import lombok.Builder;

@Builder
public record OrderCancelReq(
        Long id,
        String reason
) {
}
