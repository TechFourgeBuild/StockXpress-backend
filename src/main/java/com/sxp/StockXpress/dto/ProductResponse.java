package com.sxp.StockXpress.dto;

import lombok.*;

import java.time.LocalDateTime;
// ProductResponse.java (Return Product Data)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer availableQuantity;
    private String category;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}