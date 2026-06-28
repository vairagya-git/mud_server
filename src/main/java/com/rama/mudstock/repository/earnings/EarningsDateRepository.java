package com.rama.mudstock.repository.earnings;

import com.rama.mudstock.model.earnings.EarningsDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class EarningsDateRepository {
    private final JdbcTemplate jdbc;

    public EarningsDateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<EarningsDate> MAPPER = new RowMapper<>() {
        @Override
        public EarningsDate mapRow(ResultSet rs, int rowNum) throws SQLException {
            EarningsDate e = new EarningsDate();
            e.setId(rs.getLong("id"));
            e.setStockId(rs.getLong("stock_id"));
            e.setQuarter(rs.getString("quarter"));
            String rt = rs.getString("releaseTime");
            if (rt != null) e.setReleaseTime(EarningsDate.ReleaseTime.valueOf(rt));
            String st = rs.getString("status");
            if (st != null) e.setStatus(EarningsDate.Status.valueOf(st));
            java.sql.Date d = rs.getDate("earnings_date");
            if (d != null) e.setEarningsDate(d.toLocalDate());
            return e;
        }
    };

    public List<EarningsDate> findAll() {
        return jdbc.query("SELECT * FROM earnings_date ORDER BY earnings_date DESC", MAPPER);
    }

    public List<java.util.Map<String, Object>> listUpcoming() {
        String sql = """
                SELECT ed.id, ed.earnings_date, ed.releaseTime, ed.status, ed.quarter,
                       s.ticker
                FROM earnings_date ed
                JOIN stock s ON ed.stock_id = s.id
                WHERE ed.status = 'UPCOMING'
                ORDER BY ed.earnings_date ASC
                """;
        return jdbc.queryForList(sql);
    }

    public Optional<EarningsDate> findById(Long id) {
        List<EarningsDate> list = jdbc.query("SELECT * FROM earnings_date WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public EarningsDate save(EarningsDate e) {
        if (e.getId() == null) return insert(e);
        update(e);
        return e;
    }

    private EarningsDate insert(EarningsDate e) {
        String sql = "INSERT INTO earnings_date (stock_id, quarter, releaseTime, status, earnings_date) VALUES (?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, e.getStockId());
            ps.setString(2, e.getQuarter());
            ps.setString(3, e.getReleaseTime() == null ? null : e.getReleaseTime().name());
            ps.setString(4, e.getStatus() == null ? EarningsDate.Status.NEW.name() : e.getStatus().name());
            ps.setDate(5, e.getEarningsDate() == null ? null : java.sql.Date.valueOf(e.getEarningsDate()));
            return ps;
        }, kh);
        Number key = kh.getKey();
        if (key != null) e.setId(key.longValue());
        return e;
    }

    private void update(EarningsDate e) {
        String sql = "UPDATE earnings_date SET stock_id=?, quarter=?, releaseTime=?, status=?, earnings_date=? WHERE id=?";
        jdbc.update(sql,
            e.getStockId(),
            e.getQuarter(),
            e.getReleaseTime() == null ? null : e.getReleaseTime().name(),
            e.getStatus() == null ? null : e.getStatus().name(),
            e.getEarningsDate() == null ? null : java.sql.Date.valueOf(e.getEarningsDate()),
            e.getId());
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM earnings_date WHERE id = ?", id);
    }

    public boolean existsByStockIdAndEarningsDate(Long stockId, LocalDate earningsDate) {
        String sql = "SELECT COUNT(*) FROM earnings_date WHERE stock_id = ? AND earnings_date = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, stockId, java.sql.Date.valueOf(earningsDate));
        return count != null && count > 0;
    }
}