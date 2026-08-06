INSERT INTO system_config (`code`, `value`, `type`, `purpose`, `description`) VALUES
('useage', 'useage', 'String', 'WeeklyAnalystFirmUpdateCronjob', 'Populate weekly analyst firm details from Benzinga API'),
('enabled', 'true', 'boolean', 'WeeklyAnalystFirmUpdateCronjob', 'Weekly Analyst Firm Update > cronjob Enabled'),
('execution', 'hourly', 'String', 'WeeklyAnalystFirmUpdateCronjob', 'CronExpression for the cronjob'),
('minuteHourlyFrequency', '2', 'Integer', 'WeeklyAnalystFirmUpdateCronjob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'WeeklyAnalystFirmUpdateCronjob', 'LastUpdated dateTime'),
('forceExecute', 'false', 'boolean', 'WeeklyAnalystFirmUpdateCronjob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

('useage', 'useage', 'String', 'DailyAnalystRatingCronjob', 'Pull the Analyst rating details from Benzinga API'),
('watchlist-codes', 'MOVING_STOCK,SEMI_WATCHLIST', 'StringArray', 'DailyAnalystRatingCronjob', 'Benzinga Analyst Rating > Watchlist Codes'),
('enabled', 'true', 'boolean', 'DailyAnalystRatingCronjob', 'Benzinga Analyst Rating > cronjob Enabled'),
('execution', 'hourly', 'String', 'DailyAnalystRatingCronjob', 'CronExpression for the cronjob'),
('minuteHourlyFrequency', '2', 'Integer', 'DailyAnalystRatingCronjob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'DailyAnalystRatingCronjob', 'LastUpdated dateTime'),
('forceExecute', 'false', 'boolean', 'DailyAnalystRatingCronjob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),
('forceExecuteDailyDate', '', 'DateTime', 'DailyAnalystRatingCronjob', 'Set the date for forceExecute'),

('useage', 'useage', 'String', 'WeeklyUpcomingEarningCronjob', 'Populate the weekly upcoming earnings for the next week from yfinance'),
('enabled', 'true', 'boolean', 'WeeklyUpcomingEarningCronjob', 'Weekly Upcoming Earning Cronjob > cronjob Enabled'),
('watchlist-codes', 'MOVING_STOCK,SEMI_WATCHLIST', 'StringArray', 'WeeklyUpcomingEarningCronjob', 'Weekly Upcoming Earning Cronjob > Watchlist Codes'),
('execution', 'hourly', 'String', 'WeeklyUpcomingEarningCronjob', 'CronExpression for the cronjob'),
('minuteHourlyFrequency', '12', 'Integer', 'WeeklyUpcomingEarningCronjob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'WeeklyUpcomingEarningCronjob', 'LastUpdated dateTime'),
('forceExecute', 'false', 'boolean', 'WeeklyUpcomingEarningCronjob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

('useage', 'useage', 'String', 'DayStockMovementData', 'Populated the day stock movment data for the current day'),
('enabled', 'true', 'boolean', 'DayStockMovementData', 'Day Stock Movement Data > cronjob Enabled'),
('watchlist-codes', 'MOVING_STOCK,SEMI_WATCHLIST', 'StringArray', 'DayStockMovementData', 'fetch for teh configured watchlist'),
('execution', 'daily', 'String', 'DayStockMovementData', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'DayStockMovementData', 'LastUpdated dateTime'),
('dailyCutOffTime', '22:00', 'DateTime', 'DayStockMovementData', 'Record should only be fetched after the cutoffTime'),
('dailyDate', '2026-07-30', 'DateTime', 'DayStockMovementData', 'Use this date for the current date for the cronjob'),
('forceExecute', 'false', 'boolean', 'DayStockMovementData', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),
('forceExecuteDailyDate', '', 'DateTime', 'DayStockMovementData', 'Set the date for forceExecute'),

('useage', 'useage', 'String', 'DailyMysqlDBDump', 'Dump the Mysql and write into the location'),
('enabled', 'true', 'boolean', 'DailyMysqlDBDump', 'Mysql Stock Dump Enable property'),
('location', '/Users/rama/Library/Mobile Documents/com~apple~CloudDocs/TechExamples/mysql', 'String', 'DailyMysqlDBDump', 'Day Stock Movement Key Map Entry > Watchlist Codes'),
('execution', 'daily', 'String', 'DailyMysqlDBDump', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'DailyMysqlDBDump', 'LastUpdated dateTime'),
('dailyCutOffTime', '22:00', 'DateTime', 'DailyMysqlDBDump', 'Record should only be fetched after the cutoffTime'),
('forceExecute', 'false', 'boolean', 'DailyMysqlDBDump', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

('useage', 'useage', 'String', 'OptionsIntervalAnalyseDailyJob', 'Create and Close Optoin Contract entry'),
('enabled', 'true', 'boolean', 'OptionsIntervalAnalyseDailyJob', 'OptionContractAnalyserDailyJob Enable property'),
('execution', 'minutes', 'String', 'OptionsIntervalAnalyseDailyJob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'OptionsIntervalAnalyseDailyJob', 'Create and Close Optoin Contract entry'),
('minuteHourlyFrequency', '5', 'Integer', 'OptionsIntervalAnalyseDailyJob', 'CronExpression for the cronjob'),
('forceExecute', 'false', 'boolean', 'OptionsIntervalAnalyseDailyJob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

('useage', 'useage', 'String', 'OptionAPISnapshotFetcherJob', 'Fetch Option snapshot data for the given ticker, strike and expiration date'),
('enabled', 'true', 'boolean', 'OptionAPISnapshotFetcherJob', 'OptionAPISnapshotFetcherJob  Enable property'),
('execution', 'minutes', 'String', 'OptionAPISnapshotFetcherJob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'OptionAPISnapshotFetcherJob', 'Create and Close Optoin Contract entry'),
('startTime', '14:30', 'Time', 'OptionAPISnapshotFetcherJob', 'Cronjob Start Time'),
('endTime', '21:00', 'Time', 'OptionAPISnapshotFetcherJob', 'Cronjob End Time'),
('minuteHourlyFrequency', '5', 'Integer', 'OptionAPISnapshotFetcherJob', 'CronExpression for the cronjob'),
('forceExecute', 'false', 'boolean', 'OptionAPISnapshotFetcherJob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

('useage', 'useage', 'String', 'OptionFlatFileSnapshotFetcherJob', 'Fetch Option snapshot data for the given ticker, strike and expiration date'),
('enabled', 'true', 'boolean', 'OptionFlatFileSnapshotFetcherJob', 'OptionFlatFileSnapshotFetcherJob  Enable property'),
('execution', 'daily', 'String', 'OptionFlatFileSnapshotFetcherJob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'OptionFlatFileSnapshotFetcherJob', 'Create and Close Optoin Contract entry'),
('dailyCutOffTime', '9:00', 'DateTime', 'OptionFlatFileSnapshotFetcherJob', 'Record should only be fetched after the cutoffTime'),
('dailyDate', '2026-07-30', 'DateTime', 'OptionFlatFileSnapshotFetcherJob', 'Use this date for the current date for the cronjob'),
('forceExecute', 'false', 'boolean', 'OptionFlatFileSnapshotFetcherJob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),
('forceExecuteDailyDate', '2026-07-30', 'DateTime', 'OptionFlatFileSnapshotFetcherJob', 'Set the date for forceExecute'),

('useage', 'useage', 'String', 'OptionSnapshotIVMetrics', 'Calculate Option IV Metrics after end of the day'),
('enabled', 'true', 'boolean', 'OptionSnapshotIVMetrics', 'OptionSnapshotIVMetrics Enable property'),
('execution', 'daily', 'String', 'OptionSnapshotIVMetrics', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'OptionSnapshotIVMetrics', 'Create and Close Optoin Contract entry'),
('dailyCutOffTime', '22:00', 'DateTime', 'OptionSnapshotIVMetrics', 'Record should only be fetched after the cutoffTime'),
('forceExecute', 'false', 'boolean', 'OptionSnapshotIVMetrics', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

('useage', 'useage', 'String', 'EarningsDetailCronjob', 'Calculate Option IV Metrics after end of the day'),
('enabled', 'true', 'boolean', 'EarningsDetailCronjob', 'EarningsDetailCronjob Enable property'),
('execution', 'daily', 'String', 'EarningsDetailCronjob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'EarningsDetailCronjob', 'Create and Close Optoin Contract entry'),
('dailyCutOffTime', '22:00', 'DateTime', 'EarningsDetailCronjob', 'Record should only be fetched after the cutoffTime'),
('forceExecute', 'false', 'boolean', 'EarningsDetailCronjob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

('SystemWatchlistCodes', 'MOVING_STOCK,SEMI_WATCHLIST', 'StringArray', 'CommonSystemSettings', 'fetch for teh configured watchlist');




CREATE TABLE option_snapshot_flatfile (
    id bigint unsigned NOT NULL AUTO_INCREMENT,

    option_contract_id bigint unsigned NOT NULL,
    stock_id bigint unsigned NOT NULL,
    near_option_snapshot_id bigint unsigned NULL,

    contract_ticker VARCHAR(128),
    opt_volume INT,
    opt_open DECIMAL(6,2),
    opt_close DECIMAL(6,2),
    opt_high DECIMAL(6,2),
    opt_low DECIMAL(6,2),
    unix_time bigint unsigned NOT NULL,
    unix_utc_time DATETIME(6) NOT NULL,
    local_time DATETIME(6) NOT NULL,
    
    stock_ticker VARCHAR(128),
    stock_volume INT,
    stock_open DECIMAL(6,2),
    stock_close DECIMAL(6,2),
    stock_high DECIMAL(6,2),
    stock_low DECIMAL(6,2),

    snapshot_version bigint unsigned NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (`id`),
    
    CONSTRAINT fk_osf_stock 
    FOREIGN KEY (stock_id) 
        REFERENCES stock (id),

    CONSTRAINT fk_osf_contract
        FOREIGN KEY (option_contract_id)
        REFERENCES option_contract(id),
        
	CONSTRAINT fk_osf_near_option_snapshot
		FOREIGN KEY (near_option_snapshot_id) 
        REFERENCES option_snapshot (id),
  
     CONSTRAINT uk_osf_time
        UNIQUE (option_contract_id, unix_time),

    INDEX idx_osf_time (
        option_contract_id,
        unix_time
    )
);