package org.store.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.store.app.dto.CartItemDTO;
import org.store.app.dto.OrderItemDTO;
import org.store.app.model.OrderItem;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderItemMapper {

    /* معلومات المنتج سيتم جلبهم بشكل منفصل */
    @Mapping(target = "ProductInfo", ignore = true)
    OrderItemDTO toDto(OrderItem orderItem);

    default OrderItem toEntityFromCartItemDTO(CartItemDTO cartItemDTO) {
        if (cartItemDTO == null) return null;
        OrderItem entity = new OrderItem();
        entity.setProductId(cartItemDTO.getProductId());
        entity.setQuantity(cartItemDTO.getQuantity());
        entity.setUnitPrice(cartItemDTO.getUnitPrice());
        entity.setTotalPrice(cartItemDTO.getUnitPrice().multiply(java.math.BigDecimal.valueOf(cartItemDTO.getQuantity())));
        return entity;
    }
}
