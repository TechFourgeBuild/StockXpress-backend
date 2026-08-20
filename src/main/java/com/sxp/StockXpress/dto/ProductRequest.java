package com.sxp.StockXpress.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// ProductRequest.java (Create/Update Product)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    private String name;
    private String description;
    private Double price;
    private Integer availableQuantity;
    private String category;
    private String imageUrl;
}