package org.store.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.store.app.enums.OrderStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrderDTO extends BaseDTO implements Serializable {

    private Long customerId;
    private Long cartId;

    private Long shippingAddressId;
    private CustomerAddressDTO shippingAddress;
    private Long billingAddressId;
    private CustomerAddressDTO billingAddress;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private List<OrderItemDTO> items;
}