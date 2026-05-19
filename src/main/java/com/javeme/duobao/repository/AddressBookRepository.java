package com.javeme.duobao.repository;

import com.javeme.duobao.entity.AddressBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AddressBookRepository extends JpaRepository<AddressBook, Long> {

    List<AddressBook> findByUserId(Long userId);

    List<AddressBook> findByUserIdOrderByIsDefaultDesc(Long userId);

    @Modifying
    @Transactional
    @Query("update AddressBook set isDefault = 0 where userId = :userId")
    void resetDefault(Long userId);

    AddressBook findByUserIdAndIsDefault(Long userId, Integer isDefault);


}
