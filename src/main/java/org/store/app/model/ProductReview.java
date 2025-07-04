package org.store.app.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "product_reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "customer_id"}))
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductReview extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(columnDefinition = "TEXT")
    private String review;

    private Double rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
