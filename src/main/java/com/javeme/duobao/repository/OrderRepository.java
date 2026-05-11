package com.javeme.duobao.repository;

import com.javeme.duobao.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("select o " +
            "from Order o " +
            "where o.userId = :userId " +
            "and (:status is null or o.status = :status) " +
            "and (cast(:start as timestamp) is null or o.orderTime >= :start) " +
            "and (cast(:end as timestamp) is null or o.orderTime <= :end) " +
            "order by o.orderTime desc")
    Page<Order> findOrdersByFilters(
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param ("start")LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    @Query("select o " +
            "from Order o " +
            "where o.orderNumber = :orderNumber and o.userId = :userId")
    Optional<Order> findByOrderNumberAndUserId(
            @Param("orderNumber") String orderNumber,
            @Param("userId") Long userId);
}
