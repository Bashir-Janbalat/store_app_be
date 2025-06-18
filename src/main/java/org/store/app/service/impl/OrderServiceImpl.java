package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.common.ValueWrapper;
import org.store.app.dto.*;
import org.store.app.enums.AddressType;
import org.store.app.enums.OrderStatus;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.mapper.OrderItemMapper;
import org.store.app.mapper.OrderMapper;
import org.store.app.model.Customer;
import org.store.app.model.Order;
import org.store.app.model.OrderItem;
import org.store.app.projection.ProductInfoProjection;
import org.store.app.repository.CustomerRepository;
import org.store.app.repository.OrderRepository;
import org.store.app.service.CartService;
import org.store.app.service.CustomerAddressService;
import org.store.app.service.EmailService;
import org.store.app.service.OrderService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CustomerRepository customerRepository;
    private final CustomerAddressService customerAddressService;
    private final EmailService emailService;
    private final CartService cartService;
    private final OrderItemMapper orderItemMapper;
    private final CacheManager cacheManager;


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#customerId + '-' + #status")
    public ValueWrapper<List<OrderDTO>> getOrdersByCustomerAndStatus(Long customerId, OrderStatus status) {
        List<Order> orders = orderRepository.findByCustomerIdAndStatus(customerId, status);

        List<OrderDTO> ordersDTOS = orders.stream().map(orderMapper::toDto).toList();

        Set<Long> productIds = ordersDTOS.stream()
                .flatMap(orderDTO -> orderDTO.getItems().stream())
                .map(OrderItemDTO::getProductId).collect(Collectors.toSet());

        Map<Long, ProductInfoDTO> productInfoMap = orderRepository.findProductInfosByIds(productIds)
                .stream()
                .collect(Collectors.toMap(
                        ProductInfoProjection::getProductId,
                        p -> new ProductInfoDTO(p.getName(), p.getDescription(), p.getImageUrl()),
                        (existing, replacement) -> existing // في حالة وجود نفس المفتاح، نحتفظ بالقيمة الأولى
        ));

        for (OrderDTO orderDTO : ordersDTOS) {
            for (OrderItemDTO item : orderDTO.getItems()) {
                ProductInfoDTO productInfoDTO = productInfoMap.get(item.getProductId());
                if (productInfoDTO == null) {
                    throw new ResourceNotFoundException("Product not found with id: " + item.getProductId());
                }
                item.setProductInfo(productInfoDTO);
            }
        }
        log.info("Found {} orders for customer id: {}", ordersDTOS.size(), customerId);
        return new ValueWrapper<>(ordersDTOS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {@CacheEvict(value = "orders", key = "#customerId + '-PENDING'"), @CacheEvict(value = "orders", key = "#customerId + '-PROCESSING'"), @CacheEvict(value = "orders", key = "#customerId + '-SHIPPED'"), @CacheEvict(value = "orders", key = "#customerId + '-DELIVERED'"), @CacheEvict(value = "orders", key = "#customerId + '-CANCELLED'")})
    public OrderResponseCreatedDTO createOrder(OrderDTO orderDTO, Long customerId) {
        log.info("Creating order for customerId: {}", customerId);
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (orderDTO.getShippingAddressId() == null) {
            Long defaultShippingId = customerAddressService.getDefaultAddressId(customerId, AddressType.SHIPPING).getValue();
            log.info("Using default shipping address with ID: {}", defaultShippingId);
            orderDTO.setShippingAddressId(defaultShippingId);
        } else {
            log.info("Verifying provided shipping address ID: {}", orderDTO.getShippingAddressId());
            customerAddressService.verifyAddressOwnership(orderDTO.getShippingAddressId(), customerId);
        }

        if (orderDTO.getBillingAddressId() == null) {
            Long defaultBillingId = customerAddressService.getDefaultAddressId(customerId, AddressType.BILLING).getValue();
            log.info("Using default billing address with ID: {}", defaultBillingId);
            orderDTO.setBillingAddressId(defaultBillingId);
        } else {
            log.info("Verifying provided billing address ID: {}", orderDTO.getBillingAddressId());
            customerAddressService.verifyAddressOwnership(orderDTO.getBillingAddressId(), customerId);
        }
        log.info("Fetching cart with ID: {}", orderDTO.getCartId());
        CartDTO cart = cartService.getActiveCart(customer.getEmail(), null);
        if (cart != null && (cart.getItemDTOS() == null || cart.getItemDTOS().isEmpty())) {
            throw new ResourceNotFoundException("Cart is empty");
        }
        Order order = orderMapper.toEntity(orderDTO);
        order.setCustomer(customer);


        List<OrderItem> orderItems = cart.getItemDTOS().stream().map(cartItemDTO -> {
            OrderItem item = orderItemMapper.toEntityFromCartItemDTO(cartItemDTO);
            item.setOrder(order);
            return item;
        }).toList();
        order.setItems(orderItems);

        BigDecimal totalAmount = orderItems.stream().map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        order.setStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}, Total amount: {}", savedOrder.getId(), savedOrder.getTotalAmount());
        return new OrderResponseCreatedDTO(savedOrder.getId(), savedOrder.getTotalAmount());
    }


    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, Long currentCustomerId) {
        log.info("Updating order status. Order ID: {}, New Status: {}", orderId, newStatus);
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Long orderOwnerId = order.getCustomer().getId();

        if (!orderOwnerId.equals(currentCustomerId)) {
            throw new AccessDeniedException("You are not allowed to update this order.");
        }
        OrderStatus oldStatus = order.getStatus();

        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new IllegalStateException("Invalid status transition from " + oldStatus + " to " + newStatus);
        }
        if (oldStatus == newStatus) {
            log.info("Order already has status '{}', skipping update", newStatus);
            return order;
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        log.info("Order status updated successfully. Order ID: {}, Status: {}", orderId, newStatus);

        String oldKey = orderOwnerId + "-" + oldStatus;
        String newKey = orderOwnerId + "-" + newStatus;

        var cache = cacheManager.getCache("orders");
        if (cache != null) {
            cache.evict(oldKey);
            cache.evict(newKey);
            log.info("Cache evicted for keys: '{}' and '{}'", oldKey, newKey);
        } else {
            log.warn("Cache 'orders' not found. Skipped eviction for keys: '{}' and '{}'", oldKey, newKey);
        }
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public void sendOrderConfirmationEmail(Long orderId, String currency) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        Customer customer = order.getCustomer();
        if (customer == null || customer.getEmail() == null) {
            log.warn("No customer email found for order ID {}", orderId);
            return;
        }

        String subject = "Order Confirmation #" + order.getId();
        String body = String.format("""
                Dear %s,
                
                Thank you for your purchase!
                Your order #%d has been confirmed and is now being processed.
                
                Total Amount: %s %s
                
                Shipping to: %s, %s, %s
                
                Best regards,
                Your Online Store
                """, customer.getName(), order.getId(), order.getTotalAmount(), currency, order.getShippingAddress().getAddressLine(), order.getShippingAddress().getCity(), order.getShippingAddress().getCountry());

        emailService.sendSimpleMail(customer.getEmail(), subject, body);
        log.info("Order confirmation email sent to {}", customer.getEmail());
    }

    private boolean isValidStatusTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PENDING -> to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED;
            case PROCESSING -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
