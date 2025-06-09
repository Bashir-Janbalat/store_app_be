package org.store.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "DTO containing the email address associated with a password reset token")
public class PasswordResetTokenDTO {

    @Schema(description = "Email address of the Customer requesting password reset", example = "user@example.com")
    private String email;
}
