package com.rama.mudstock.facade;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.enums.SystemRepositoryEnum.OptionContractStatusEnum;
import com.rama.mudstock.enums.SystemRepositoryEnum.OptionSourceEnum;
import com.rama.mudstock.repository.option.OptionContractRepository;
import com.rama.mudstock.service.S3OptionFlatfileService;

@Service
public class OptionFlatFileSnapshotFetcherFacade {

    private static final Logger log = LoggerFactory.getLogger(OptionFlatFileSnapshotFetcherFacade.class);
    private static final String SOURCE = OptionSourceEnum.FLAT_FILE.name();
    private static final String STATUS = OptionContractStatusEnum.ACTIVE.name();
    private final OptionContractRepository optionContractRepository;
    private final S3OptionFlatfileService s3OptionFlatfileService;

    public OptionFlatFileSnapshotFetcherFacade(OptionContractRepository optionContractRepository,
                                               S3OptionFlatfileService s3OptionFlatfileService) {
        this.optionContractRepository = optionContractRepository;
        this.s3OptionFlatfileService = s3OptionFlatfileService;
    }

    /**
     * Placeholder hook for flat-file snapshot fetch and store logic.
     */
    public int fetchAndStoreSnapshots(long snapshotVersion, LocalDate targetDate) {
        LocalDate effectiveDate = targetDate == null ? LocalDate.now() : targetDate;
        List<Map<String, Object>> contracts = optionContractRepository.getOptionContractsWithTickerByStatus(
            STATUS,
            true,
            List.of(SOURCE));

        for (Map<String, Object> contract : contracts) {
            String stockTicker = toString(contract.get("ticker"));
            String contractTicker = toString(contract.get("contract_ticker"));

            if (stockTicker == null || contractTicker == null) {
                log.warn("{}: skipping contract with missing ticker/contract_ticker -> {}", SOURCE, contract);
                continue;
            }

            try {
                Map<String, Object> optionRows = s3OptionFlatfileService.fetchOptionRows(effectiveDate, contractTicker, 3);
                Map<String, Object> stockRows = s3OptionFlatfileService.fetchStockRows(effectiveDate, stockTicker, 3);

                logRows("option", contractTicker, stockTicker, optionRows);
                logRows("stock", contractTicker, stockTicker, stockRows);
            } catch (Exception ex) {
                log.error("{}: failed fetching flat-file rows for stockTicker={} contractTicker={} date={}",
                    SOURCE,
                    stockTicker,
                    contractTicker,
                    effectiveDate,
                    ex);
            }
        }

        return 0;
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
}
