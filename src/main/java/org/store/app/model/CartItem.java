package org.store.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(name = "unique_cart_product", columnNames = {"cart_id", "product_id"}))
@EqualsAndHashCode(callSuper = true)
@Data
public class CartItem extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false, foreignKey = @ForeignKey(name = "FK_cart_item_cart"))
    private Cart cart;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

}
