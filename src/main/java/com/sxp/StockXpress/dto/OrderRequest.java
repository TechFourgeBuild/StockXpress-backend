package com.sxp.StockXpress.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
// OrderRequest.java (Place Order)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String shippingAddress;
    private List<OrderItemRequest> items;  //  List of products with quantity
}