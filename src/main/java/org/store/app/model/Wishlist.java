package org.store.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.store.app.enums.WishlistStatus;

import java.util.List;

@Entity
@Table(name = "wishlist")
@EqualsAndHashCode(callSuper = true)
@Data
public class Wishlist extends BaseEntity {
    
    @ManyToOne
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "FK_wishlist_customer"))
    private Customer customer;

    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WishlistStatus status = WishlistStatus.ACTIVE;

    @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WishlistItem> items;
}
