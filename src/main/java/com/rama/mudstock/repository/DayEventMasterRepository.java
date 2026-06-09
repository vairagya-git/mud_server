package com.rama.mudstock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.model.DayEventMaster;

@Repository
public interface DayEventMasterRepository extends JpaRepository<DayEventMaster, Long> {
    // basic CRUD provided by JpaRepository
    java.util.Optional<DayEventMaster> findByCode(String code);
}
