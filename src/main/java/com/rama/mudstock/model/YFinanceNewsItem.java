package com.rama.mudstock.model;

public class YFinanceNewsItem {
    private String title;
    private String summary;
    /** Published datetime — may be an ISO-8601 string or a Unix timestamp string depending on the Python service. */
    private String published;
    private String url;
    private String source;

    public YFinanceNewsItem() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getPublished() { return published; }
    public void setPublished(String published) { this.published = published; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
