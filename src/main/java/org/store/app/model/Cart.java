package org.store.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.store.app.enums.CartStatus;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart")
@EqualsAndHashCode(callSuper = true)
@Data
public class Cart extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "FK_cart_customer"))
    private Customer customer;

    @Column(name = "session_id")
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status = CartStatus.ACTIVE;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();
}
