package com.rama.mudstock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rama.mudstock.model.Watchlist;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
	java.util.Optional<Watchlist> findByName(String name);

	java.util.Optional<Watchlist> findByCode(String code);
}
