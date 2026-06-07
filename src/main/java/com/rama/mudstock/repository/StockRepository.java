package com.rama.mudstock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rama.mudstock.model.Stock;

public interface StockRepository extends JpaRepository<Stock, Long> {
	java.util.Optional<Stock> findByTicker(String ticker);
}
