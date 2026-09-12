package com.sxp.StockXpress.controller;

import com.sxp.StockXpress.dto.OrderRequest;
import com.sxp.StockXpress.dto.OrderResponse;
import com.sxp.StockXpress.enums.OrderStatus;
import com.sxp.StockXpress.enums.PaymentStatus;
import com.sxp.StockXpress.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "order-controller", description = "APIs for Order management")
public class OrderController {

    private final OrderService orderService;


    //  AUTHENTICATED — Place order
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        log.info("🛒 placing new order");
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    //  AUTHENTICATED — Get current user's orders
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        log.info("📦 Fetching current user's orders");
        return ResponseEntity.ok(orderService.getUserOrders());
    }


    //  AUTHENTICATED — Get order by ID
   @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        log.info("📦 Fetching order with ID: {}",id);
        return ResponseEntity.ok(orderService.getOrderById(id));
    }


    //  AUTHENTICATED — Get order by order number
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        log.info("📦 Fetching order with number: {}", orderNumber);
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
    }


    //  AUTHENTICATED — Cancel order
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        log.info("🗑️ Cancelling order with ID: {}", id);
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }


    //  ADMIN + WAREHOUSE — Get all orders
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("📦 Fetching all orders (Admin/Warehouse)");
        return ResponseEntity.ok(orderService.getAllOrders());
    }


    //  ADMIN + WAREHOUSE — Update order status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status
    ) {
        log.info("📦 Updating order status: {} → {}", id, status);
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }


    //  ADMIN — Update payment status
    @PatchMapping("/{id}/payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam PaymentStatus paymentStatus) {
        log.info("💳 Updating payment status: {} → {}", id, paymentStatus);
        return ResponseEntity.ok(orderService.updatePaymentStatus(id, paymentStatus));
    }
}
