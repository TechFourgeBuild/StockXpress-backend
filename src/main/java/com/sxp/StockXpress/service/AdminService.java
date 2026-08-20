package com.sxp.StockXpress.service;

import com.sxp.StockXpress.dto.AdminRegisterRequest;
import com.sxp.StockXpress.dto.DashboardStatsResponse;
import com.sxp.StockXpress.dto.UserResponse;
import com.sxp.StockXpress.entity.Order;
import com.sxp.StockXpress.entity.User;
import com.sxp.StockXpress.enums.OrderStatus;
import com.sxp.StockXpress.enums.Role;
import com.sxp.StockXpress.exception.BusinessException;
import com.sxp.StockXpress.exception.ResourceNotFoundException;
import com.sxp.StockXpress.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryLogRepository inventoryLogRepository;

    // ============================================================
    // 1️⃣ GET DASHBOARD STATISTICS
    // ============================================================
    public DashboardStatsResponse getDashboardStats() {
        Long totalUsers = userRepository.count();
        Long totalProducts = productRepository.count();
        Long totalOrders = orderRepository.count();
        Double totalRevenue = orderRepository.getTotalRevenue();

        // ✅ Get pending orders
        Long pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING).stream().count();

        // ✅ Get low stock products (threshold = 5)
        Long lowStockProducts = (long) productRepository.findLowStockProducts(5).size();

        log.info("📊 Dashboard stats fetched: Users={}, Products={}, Orders={}, Revenue={}", totalUsers, totalProducts, totalOrders,totalRevenue);

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue !=null ? totalRevenue:0.0)
                .pendingOrders(pendingOrders)
                .lowStockProducts(lowStockProducts)
                .build();
    }

    // ============================================================
    // 2️⃣ GET ORDERS IN LAST N DAYS
    // ============================================================
    public Long getOrdersInLastDays(int days) {
        LocalDateTime date = LocalDateTime.now().minusDays(days);
        return (long) orderRepository.findOrdersFromLastDays(date).size();
    }

    // ============================================================
    // 3️⃣ GET TOTAL REVENUE
    // ============================================================
    public Double getTotalRevenue() {
        Double revenue = orderRepository.getTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }

    // ============================================================
    // 4️⃣ GET TOTAL USERS
    // ============================================================
    public Long getTotalUsers() {
        return userRepository.count();
    }

    // ============================================================
    // 5️⃣ GET TOTAL PRODUCTS
    // ============================================================
    public Long getTotalProducts() {
        return productRepository.count();
    }

    // ============================================================
    // 6️⃣ GET PENDING ORDERS COUNT
    // ============================================================
    public Long getPendingOrdersCount() {
        return (long) orderRepository.findByStatus(OrderStatus.PENDING).size();
    }

    // ============================================================
    // 7️⃣ GET LOW STOCK PRODUCTS COUNT
    // ============================================================
    public Long getLowStockProductsCount(int threshold) {
        return (long) productRepository.findLowStockProducts(threshold).size();
    }

    // ============================================================
    // 👑 ADMIN — Register User with Role
    // ============================================================
    @Transactional
    public String registerUserWithRole(AdminRegisterRequest request) {

        // ✅ Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Email already registered!");
        }

        // ✅ Validate role (only WAREHOUSE_MANAGER or ADMIN allowed)
        if (request.getRole() == Role.CUSTOMER) {
            throw new BusinessException("Use /register for CUSTOMER role");
        }

        // ✅ Create user with specified role
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        userRepository.save(user);
        log.info("✅ Admin created new user with role: {}", request.getRole());

        return "User registered successfully with role: " + request.getRole();
    }

    // ============================================================
    // 👥 ADMIN — Get all users
    // ============================================================
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // 👥 ADMIN — Get user by ID
    // ============================================================
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return mapToUserResponse(user);
    }

    // ============================================================
    // 👥 ADMIN — Update user role
    // ============================================================
    @Transactional
    public UserResponse updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // ✅ Prevent admin from downgrading themselves
         String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
         User currentUser = userRepository.findByEmail(currentUserEmail)
                 .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
         if (currentUser.getId().equals(userId)) {
             throw new BusinessException("You cannot change your own role");
         }

        user.setRole(newRole);
        User updatedUser = userRepository.save(user);
        log.info("✅ User role updated: {} → {}", updatedUser.getEmail(), newRole);
        return mapToUserResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // ✅ Prevent admin from deleting themselves
         String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
         User currentUser = userRepository.findByEmail(currentUserEmail)
                 .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
         if (currentUser.getId().equals(userId)) {
             throw new BusinessException("You cannot delete your own account");
         }

        // ✅ First, delete all refresh tokens for this user
        refreshTokenRepository.deleteByUserId(userId);

        // ✅ Step 2: Get all orders of this user
        List<Order> orders = orderRepository.findByUserId(userId);

        // ✅ Step 3: Delete order_items first (via OrderItemRepository)
        for (Order order : orders) {
            orderItemRepository.deleteByOrderId(order.getId());
        }

        // ✅ Delete orders (cascade will handle order_items)
        orderRepository.deleteByUserId(userId);

        // 5. Delete inventory logs
        inventoryLogRepository.deleteByChangedById(userId);

        userRepository.delete(user);
        log.info("🗑️ User deleted: {} ({})", user.getName(), user.getEmail());
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================
    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
