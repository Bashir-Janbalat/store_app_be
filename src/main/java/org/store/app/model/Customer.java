package org.store.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Entity
@Table(name = "customers")
@EqualsAndHashCode(callSuper = true)
@Data
public class Customer extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    private String name;
    private String phone;
    private String password;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "dial_code", length = 10)
    private String dialCode;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "customer_roles",
            joinColumns = @JoinColumn(name = "customer_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Set<Role> roles;

}