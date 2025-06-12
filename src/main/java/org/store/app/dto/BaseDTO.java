package org.store.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaseDTO implements Serializable {

    @Schema(description = "Unique identifier of the entity", example = "1")
    private Long id;
    @Schema(description = "Timestamp when the entity was created", example = "2025-06-08T12:34:56")
    private LocalDateTime createdAt;
    @Schema(description = "Timestamp when the entity was last updated", example = "2025-06-08T13:00:00")
    private LocalDateTime updatedAt;
}
