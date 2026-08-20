package com.sxp.StockXpress.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// StockUpdateRequest.java (Warehouse Manager - Update Stock)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateRequest {
    private Long productId;
    private Integer quantity;        // ✅ New quantity to set
    private String reason;           // ✅ Why stock is being updated
}