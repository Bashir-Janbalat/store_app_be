package org.store.app.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AuthResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String accessToken;
}
