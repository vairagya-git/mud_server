package com.rama.mudstock.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rama.mudstock.repository.daystock.DayStockMovementEntryRepository;

@Service
public class DayStockMovementDataEnrichementService {

    private final DayStockMovementEntryRepository dayStockMovementEntryRepository;

    public DayStockMovementDataEnrichementService(DayStockMovementEntryRepository dayStockMovementEntryRepository) {
        this.dayStockMovementEntryRepository = dayStockMovementEntryRepository;
    }

    public int enrichPriceMatchDateTimes(List<Long> stockIds, LocalDate targetDate) {
        return dayStockMovementEntryRepository.enrichPriceMatchDateTimes(stockIds, targetDate);
    }
}