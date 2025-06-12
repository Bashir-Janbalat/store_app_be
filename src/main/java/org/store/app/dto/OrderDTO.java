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
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Long shippingAddressId;
    private CustomerAddressDTO shippingAddress;
    private CustomerAddressDTO billingAddress;
    private Long billingAddressId;
    private List<OrderItemDTO> items;
}