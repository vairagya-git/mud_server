package com.rama.mudstock.model;

import java.util.List;
import java.util.Map;

/**
 * DTO for the yfinance Python REST response at /ticker/{symbol}.
 *
 * Expected JSON shape:
 * {
 *   "ticker": "SNDK",
 *   "news": [
 *     { "title": "...", "summary": "...", "published": "...", "url": "...", "source": "..." }
 *   ],
 *   "calendar": {
 *     "Earnings Date": ["2026-07-15", "2026-10-22"]
 *   }
 * }
 *
 * The "calendar" values may be a single string or a list of date strings.
 */
public class YFinanceTickerResponse {
    private String ticker;
    private List<YFinanceNewsItem> news;
    /** calendar.get("Earnings Date") holds one or more date strings. */
    private Map<String, Object> calendar;

    public YFinanceTickerResponse() {}

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public List<YFinanceNewsItem> getNews() { return news; }
    public void setNews(List<YFinanceNewsItem> news) { this.news = news; }

    public Map<String, Object> getCalendar() { return calendar; }
    public void setCalendar(Map<String, Object> calendar) { this.calendar = calendar; }
}
