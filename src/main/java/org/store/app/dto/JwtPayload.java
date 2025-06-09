package org.store.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JWT payload containing Customer identity and roles")
public class JwtPayload {
    @Schema(description = "Customer ID", example = "123")
    private Long id;

    @Schema(description = "Customer email", example = "user@example.com")
    private String email;

    @Schema(description = "Customer full name", example = "John Doe")
    private String name;

    @Schema(description = "List of Customer roles", example = "[\"ROLE_CUSTOMER\"")
    private List<String> roles;
}