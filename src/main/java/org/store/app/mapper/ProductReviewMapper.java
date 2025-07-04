package org.store.app.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.store.app.dto.ProductReviewDTO;
import org.store.app.model.ProductReview;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductReviewMapper {

    @Mapping(source = "customer.name", target = "customer.name")
    @Mapping(source = "customer.email", target = "customer.email")
    @Mapping(source = "customer.phone", target = "customer.phone")
    @Mapping(source = "customer.countryCode", target = "customer.countryCode")
    @Mapping(source = "customer.dialCode", target = "customer.dialCode")
    @Mapping(source = "customer.roles", target = "customer.roles")
    ProductReviewDTO toDto(ProductReview entity);

    ProductReview toEntity(ProductReviewDTO dto);

}
