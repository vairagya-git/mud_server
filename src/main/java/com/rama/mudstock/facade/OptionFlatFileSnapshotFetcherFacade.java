package com.rama.mudstock.facade;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.rama.mudstock.enums.SystemRepositoryEnum.OptionContractStatusEnum;
import com.rama.mudstock.enums.SystemRepositoryEnum.OptionIntervalAnalyseStatusEnum;
import com.rama.mudstock.enums.SystemRepositoryEnum.OptionSourceEnum;
import com.rama.mudstock.model.option.TickerOptionSnapshotData;
import com.rama.mudstock.model.option.TickerStockSnapshotData;
import com.rama.mudstock.repository.option.OptionContractRepository;
import com.rama.mudstock.repository.option.OptionSnapshotFlatfileRepository;
import com.rama.mudstock.service.OptionDataContractService;
import com.rama.mudstock.service.S3OptionFlatfileService;
import com.rama.mudstock.util.TypeConverstionUtil;

@Service
public class OptionFlatFileSnapshotFetcherFacade {

    private static final Logger log = LoggerFactory.getLogger(OptionFlatFileSnapshotFetcherFacade.class);
    private static final String SOURCE = OptionSourceEnum.FLAT_FILE.name();
    private static final List<String> SOURCES = List.of(OptionSourceEnum.FLAT_FILE.name(), OptionSourceEnum.BOTH.name());
    private final OptionContractRepository optionContractRepository;
    private final OptionSnapshotFlatfileRepository optionSnapshotFlatfileRepository;
    private final S3OptionFlatfileService s3OptionFlatfileService;
    private final OptionDataContractService optionDataContractService;
    private Map<String, List<TickerOptionSnapshotData>> optionRowsDaysDataCache;
    private Map<String, Map<Long, TickerStockSnapshotData>> stockRowsDaysDataCache;

    public OptionFlatFileSnapshotFetcherFacade(OptionContractRepository optionContractRepository,
                                               OptionSnapshotFlatfileRepository optionSnapshotFlatfileRepository,
                                               S3OptionFlatfileService s3OptionFlatfileService,
                                               OptionDataContractService optionDataContractService) {
        this.optionContractRepository = optionContractRepository;
        this.optionSnapshotFlatfileRepository = optionSnapshotFlatfileRepository;
        this.s3OptionFlatfileService = s3OptionFlatfileService;
        this.optionDataContractService = optionDataContractService;
    }

