package com.javeme.duobao.repository;

import com.javeme.duobao.entity.Review;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository {

    boolean existsByOrderItemId(Long oderItemId);

    void save(Review review);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    Double calculateAverageRating(Long productId);
}
