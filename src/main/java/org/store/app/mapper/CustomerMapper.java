package org.store.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.store.app.dto.CustomerDTO;
import org.store.app.model.Customer;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {

    Customer toEntity(CustomerDTO customerDTO);
    CustomerDTO toDto(Customer customer);
}
