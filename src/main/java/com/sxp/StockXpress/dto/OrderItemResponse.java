package com.sxp.StockXpress.dto;

import lombok.*;

// OrderItemResponse.java (Return Order Items)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double pricePerUnit;
    private Double totalPrice;
}
