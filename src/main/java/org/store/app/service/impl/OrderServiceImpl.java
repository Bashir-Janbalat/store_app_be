package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.dto.OrderDTO;
import org.store.app.dto.OrderItemDTO;
import org.store.app.dto.OrderResponseCreatedDTO;
import org.store.app.dto.ProductInfoDTO;
import org.store.app.enums.AddressType;
import org.store.app.enums.CartStatus;
import org.store.app.enums.OrderStatus;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.mapper.OrderItemMapper;
import org.store.app.mapper.OrderMapper;
import org.store.app.model.Cart;
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


    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCustomerAndStatus(Long customerId, OrderStatus status) {
        List<Order> orders = orderRepository.findByCustomerIdAndStatus(customerId, status);

        List<OrderDTO> ordersDTOS = orders.stream().map(orderMapper::toDto).toList();

        Set<Long> productIds = ordersDTOS.stream()
                .flatMap(orderDTO -> orderDTO.getItems().stream())
                .map(OrderItemDTO::getProductId)
                .collect(Collectors.toSet());

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
        return ordersDTOS;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponseCreatedDTO createOrder(OrderDTO orderDTO, Long customerId) {
        log.info("Creating order for customerId: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (orderDTO.getShippingAddressId() == null) {
            Long defaultShippingId = customerAddressService.getDefaultAddressId(customerId, AddressType.SHIPPING);
            log.info("Using default shipping address with ID: {}", defaultShippingId);
            orderDTO.setShippingAddressId(defaultShippingId);
        } else {
            log.info("Verifying provided shipping address ID: {}", orderDTO.getShippingAddressId());
            customerAddressService.verifyAddressOwnership(orderDTO.getShippingAddressId(), customerId);
        }

        if (orderDTO.getBillingAddressId() == null) {
            Long defaultBillingId = customerAddressService.getDefaultAddressId(customerId, AddressType.BILLING);
            log.info("Using default billing address with ID: {}", defaultBillingId);
            orderDTO.setBillingAddressId(defaultBillingId);
        } else {
            log.info("Verifying provided billing address ID: {}", orderDTO.getBillingAddressId());
            customerAddressService.verifyAddressOwnership(orderDTO.getBillingAddressId(), customerId);
        }

        Order order = orderMapper.toEntity(orderDTO);
        order.setCustomer(customer);

        log.info("Fetching cart with ID: {}", orderDTO.getCartId());
        Cart cart = cartService.getCartById(orderDTO.getCartId(), CartStatus.ACTIVE, customer.getId());

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> {
                    OrderItem item = orderItemMapper.toEntityFromCartItem(cartItem);
                    item.setOrder(order);
                    return item;
                }).toList();
        order.setItems(orderItems);

        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        order.setStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}, Total amount: {}", savedOrder.getId(), savedOrder.getTotalAmount());
        return new OrderResponseCreatedDTO(savedOrder.getId(), savedOrder.getTotalAmount());
    }


    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        log.info("Updating order status. Order ID: {}, New Status: {}", orderId, status);
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setStatus(status);
        orderRepository.save(order);
        log.info("Order status updated successfully. Order ID: {}, Status: {}", orderId, status);
    }

    @Override
    public void sendOrderConfirmationEmail(Long orderId, String currency) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

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
                        """,
                customer.getName(),
                order.getId(),
                order.getTotalAmount(),
                currency,
                order.getShippingAddress().getAddressLine(),
                order.getShippingAddress().getCity(),
                order.getShippingAddress().getCountry()
        );

        emailService.sendSimpleMail(customer.getEmail(), subject, body);
        log.info("Order confirmation email sent to {}", customer.getEmail());
    }
}
