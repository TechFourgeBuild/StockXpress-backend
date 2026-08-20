package com.sxp.StockXpress.dto;

import com.sxp.StockXpress.enums.OrderStatus;
import com.sxp.StockXpress.enums.PaymentStatus;
import lombok.*;
// OrderResponse.java (Return Order Data)
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private Double totalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String shippingAddress;
    private LocalDateTime deliveryDate;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
}