package com.javeme.duobao.repository;

import com.javeme.duobao.entity.OrderItem;
import com.javeme.duobao.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query(value = "SELECT product_id, sum(quantity) AS total_sold " +
            "FROM order_item " +
            "GROUP BY product_id " +
            "ORDER BY total_sold DESC " +
            "LIMIT 10", nativeQuery = true)
    List<Object[]> findTopSellingProductIds();

    @Query("SELECT COUNT(oi) " +
            "FROM OrderItem oi " +
            "JOIN Order o " +
            "ON oi.orderId = o.id " +
            "WHERE oi.id = :orderItemId " +
            "AND o.userId = :userId " +
            "AND o.status = 1")
    int countValidPurchases(Long orderItemId, Long userId);
}
