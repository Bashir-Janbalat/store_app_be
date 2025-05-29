package org.store.app.service;

import org.store.app.dto.CustomerDTO;
import org.store.app.dto.LoginDTO;

public interface AuthService {
    String login(LoginDTO loginDto);
    void signup(CustomerDTO customerDto);

}
