package com.javeme.duobao.repository;

import com.javeme.duobao.entity.FlashSale;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {

    @Query("SELECT f " +
            "FROM FlashSale f " +
            "WHERE f.startTime >= :startTime AND f.endTime <= :endTime " +
            "ORDER BY f.startTime ASC")
    List<FlashSale> getCalendar(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);


    @Modifying
    @Transactional
    @Query("UPDATE FlashSale f SET f.status = 1 WHERE f.endTime < :now")
    void updateToActive(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE FlashSale f SET f.status = 2 WHERE f.endTime <:now AND f.status = 1")
    void updateToExpired(@Param("now") LocalDateTime now);

    List<FlashSale> findByStatusAndStartTimeLessThanEqual(Integer status, LocalDateTime now);

    List<FlashSale> findByStatusAndEndTimeLessThanEqual(Integer status, LocalDateTime now);
}
