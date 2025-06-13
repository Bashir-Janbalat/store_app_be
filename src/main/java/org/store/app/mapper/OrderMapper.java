package org.store.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.store.app.dto.OrderDTO;
import org.store.app.model.Order;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {


    @Mapping(source = "cart.id",target = "cartId")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "shippingAddress.id", target = "shippingAddressId")
    @Mapping(source = "billingAddress.id", target = "billingAddressId")
    @Mapping(source = "shippingAddress.customer.id", target = "shippingAddress.customerId")
    @Mapping(source = "billingAddress.customer.id", target = "billingAddress.customerId")
    OrderDTO toDto(Order order);

    @Mapping(source = "cartId", target = "cart.id")
    @Mapping(source = "customerId", target = "customer.id")
    @Mapping(source = "shippingAddressId", target = "shippingAddress.id")
    @Mapping(source = "billingAddressId", target = "billingAddress.id")
    Order toEntity(OrderDTO orderDTO);
}
