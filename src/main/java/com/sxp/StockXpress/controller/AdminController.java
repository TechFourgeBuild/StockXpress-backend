package com.sxp.StockXpress.controller;

import com.sxp.StockXpress.dto.AdminRegisterRequest;
import com.sxp.StockXpress.dto.DashboardStatsResponse;
import com.sxp.StockXpress.dto.OrderResponse;
import com.sxp.StockXpress.dto.UserResponse;
import com.sxp.StockXpress.entity.Order;
import com.sxp.StockXpress.entity.User;
import com.sxp.StockXpress.enums.Role;
import com.sxp.StockXpress.exception.ResourceNotFoundException;
import com.sxp.StockXpress.repository.OrderItemRepository;
import com.sxp.StockXpress.repository.OrderRepository;
import com.sxp.StockXpress.repository.UserRepository;
import com.sxp.StockXpress.service.AdminService;
import com.sxp.StockXpress.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "admin-controller", description = "APIs for Admin control panel")
public class AdminController {

    private final AdminService adminService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    // ============================================================
    // 📊 ADMIN — Dashboard statistics
    // ============================================================
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        log.info("📊 Admin fetching dashboard stats");
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ============================================================
    // 💰 ADMIN — Get total revenue
    // ============================================================
    @GetMapping("/revenue")
    public ResponseEntity<Double> getTotalRevenue() {
        log.info("💰 Admin fetching total revenue");
        return ResponseEntity.ok(adminService.getTotalRevenue());
    }

    // ============================================================
    // 👥 ADMIN — Get total users count
    // ============================================================
    @GetMapping("/users/count")
    public ResponseEntity<Long> getTotalUsers() {
        log.info("👥 Admin fetching total users");
        return ResponseEntity.ok(adminService.getTotalUsers());
    }

    // ============================================================
    // 📦 ADMIN — Get total products
    // ============================================================
    @GetMapping("/products/count")
    public ResponseEntity<Long> getTotalProducts() {
        log.info("📦 Admin fetching total products");
        return ResponseEntity.ok(adminService.getTotalProducts());
    }

    // ============================================================
    // 📋 ADMIN — Get pending orders count
    // ============================================================
    @GetMapping("/orders/pending")
    public ResponseEntity<Long> getPendingOrders() {
        log.info("📋 Admin fetching pending orders");
        return ResponseEntity.ok(adminService.getPendingOrdersCount());
    }

    // ============================================================
    // ⚠️ ADMIN — Get low stock products count
    // ============================================================
    @GetMapping("/products/low-stock")
    public ResponseEntity<Long> getLowStockProducts(
            @RequestParam(defaultValue = "5") int threshold) {
        log.info("⚠️ Admin fetching low stock products (threshold: {})", threshold);
        return ResponseEntity.ok(adminService.getLowStockProductsCount(threshold));
    }

    // ============================================================
    // 📅 ADMIN — Get orders in last N days
    // ============================================================
    // ✅ Return List of Orders, not count
    @GetMapping("/orders/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getOrdersInLastDays(@RequestParam(defaultValue = "7") int days) {
        LocalDateTime date = LocalDateTime.now().minusDays(days);
        List<Order> orders = orderRepository.findOrdersFromLastDays(date);
        List<OrderResponse> responses = orders.stream()
                .map(order -> orderService.mapToResponse(order, orderItemRepository.findByOrderId(order.getId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // ============================================================
    // 👑 ADMIN — Register new user with role
    // ============================================================
    @PostMapping("/register")
    public ResponseEntity<String> registerUserWithRole(@RequestBody AdminRegisterRequest request) {
        log.info("👑 Admin registering new user with role: {}", request.getRole());
        String message = adminService.registerUserWithRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    // ============================================================
// 👥 ADMIN — Get all users
// ============================================================
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("👥 Admin fetching all users");
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // ============================================================
    // 👥 ADMIN — Get user by ID
    // ============================================================
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        log.info("👥 Admin fetching user by ID: {}", userId);
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    // ============================================================
    // 👥 ADMIN — Update user role
    // ============================================================
    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long userId,
            @RequestParam Role role) {
        log.info("👥 Admin updating user role: {} → {}", userId, role);
        return ResponseEntity.ok(adminService.updateUserRole(userId, role));
    }

    // ============================================================
    // 👥 ADMIN — Delete user
    // ============================================================
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info("🗑️ Admin deleting user: {}", userId);
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

}