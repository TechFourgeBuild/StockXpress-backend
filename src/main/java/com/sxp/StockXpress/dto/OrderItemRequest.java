package com.sxp.StockXpress.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// OrderItemRequest.java (Items inside Order)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    private Long productId;
    private Integer quantity;
}
