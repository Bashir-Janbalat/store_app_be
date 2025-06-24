package org.store.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "Data transfer object for customer information")
public class CustomerDTO extends BaseDTO implements Serializable {

    @NotEmpty(message = "Name is required")
    @Schema(description = "Full name of the customer", example = "John Doe")
    private String name;

    @NotEmpty(message = "Email is required")
    @Email(message = "Invalid email format. Please try again with a valid email.")
    @Schema(description = "Email address of the customer", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Country code (ISO Alpha-2 format)", example = "DE")
    @Size(min = 2, max = 2, message = "Country code must be 2 characters (ISO Alpha-2)")
    private String countryCode;

    @Schema(description = "International dialing code", example = "+1")
    @Pattern(regexp = "^\\+\\d{1,5}$", message = "Dial code must start with '+' followed by 1 to 5 digits")
    private String dialCode;

    @Schema(description = "Phone number of the customer", example = "+1234567890")
    private String phone;

    @NotNull(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter")
    @Pattern(regexp = ".*[a-z].*", message = "Password must contain at least one lowercase letter")
    @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one number")
    @Pattern(regexp = ".*[@#$%^&+=].*", message = "Password must contain at least one special character")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "Password for the customer account (write-only)", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Builder.Default
    @Schema(description = "Set of roles assigned to the customer")
    private Set<RoleDTO> roles = new HashSet<>();

}
