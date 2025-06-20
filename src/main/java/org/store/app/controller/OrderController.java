package org.store.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.store.app.common.ValueWrapper;
import org.store.app.dto.OrderDTO;
import org.store.app.dto.OrderResponseCreatedDTO;
import org.store.app.enums.OrderStatus;
import org.store.app.security.userdetails.CustomUserDetails;
import org.store.app.service.OrderService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order API", description = "Operations related to customer orders")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create a new order for the current customer")
    @PostMapping
    public ResponseEntity<OrderResponseCreatedDTO> createOrder(@Parameter(description = "the ID of billing Address ") @RequestParam(required = false) Long billingAddressId,
                                                               @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        OrderResponseCreatedDTO response = orderService.createOrder(billingAddressId, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Cancel an order manually")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable Long orderId, @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, userDetails.getId());
            return ResponseEntity.ok("Order " + orderId + " has been cancelled.");
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to cancel this order.");
        } catch (Exception e) {
            log.error("Failed to cancel order {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel order");
        }
    }

    @Operation(summary = "Get all orders for the current customer filtered by status")
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrdersForCurrentCustomer(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails, @Parameter(description = "Filter orders by status") @RequestParam(defaultValue = "PROCESSING") OrderStatus status) {
        ValueWrapper<List<OrderDTO>> orderDTOS = orderService.getOrdersByCustomerAndStatus(userDetails.getId(), status);
        return ResponseEntity.ok(orderDTOS.getValue());
    }
}
