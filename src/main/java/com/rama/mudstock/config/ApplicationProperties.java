package com.rama.mudstock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class ApplicationProperties {

    private OptionAnalysis optionAnalysis = new OptionAnalysis();
    private Massive massive = new Massive();
    private AlphaVantage alphavantage = new AlphaVantage();
    private Python python = new Python();
    private Earnings earnings = new Earnings();
    private DayStockMovementKeyMapEntry dayStockMovementKeyMapEntry = new DayStockMovementKeyMapEntry();
    private Spring spring = new Spring();

    public OptionAnalysis getOptionAnalysis() {
        return optionAnalysis;
    }

    public void setOptionAnalysis(OptionAnalysis optionAnalysis) {
        this.optionAnalysis = optionAnalysis;
    }

    public Massive getMassive() {
        return massive;
    }

    public void setMassive(Massive massive) {
        this.massive = massive;
    }

    public AlphaVantage getAlphavantage() {
        return alphavantage;
    }

    public void setAlphavantage(AlphaVantage alphavantage) {
        this.alphavantage = alphavantage;
    }

    public Python getPython() {
        return python;
    }

    public void setPython(Python python) {
        this.python = python;
    }

    public Earnings getEarnings() {
        return earnings;
    }

    public void setEarnings(Earnings earnings) {
        this.earnings = earnings;
    }

    public DayStockMovementKeyMapEntry getDayStockMovementKeyMapEntry() {
        return dayStockMovementKeyMapEntry;
    }

    public void setDayStockMovementKeyMapEntry(DayStockMovementKeyMapEntry dayStockMovementKeyMapEntry) {
        this.dayStockMovementKeyMapEntry = dayStockMovementKeyMapEntry;
    }

    public Spring getSpring() {
        return spring;
    }

    public void setSpring(Spring spring) {
        this.spring = spring;
    }

    public long getSnapshotRefreshMs() {
        return optionAnalysis.getSnapshotRefreshMs();
    }

    public void setSnapshotRefreshMs(long snapshotRefreshMs) {
        this.optionAnalysis.setSnapshotRefreshMs(snapshotRefreshMs);
    }

    public static class OptionAnalysis {
        private long snapshotRefreshMs = 120000L;

        public long getSnapshotRefreshMs() {
            return snapshotRefreshMs;
        }

        public void setSnapshotRefreshMs(long snapshotRefreshMs) {
            this.snapshotRefreshMs = snapshotRefreshMs;
        }
    }

    public static class Massive {
        private String baseUrl;
        private String apikey;
        private String openClose = "";
        private String tickerAggregate = "";
        private String optionSnapshot = "";
        private String benzingaFirm = "";
        private String benzingaFirmId = "";
        private String benzingaAnalyst = "";
        private String benzingaAnalystRating = "";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApikey() {
            return apikey;
        }

        public void setApikey(String apikey) {
            this.apikey = apikey;
        }

        public String getOpenClose() {
            return openClose;
        }

        public void setOpenClose(String openClose) {
            this.openClose = openClose;
        }

        public String getTickerAggregate() {
            return tickerAggregate;
        }

        public void setTickerAggregate(String tickerAggregate) {
            this.tickerAggregate = tickerAggregate;
        }

        public String getOptionSnapshot() {
            return optionSnapshot;
        }

        public void setOptionSnapshot(String optionSnapshot) {
            this.optionSnapshot = optionSnapshot;
        }

        public String getBenzingaFirm() {
            return benzingaFirm;
        }

        public void setBenzingaFirm(String benzingaFirm) {
            this.benzingaFirm = benzingaFirm;
        }

        public String getBenzingaFirmId() {
            return benzingaFirmId;
        }

        public void setBenzingaFirmId(String benzingaFirmId) {
            this.benzingaFirmId = benzingaFirmId;
        }

        public String getBenzingaAnalyst() {
            return benzingaAnalyst;
        }

        public void setBenzingaAnalyst(String benzingaAnalyst) {
            this.benzingaAnalyst = benzingaAnalyst;
        }

        public String getBenzingaAnalystRating() {
            return benzingaAnalystRating;
        }

        public void setBenzingaAnalystRating(String benzingaAnalystRating) {
            this.benzingaAnalystRating = benzingaAnalystRating;
        }
    }

    public static class AlphaVantage {
        private String function;
        private String apikey;

        public String getFunction() {
            return function;
        }

        public void setFunction(String function) {
            this.function = function;
        }

        public String getApikey() {
            return apikey;
        }

        public void setApikey(String apikey) {
            this.apikey = apikey;
        }
    }

    public static class Python {
        private String yfinanceUrl;
        private String ticker;

        public String getYfinanceUrl() {
            return yfinanceUrl;
        }

        public void setYfinanceUrl(String yfinanceUrl) {
            this.yfinanceUrl = yfinanceUrl;
        }

        public String getTicker() {
            return ticker;
        }

        public void setTicker(String ticker) {
            this.ticker = ticker;
        }
    }

    public static class Earnings {
        private Api api = new Api();

        public Api getApi() {
            return api;
        }

        public void setApi(Api api) {
            this.api = api;
        }

        public static class Api {
            private int callsPerMinute = 5;

            public int getCallsPerMinute() {
                return callsPerMinute;
            }

            public void setCallsPerMinute(int callsPerMinute) {
                this.callsPerMinute = callsPerMinute;
            }
        }
    }

    public static class DayStockMovementKeyMapEntry {
        private String cron = "";

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class Spring {
        private Datasource datasource = new Datasource();

        public Datasource getDatasource() {
            return datasource;
        }

        public void setDatasource(Datasource datasource) {
            this.datasource = datasource;
        }

        public static class Datasource {
            private String url;
            private String username;
            private String password = "";

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }
        }
    }
}