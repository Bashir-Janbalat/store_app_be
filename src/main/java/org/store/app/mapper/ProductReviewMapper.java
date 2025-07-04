package org.store.app.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.store.app.dto.ProductReviewDTO;
import org.store.app.model.ProductReview;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductReviewMapper {

    @Mapping(source = "customer.name", target = "reviewerName")
    ProductReviewDTO toDto(ProductReview entity);


}
