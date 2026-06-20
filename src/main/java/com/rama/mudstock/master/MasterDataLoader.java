package com.rama.mudstock.master;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.rama.mudstock.model.MasterMarketHoliday;
import com.rama.mudstock.repository.MasterMarketHolidayRepository;

/**
 * Loads every master table listed under {@code master.tables} in application YAML
 * into {@link MasterDataCache} once the application context is fully started.
 *
 * <p>Adding a new master table requires:
 * <ol>
 *   <li>Adding the table name to {@code master.tables} in the YAML.</li>
 *   <li>Adding a corresponding {@code case} block in {@link #loadTable}.</li>
 *   <li>Adding a typed accessor in {@link MasterDataCache}.</li>
 * </ol>
 */
@Component
public class MasterDataLoader {

    private static final Logger log = LoggerFactory.getLogger(MasterDataLoader.class);

    private final MasterTableProperties properties;
    private final MasterDataCache cache;
    private final MasterMarketHolidayRepository masterMarketHolidayRepository;

    public MasterDataLoader(MasterTableProperties properties,
                            MasterDataCache cache,
                            MasterMarketHolidayRepository masterMarketHolidayRepository) {
        this.properties = properties;
        this.cache = cache;
        this.masterMarketHolidayRepository = masterMarketHolidayRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        List<String> tables = properties.getTables();
        if (tables == null || tables.isEmpty()) {
            log.info("MasterDataLoader: no master tables configured under 'master.tables'");
            return;
        }
        log.info("MasterDataLoader: loading {} master table(s): {}", tables.size(), tables);
        for (String tableName : tables) {
            try {
                loadTable(tableName);
            } catch (Exception ex) {
                log.error("MasterDataLoader: failed to load master table '{}': {}", tableName, ex.getMessage(), ex);
            }
        }
        log.info("MasterDataLoader: finished loading. Cached tables: {}", cache.loadedTables());
    }

    private void loadTable(String tableName) {
        switch (tableName) {
            case MasterDataCache.MARKET_HOLIDAYS -> {
                List<MasterMarketHoliday> rows = masterMarketHolidayRepository.findAll();
                cache.putMarketHolidays(rows);
                log.info("MasterDataLoader: loaded {} row(s) from '{}'", rows.size(), tableName);
            }
            default ->
                log.warn("MasterDataLoader: unknown master table '{}' — skipping", tableName);
        }
    }
}
