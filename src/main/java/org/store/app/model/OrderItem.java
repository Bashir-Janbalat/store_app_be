package org.store.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@EqualsAndHashCode(callSuper = true)
@Data
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
