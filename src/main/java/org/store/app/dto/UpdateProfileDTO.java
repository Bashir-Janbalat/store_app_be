package org.store.app.dto;


import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UpdateProfileDTO {

    @NotEmpty(message = "Name is required")
    private String name;

    @NotEmpty(message = "Phone is required")
    private String phone;

    @NotEmpty(message = "Dial code is required")
    private String dialCode;

    @NotEmpty(message = "Country code is required")
    private String countryCode;
}