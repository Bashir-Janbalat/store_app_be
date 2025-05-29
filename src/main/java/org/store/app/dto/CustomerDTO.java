package org.store.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO extends BaseDTO implements Serializable {

    @NotEmpty(message = "Name is required")
    private String name;
    @NotEmpty(message = "Email is required")
    @Email(message = "Invalid email format. Please try again with a valid email.")
    private String email;
    private String phone;
    @NotNull(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter")
    @Pattern(regexp = ".*[a-z].*", message = "Password must contain at least one lowercase letter")
    @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one number")
    @Pattern(regexp = ".*[@#$%^&+=].*", message = "Password must contain at least one special character")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    @Builder.Default
    private Set<RoleDTO> roles = new HashSet<>();

}
