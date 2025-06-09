package org.store.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "Request DTO to reset the password using a reset token and new password")
public class PasswordResetRequestDTO implements Serializable {
    @NotBlank
    @Schema(description = "Password reset token", example = "f4a8c1e7-9d2b-4a1e-9f6a-123456789abc")
    private String token;

    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter")
    @Pattern(regexp = ".*[a-z].*", message = "Password must contain at least one lowercase letter")
    @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one number")
    @Pattern(regexp = ".*[@#$%^&+=].*", message = "Password must contain at least one special character")
    @Schema(description = "New password (min 8 chars, including uppercase, lowercase, number and special character)", example = "NewPassw0rd!")
    private String newPassword;
}
