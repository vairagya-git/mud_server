package com.rama.mudstock.facade;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.repository.option.OptionSnapshotIVMetricRepository;

@Service
public class OptionSnapshotIVMetricsFacade {

    private static final Logger log = LoggerFactory.getLogger(OptionSnapshotIVMetricsFacade.class);

    private final OptionSnapshotIVMetricRepository optionSnapshotIVMetricRepository;

    public OptionSnapshotIVMetricsFacade(OptionSnapshotIVMetricRepository optionSnapshotIVMetricRepository) {
        this.optionSnapshotIVMetricRepository = optionSnapshotIVMetricRepository;
    }

    public int calculateForDate(LocalDate ivDate) {
        int affectedRows = optionSnapshotIVMetricRepository.upsertDailyMetrics(ivDate);
        if (affectedRows == 0) {
            log.info("OptionSnapshotIVMetricsFacade: no IV metric rows generated for ivDate={}", ivDate);
        } else {
            log.info("OptionSnapshotIVMetricsFacade: upserted {} IV metric row(s) for ivDate={}", affectedRows, ivDate);
        }
        return affectedRows;
    }
}