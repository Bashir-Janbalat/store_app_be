package org.store.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseDTO implements Serializable {

    private Long id;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
