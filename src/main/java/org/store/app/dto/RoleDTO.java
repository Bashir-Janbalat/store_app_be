package org.store.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Role data transfer object representing Customer roles")
public class RoleDTO implements Serializable {

    @Schema(description = "Unique identifier of the role", example = "1")
    private Long id;
    @NotBlank
    @Schema(description = "Name of the role", example = "ROLE_CUSTOMER")
    private String name;
}
