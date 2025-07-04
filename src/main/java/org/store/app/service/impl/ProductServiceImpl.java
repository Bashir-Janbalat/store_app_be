package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.common.ValueWrapper;
import org.store.app.dto.ProductReviewDTO;
import org.store.app.enums.OrderStatus;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.mapper.ProductReviewMapper;
import org.store.app.model.Customer;
import org.store.app.model.ProductReview;
import org.store.app.repository.CustomerRepository;
import org.store.app.repository.ProductReviewRepository;
import org.store.app.service.OrderService;
import org.store.app.service.ProductService;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final CustomerRepository customerRepository;
    private final ProductReviewRepository reviewRepository;
    private final ProductReviewMapper reviewMapper;
    private final OrderService orderService;


    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "productReviews", key = "#productId"),
            @CacheEvict(value = "orders", key = "#customerId + '-DELIVERED'"),
    })
    public void AddReview(Long customerId, Long productId, Double rating, String review) {
        log.info("Adding review for productId={} by customerId={}", productId, customerId);

        Long foundProductId = reviewRepository.findProductId(productId);
        if (foundProductId == null) {
            log.warn("Product with id={} not found", productId);
            throw new ResourceNotFoundException("Product with id " + productId + " not found.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        boolean isCustomerBought = orderService.hasCustomerPurchasedProduct(customerId, productId, OrderStatus.DELIVERED);
        if (!isCustomerBought) {
            log.warn("Customer with id={} has not purchased product {}", customerId, productId);
            throw new IllegalStateException("Customer must purchase the product before reviewing.");
        }


        boolean alreadyExists = reviewRepository.existsByProductIdAndCustomerId(productId, customerId);
        if (alreadyExists) {
            log.warn("Customer with id={} already reviewed product {}", customerId, productId);
            throw new IllegalArgumentException("Customer already reviewed this product");
        }

        if (rating < 0.5 || rating > 5.0) {
            throw new IllegalArgumentException("Rating must be between 0.5 and 5.0");
        }
        ProductReview reviewEntity = new ProductReview();
        reviewEntity.setProductId(productId);
        reviewEntity.setCustomer(customer);
        reviewEntity.setRating(rating);
        reviewEntity.setReview(review);
        reviewRepository.save(reviewEntity);
        log.info("Review saved for productId={} by customerId={}", productId, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "productReviews", key = "#productId")
    public ValueWrapper<List<ProductReviewDTO>> getReviewsFor(Long productId) {
        log.info("Fetching reviews for productId={}", productId);
        List<ProductReview> reviews = reviewRepository.findAllByProductId(productId);
        return new ValueWrapper<>(reviews.stream().map(reviewMapper::toDto).toList());
    }
}
