package com.javeme.duobao.service;

import com.javeme.duobao.dto.ReviewDTO;
import com.javeme.duobao.entity.Product;
import com.javeme.duobao.entity.Review;
import com.javeme.duobao.repository.OrderItemRepository;
import com.javeme.duobao.repository.ProductRepository;
import com.javeme.duobao.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void submitReview(Long userId, ReviewDTO reviewDTO) {

        //check whether review table got this orderItemId, one user can review one time for an orderItem
        boolean result = reviewRepository.existsByOrderItemId(reviewDTO.getOrderItemId());
        if (result) {
            throw new RuntimeException("You have already review this item");
        }

        //check whether user has purchased this item
        int count = orderItemRepository.countValidPurchases(reviewDTO.getOrderItemId(), userId);

        if (count  == 0) {

            throw new RuntimeException("You have not purchased this item");
        }

        Review review = new Review();
        BeanUtils.copyProperties(reviewDTO, review);
        review.setUserId(userId);
        review.setCreateTime(LocalDateTime.now());
        reviewRepository.save(review);

        //Count average rating
        Double newAverage = reviewRepository.calculateAverageRating(reviewDTO.getProductId());

        Product product = productRepository.findById(reviewDTO.getProductId()).orElseThrow(() ->
                new RuntimeException("Product not found"));

        double roundedRating = Math.round(newAverage * 10.0) / 10.0;
        product.setRating(roundedRating);
        productRepository.save(product);


    }


}
