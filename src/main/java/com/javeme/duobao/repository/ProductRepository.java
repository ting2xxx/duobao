package com.javeme.duobao.repository;

import com.javeme.duobao.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product>findByCategoryIdAndStatus(Long categoryId, Integer status);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :productId AND p.stock >= :quantity")
    int deductStock(@Param("productId") Long producId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :productId AND p.stock >= :quantity")
    void increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    List<Product> findByIsFlashSale(Boolean isFlashSale);


}
