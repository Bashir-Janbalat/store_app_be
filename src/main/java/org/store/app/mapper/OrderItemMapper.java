package org.store.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.store.app.dto.OrderItemDTO;
import org.store.app.model.CartItem;
import org.store.app.model.OrderItem;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderItemMapper {

    /* معلومات المنتج سيتم جلبهم بشكل منفصل */
    @Mapping(target = "ProductInfo", ignore = true)
    OrderItemDTO toDto(OrderItem orderItem);

    default OrderItem toEntityFromCartItem(CartItem cartItem) {
        if (cartItem == null) return null;
        OrderItem entity = new OrderItem();
        entity.setProductId(cartItem.getProductId());
        entity.setQuantity(cartItem.getQuantity());
        entity.setUnitPrice(cartItem.getUnitPrice());
        entity.setTotalPrice(cartItem.getUnitPrice().multiply(java.math.BigDecimal.valueOf(cartItem.getQuantity())));
        return entity;
    }
}