    /**
     * Placeholder hook for flat-file snapshot fetch and store logic.
     */
    public int fetchAndStoreSnapshots(long snapshotVersion, LocalDate targetDate) {
        try {
            List<Map<String, Object>> contracts = loadContractsForFlatFileRun();

            if (optionRowsDaysDataCache == null) {
                // Load once per execution (all option rows grouped by ticker).
                optionRowsDaysDataCache = s3OptionFlatfileService.loadOptionRowsDaysData(targetDate);
            }
            if (stockRowsDaysDataCache == null) {
                // Load once per execution (all stock rows grouped by ticker -> window_start).
                stockRowsDaysDataCache = s3OptionFlatfileService.loadStockRowsDaysData(targetDate);
            }

            log.info("{}: fetchAndStoreSnapshots started. snapshotVersion={}, targetDate={}, contracts={}, optionTickerGroups={}, stockTickerGroups={}",
                SOURCE, snapshotVersion, targetDate, contracts.size(), optionRowsDaysDataCache.size(), stockRowsDaysDataCache.size());

            int inserted = 0;
            for (Map<String, Object> contract : contracts) {
                String stockTicker = toString(contract.get("ticker"));
                String contractTicker = toString(contract.get("contract_ticker"));
                Long optionContractId = TypeConverstionUtil.toLong(contract.get("id"));
                Long stockId = TypeConverstionUtil.toLong(contract.get("stock_id"));

                if (stockTicker == null || contractTicker == null || optionContractId == null || stockId == null) {
                    log.warn("{}: skipping contract with missing required fields -> {}", SOURCE, contract);
                    continue;
                }

                int insertedForContract = 0;
                int duplicateForContract = 0;
                int skippedMissingWindowStart = 0;
                int skippedMissingStockMatch = 0;

                try {
                    List<TickerOptionSnapshotData> optionRows = optionRowsDaysDataCache.getOrDefault(contractTicker, List.of());
                    Map<Long, TickerStockSnapshotData> stockByWindowStart = stockRowsDaysDataCache.getOrDefault(stockTicker, Map.of());

                    log.info("{}: contract fetch rows. optionContractId={}, contractTicker={}, stockTicker={}, optionRows={}, stockRows={}",
                        SOURCE, optionContractId, contractTicker, stockTicker, optionRows.size(), stockByWindowStart.size());

                    for (TickerOptionSnapshotData optionRow : optionRows) {
                        Long windowStart = optionRow.windowStart();
                        if (windowStart == null) {
                            skippedMissingWindowStart++;
                            continue;
                        }

                        TickerStockSnapshotData stockRow = stockByWindowStart.get(windowStart);
                        if (stockRow == null) {
                            skippedMissingStockMatch++;
                            log.warn("{}: no stock match. optionContractId={}, optionWindowStart={}", SOURCE, optionContractId, windowStart);
                        }

                        Timestamp utcTs = TypeConverstionUtil.toTimestampFromEpochNanos(windowStart);
                        Timestamp localTs = TypeConverstionUtil.toPortugalTimestampFromEpochNanos(windowStart);

                        try {
                            int written = optionSnapshotFlatfileRepository.insert(
                                optionContractId,
                                stockId,
                                optionRow.ticker() != null ? optionRow.ticker() : contractTicker,
                                optionRow.volume(),
                                optionRow.open(),
                                optionRow.close(),
                                optionRow.high(),
                                optionRow.low(),
                                windowStart,
                                utcTs,
                                localTs,
                                stockRow != null && stockRow.ticker() != null ? stockRow.ticker() : stockTicker,
                                stockRow == null ? null : stockRow.volume(),
                                stockRow == null ? null : stockRow.open(),
                                stockRow == null ? null : stockRow.close(),
                                stockRow == null ? null : stockRow.high(),
                                stockRow == null ? null : stockRow.low(),
                                snapshotVersion);
                            inserted += written;
                            insertedForContract += written;
                        } catch (DuplicateKeyException ex) {
                            duplicateForContract++;
                        }
                    }

                    log.info("{}: contract done. optionContractId={}, inserted={}, duplicates={}, skippedMissingWindowStart={}, missingStockMatch={}",
                        SOURCE, optionContractId, insertedForContract, duplicateForContract, skippedMissingWindowStart, skippedMissingStockMatch);

                } catch (Exception ex) {
                    log.error("{}: failed fetching flat-file rows for stockTicker={} contractTicker={} date={}",
                        SOURCE,
                        stockTicker,
                        contractTicker,
                        targetDate,
                        ex);
                }
            }

            int completedEntries = optionDataContractService.completeExpiredActiveEntries(
                OptionIntervalAnalyseStatusEnum.FLAT_FILE_COMPLETED.name());
            log.info("{}: completed {} options_interval_analyse row(s) after flat-file processing", SOURCE, completedEntries);

            log.info("{}: fetchAndStoreSnapshots completed. snapshotVersion={}, targetDate={}, totalInserted={}",
                SOURCE, snapshotVersion, targetDate, inserted);
            return inserted;
        } finally {
            optionRowsDaysDataCache = null;
            stockRowsDaysDataCache = null;
            log.info("{}: cleared in-memory optionRowsDaysDataCache and stockRowsDaysDataCache after execution", SOURCE);
        }
    }

    private void logRows(String feedType,
                         String contractTicker,
                         String stockTicker,
                         Map<String, Object> payload) {
        Object rows = payload.get("records");
        log.info("{} {} rows for contractTicker={} stockTicker={}: {}",
            SOURCE,
            feedType,
            contractTicker,
            stockTicker,
            rows);
    }

    private String toString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private List<Map<String, Object>> loadContractsForFlatFileRun() {
        return optionContractRepository.getOptionContractsWithTickerByStatus(
            List.of(
                OptionContractStatusEnum.ACTIVE.name(),
                OptionContractStatusEnum.API_COMPLETED.name()
            ),
            true,
            SOURCES
        );
    }
}