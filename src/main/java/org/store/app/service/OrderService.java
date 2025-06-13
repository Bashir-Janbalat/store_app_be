package org.store.app.service;

import org.store.app.dto.OrderDTO;
import org.store.app.dto.OrderResponseCreatedDTO;
import org.store.app.enums.OrderStatus;

import java.util.List;

public interface OrderService {

    List<OrderDTO> getOrdersByCustomerAndStatus(Long customerId,OrderStatus status);

    OrderResponseCreatedDTO createOrder(OrderDTO orderDTO, Long customerId);

    void updateOrderStatus(Long orderId, OrderStatus status);

    void sendOrderConfirmationEmail(Long orderId, String currency);

}
