package org.store.app.service;

import org.store.app.dto.CustomerDTO;

public interface CustomerService {

    CustomerDTO createCustomer(CustomerDTO customerDTO);
    void updatePassword(String email, String newPassword);
}
