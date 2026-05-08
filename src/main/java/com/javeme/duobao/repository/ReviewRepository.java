package com.javeme.duobao.repository;

import com.javeme.duobao.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderItemId(Long oderItemId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    Double calculateAverageRating(Long productId);
}
