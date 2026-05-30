package com.rama.mudstock.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rama.mudstock.model.Stock;
import com.rama.mudstock.repository.StockRepository;
import com.rama.mudstock.service.AlphaVantageStockService;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final StockRepository repository;
    private final AlphaVantageStockService stockService;

    public StockController(StockRepository repository, AlphaVantageStockService stockService) {
        this.repository = repository;
        this.stockService = stockService;
    }

    @GetMapping
    public List<Stock> list() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Stock> create(@RequestBody Stock stock) {
        Stock saved = repository.save(stock);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{ticker}/timeseries")
    public ResponseEntity<String> getTimeSeries(@org.springframework.web.bind.annotation.PathVariable String ticker) {
        String body = stockService.fetchDailyTimeSeries(ticker);
        return ResponseEntity.ok(body);
    }
}
