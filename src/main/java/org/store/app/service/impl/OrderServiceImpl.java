package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.store.app.dto.OrderDTO;
import org.store.app.dto.OrderResponseCreatedDTO;
import org.store.app.enums.AddressType;
import org.store.app.enums.OrderStatus;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.mapper.OrderMapper;
import org.store.app.model.Customer;
import org.store.app.model.Order;
import org.store.app.model.OrderItem;
import org.store.app.repository.CustomerRepository;
import org.store.app.repository.OrderRepository;
import org.store.app.service.CustomerAddressService;
import org.store.app.service.EmailService;
import org.store.app.service.OrderService;

import java.math.BigDecimal;
import java.util.List;
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


    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrdersForCurrentCustomer(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return orders.stream().map(orderMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponseCreatedDTO createOrder(OrderDTO orderDTO, Long customerId) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (orderDTO.getShippingAddressId() == null) {
            Long defaultShippingId = customerAddressService.getDefaultAddressId(customerId, AddressType.SHIPPING);
            orderDTO.setShippingAddressId(defaultShippingId);
        } else {
            customerAddressService.verifyAddressOwnership(orderDTO.getShippingAddressId(), customerId);
        }

        if (orderDTO.getBillingAddressId() == null) {
            Long defaultBillingId = customerAddressService.getDefaultAddressId(customerId, AddressType.BILLING);
            orderDTO.setBillingAddressId(defaultBillingId);
        } else {
            customerAddressService.verifyAddressOwnership(orderDTO.getBillingAddressId(), customerId);
        }

        Order order = orderMapper.toEntity(orderDTO);
        order.setCustomer(customer);

        if (!CollectionUtils.isEmpty(order.getItems())) {
            order.getItems().forEach(item -> {
                item.setOrder(order);
                if (item.getUnitPrice() != null && item.getQuantity() != null) {
                    BigDecimal totalPrice = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    item.setTotalPrice(totalPrice);
                } else {
                    item.setTotalPrice(BigDecimal.ZERO); // fallback
                }
            });

            BigDecimal totalAmount = order.getItems().stream().map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

            order.setTotalAmount(totalAmount);
        } else {
            order.setTotalAmount(BigDecimal.ZERO);
        }

        order.setStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);
        return new OrderResponseCreatedDTO(savedOrder.getId(), savedOrder.getTotalAmount());
    }


    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setStatus(status);
        orderRepository.save(order);
    }

    @Override
    public void sendOrderConfirmationEmail(Long orderId,String currency) {
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
