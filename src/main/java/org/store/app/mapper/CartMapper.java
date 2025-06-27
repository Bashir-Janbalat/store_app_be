package org.store.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.store.app.dto.CartDTO;
import org.store.app.dto.CartItemDTO;
import org.store.app.dto.ProductInfoDTO;
import org.store.app.model.Cart;
import org.store.app.projection.CartItemProductProjection;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CartMapper {

    @Mapping(target = "itemDTOS", source = "cart.items")
    @Mapping(target = "cartId", source = "cart.id")
    CartDTO toDto(Cart cart);

    default CartDTO toDtoFromProjections(Long cartId, List<CartItemProductProjection> cartItemProductProjections) {
        CartDTO cartDTO = new CartDTO();
        cartDTO.setCartId(cartId);
        List<CartItemDTO> itemDTOS = new ArrayList<>();
        for (CartItemProductProjection p : cartItemProductProjections) {
            ProductInfoDTO productInfoDTO = new ProductInfoDTO(p.getName(), p.getDescription(), p.getImageUrl(), p.getTotalStock());
            CartItemDTO itemDTO = new CartItemDTO(p.getProductId(), p.getQuantity(), p.getUnitPrice(), productInfoDTO);
            itemDTOS.add(itemDTO);
        }
        cartDTO.setItemDTOS(itemDTOS);
        return cartDTO;
    }
}
