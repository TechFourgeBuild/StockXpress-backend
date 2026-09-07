package com.sxp.StockXpress.service;

import com.sxp.StockXpress.dto.OrderItemRequest;
import com.sxp.StockXpress.dto.OrderItemResponse;
import com.sxp.StockXpress.dto.OrderRequest;
import com.sxp.StockXpress.dto.OrderResponse;
import com.sxp.StockXpress.entity.Order;
import com.sxp.StockXpress.entity.OrderItem;
import com.sxp.StockXpress.entity.Product;
import com.sxp.StockXpress.entity.User;
import com.sxp.StockXpress.enums.ChangeType;
import com.sxp.StockXpress.enums.OrderStatus;
import com.sxp.StockXpress.enums.PaymentStatus;
import com.sxp.StockXpress.exception.BusinessException;
import com.sxp.StockXpress.exception.ResourceNotFoundException;
import com.sxp.StockXpress.repository.OrderItemRepository;
import com.sxp.StockXpress.repository.OrderRepository;
import com.sxp.StockXpress.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final InventoryLogService inventoryLogService;


    //  PLACE ORDER — WITH OPTIMISTIC LOCKING & TRANSACTION
    @Transactional
    @Retryable(
            value = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public OrderResponse placeOrder(OrderRequest request) {
        //  Get current logged-in user
        User currentUser = userService.getCurrentUser();

        //  Generate unique order number
        String orderNumber = generateOrderNumber();

        //  Create Order entity
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(currentUser)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .totalAmount(0.0)
                .build();

        //  Save order first to get ID
        Order savedOrder = orderRepository.save(order);

        //  Process each time
        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0.0;

        for(OrderItemRequest itemRequest : request.getItems()) {
            //  Get product with version
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(()->new ResourceNotFoundException(
                            "Product not found with ID: "+itemRequest.getProductId()
                    ));

            //  check stock availability
            if(product.getAvailableQuantity()<itemRequest.getQuantity()) {
                throw new BusinessException(
                        "Insufficient stock for product: "+product.getName()+
                                ". Available: "+product.getAvailableQuantity()+", Requested:  "+itemRequest.getQuantity()
                );
            }

            //  Deduct stock using Optimistic Locking
            int updateRows = productRepository.updateStockWithVersion(
                    product.getId(),
                    product.getAvailableQuantity()-itemRequest.getQuantity(),
                    product.getVersion()
            );

            if(updateRows == 0){
                log.warn("⚠️ Optimistic locking failure for product: {}",product.getName());
                throw new OptimisticLockingFailureException(
                        "Stock update failed for product: "+product.getName()+". Please retry."
                );
            }

            //  Refresh product to get updated version and quantity
            Product updateProduct = productRepository.findById(product.getId())
                    .orElseThrow(()->new ResourceNotFoundException(
                            "Product not found  with ID: "+product.getId()
                    ));

            //  Create OrderItem
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(updateProduct)
                    .quantity(itemRequest.getQuantity())
                    .pricePerUnit(updateProduct.getPrice())
                    .totalPrice(updateProduct.getPrice()*itemRequest.getQuantity())
                    .build();

            orderItems.add(orderItem);
            totalAmount += orderItem.getTotalPrice();

            //  Log inventory change (DEDUCTED)
            inventoryLogService.logStockChange(
                    product.getId(),
                    currentUser.getId(),
                    product.getAvailableQuantity(),
                    updateProduct.getAvailableQuantity(),
                    ChangeType.DEDUCTED,
                    "Order placed - "+orderNumber
            );
        }

        //  Save all OrderItems
        orderItemRepository.saveAll(orderItems);

        //  Update order total amount
        savedOrder.setTotalAmount(totalAmount);
        savedOrder.setPaymentStatus(PaymentStatus.PAID);  //  Assuming payment success
        savedOrder.setStatus(OrderStatus.CONFIRMED);

        Order finalOrder = orderRepository.save(savedOrder);

        log.info("✅ Order placed successfully: {} (User: {}, Total: ₹{})",orderNumber, currentUser.getEmail(), totalAmount);

        return mapToResponse(finalOrder, orderItems);
    }


    //  GET ORDER BY ID
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Order not found with ID: "+id));

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(id);
        return mapToResponse(order, orderItems);
    }


    //  GET ORDER BY ORDER NUMBER
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(()->new ResourceNotFoundException(
                        "Order not found with number: "+orderNumber
                ));

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        return mapToResponse(order, orderItems);
    }


    //  GET ALL ORDERS OF LOGGED-IN USER
    public List<OrderResponse> getUserOrders() {
        User currentUser = userService.getCurrentUser();
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());

        return orders.stream()
                .map(order->{
                    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                    return mapToResponse(order, items);
                }).collect(Collectors.toList());
    }


    //  GET ALL ORDERS (ADMIN / WAREHOUSE_MANAGER)
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .map(order->{
                    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                    return mapToResponse(order, items);
                }).collect(Collectors.toList());
    }


    //  CANCEL ORDER — RESTORE STOCK
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(()->new ResourceNotFoundException("Order not found with ID: "+orderId));

        //  Check if order can be cancelled
        if(order.getStatus()==OrderStatus.DELIVERED || order.getStatus()==OrderStatus.CANCELLED) {
            throw new BusinessException(
                    "Order cannot be cancelled. Current status: "+order.getStatus()
            );
        }

        User currentUser = userService.getCurrentUser();

        //  Restore stock for each item
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        for(OrderItem item : orderItems) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(()-> new ResourceNotFoundException(
                            "Product not found with ID: "+item.getProduct().getId()
                    ));

            Integer oldQuantity = product.getAvailableQuantity();
            Integer newQuantity = oldQuantity + item.getQuantity();

            //  Update stock using Optimistic Locking
            int updateRows = productRepository.updateStockWithVersion(
                    product.getId(),
                    newQuantity,
                    product.getVersion()
            );

            if(updateRows == 0) {
                log.warn("⚠️ Optimistic locking failure while cancelling order: {}",order.getOrderNumber());

                throw new OptimisticLockingFailureException(
                        "Stock update failed while cancelling order. Please retry."
                );
            }

            Product updatedProduct = productRepository.findById(product.getId())
                    .orElseThrow(()->new ResourceNotFoundException(
                            "Product not found with ID: "+product.getId()
                    ));

            //  Log inventory change (ADDED)
            inventoryLogService.logStockChange(
                    product.getId(),
                    currentUser.getId(),
                    oldQuantity,
                    newQuantity,
                    ChangeType.ADDED,
                    "Order cancelled - "+order.getOrderNumber()
            );
        }

        //  Update order status
        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);

        log.info("🪣 Order cancelled: {} (User: {})", order.getOrderNumber(), currentUser.getEmail());

        return mapToResponse(cancelledOrder, orderItems);
    }


    //  UPDATE ORDER STATUS (ADMIN / WAREHOUSE_MANAGER)
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(()->new ResourceNotFoundException("Order not found with ID: "+orderId));

        //  Validate status transition
        if(order.getStatus()==OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
            throw new BusinessException("Cancelled order cannot be updated to: "+newStatus);
        }

        if(order.getStatus()==OrderStatus.DELIVERED && newStatus != OrderStatus.DELIVERED) {
            throw new BusinessException("Delivered order cannot be updated to: "+newStatus);
        }

        order.setStatus(newStatus);

        //  If delivered, set delivery date
        if(newStatus == OrderStatus.DELIVERED) {
            order.setDeliveryDate(LocalDateTime.now());
        }

        Order updatedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        log.info("📦 Order status updated: {} -> {} (Order: {})", order.getOrderNumber(), order.getStatus(), newStatus);

        return mapToResponse(updatedOrder, orderItems);
    }


    //  UPDATE PAYMENT STATUS (ADMIN)
    @Transactional
    public OrderResponse updatePaymentStatus(Long orderId, PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(()->new ResourceNotFoundException("Order not found with ID: "+orderId));

        order.setPaymentStatus(paymentStatus);

        //  If payment successful, confirm order
        if(paymentStatus == PaymentStatus.PAID) {
            order.setStatus(OrderStatus.CONFIRMED);
        }

        Order updatedOrder = orderRepository.save(order);
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        log.info("💳 Payment status updated: {} -> {} (Order: {})", order.getOrderNumber(), paymentStatus, order.getOrderNumber());

        return mapToResponse(updatedOrder, orderItems);
    }


    // PRIVATE HELPERS
    private String generateOrderNumber() {
        return "ORD-"+ UUID.randomUUID().toString().substring(0,8).toUpperCase();
    }

    public OrderResponse mapToResponse(Order order, List<OrderItem> orderItems) {
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item-> OrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .pricePerUnit(item.getPricePerUnit())
                        .totalPrice(item.getTotalPrice())
                        .build()
                ).collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .shippingAddress(order.getShippingAddress())
                .deliveryDate(order.getDeliveryDate())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }

}
