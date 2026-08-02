package com.rama.mudstock.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.rama.mudstock.model.option.ContractSnapshotDetail;
import com.rama.mudstock.model.option.OptionTrackingFlatfileRow;
import com.rama.mudstock.model.option.OptionTrackingSnapshotRow;
import com.rama.mudstock.repository.option.OptionTrackingSnapshotRepository;

@Service
public class OptionTrackingSnapshotService {

    private final OptionTrackingSnapshotRepository optionTrackingSnapshotRepository;

    public OptionTrackingSnapshotService(OptionTrackingSnapshotRepository optionTrackingSnapshotRepository) {
        this.optionTrackingSnapshotRepository = optionTrackingSnapshotRepository;
    }

    public List<OptionTrackingSnapshotRow> getTrackingSnapshotRows(List<Long> contractIds) {
        List<OptionTrackingFlatfileRow> rows = optionTrackingSnapshotRepository.findFlatfileRowsForContracts(contractIds);

        Map<Long, RowAccumulator> byUnixTime = new LinkedHashMap<>();

        for (OptionTrackingFlatfileRow row : rows) {
            Long unixTime = row.unixTime();
            if (unixTime == null) {
                continue;
            }

            RowAccumulator acc = byUnixTime.computeIfAbsent(unixTime, k -> new RowAccumulator());
            acc.unixTime = unixTime;
            if (acc.localTime == null) {
                acc.localTime = row.localTime();
            }
            if (acc.stockOpen == null) {
                acc.stockOpen = row.stockOpen();
            }
            if (acc.stockClose == null) {
                acc.stockClose = row.stockClose();
            }

            Long optionContractId = row.optionContractId();
            if (optionContractId == null) {
                continue;
            }

            ContractSnapshotDetail detail = new ContractSnapshotDetail(
                optionContractId,
                row.contractTicker(),
                row.optVolume(),
                row.optOpen(),
                row.optClose(),
                row.optHigh(),
                row.optLow(),
                row.nearOptionSnapshotId(),
                row.bid(),
                row.ask(),
                row.midpoint(),
                row.delta(),
                row.gamma(),
                row.theta(),
                row.vega());

            acc.contractDetails.put(optionContractId, detail);
        }

        List<OptionTrackingSnapshotRow> result = new ArrayList<>();
        for (RowAccumulator acc : byUnixTime.values()) {
            result.add(new OptionTrackingSnapshotRow(
                acc.unixTime,
                acc.localTime,
                acc.stockOpen,
                acc.stockClose,
                acc.contractDetails));
        }
        return result;
    }

    private static class RowAccumulator {
        Long unixTime;
        Timestamp localTime;
        BigDecimal stockOpen;
        BigDecimal stockClose;
        Map<Long, ContractSnapshotDetail> contractDetails = new LinkedHashMap<>();
    }
}
