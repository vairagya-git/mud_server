package com.rama.mudstock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.rama.mudstock.model.SystemConfig;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    Optional<SystemConfig> findByCode(String code);

    @Modifying
    @Transactional
    @Query("UPDATE SystemConfig sc SET sc.value = :value WHERE sc.code = :code")
    int updateValueByCode(@Param("code") String code, @Param("value") String value);
}
