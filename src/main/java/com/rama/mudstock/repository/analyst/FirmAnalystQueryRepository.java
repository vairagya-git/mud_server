package com.rama.mudstock.repository.analyst;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC-based read-only queries for analyst data that require joins across
 * firm, firm_analyst, firm_analyst_stock_rating, and stock tables.
 */
@Repository
public class FirmAnalystQueryRepository {

    private final JdbcTemplate jdbc;

    public FirmAnalystQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns all analysts with their firm name joined in.
     */
    public List<Map<String, Object>> listAllAnalystsWithFirmName() {
        String sql = """
                SELECT fa.id, fa.benzinga_analyst_id, fa.full_name, fa.last_updated,
                       fa.overall_avg_return, fa.overall_avg_return_percentile,
                       fa.overall_success_rate, fa.smart_score,
                       fa.total_ratings, fa.total_ratings_percentile,
                       fa.created_at, fa.updated_at,
                       f.name AS firm_name, f.benzinga_firm_id AS firm_benzinga_id
                FROM firm_analyst fa
                JOIN firm f ON fa.firm_id = f.id
                ORDER BY f.name, fa.full_name
                """;
        return jdbc.queryForList(sql);
    }

    /**
     * Returns analyst stock ratings filtered by any combination of ticker, analyst name, and firm name.
     * Pass {@code null} or blank for any parameter to skip that filter.
     */
    public List<Map<String, Object>> listAllRatingsWithMeta(String ticker, String analystName, String firmName) {
        StringBuilder sql = new StringBuilder("""
                SELECT fasr.id, fasr.rating, fasr.previous_rating,
                       fasr.rating_action, fasr.price_target_action,
                       fasr.price_target, fasr.previous_price_target,
                       fasr.price_percent_change,
                       fasr.adjusted_price_target, fasr.previous_adjusted_price_target,
                       fasr.importance, fasr.date, fasr.last_updated,
                       fasr.benzinga_calendar_url, fasr.benzinga_news_url,
                       fa.full_name AS analyst_name,
                       f.name AS firm_name,
                       s.ticker AS ticker
                FROM firm_analyst_stock_rating fasr
                JOIN firm_analyst fa ON fasr.firm_analyst_id = fa.id
                JOIN firm f ON fasr.firm_id = f.id
                JOIN stock s ON fasr.stock_id = s.id
                """);

        java.util.List<Object> params = new java.util.ArrayList<>();
        java.util.List<String> conditions = new java.util.ArrayList<>();

        if (ticker != null && !ticker.isBlank()) {
            conditions.add("UPPER(s.ticker) = UPPER(?)");
            params.add(ticker.trim());
        }
        if (analystName != null && !analystName.isBlank()) {
            conditions.add("fa.full_name = ?");
            params.add(analystName.trim());
        }
        if (firmName != null && !firmName.isBlank()) {
            conditions.add("f.name = ?");
            params.add(firmName.trim());
        }

        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append("\n");
        }
        sql.append("ORDER BY fasr.date DESC, s.ticker");

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    /**
     * Returns a distinct sorted list of all tickers that have ratings, for use in filter dropdowns.
     */
    public List<String> listDistinctRatingTickers() {
        String sql = """
                SELECT DISTINCT s.ticker
                FROM firm_analyst_stock_rating fasr
                JOIN stock s ON fasr.stock_id = s.id
                ORDER BY s.ticker
                """;
        return jdbc.queryForList(sql, String.class);
    }

    /**
     * Returns a distinct sorted list of analyst names that appear in ratings, for use in filter dropdowns.
     */
    public List<String> listDistinctRatingAnalysts() {
        String sql = """
                SELECT DISTINCT fa.full_name
                FROM firm_analyst_stock_rating fasr
                JOIN firm_analyst fa ON fasr.firm_analyst_id = fa.id
                ORDER BY fa.full_name
                """;
        return jdbc.queryForList(sql, String.class);
    }

    /**
     * Returns a distinct sorted list of firm names that appear in ratings, for use in filter dropdowns.
     */
    public List<String> listDistinctRatingFirms() {
        String sql = """
                SELECT DISTINCT f.name
                FROM firm_analyst_stock_rating fasr
                JOIN firm f ON fasr.firm_id = f.id
                ORDER BY f.name
                """;
        return jdbc.queryForList(sql, String.class);
    }
}
