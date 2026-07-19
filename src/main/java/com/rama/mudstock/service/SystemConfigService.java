package com.rama.mudstock.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.model.SystemConfig;
import com.rama.mudstock.repository.SystemConfigRepository;
import com.rama.mudstock.util.TypeConverstionUtil;

/**
 * Service for reading and updating {@code system_config} table entries.
 *
 * <p>The {@code type} column drives value conversion:
 * <ul>
 *   <li>{@code Date}       — value is returned as a {@link LocalDate} (date only, no timestamp)</li>
 *   <li>{@code StringArray} — value is returned as {@link java.util.List} of comma-separated strings</li>
 *   <li>{@code Boolean}    — value is returned as {@link Boolean}</li>
 *   <li>Anything else      — value is returned as the raw {@link String}</li>
 * </ul>
 */
@Service
public class SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigService.class);

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    /**
     * Returns all system config entities ordered by code.
     */
    public List<SystemConfig> findAllEntities() {
        return systemConfigRepository.findAll();
    }

    /**
     * Returns all system config entries as a map of code → typed value.
     * The Java type of each map value depends on the {@code type} column:
        * {@code Date} → {@link LocalDate}, {@code StringArray} → {@link java.util.List},
        * {@code Boolean} → {@link Boolean}, otherwise {@link String}.
     */
    public Map<String, Object> findAll() {
        List<SystemConfig> configs = systemConfigRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();
        for (SystemConfig cfg : configs) {
            result.put(cfg.getCode(), convertValue(cfg));
        }
        return result;
    }

    /**
     * Returns the typed value for a single config entry identified by {@code purpose} + {@code code},
     * or {@link Optional#empty()} if no row matches.
     */
    public Optional<Object> findByPurposeAndCode(String purpose, String code) {
        return systemConfigRepository.findByPurposeAndCode(purpose, code)
                .map(this::convertValue);
    }

    /**
     * Returns all typed config values for the given purpose as code -> value map.
     */
    public Map<String, Object> findAllByPurpose(String purpose) {
        List<SystemConfig> configs = systemConfigRepository.findByPurpose(purpose);
        Map<String, Object> result = new LinkedHashMap<>();
        for (SystemConfig cfg : configs) {
            result.put(cfg.getCode(), convertValue(cfg));
        }
        return result;
    }

    /**
     * Returns the raw {@link SystemConfig} entity for {@code purpose} + {@code code},
     * or {@link Optional#empty()} if not found.
     */
    public Optional<SystemConfig> findEntityByPurposeAndCode(String purpose, String code) {
        return systemConfigRepository.findByPurposeAndCode(purpose, code);
    }

    /**
     * Updates the {@code value} column for the config identified by {@code purpose} + {@code code}.
     *
     * @param purpose the config purpose namespace
     * @param code    the config code (not globally unique)
     * @param value   the new raw string value to store
     * @return {@code true} if a row was updated, {@code false} if not found
     */
    public boolean updateValue(String purpose, String code, String value) {
        int updated = systemConfigRepository.updateValueByPurposeAndCode(purpose, code, value);
        if (updated == 0) {
            log.warn("SystemConfigService.updateValue: no config found for purpose={} code={}", purpose, code);
            return false;
        }
        log.info("SystemConfigService.updateValue: updated purpose={} code={} value={}", purpose, code, value);
        return true;
    }

    /**
     * Saves (updates) an existing {@link SystemConfig} entity.
     */
    public SystemConfig save(SystemConfig config) {
        return systemConfigRepository.save(config);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private Object convertValue(SystemConfig cfg) {
        String type = cfg.getType();
        String raw = cfg.getValue();
        if ("Date".equalsIgnoreCase(type)) {
            return parseDate(cfg.getCode(), raw);
        }
        if ("StringArray".equalsIgnoreCase(type)) {
            return parseStringArray(raw);
        }
        if ("Boolean".equalsIgnoreCase(type) || "bool".equalsIgnoreCase(type)) {
            return parseBoolean(cfg.getCode(), raw);
        }
        return raw;
    }

    private List<String> parseStringArray(String raw) {
        return TypeConverstionUtil.toStringList(raw);
    }

    private Boolean parseBoolean(String code, String raw) {
        if (raw == null || raw.isBlank()) {
            return Boolean.FALSE;
        }
        Boolean parsed = TypeConverstionUtil.toBooleanStrict(raw);
        if (parsed != null) {
            return parsed;
        }
        log.warn("SystemConfigService: code={} has type=Boolean but value '{}' is invalid; returning false", code, raw);
        return Boolean.FALSE;
    }

    private LocalDate parseDate(String code, String raw) {
        LocalDate parsed = TypeConverstionUtil.toLocalDateFromDateOrDateTimePrefix(raw);
        if (parsed == null && raw != null && !raw.isBlank()) {
            log.warn("SystemConfigService: code={} has type=Date but value '{}' could not be parsed; returning null", code, raw);
        }
        return parsed;
    }
}
