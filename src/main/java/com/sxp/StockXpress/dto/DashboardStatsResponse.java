package com.sxp.StockXpress.dto;

import lombok.*;

// DashboardStatsResponse.java (Admin Dashboard)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private Long totalUsers;
    private Long totalProducts;
    private Long totalOrders;
    private Double totalRevenue;
    private Long pendingOrders;
    private Long lowStockProducts;
}