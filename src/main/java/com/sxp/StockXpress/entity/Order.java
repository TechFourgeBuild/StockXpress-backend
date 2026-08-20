package com.sxp.StockXpress.entity;

import com.sxp.StockXpress.enums.OrderStatus;
import com.sxp.StockXpress.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false)
    private String orderNumber;       // ✅ UUID — unique order reference

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;      // ✅ Who placed the order

    @Column(nullable = false)
    private Double totalAmount;   // ✅ Total bill amount

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;   // ✅ PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus; // ✅ PENDING, PAID, FAILED, REFUNDED

    @Column(length = 500)
    private String shippingAddress;    // ✅ Delivery address

    private LocalDateTime deliveryDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
