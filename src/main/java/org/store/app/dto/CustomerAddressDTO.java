package org.store.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.store.app.enums.AddressType;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CustomerAddressDTO extends BaseDTO implements Serializable {

    private Long customerId;
    @NotBlank(message = "Address line must not be blank")
    @Size(max = 255, message = "Address line must be less than 255 characters")
    private String addressLine;
    @NotBlank(message = "City must not be blank")
    @Size(max = 100, message = "City must be less than 100 characters")
    private String city;
    @NotBlank(message = "State must not be blank")
    @Size(max = 100, message = "State must be less than 100 characters")
    private String state;
    @NotBlank(message = "Postal code must not be blank")
    @Size(max = 20, message = "Postal code must be less than 20 characters")
    private String postalCode;
    @NotBlank(message = "Country must not be blank")
    @Size(max = 100, message = "Country must be less than 100 characters")
    private String country;
    @Builder.Default
    private AddressType addressType = AddressType.SHIPPING;
    @Builder.Default
    @NotNull(message = "Default address flag must not be null")
    private Boolean defaultAddress = false;

}
