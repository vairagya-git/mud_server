package com.rama.mudstock.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.enums.SystemRepositoryEnum.OptionContractStatusEnum;
import com.rama.mudstock.enums.SystemRepositoryEnum.OptionIntervalAnalyseStatusEnum;
import com.rama.mudstock.enums.SystemRepositoryEnum.OptionSourceEnum;
import com.rama.mudstock.model.option.OptionsInternalAnalyseEntity;
import com.rama.mudstock.repository.option.OptionContractRepository;
import com.rama.mudstock.repository.option.OptionIntervalAnalyseRepository;

@Service
public class OptionDataContractService {

    private static final Logger log = LoggerFactory.getLogger(OptionDataContractService.class);

    private final OptionContractRepository optionContractRepository;
    private final OptionIntervalAnalyseRepository optionIntervalAnalyseRepository;

    public OptionDataContractService(OptionContractRepository optionContractRepository,
                                     OptionIntervalAnalyseRepository optionIntervalAnalyseRepository) {
        this.optionContractRepository = optionContractRepository;
        this.optionIntervalAnalyseRepository = optionIntervalAnalyseRepository;
    }

    public int completeExpiredActiveEntries(String completionSource) {
        LocalDate today = LocalDate.now();
        List<OptionsInternalAnalyseEntity> activeEntries = optionIntervalAnalyseRepository
            .getOptionsInternalAnalyseByStatusAndExpirationBefore(OptionIntervalAnalyseStatusEnum.ACTIVE.name(), today);

        int completedCount = 0;
        for (OptionsInternalAnalyseEntity entry : activeEntries) {
            Long entryId = entry.id();
            LocalDate expirationDate = entry.expirationDate();
            if (entryId == null || expirationDate == null) {
                continue;
            }
            if (!expirationDate.isBefore(today)) {
                continue;
            }

            if (updateCompletionStatus(entry, completionSource, today)) {
                completedCount++;
            }
        }

        return completedCount;
    }

    public boolean updateCompletionStatus(OptionsInternalAnalyseEntity entry, String completionSource, LocalDate today) {
        Long entryId = entry.id();
        if (entryId == null) {
            return false;
        }

        StatusResolution resolution = resolveStatusResolution(entry, completionSource);
        if (resolution == null) {
            log.warn("OptionDataContractService: unable to resolve completion status for entry id={} source={} status={}",
                entryId,
                entry.source(),
                entry.status());
            return false;
        }

        int updatedContracts = optionContractRepository.markContractsStatusForInterval(entryId, resolution.contractStatus());
        if (updatedContracts > 0) {
            log.info("OptionDataContractService: marked {} option_contract row(s) status={} for interval entryId={}",
                updatedContracts,
                resolution.contractStatus(),
                entryId);
        }

        optionIntervalAnalyseRepository.updateStatusById(entryId, resolution.intervalStatus());
        log.info("OptionDataContractService: entry id={} moved -> {} (expirationDate={} < today={})",
            entryId,
            resolution.intervalStatus(),
            entry.expirationDate(),
            today);

        return true;
    }

    private StatusResolution resolveStatusResolution(OptionsInternalAnalyseEntity entry, String completionSource) {
        String source = entry.source() == null ? "" : entry.source().trim().toUpperCase();
        String normalizedCompletionSource = completionSource == null ? "" : completionSource.trim().toUpperCase();

        boolean isApiCompletion = OptionIntervalAnalyseStatusEnum.API_COMPLETED.name().equals(normalizedCompletionSource);
        boolean isFlatFileCompletion = OptionIntervalAnalyseStatusEnum.FLAT_FILE_COMPLETED.name().equals(normalizedCompletionSource);

        if (OptionSourceEnum.API.name().equals(source) && isApiCompletion) {
            return new StatusResolution(
                OptionIntervalAnalyseStatusEnum.COMPLETED.name(),
                OptionContractStatusEnum.COMPLETED.name());
        }

        if (OptionSourceEnum.FLAT_FILE.name().equals(source) && isFlatFileCompletion) {
            return new StatusResolution(
                OptionIntervalAnalyseStatusEnum.COMPLETED.name(),
                OptionContractStatusEnum.COMPLETED.name());
        }

        if (OptionSourceEnum.BOTH.name().equals(source)) {
            if (isApiCompletion) {
                return new StatusResolution(
                    OptionIntervalAnalyseStatusEnum.API_COMPLETED.name(),
                    OptionContractStatusEnum.API_COMPLETED.name());
            }
            if (isFlatFileCompletion) {
                return new StatusResolution(
                    OptionIntervalAnalyseStatusEnum.FLAT_FILE_COMPLETED.name(),
                    OptionContractStatusEnum.FLAT_FILE_COMPLETED.name());
            }
        }

        return null;
    }

    /**
     * Returns available expiration dates (with options_interval_analyse.id) for a given stock ticker,
     * intended for frontend expiry-date selection.
     */
    public Map<LocalDate, Long> findExpirationDatesForTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return Map.of();
        }
        return optionIntervalAnalyseRepository.findExpirationDatesByTicker(ticker.trim());
    }

    /**
     * Returns contract_ticker -> option_contract.id for all contracts belonging to
     * the given options_interval_analyse.id, for later lookup use.
     */
    public Map<String, Long> findContractTickerIdsForIntervalAnalyse(Long optionsIntervalAnalyseId) {
        if (optionsIntervalAnalyseId == null) {
            return Map.of();
        }

        List<Map<String, Object>> rows = optionContractRepository.findContractTickersByIntervalAnalyseId(optionsIntervalAnalyseId);

        Map<String, Long> result = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String contractTicker = (String) row.get("contract_ticker");
            Long id = ((Number) row.get("id")).longValue();
            result.put(contractTicker, id);
        }
        return result;
    }

    public record StatusResolution(String intervalStatus, String contractStatus) {
    }
}