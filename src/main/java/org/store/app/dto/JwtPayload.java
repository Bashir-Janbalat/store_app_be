package org.store.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtPayload {
    private Long id;
    private String email;
    private String name;
    private List<String> roles;
}