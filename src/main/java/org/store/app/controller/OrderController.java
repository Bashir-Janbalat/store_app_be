package org.store.app.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.store.app.dto.OrderDTO;
import org.store.app.dto.OrderResponseCreatedDTO;
import org.store.app.security.userdetails.CustomUserDetails;
import org.store.app.service.OrderService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseCreatedDTO> createOrder(@RequestBody OrderDTO orderDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        if (!orderDTO.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        OrderResponseCreatedDTO response = orderService.createOrder(orderDTO, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrdersForCurrentCustomer(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<OrderDTO> orderDTOS = orderService.getAllOrdersForCurrentCustomer(userDetails.getId());
        return ResponseEntity.ok(orderDTOS);
    }
}
