package org.store.app.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.store.app.dto.CustomerAddressDTO;
import org.store.app.model.CustomerAddress;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerAddressMapper {


    @Mapping(source = "customer.id", target = "customerId")
    CustomerAddressDTO toDto(CustomerAddress address);


    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CustomerAddress toEntity(CustomerAddressDTO dto);
}
