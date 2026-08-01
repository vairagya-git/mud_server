package com.rama.mudstock.service;

import java.time.LocalDate;
import java.util.List;

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

            StatusResolution resolution = resolveCompletionStatus(entry, completionSource);
            if (resolution == null) {
                log.warn("OptionDataContractService: unable to resolve completion status for entry id={} source={} status={}",
                    entryId,
                    entry.source(),
                    entry.status());
                continue;
            }

            int updatedContracts = optionContractRepository.markContractsStatusForInterval(entryId, resolution.contractStatus());
            if (updatedContracts > 0) {
                log.info("OptionDataContractService: marked {} option_contract row(s) status={} for interval entryId={}",
                    updatedContracts,
                    resolution.contractStatus(),
                    entryId);
            }

            optionIntervalAnalyseRepository.updateStatusById(entryId, resolution.intervalStatus());
            completedCount++;
            log.info("OptionDataContractService: entry id={} moved -> {} (expirationDate={} < today={})",
                entryId,
                resolution.intervalStatus(),
                expirationDate,
                today);
        }

        return completedCount;
    }

    public StatusResolution resolveCompletionStatus(OptionsInternalAnalyseEntity entry, String completionSource) {
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

    public record StatusResolution(String intervalStatus, String contractStatus) {
    }
}