package org.store.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.store.app.enums.AddressType;

@Entity
@Table(name = "customer_addresses")
@EqualsAndHashCode(callSuper = true)
@Data
public class CustomerAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", columnDefinition = "ENUM('SHIPPING', 'BILLING') DEFAULT 'SHIPPING'")
    private AddressType addressType = AddressType.SHIPPING;

    @Column(name = "is_default", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean defaultAddress = false;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean deleted = false;
}
