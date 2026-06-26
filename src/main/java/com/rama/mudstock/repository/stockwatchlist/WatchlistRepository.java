package com.rama.mudstock.repository.stockwatchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rama.mudstock.model.stockwatchlist.Watchlist;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
	java.util.Optional<Watchlist> findByName(String name);

	java.util.Optional<Watchlist> findByCode(String code);

	@Query("select distinct w from Watchlist w left join fetch w.stocks where w.code = :code")
	java.util.Optional<Watchlist> findByCodeWithStocks(@Param("code") String code);
}
