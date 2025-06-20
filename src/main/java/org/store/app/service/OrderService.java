package org.store.app.service;

import org.store.app.common.ValueWrapper;
import org.store.app.dto.OrderDTO;
import org.store.app.dto.OrderResponseCreatedDTO;
import org.store.app.enums.OrderStatus;
import org.store.app.model.Order;

import java.util.List;

public interface OrderService {

    ValueWrapper<List<OrderDTO>> getOrdersByCustomerAndStatus(Long customerId, OrderStatus status);

    OrderResponseCreatedDTO createOrder(Long billingAddressId, Long customerId);

    Order updateOrderStatus(Long orderId, OrderStatus status, Long customerId);

     void sendOrderConfirmationEmail(Long orderId, String currency);

}
