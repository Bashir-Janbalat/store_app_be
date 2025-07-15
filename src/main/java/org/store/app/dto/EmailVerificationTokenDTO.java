package org.store.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailVerificationTokenDTO {

    private String token;
    private String email;
    private boolean used;
}
