package org.store.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "customer_addresses")
@EqualsAndHashCode(callSuper = true)
@Data
public class CustomerAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer; // Angenommen Customer Entity existiert

    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
