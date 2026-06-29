package com.rama.mudstock.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.model.SystemConfig;
import com.rama.mudstock.repository.SystemConfigRepository;

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
     * Returns the typed value for a single config entry identified by {@code code},
     * or {@link Optional#empty()} if the code does not exist.
     */
    public Optional<Object> findByCode(String code) {
        return systemConfigRepository.findByCode(code)
                .map(this::convertValue);
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
     * Returns the raw {@link SystemConfig} entity for {@code code},
     * or {@link Optional#empty()} if not found.
     */
    public Optional<SystemConfig> findEntityByCode(String code) {
        return systemConfigRepository.findByCode(code);
    }

    /**
     * Updates the {@code value} column for the config identified by {@code code}.
     *
     * @param code  the unique config code
     * @param value the new raw string value to store
     * @return {@code true} if a row was updated, {@code false} if the code was not found
     */
    public boolean updateValue(String code, String value) {
        int updated = systemConfigRepository.updateValueByCode(code, value);
        if (updated == 0) {
            log.warn("SystemConfigService.updateValue: no config found for code={}", code);
            return false;
        }
        log.info("SystemConfigService.updateValue: updated code={} value={}", code, value);
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
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private Boolean parseBoolean(String code, String raw) {
        if (raw == null || raw.isBlank()) {
            return Boolean.FALSE;
        }
        String normalized = raw.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return Boolean.FALSE;
        }
        log.warn("SystemConfigService: code={} has type=Boolean but value '{}' is invalid; returning false", code, raw);
        return Boolean.FALSE;
    }

    private LocalDate parseDate(String code, String raw) {
        if (raw == null || raw.isBlank()) return null;
        // Accept ISO date (yyyy-MM-dd) or datetime prefix (yyyy-MM-ddT...)
        String datePart = raw.length() >= 10 ? raw.substring(0, 10) : raw;
        try {
            return LocalDate.parse(datePart);
        } catch (DateTimeParseException e) {
            log.warn("SystemConfigService: code={} has type=Date but value '{}' could not be parsed; returning null", code, raw);
            return null;
        }
    }
}
