package com.rama.mudstock.master;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code master.tables} list from application YAML.
 *
 * Example YAML:
 * <pre>
 * master:
 *   tables:
 *     - market_holidays
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "master")
public class MasterTableProperties {

    /**
     * Names of the master tables to load and cache on startup.
     */
    private List<String> tables = new ArrayList<>();

    public List<String> getTables() { return tables; }
    public void setTables(List<String> tables) { this.tables = tables; }
}
