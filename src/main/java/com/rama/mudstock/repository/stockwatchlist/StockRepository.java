package com.rama.mudstock.repository.stockwatchlist;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rama.mudstock.model.stockwatchlist.Stock;

public interface StockRepository extends JpaRepository<Stock, Long> {
	java.util.Optional<Stock> findByTicker(String ticker);
}
