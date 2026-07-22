

/* Analyst Rating */
CREATE TABLE `firm` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `benzinga_firm_id` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `currency` varchar(32) DEFAULT NULL,
  `last_updated` date DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT unique_earnings_upcoming UNIQUE (`benzinga_firm_id`, `name`)
) ENGINE=InnoDB;

select * from firm_analyst
where benzinga_analyst_id = "585939c2ee7bf600010fe946";

SELECT * from firm 
where benzinga_firm_id = "6065edf1a93f970001bad529";

CREATE TABLE `firm_analyst` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `firm_id` bigint unsigned NOT NULL,
  `benzinga_analyst_id` varchar(64) NOT NULL,
  `benzinga_firm_id` varchar(64) NOT NULL,
  `full_name` varchar(128) NOT NULL,
  `last_updated` date DEFAULT NULL,
  `overall_avg_return` decimal(20,2) DEFAULT NULL,
  `overall_avg_return_percentile` decimal(20,2) DEFAULT NULL,
  `overall_success_rate` decimal(20,2) DEFAULT NULL,
  `smart_score` decimal(20,2) DEFAULT NULL,
  `total_ratings` decimal(20,2) DEFAULT NULL,
  `total_ratings_percentile` decimal(20,2) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_firm` (`firm_id`),
  CONSTRAINT `fk_firm` FOREIGN KEY (`firm_id`) REFERENCES `firm` (`id`),
  CONSTRAINT unique_earnings_upcoming UNIQUE (`benzinga_analyst_id`, `benzinga_firm_id`)
) ENGINE=InnoDB;

select * from firm_analyst_stock_rating;

drop table firm_analyst_stock_rating;

CREATE TABLE `firm_analyst_stock_rating` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `firm_analyst_id` bigint unsigned NOT NULL,
  `firm_id` bigint unsigned NOT NULL,
  `stock_id` bigint unsigned NOT NULL,
   `rating_action` ENUM('maintains', 'downgrades', 'upgrades', 'initiates_coverage_on', 'reiterates', "reinstates", 'resumes_coverage', 'suspends_coverage', "assumes") NOT NULL,
   `price_target_action` ENUM('maintains', 'lowers', 'raises', 'announces', 'removes', 'resumes', "none") NOT NULL default 'none',
   `rating` ENUM('buy', "strong buy", 'outperform', 'underweight', "underperform", "peer perform", 'overweight', 'positive', 'equal-weight', 'sector perform', 'sector outperform', 'sector weight', "market perform", 'market outperform', 'neutral', 'hold', "sell", "perform", "reduce", "in-line") NOT NULL,
   `previous_rating` ENUM('buy', "strong buy", 'outperform','underweight', "underperform", "peer perform", 'overweight', 'positive', 'equal-weight', 'sector perform', 'sector outperform', 'sector weight', "market perform", 'market outperform', 'neutral', 'hold', "sell", "perform", "reduce", "in-line", "none") NOT NULL default 'none',
   `price_target` decimal(20,0) NOT NULL,
   `previous_price_target`  decimal(20,0) DEFAULT NULL,
   `price_percent_change`  decimal(20,2) DEFAULT NULL,
   `adjusted_price_target` decimal(20,0) DEFAULT NULL,
   `previous_adjusted_price_target` decimal(20,0) DEFAULT NULL,
   `importance` int NOT NULL, 
   `last_updated` date DEFAULT NULL,
   `date` date DEFAULT NULL,
   `benzinga_calendar_url` varchar(512) NOT NULL,
   `benzinga_news_url` varchar(512) NOT NULL,
   `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
   `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  KEY `fk_fasr_stock` (`stock_id`),
  CONSTRAINT `fk_fasr_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`),
  KEY `fk_fasr_analyst` (`firm_analyst_id`),
  CONSTRAINT `fk_fasr_analyst` FOREIGN KEY (`firm_analyst_id`) REFERENCES `firm_analyst` (`id`),
  KEY `fk_fasr_firm` (`firm_id`),
  CONSTRAINT `fk_fasr_firm` FOREIGN KEY (`firm_id`) REFERENCES `firm` (`id`),
  CONSTRAINT unique_earnings_upcoming UNIQUE (`firm_analyst_id`, `firm_id`, `stock_id`, `date`)
) ENGINE=InnoDB;

/*  Earnings Data */

select * from earnings_date;

CREATE TABLE `earnings_date` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `stock_id` bigint unsigned NOT NULL,
  `quarter` varchar(64) DEFAULT NULL,
  `releaseTime` ENUM('AFTER_MARKET', 'BEFORE_MARKET') NOT NULL,
  `status` ENUM('NEW', 'UPCOMING', 'PROCESSING', 'PROCESSED') NOT NULL DEFAULT 'NEW',
  `earnings_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_earnings_date` (`stock_id`),
  CONSTRAINT `fk_earnings_date` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`),
  CONSTRAINT unique_earnings_upcoming UNIQUE (`stock_id`, `earnings_date`)
) ENGINE=InnoDB;

CREATE TABLE `earnings_date_entry` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `stock_id` bigint unsigned NOT NULL,
  `earnings_date_id` bigint unsigned NOT NULL,
  `datePeriod` ENUM('OneWeekBefore', '4DaysBefore', '3DaysBefore', '2DaysBefore', '1DayBefore', 'EarningDay','1DayAfter', '2DaysAfter', '3DaysAfter', '4DaysAfter', 'OneWeekAfter', 'TwoWeekAfter') NOT NULL,
  `Open` decimal(20,2) DEFAULT NULL,
  `close` decimal(20,2) DEFAULT NULL,
  `high` decimal(20,2) DEFAULT NULL,
  `low` decimal(20,2) DEFAULT NULL,
  `volume` decimal(20,2) DEFAULT NULL,
  `percentage` decimal(20,2) DEFAULT NULL,
  `value` decimal(20,2) DEFAULT NULL,
  `from` date DEFAULT NULL,
  `status` ENUM('NEW', 'DONE') NOT NULL DEFAULT 'NEW',
  PRIMARY KEY (`id`),
  KEY `fk_ede_stock` (`stock_id`),
  KEY `fk_ede_earnings_date` (`earnings_date_id`),
  CONSTRAINT `fk_ede_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`),
  CONSTRAINT `fk_ede_earnings_date` FOREIGN KEY (`earnings_date_id`) REFERENCES `earnings_date` (`id`),
  CONSTRAINT unique_earnings_upcoming UNIQUE (`stock_id`, `earnings_date_id`,`datePeriod`)
) ENGINE=InnoDB;


select * from day_stock_movement_entry;

/*  Day Stock Movement */

select * from day_stock_movement_key
order by id desc;

DELETE dsmm
FROM day_stock_movement_map dsmm
JOIN day_stock_movement_key dsmk
    ON dsmm.day_stock_movement_key_id = dsmk.id
WHERE dsmk.code = '16_JUL_26_MOVING_STOCK';
DELETE from day_stock_movement_key where code = "16_JUL_26_MOVING_STOCK";

CREATE TABLE `day_stock_movement_key` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `description` varchar(512) NOT NULL,
  `date` date DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `unique_day_stock_movement_key` UNIQUE (`code`)
) ENGINE=InnoDB;

CREATE TABLE `day_stock_movement_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `stock_id` bigint unsigned NOT NULL,
  `day_stock_movement_key_id` bigint unsigned NOT NULL,
  `status` ENUM('NEW', 'PROCESSED', 'MARKET_CLOSED') NOT NULL default 'NEW',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_dsm_stock` (`stock_id`),
  KEY `fk_dsm_day_stock_movement_key` (`day_stock_movement_key_id`),
  CONSTRAINT `fk_dsm_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`),
  CONSTRAINT `fk_dsm_day_stock_movement_key` FOREIGN KEY (`day_stock_movement_key_id`) REFERENCES `day_stock_movement_key` (`id`),
  CONSTRAINT `unique_dsm_stock_key` UNIQUE (`stock_id`, `day_stock_movement_key_id`)
) ENGINE=InnoDB;

select * from day_stock_movement_entry
order by id desc

CREATE TABLE `day_stock_movement_entry` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `day_stock_movement_map_id` bigint unsigned NOT NULL,
  `pre_day_close` decimal(20,2) NOT NULL,
  `cur_day_open` decimal(20,2) NOT NULL,
  `cur_day_close` decimal(20,2) NOT NULL,
  `cur_day_high` decimal(20,2) NOT NULL,
  `cur_day_low` decimal(20,2) NOT NULL,
  `cur_day_vol_weight` decimal(20,2) NOT NULL,
  `cur_day_volume` bigint unsigned NOT NULL,
  `change_percent` decimal(20,2) DEFAULT NULL,
  `day_opening_change_percent` decimal(20,2) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_dsme_day_stock_movement_map` (`day_stock_movement_map_id`),
  CONSTRAINT `fk_dsme_day_stock_movement_map` FOREIGN KEY (`day_stock_movement_map_id`) REFERENCES `day_stock_movement_map` (`id`),
  CONSTRAINT `unique_day_stock_movement_entry` UNIQUE (`day_stock_movement_map_id`)
) ENGINE=InnoDB;

/****** MASTER TABLE ********/

CREATE TABLE `master_market_holidays` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `year` varchar(64) NOT NULL,
  `country` varchar(64) NOT NULL,
  `holiday_date` date DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT unique_day_event_master UNIQUE (`year`, `country`, `holiday_date`)
) ENGINE=InnoDB;

INSERT INTO master_market_holidays (`year`, `country`, `holiday_date`) VALUES
('2026', 'USA', '2026-01-01'),
('2026', 'USA', '2026-01-19'),
('2026', 'USA', '2026-02-16'),
('2026', 'USA', '2026-04-03'),
('2026', 'USA', '2026-05-25'),
('2026', 'USA', '2026-06-19'),
('2026', 'USA', '2026-07-03'),
('2026', 'USA', '2026-09-07'),
('2026', 'USA', '2026-11-26'),
('2026', 'USA', '2026-12-25'),

('2027', 'USA', '2027-01-01'),
('2027', 'USA', '2027-01-18'),
('2027', 'USA', '2027-02-15'),
('2027', 'USA', '2027-03-26'),
('2027', 'USA', '2027-05-31'),
('2027', 'USA', '2027-06-18'),
('2027', 'USA', '2027-07-05'),
('2027', 'USA', '2027-09-06'),
('2027', 'USA', '2027-11-25'),
('2027', 'USA', '2027-12-24');

CREATE TABLE `system_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `value` varchar(256) NOT NULL,
  `type` varchar(64) NOT NULL,
  `purpose` varchar(64) NOT NULL,
  `description` varchar(1024) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT unique_day_event_master UNIQUE (`code`, `purpose`)
) ENGINE=InnoDB;

delete from system_config;

INSERT INTO system_config (`code`, `value`, `type`, `purpose`, `description`) VALUES
/* WeeklyAnalystFirmUpdateCronjob Settings*/
('useage', 'useage', 'String', 'WeeklyAnalystFirmUpdateCronjob', 'Populate weekly analyst firm details from Benzinga API'),
('enabled', 'true', 'boolean', 'WeeklyAnalystFirmUpdateCronjob', 'Weekly Analyst Firm Update > cronjob Enabled'),
('execution', 'hourly', 'String', 'WeeklyAnalystFirmUpdateCronjob', 'CronExpression for the cronjob'),
('minuteHourlyFrequency', '2', 'Integer', 'WeeklyAnalystFirmUpdateCronjob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'WeeklyAnalystFirmUpdateCronjob', 'LastUpdated dateTime'),
('forceExecute', 'false', 'boolean', 'WeeklyAnalystFirmUpdateCronjob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

/* DailyAnalystRatingCronjob Settings*/
('useage', 'useage', 'String', 'DailyAnalystRatingCronjob', 'Pull the Analyst rating details from Benzinga API'),
('watchlist-codes', 'MOVING_STOCK,SEMI_WATCHLIST', 'StringArray', 'DailyAnalystRatingCronjob', 'Benzinga Analyst Rating > Watchlist Codes'),
('enabled', 'true', 'boolean', 'DailyAnalystRatingCronjob', 'Benzinga Analyst Rating > cronjob Enabled'),
('execution', 'hourly', 'String', 'DailyAnalystRatingCronjob', 'CronExpression for the cronjob'),
('minuteHourlyFrequency', '2', 'Integer', 'DailyAnalystRatingCronjob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'DailyAnalystRatingCronjob', 'LastUpdated dateTime'),
('forceExecute', 'false', 'boolean', 'DailyAnalystRatingCronjob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

/* WeeklyUpcomingEarningCronjob Settings*/
('useage', 'useage', 'String', 'WeeklyUpcomingEarningCronjob', 'Populate the weekly upcoming earnings for the next week from yfinance'),
('enabled', 'true', 'boolean', 'WeeklyUpcomingEarningCronjob', 'Weekly Upcoming Earning Cronjob > cronjob Enabled'),
('watchlist-codes', 'MOVING_STOCK,SEMI_WATCHLIST', 'StringArray', 'WeeklyUpcomingEarningCronjob', 'Weekly Upcoming Earning Cronjob > Watchlist Codes'),
('execution', 'hourly', 'String', 'WeeklyUpcomingEarningCronjob', 'CronExpression for the cronjob'),
('minuteHourlyFrequency', '12', 'Integer', 'WeeklyUpcomingEarningCronjob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'WeeklyUpcomingEarningCronjob', 'LastUpdated dateTime'),
('forceExecute', 'false', 'boolean', 'WeeklyUpcomingEarningCronjob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

/*DayStockMovementData Settings*/
('useage', 'useage', 'String', 'DayStockMovementData', 'Populated the day stock movment data for the current day'),
('enabled', 'true', 'boolean', 'DayStockMovementData', 'Day Stock Movement Data > cronjob Enabled'),
('execution', 'daily', 'String', 'DayStockMovementData', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'DayStockMovementData', 'LastUpdated dateTime'),
('cutOffTime', '22:00', 'DateTime', 'DayStockMovementData', 'Record should only be fetched after the cutoffTime'),
('forceExecute', 'false', 'boolean', 'DayStockMovementData', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),


/*DayStockMovementCleanup Settings*/
('useage', 'useage', 'String', 'DayStockMovementCleanup', 'Cleanup the day stock movement data for the current day'),
('enabled', 'true', 'boolean', 'DayStockMovementCleanup', 'Day Stock Movement Cleanup > cronjob Enabled'),
('execution', 'daily', 'String', 'DayStockMovementCleanup', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'DayStockMovementCleanup', 'LastUpdated dateTime'),
('cutOffTime', '23:00', 'DateTime', 'DayStockMovementCleanup', 'Record should only be fetched after the cutoffTime'),
('forceExecute', 'false', 'boolean', 'DayStockMovementCleanup', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

/*DayStockMovementKeyMapEntry Settings*/
('useage', 'useage', 'String', 'DayStockMovementKeyMapEntry', 'Populate the day stock movement key map entry for the current day'),
('enabled', 'true', 'boolean', 'DayStockMovementKeyMapEntry', 'Day Stock Movement Key Map Entry > cronjob Enabled'),
('watchlist-codes', 'MOVING_STOCK,SEMI_WATCHLIST', 'StringArray', 'DayStockMovementKeyMapEntry', 'Day Stock Movement Key Map Entry > Watchlist Codes'),
('execution', 'daily', 'String', 'DayStockMovementKeyMapEntry', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'DayStockMovementKeyMapEntry', 'LastUpdated dateTime'),
('cutOffTime', '19:00', 'DateTime', 'DayStockMovementKeyMapEntry', 'Record should only be fetched after the cutoffTime'),
('minuteHourlyFrequency', '1', 'DateTime', 'DayStockMovementKeyMapEntry', 'Incase want to push date temporarily. Change execution to minutes'),
('forceExecute', 'false', 'boolean', 'DayStockMovementKeyMapEntry', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

/*MYSQL DB Dump Settings*/
('useage', 'useage', 'String', 'DailyMysqlDBDump', 'Dump the Mysql and write into the location'),
('enabled', 'true', 'boolean', 'DailyMysqlDBDump', 'Mysql Stock Dump Enable property'),
('location', '/Users/rama/Library/Mobile Documents/com~apple~CloudDocs/TechExamples/mysql', 'String', 'DailyMysqlDBDump', 'Day Stock Movement Key Map Entry > Watchlist Codes'),
('execution', 'daily', 'String', 'DailyMysqlDBDump', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'DailyMysqlDBDump', 'LastUpdated dateTime'),
('cutOffTime', '22:00', 'DateTime', 'DailyMysqlDBDump', 'Record should only be fetched after the cutoffTime'),
('forceExecute', 'false', 'boolean', 'DailyMysqlDBDump', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

/*OptionsIntervalAnalyseDailyJob Settings*/
('useage', 'useage', 'String', 'OptionsIntervalAnalyseDailyJob', 'Create and Close Optoin Contract entry'),
('enabled', 'true', 'boolean', 'OptionsIntervalAnalyseDailyJob', 'OptionContractAnalyserDailyJob Enable property'),
('execution', 'minutes', 'String', 'OptionsIntervalAnalyseDailyJob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'OptionsIntervalAnalyseDailyJob', 'Create and Close Optoin Contract entry'),
('minuteHourlyFrequency', '5', 'Integer', 'OptionsIntervalAnalyseDailyJob', 'CronExpression for the cronjob'),
('forceExecute', 'false', 'boolean', 'OptionsIntervalAnalyseDailyJob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

/*OptionSnapshotFetcherJob Settings*/
('useage', 'useage', 'String', 'OptionSnapshotFetcherJob', 'Fetch Option snapshot data for the given ticker, strike and expiration date'),
('enabled', 'true', 'boolean', 'OptionSnapshotFetcherJob', 'OptionSnapshotFetcherJob Enable property'),
('execution', 'minutes', 'String', 'OptionSnapshotFetcherJob', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'OptionSnapshotFetcherJob', 'Create and Close Optoin Contract entry'),
('startTime', '14:30', 'Time', 'OptionSnapshotFetcherJob', 'Cronjob Start Time'),
('endTime', '21:00', 'Time', 'OptionSnapshotFetcherJob', 'Cronjob End Time'),
('minuteHourlyFrequency', '5', 'Integer', 'OptionSnapshotFetcherJob', 'CronExpression for the cronjob'),
('forceExecute', 'false', 'boolean', 'OptionSnapshotFetcherJob', 'Set this flag if you want to execute this cronjob by overriding all the other flag'),

/*OptionSnapshotIVMetrics Settings*/
('useage', 'useage', 'String', 'OptionSnapshotIVMetrics', 'Calculate Option IV Metrics after end of the day'),
('enabled', 'true', 'boolean', 'OptionSnapshotIVMetrics', 'OptionSnapshotIVMetrics Enable property'),
('execution', 'daily', 'String', 'OptionSnapshotIVMetrics', 'CronExpression for the cronjob'),
('lastUpdated', '', 'DateTime', 'OptionSnapshotIVMetrics', 'Create and Close Optoin Contract entry'),
('cutOffTime', '22:00', 'DateTime', 'OptionSnapshotIVMetrics', 'Record should only be fetched after the cutoffTime'),
('forceExecute', 'false', 'boolean', 'OptionSnapshotIVMetrics', 'Set this flag if you want to execute this cronjob by overriding all the other flag');


/**** OPTION CONTRACT START ****/

select * from options_interval_analyse;

CREATE TABLE options_interval_analyse (
    id  bigint unsigned NOT NULL AUTO_INCREMENT,
    stock_id bigint unsigned NOT NULL,
    contract_type ENUM('CALL', 'PUT', 'BOTH') NOT NULL,
    status ENUM('CREATE_CONTRACT', 'ACTIVE', 'CLOSE', 'PARTIALLY_COMPLETED', 'COMPLETED') NOT NULL default 'CREATE_CONTRACT',
    expiration_date DATE NOT NULL,
    strike_from DECIMAL(12,4) NOT NULL,
    strike_to DECIMAL(12,4) NOT NULL,
    `interval` DECIMAL(12,2) NOT NULL,
    created_at timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `fk_ota_stock` (`stock_id`),
  CONSTRAINT `fk_ota_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`),
  CONSTRAINT `unique_ota_option_to_analyse` UNIQUE (`stock_id`, contract_type,`expiration_date`,strike_from,strike_to)
);

ALTER TABLE options_interval_analyse
    modify COLUMN
    status ENUM('CREATE_CONTRACT', 'ACTIVE', 'CLOSE', 'PARTIALLY_COMPLETED','COMPLETED') NOT NULL default 'CREATE_CONTRACT';

select * from option_contract;

CREATE TABLE option_contract (
    id  bigint unsigned NOT NULL AUTO_INCREMENT,
    stock_id bigint unsigned NOT NULL,
    contract_type ENUM('CALL', 'PUT') NOT NULL,
    status ENUM('ACTIVE', 'COMPLETED') NOT NULL default 'ACTIVE',
    exercise_style VARCHAR(32),
    expiration_date DATE NOT NULL,
    strike_price DECIMAL(12,4) NOT NULL,
    shares_per_contract INT NOT NULL DEFAULT 100,
  contract_ticker VARCHAR(128),
    created_at timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
    KEY `fk_optcon_stock` (`stock_id`),
    CONSTRAINT `fk_optcon_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`),
    CONSTRAINT uk_optcon_contract_ticker UNIQUE (contract_ticker),
  CONSTRAINT `unique_optcon_option_contract` UNIQUE (`stock_id`, contract_type,`expiration_date`,contract_ticker)
);


select * from option_snapshot os
join option_contract oc on os.option_contract_id = oc.id
where oc.contract_ticker = "O:INTC260717C00100000";

delete from option_snapshot;
drop table option_snapshot;
drop table option_strategy_leg;
drop table option_strategy_leg_snapshot;

select * from option_snapshot;

CREATE TABLE option_snapshot (
    id bigint unsigned NOT NULL AUTO_INCREMENT,

    option_contract_id bigint unsigned NOT NULL,
    stock_id bigint unsigned NOT NULL,

-- Time this record was collected by the Java application
    snapshot_time DATETIME(6) NOT NULL,

-- Provider timestamps
    option_quote_time DATETIME(6),
    option_trade_time DATETIME(6),
    underlying_time DATETIME(6),

    underlying_price DECIMAL(14,4) NOT NULL,
    break_even_price DECIMAL(14,4),
    change_to_break_even DECIMAL(14,4),

-- Greek
    bid DECIMAL(6,2),
    ask DECIMAL(6,2),
    midpoint DECIMAL(6,2),
    last_trade_price DECIMAL(6,2),
    bid_size INT,
    ask_size INT,
    last_trade_size INT,

# Greek
    implied_volatility DECIMAL(6,2),
    open_interest INT,
    day_volume INT,

# Greek
    delta DECIMAL(12,3),
    gamma DECIMAL(12,3),
    theta DECIMAL(12,3),
    vega DECIMAL(12,3),

    quote_timeframe ENUM('DELAYED', 'REAL-TIME') NOT NULL default 'DELAYED',
    underlying_timeframe ENUM('DELAYED', 'REAL-TIME') NOT NULL default 'DELAYED',
    trade_timeframe ENUM('DELAYED', 'REAL-TIME') NOT NULL DEFAULT 'DELAYED',
    
    bid_exchange INT,
    ask_exchange INT,
    last_trade_exchange INT,

    snapshot_version bigint unsigned NOT NULL;
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (`id`),
    
    CONSTRAINT fk_option_snapshot_stock 
    FOREIGN KEY (stock_id) 
        REFERENCES stock (id),

    CONSTRAINT fk_option_snapshot_contract
        FOREIGN KEY (option_contract_id)
        REFERENCES option_contract(id),
  
     CONSTRAINT uk_option_snapshot_contract_time
        UNIQUE (option_contract_id, option_quote_time),

    INDEX idx_snapshot_contract_time (
        option_contract_id,
        option_quote_time
    )
);


SELECT
  id,
  created_at,
  UNIX_TIMESTAMP(DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:00')) AS derived_snapshot_version
FROM option_snapshot
ORDER BY created_at
LIMIT 50;


-- MySQL 8.0+ (uses window functions)
-- Rule: start a new group only when gap from previous row is > 120 seconds.
-- snapshot_version = UNIX timestamp of first created_at in that group.

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_snapshot_group;
CREATE TEMPORARY TABLE tmp_snapshot_group AS
SELECT
    t.id,
    t.created_at,
    SUM(t.is_new_group) OVER (ORDER BY t.created_at, t.id) AS grp_id
FROM (
    SELECT
        os.id,
        os.created_at,
        CASE
            WHEN LAG(os.created_at) OVER (ORDER BY os.created_at, os.id) IS NULL THEN 1
            WHEN TIMESTAMPDIFF(
                    SECOND,
                    LAG(os.created_at) OVER (ORDER BY os.created_at, os.id),
                    os.created_at
                 ) > 120 THEN 1
            ELSE 0
        END AS is_new_group
    FROM option_snapshot os
    WHERE os.created_at IS NOT NULL
) t;

DROP TEMPORARY TABLE IF EXISTS tmp_snapshot_version;
CREATE TEMPORARY TABLE tmp_snapshot_version AS
SELECT
    g.id,
    UNIX_TIMESTAMP(s.group_start_time) AS new_snapshot_version
FROM tmp_snapshot_group g
JOIN (
    SELECT
        grp_id,
        MIN(created_at) AS group_start_time
    FROM tmp_snapshot_group
    GROUP BY grp_id
) s
ON s.grp_id = g.grp_id;

UPDATE option_snapshot os
JOIN tmp_snapshot_version v ON v.id = os.id
SET os.snapshot_version = v.new_snapshot_version;

COMMIT;

-- Validation: one snapshot_version per computed group
SELECT
    g.grp_id,
    MIN(g.created_at) AS group_start,
    MAX(g.created_at) AS group_end,
    COUNT(*) AS rows_in_group,
    COUNT(DISTINCT os.snapshot_version) AS distinct_versions
FROM tmp_snapshot_group g
JOIN option_snapshot os ON os.id = g.id
GROUP BY g.grp_id
HAVING COUNT(DISTINCT os.snapshot_version) > 1;

ALTER TABLE option_snapshot
    ADD COLUMN `snapshot_version` bigint unsigned NOT NULL
    AFTER `last_trade_exchange`;

CREATE TABLE option_strategy_definition (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    strategy_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,

    minimum_legs SMALLINT NOT NULL,
    maximum_legs SMALLINT NOT NULL,

    allow_roll BOOLEAN NOT NULL DEFAULT TRUE,
    allow_partial_close BOOLEAN NOT NULL DEFAULT TRUE,
    allow_rebalance BOOLEAN NOT NULL DEFAULT TRUE,

    display_order SMALLINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_strategy_definition_code (strategy_code)
);

INSERT INTO option_strategy_definition
(
    strategy_code,
    display_name,
    description,
    minimum_legs,
    maximum_legs,
    allow_roll,
    allow_partial_close,
    allow_rebalance,
    display_order,
    active
)
VALUES
('LONG_CALL',             'Long Call',             'Buy a single call option.',                                        1, 1, TRUE, TRUE, TRUE,  1, TRUE),
('LONG_PUT',              'Long Put',              'Buy a single put option.',                                         1, 1, TRUE, TRUE, TRUE,  2, TRUE),
('LONG_STRADDLE',         'Long Straddle',         'Buy a call and put with the same strike.',                         2, 2, TRUE, TRUE, TRUE,  3, TRUE),
('LONG_STRANGLE',         'Long Strangle',         'Buy a call and put with different strikes.',                       2, 2, TRUE, TRUE, TRUE,  4, TRUE),
('BULL_CALL_SPREAD',      'Bull Call Spread',      'Bullish debit spread using calls.',                               2, 2, TRUE, TRUE, TRUE,  5, TRUE),
('BEAR_CALL_SPREAD',      'Bear Call Spread',      'Bearish credit spread using calls.',                              2, 2, TRUE, TRUE, TRUE,  6, TRUE),
('BULL_PUT_SPREAD',       'Bull Put Spread',       'Bullish credit spread using puts.',                               2, 2, TRUE, TRUE, TRUE,  7, TRUE),
('BEAR_PUT_SPREAD',       'Bear Put Spread',       'Bearish debit spread using puts.',                                2, 2, TRUE, TRUE, TRUE,  8, TRUE),
('IRON_CONDOR',           'Iron Condor',           'Neutral four-leg premium strategy.',                              4, 4, TRUE, TRUE, TRUE,  9, TRUE),
('IRON_BUTTERFLY',        'Iron Butterfly',        'Neutral four-leg strategy centered around one strike.',           4, 4, TRUE, TRUE, TRUE, 10, TRUE),
('REVERSE_IRON_CONDOR',   'Reverse Iron Condor',   'Debit version of an Iron Condor.',                                4, 4, TRUE, TRUE, TRUE, 11, TRUE),
('CUSTOM',                'Custom Strategy',       'User-defined option strategy.',                                   1, 20, TRUE, TRUE, TRUE, 99, TRUE);


CREATE TABLE option_strategy_definition_leg (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    strategy_definition_id BIGINT UNSIGNED NOT NULL,

    leg_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,

    contract_type ENUM(
        'CALL',
        'PUT'
    ) NOT NULL,

    position_side ENUM(
        'LONG',
        'SHORT'
    ) NOT NULL,

    quantity SMALLINT UNSIGNED NOT NULL DEFAULT 1,

    leg_order SMALLINT UNSIGNED NOT NULL,

    expiration_group SMALLINT UNSIGNED NOT NULL DEFAULT 1,

    required BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_strategy_definition_leg
        FOREIGN KEY (strategy_definition_id)
        REFERENCES option_strategy_definition (id),

    UNIQUE KEY uk_strategy_definition_leg_code (
        strategy_definition_id,
        leg_code
    ),

    UNIQUE KEY uk_strategy_definition_leg_order (
        strategy_definition_id,
        leg_order
    )
);

-- ============================================================
-- LONG CALL
-- ============================================================

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_CALL', 'Long Call', 'CALL', 'LONG', 1, 1
FROM option_strategy_definition
WHERE strategy_code = 'LONG_CALL';


-- ============================================================
-- LONG PUT
-- ============================================================

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_PUT', 'Long Put', 'PUT', 'LONG', 1, 1
FROM option_strategy_definition
WHERE strategy_code = 'LONG_PUT';


-- ============================================================
-- LONG STRADDLE
-- ============================================================

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_CALL', 'Long Call', 'CALL', 'LONG', 1, 1
FROM option_strategy_definition
WHERE strategy_code = 'LONG_STRADDLE';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_PUT', 'Long Put', 'PUT', 'LONG', 1, 2
FROM option_strategy_definition
WHERE strategy_code = 'LONG_STRADDLE';


-- ============================================================
-- LONG STRANGLE
-- ============================================================

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_CALL', 'Long Call', 'CALL', 'LONG', 1, 1
FROM option_strategy_definition
WHERE strategy_code = 'LONG_STRANGLE';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_PUT', 'Long Put', 'PUT', 'LONG', 1, 2
FROM option_strategy_definition
WHERE strategy_code = 'LONG_STRANGLE';


-- ============================================================
-- BULL CALL SPREAD
-- ============================================================

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_CALL', 'Long Call', 'CALL', 'LONG', 1, 1
FROM option_strategy_definition
WHERE strategy_code = 'BULL_CALL_SPREAD';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'SHORT_CALL', 'Short Call', 'CALL', 'SHORT', 1, 2
FROM option_strategy_definition
WHERE strategy_code = 'BULL_CALL_SPREAD';


-- ============================================================
-- BEAR CALL SPREAD
-- ============================================================

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'SHORT_CALL', 'Short Call', 'CALL', 'SHORT', 1, 1
FROM option_strategy_definition
WHERE strategy_code = 'BEAR_CALL_SPREAD';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_CALL', 'Long Call', 'CALL', 'LONG', 1, 2
FROM option_strategy_definition
WHERE strategy_code = 'BEAR_CALL_SPREAD';


-- ============================================================
-- BULL PUT SPREAD
-- ============================================================

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'SHORT_PUT', 'Short Put', 'PUT', 'SHORT', 1, 1
FROM option_strategy_definition
WHERE strategy_code = 'BULL_PUT_SPREAD';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_PUT', 'Long Put', 'PUT', 'LONG', 1, 2
FROM option_strategy_definition
WHERE strategy_code = 'BULL_PUT_SPREAD';


-- ============================================================
-- BEAR PUT SPREAD
-- ============================================================

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_PUT', 'Long Put', 'PUT', 'LONG', 1, 1
FROM option_strategy_definition
WHERE strategy_code = 'BEAR_PUT_SPREAD';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'SHORT_PUT', 'Short Put', 'PUT', 'SHORT', 1, 2
FROM option_strategy_definition
WHERE strategy_code = 'BEAR_PUT_SPREAD';


-- ============================================================
-- IRON CONDOR
-- ============================================================

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_PUT', 'Long Put', 'PUT', 'LONG', 1, 1
FROM option_strategy_definition
WHERE strategy_code = 'IRON_CONDOR';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'SHORT_PUT', 'Short Put', 'PUT', 'SHORT', 1, 2
FROM option_strategy_definition
WHERE strategy_code = 'IRON_CONDOR';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'SHORT_CALL', 'Short Call', 'CALL', 'SHORT', 1, 3
FROM option_strategy_definition
WHERE strategy_code = 'IRON_CONDOR';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_CALL', 'Long Call', 'CALL', 'LONG', 1, 4
FROM option_strategy_definition
WHERE strategy_code = 'IRON_CONDOR';


-- ============================================================
-- IRON BUTTERFLY
-- ============================================================

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_PUT', 'Long Put', 'PUT', 'LONG', 1, 1
FROM option_strategy_definition
WHERE strategy_code = 'IRON_BUTTERFLY';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'SHORT_PUT', 'Short Put', 'PUT', 'SHORT', 1, 2
FROM option_strategy_definition
WHERE strategy_code = 'IRON_BUTTERFLY';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'SHORT_CALL', 'Short Call', 'CALL', 'SHORT', 1, 3
FROM option_strategy_definition
WHERE strategy_code = 'IRON_BUTTERFLY';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_CALL', 'Long Call', 'CALL', 'LONG', 1, 4
FROM option_strategy_definition
WHERE strategy_code = 'IRON_BUTTERFLY';


-- ============================================================
-- REVERSE IRON CONDOR
-- ============================================================

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'SHORT_PUT', 'Short Put', 'PUT', 'SHORT', 1, 1
FROM option_strategy_definition
WHERE strategy_code = 'REVERSE_IRON_CONDOR';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_PUT', 'Long Put', 'PUT', 'LONG', 1, 2
FROM option_strategy_definition
WHERE strategy_code = 'REVERSE_IRON_CONDOR';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'LONG_CALL', 'Long Call', 'CALL', 'LONG', 1, 3
FROM option_strategy_definition
WHERE strategy_code = 'REVERSE_IRON_CONDOR';

INSERT INTO option_strategy_definition_leg
(strategy_definition_id, leg_code, display_name, contract_type, position_side, quantity, leg_order)
SELECT id, 'SHORT_CALL', 'Short Call', 'CALL', 'SHORT', 1, 4
FROM option_strategy_definition
WHERE strategy_code = 'REVERSE_IRON_CONDOR';



CREATE TABLE option_strategy (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    strategy_definition_id BIGINT UNSIGNED NOT NULL,

    stock_id BIGINT UNSIGNED NOT NULL,

    trade_mode ENUM(
        'LIVE',
        'SIMULATION'
    ) NOT NULL,

    strategy_action ENUM(
        'NEW',
        'ROLL',
        'REBALANCE'
    ) NOT NULL DEFAULT 'NEW',

    status ENUM(
        'OPEN',
        'CLOSED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'OPEN',

    previous_strategy_id BIGINT UNSIGNED NULL,

    entry_time DATETIME(6) NOT NULL,
    exit_time DATETIME(6) NULL,

    entry_underlying_price DECIMAL(18,6) NULL,
    exit_underlying_price DECIMAL(18,6) NULL,

    realized_pnl DECIMAL(18,6) NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_option_strategy_definition
        FOREIGN KEY (strategy_definition_id)
        REFERENCES option_strategy_definition (id),

    CONSTRAINT fk_option_strategy_stock
        FOREIGN KEY (stock_id)
        REFERENCES stock (id),

    CONSTRAINT fk_option_strategy_previous
        FOREIGN KEY (previous_strategy_id)
        REFERENCES option_strategy (id)
);


CREATE TABLE option_strategy_leg (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    option_strategy_id BIGINT UNSIGNED NOT NULL,
    option_contract_id BIGINT UNSIGNED NOT NULL,

    leg_number SMALLINT UNSIGNED NOT NULL,

    position_side ENUM(
        'LONG',
        'SHORT'
    ) NOT NULL,

    quantity INT UNSIGNED NOT NULL DEFAULT 1,

    entry_snapshot_id BIGINT UNSIGNED NULL,
    entry_price DECIMAL(14,4) NOT NULL,

    exit_snapshot_id BIGINT UNSIGNED NULL,
    exit_price DECIMAL(14,4) NULL,

    realized_pnl DECIMAL(18,4) NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_strategy_leg_strategy
        FOREIGN KEY (option_strategy_id)
        REFERENCES option_strategy(id),

    CONSTRAINT fk_strategy_leg_contract
        FOREIGN KEY (option_contract_id)
        REFERENCES option_contract(id),

    CONSTRAINT fk_strategy_leg_entry_snapshot
        FOREIGN KEY (entry_snapshot_id)
        REFERENCES option_snapshot(id),

    CONSTRAINT fk_strategy_leg_exit_snapshot
        FOREIGN KEY (exit_snapshot_id)
        REFERENCES option_snapshot(id),

    CONSTRAINT uk_strategy_leg_number
        UNIQUE (option_strategy_id, leg_number)
);

CREATE TABLE option_strategy_snapshot (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    option_strategy_id BIGINT UNSIGNED NOT NULL,

    snapshot_time DATETIME(6) NOT NULL,

    underlying_price DECIMAL(14,4) NOT NULL,

    entry_cost DECIMAL(18,4) NOT NULL,
    current_market_value DECIMAL(18,4) NOT NULL,

    unrealized_pnl DECIMAL(18,4) NOT NULL,
    unrealized_pnl_pct DECIMAL(12,4),

    realized_pnl DECIMAL(18,4) NOT NULL DEFAULT 0,
    total_pnl DECIMAL(18,4) NOT NULL,

    net_delta DECIMAL(14,8),
    net_gamma DECIMAL(14,8),
    net_theta DECIMAL(14,8),
    net_vega DECIMAL(14,8),

    average_iv DECIMAL(10,6),

    total_open_interest BIGINT,
    total_day_volume BIGINT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_strategy_snapshot_strategy
        FOREIGN KEY (option_strategy_id)
        REFERENCES option_strategy(id),

    CONSTRAINT uk_strategy_snapshot_time
        UNIQUE (option_strategy_id, snapshot_time),

    INDEX idx_strategy_snapshot_lookup (
        option_strategy_id,
        snapshot_time
    )
);


CREATE TABLE option_strategy_leg_snapshot (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    option_strategy_snapshot_id BIGINT UNSIGNED NOT NULL,
    option_strategy_leg_id BIGINT UNSIGNED NOT NULL,
    option_snapshot_id BIGINT UNSIGNED NOT NULL,

    current_market_value DECIMAL(18,4) NOT NULL,
    unrealized_pnl DECIMAL(18,4) NOT NULL,
    unrealized_pnl_pct DECIMAL(12,4),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_leg_snapshot_strategy_snapshot
        FOREIGN KEY (option_strategy_snapshot_id)
        REFERENCES option_strategy_snapshot(id),

    CONSTRAINT fk_leg_snapshot_strategy_leg
        FOREIGN KEY (option_strategy_leg_id)
        REFERENCES option_strategy_leg(id),

    CONSTRAINT fk_leg_snapshot_option_snapshot
        FOREIGN KEY (option_snapshot_id)
        REFERENCES option_snapshot(id),

    CONSTRAINT uk_strategy_leg_snapshot
        UNIQUE (
            option_strategy_snapshot_id,
            option_strategy_leg_id
        )
);


 max_iv_stock_distance DECIMAL(10,2),
  min_iv_stock_distance DECIMAL(10,2),

/*** Metrics tables ***/

CREATE TABLE option_snapshot_iv_metric (

    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    stock_id BIGINT UNSIGNED NOT NULL,
    option_contract_id BIGINT UNSIGNED NOT NULL,

    max_iv DECIMAL(10,2),
    max_iv_time TIME(6),
    max_iv_stock_distance DECIMAL(10,2),

    min_iv DECIMAL(10,2),
    min_iv_time TIME(6),
    min_iv_stock_distance DECIMAL(10,2),

    iv_date DATE NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_option_snapshot_iv_metric_stock
        FOREIGN KEY (stock_id)
        REFERENCES stock(id),

    CONSTRAINT fk_option_snapshot_iv_metric_contract
        FOREIGN KEY (option_contract_id)
        REFERENCES option_contract(id),

    CONSTRAINT uk_option_snapshot_iv_metric
        UNIQUE (
            stock_id,
            option_contract_id,
            iv_date
        )
);

/***** OPTION CONTRACT END ****/


/******************/

CREATE TABLE `split_data` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `stock_id` bigint unsigned NOT NULL,
  `ticker` varchar(255) DEFAULT NULL,
  `earnings_date` date DEFAULT NULL,
  `priceBeforeSplit` decimal(20,2) DEFAULT NULL,
  `priceAfterSplit` decimal(20,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT unique_earnings_upcoming UNIQUE (`ticker`, `stock_id`, `earnings_date`)
) ENGINE=InnoDB;

CREATE TABLE `stock_inside_details` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `upload_stock_inside_details_id` bigint unsigned NOT NULL,
  `ticker` varchar(255) DEFAULT NULL,
  `newsDate` date NOT NULL,
  `code` varchar(64) NOT NULL,
  `analystSource` varchar(64) NOT NULL,
  `stockPrice` decimal(20,2) DEFAULT NULL,
  `percentage` decimal(20,2) DEFAULT NULL,
  `details` varchar(255) NOT NULL,
  `seqNo` bigint unsigned NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT unique_stock_inside_details UNIQUE (`ticker`,`newsDate`,`code`,`analystSource`,`seqNo`)
) ENGINE=InnoDB;

CREATE TABLE stock_inside_details_stock (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  stock_inside_details_id BIGINT UNSIGNED NOT NULL,
  stock_id BIGINT UNSIGNED NOT NULL,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_stock_inside_details_stock (stock_inside_details_id, stock_id),

  CONSTRAINT fk_stock_inside_details
    FOREIGN KEY (stock_inside_details_id) REFERENCES stock_inside_details(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_stock_inside_details_stock
    FOREIGN KEY (stock_id) REFERENCES stock(id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE `filing_13f` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `fund_manager_id` bigint unsigned NOT NULL,
  `accession_number` varchar(50) NOT NULL,
  `form_type` varchar(20) DEFAULT '13F-HR',
  `filing_date` date NOT NULL,
  `period_end` date NOT NULL,
  `total_value` decimal(20,2) DEFAULT NULL,
  `number_of_positions` int DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_filing_fund_manager` (`fund_manager_id`),
  CONSTRAINT `fk_filing_fund_manager` FOREIGN KEY (`fund_manager_id`) REFERENCES `fund_manager` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `filing_13f_holding` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `filing_13f_id` bigint unsigned NOT NULL,
  `stock_id` bigint unsigned DEFAULT NULL,
  `ticker` varchar(32) DEFAULT NULL,
  `name_of_issuer` varchar(255) NOT NULL,
  `title_of_class` varchar(255) NOT NULL,
  `cusip` char(9) NOT NULL,
  `figi` varchar(32) DEFAULT NULL,
  `value` decimal(20,2) NOT NULL,
  `shares` bigint unsigned NOT NULL,
  `per_share_value` decimal(20,8) NOT NULL,
  `prn_amt` bigint unsigned NOT NULL,
  `prn` varchar(8) NOT NULL,
  `put_call` varchar(4) DEFAULT NULL,
  `investment_discretion` varchar(32) DEFAULT NULL,
  `manager` varchar(255) DEFAULT NULL,
  `sole` bigint unsigned NOT NULL DEFAULT '0',
  `shared` bigint unsigned NOT NULL DEFAULT '0',
  `none` bigint unsigned NOT NULL DEFAULT '0',
  `quarter` varchar(16) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_filing_13f_holding_filing` (`filing_13f_id`),
  KEY `fk_filing_13f_holding_stock` (`stock_id`),
  CONSTRAINT `fk_filing_13f_holding_filing` FOREIGN KEY (`filing_13f_id`) REFERENCES `filing_13f` (`id`),
  CONSTRAINT `fk_filing_13f_holding_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `fund_manager` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `cik` varchar(20) NOT NULL,
  `manager_type` varchar(100) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `search_code` varchar(128) DEFAULT NULL,
  `investment_areas` varchar(512) NOT NULL,
  `13f_name` varchar(256) DEFAULT NULL,
  `former_name` varchar(256) DEFAULT NULL,
  `former_cik` varchar(256) DEFAULT NULL,
  `website_url` varchar(256) default NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_cik` (`cik`)
) ENGINE=InnoDB;

CREATE TABLE `flag_fund_manager` (
  `flag_id` bigint unsigned NOT NULL,
  `fund_manager_id` bigint unsigned NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY `idx_flag_fund_manager` (`flag_id`,`fund_manager_id`),
  CONSTRAINT unique_ffm_flag UNIQUE (`flag_id`,`fund_manager_id`),
  CONSTRAINT `fk_ffm_flag` FOREIGN KEY (`flag_id`) REFERENCES `flag` (`id`),
  CONSTRAINT `fk_ffm_fund_manager` FOREIGN KEY (`fund_manager_id`) REFERENCES `fund_manager` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `stock_fund_manager_quarter` (
  `stock_id` bigint unsigned NOT NULL,
  `fund_manager_id` bigint unsigned NOT NULL,
  `quarter` varchar(32) NOT NULL,
  `value` decimal(20,2) NOT NULL,
  `percentage` varchar(32) NOT NULL,
  `shares` bigint unsigned NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY `idx_stock_fund_manager_quarter` (`stock_id`,`fund_manager_id`, `quarter`),
  CONSTRAINT unique_sfmq_flag UNIQUE (`stock_id`,`fund_manager_id`, `quarter`),
  CONSTRAINT `fk_sfmq_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`),
  CONSTRAINT `fk_sfmq_fund_manager` FOREIGN KEY (`fund_manager_id`) REFERENCES `fund_manager` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `stock_fund_manager_compare` (
  `stock_id` bigint unsigned NOT NULL,
  `fund_manager_id` bigint unsigned NOT NULL,
  `optionType` varchar(32) DEFAULT NULL,
  `share_pre_quarter` decimal(20,2) NOT NULL,
  `share_cur_quarter` decimal(20,2) NOT NULL,
  `share_diff_quarter` decimal(20,2) NOT NULL,
  `share_change` varchar(32) NOT NULL,
  `value_pre_quarter` decimal(20,2) NOT NULL,
  `value_cur_quarter` decimal(20,2) NOT NULL,
  `value_diff_quarter` decimal(20,2) NOT NULL,
  `value_change` varchar(32) NOT NULL,
  `quarter_to_quarter` varchar(32) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY `idx_stock_fund_manager` (`stock_id`,`fund_manager_id`,`quarter_to_quarter`),
  CONSTRAINT unique_sfmc_flag UNIQUE (`stock_id`,`fund_manager_id`,`quarter_to_quarter`),
  CONSTRAINT `fk_sfmc_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`),
  CONSTRAINT `fk_sfmc_fund_manager` FOREIGN KEY (`fund_manager_id`) REFERENCES `fund_manager` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `fund_manager_people` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `fund_manager_id` bigint unsigned DEFAULT NULL,
  `reputation` ENUM('SPOTON', 'HIGH', 'MODERATE', 'OCCASSIONAL', 'RANDOM'),
  `website_url` varchar(256) default NULL,
  `search_code` varchar(128) DEFAULT NULL,
  `focus_area`  ENUM('SEMICONDUCTOR', 'INTERNET_DIGITAL_MEDIA_AND_TRAVEL', 'COMMUNICATIONS_SERVICES', 'FINTECH', 'SECURITY_SOFTWARE', 'ENTERPRISE_SOFTWARE', 'VERTICAL_SOFTWARE') NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_fmp_fund_manager_id` (`fund_manager_id`),
  CONSTRAINT `fk_fmp_fund_manager_id` FOREIGN KEY (`fund_manager_id`) REFERENCES `fund_manager` (`id`)
) ENGINE=InnoDB;

alter table `fund_manager_people`
MODIFY COLUMN focus_area ENUM('SEMICONDUCTOR', 'INTERNET_DIGITAL_MEDIA_AND_TRAVEL', 
'COMMUNICATIONS_SERVICES', 'FINTECH', 'SECURITY_SOFTWARE', 'ENTERPRISE_SOFTWARE', 
'VERTICAL_SOFTWARE', 'ALL_EQUITY') NOT NULL;

CREATE TABLE `fund_manager_insight` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ufmsid_id` bigint unsigned NOT NULL,
  `fund_manager_id` bigint unsigned DEFAULT NULL,
  `fund_manager_people_id` bigint unsigned DEFAULT NULL,
  `ticker` varchar(255) DEFAULT NULL,
  `seqNo` bigint unsigned NOT NULL,
  `newsDate` date NOT NULL,
  `code` varchar(64) NOT NULL,
  `details` varchar(512) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_fmi_fund_manager_id` (`fund_manager_id`),
  CONSTRAINT unique_fund_manager_insight UNIQUE (`ticker`,`newsDate`,`code`,`seqNo`),
  CONSTRAINT `fk_fmi_fmp_id` FOREIGN KEY (`fund_manager_people_id`) REFERENCES `fund_manager_people` (`id`),
  CONSTRAINT `fk_fmi_fund_manager_id` FOREIGN KEY (`fund_manager_id`) REFERENCES `fund_manager` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `fund_manager_insight_stock` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `fund_manager_insight_id` bigint unsigned NOT NULL,
  `stock_id` bigint unsigned NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_fund_manager_insight_stock` (`fund_manager_insight_id`,`stock_id`),
  CONSTRAINT unique_fund_manager_insight_stock UNIQUE (`fund_manager_insight_id`,`stock_id`),
  CONSTRAINT `fk_fmpi_fmi` FOREIGN KEY (`fund_manager_insight_id`) REFERENCES `fund_manager_insight` (`id`),
  CONSTRAINT `fk_fmpi_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`)
) ENGINE=InnoDB;

/* @TODO Not needed. 
CREATE TABLE `fund_manager_people_insight` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `fund_manager_people_id` bigint unsigned DEFAULT NULL,
  `details` varchar(512) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_fmpi_fund_manager_id` (`fund_manager_people_id`),
  CONSTRAINT `fk_fmpi_fund_manager_people_id` FOREIGN KEY (`fund_manager_people_id`) REFERENCES `fund_manager_people` (`id`)
) ENGINE=InnoDB;
*/

CREATE TABLE `past_stock` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ticker` varchar(32) NOT NULL,
  `cusip` varchar(32) NOT NULL,
  `cl` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `purchased_at` timestamp NULL DEFAULT NULL,
  `sold_at` timestamp NULL DEFAULT NULL,
  `purchase_price` double(16,2) NOT NULL,
  `sold_price` double(16,2) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `sector` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(124) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `tradingview_name` varchar(100) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  UNIQUE KEY `tradingview_name` (`tradingview_name`)
) ENGINE=InnoDB;

/* Insert Sector */
insert into sector(code, name, tradingview_name) values ("COMMERCIAL_SERVICES", "COMMERCIAL SERVICES", "Commercial services");
insert into sector(code, name, tradingview_name) values ("COMMUNICATIONS", "COMMUNICATIONS", "Communications");
insert into sector(code, name, tradingview_name) values ("CONSUMER_DURABLES", "CONSUMER DURABLES", "Consumer durables");
insert into sector(code, name, tradingview_name) values ("CONSUMER_NON_DURABLES", "CONSUMER NON DURABLES", "Consumer non-durables");
insert into sector(code, name, tradingview_name) values ("CONSUMER_SERVICES", "CONSUMER SERVICES", "Consumer services");
insert into sector(code, name, tradingview_name) values ("DISTRIBUTION_SERVICES", "DISTRIBUTION SERVICES", "Distribution services");
insert into sector(code, name, tradingview_name) values ("ELECTRONIC_TECHNOLOGY", "ELECTRONIC TECHNOLOGY", "Electronic technology");
insert into sector(code, name, tradingview_name) values ("ENERGY_MINERALS", "ENERGY MINERALS", "Energy minerals");
insert into sector(code, name, tradingview_name) values ("FINANCE", "FINANCE", "Finance");
insert into sector(code, name, tradingview_name) values ("HEALTH_SERVICES", "HEALTH SERVICES", "Health services");
insert into sector(code, name, tradingview_name) values ("HEALTH_TECHNOLOGY", "HEALTH TECHNOLOGY", "Health technology");
insert into sector(code, name, tradingview_name) values ("INDUSTRIAL_SERVICES", "INDUSTRIAL SERVICES", "Industrial services");
insert into sector(code, name, tradingview_name) values ("MISCELLANEOUS", "MISCELLANEOUS", "Miscellaneous");
insert into sector(code, name, tradingview_name) values ("NON_ENERGY_MINERALS", "NON ENERGY MINERALS", "Non-energy minerals");
insert into sector(code, name, tradingview_name) values ("PROCESS_INDUSTRIES", "PROCESS INDUSTRIES", "Process industries");
insert into sector(code, name, tradingview_name) values ("PRODUCER_MANUFACTURING", "PRODUCER MANUFACTURING", "Producer manufacturing");
insert into sector(code, name, tradingview_name) values ("RETAIL_TRADE", "RETAIL TRADE", "Retail trade");
insert into sector(code, name, tradingview_name) values ("TECHNOLOGY_SERVICES", "TECHNOLOGY SERVICES", "Technology services");
insert into sector(code, name, tradingview_name) values ("TRANSPORTATION", "TRANSPORTATION", "Transportation");
insert into sector(code, name, tradingview_name) values ("UTILITIES", "UTILITIES", "Utilities");


CREATE TABLE `stock` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ticker` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `cusip` varchar(32) NOT NULL,
  `cik` varchar(20) NOT NULL,
  `cl` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `exchange` varchar(64) DEFAULT NULL,
  `sector` varchar(128) DEFAULT NULL,
  `industry` varchar(255) DEFAULT NULL,
  `isin` varchar(32) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `label` varchar(128) DEFAULT NULL,
  `past_stock` tinyint(1) DEFAULT '0',
  `sp500` tinyint(1) DEFAULT '0',
  `nasdaq100` tinyint(1) DEFAULT '0',
  `dji30` tinyint(1) DEFAULT '0',
  `russell2000` tinyint(1) DEFAULT '0',
  `phlxSemiIndex` tinyint(1) DEFAULT '0',
  `sector_id` bigint unsigned DEFAULT NULL,
  `country` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_stock_cusip` (`cusip`),
  CONSTRAINT unique_stock_ticker UNIQUE (`ticker`),
  KEY `idx_stock_ticker` (`ticker`),
  KEY `fk_stock_sector` (`sector_id`),
  CONSTRAINT `fk_stock_sector` FOREIGN KEY (`sector_id`) REFERENCES `sector` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;


CREATE TABLE `sector_stock` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sector_id` bigint unsigned NOT NULL,
  `stock_id` bigint unsigned NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT unique_sector_stock UNIQUE (`sector_id`,`stock_id`),
  KEY `fk_sector_stock_sector` (`sector_id`),
  KEY `fk_sector_stock_stock` (`stock_id`),
  CONSTRAINT `fk_sector_stock_sector` FOREIGN KEY (`sector_id`) REFERENCES `sector` (`id`),
  CONSTRAINT `fk_sector_stock_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `watchlist` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `country` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB;

CREATE TABLE `watchlist_stock` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `watchlist_id` bigint unsigned NOT NULL,
  `stock_id` bigint unsigned NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_watchlist_stock` (`watchlist_id`,`stock_id`),
  CONSTRAINT unique_watchlist_stock UNIQUE (`watchlist_id`,`stock_id`),
  CONSTRAINT `fk_watchlist_stock_watchlist` FOREIGN KEY (`watchlist_id`) REFERENCES `watchlist` (`id`),
  CONSTRAINT `fk_watchlist_stock_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `table_filter_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `table_name` varchar(100) NOT NULL,
  `field_name` varchar(100) NOT NULL,
  `filter_value` varchar(100) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT unique_watchlist_stock UNIQUE (`table_name`,`field_name`,`filter_value`)
) ENGINE=InnoDB;

/* Price Table */
CREATE TABLE stock_days_price (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  
  stock_id bigint unsigned NOT NULL,
  priceDate date NOT NULL,

  price DECIMAL(5,2) DEFAULT NULL,
  price_currency CHAR(3) DEFAULT NULL,

  price_change_1d DECIMAL(5,2) DEFAULT NULL,
  price_change_pct_1d DECIMAL(5,2) DEFAULT NULL,

  price_change_1w DECIMAL(5,2) DEFAULT NULL,
  performance_pct_1w DECIMAL(5,2) DEFAULT NULL,

  price_change_1m DECIMAL(5,2) DEFAULT NULL,
  performance_pct_1m DECIMAL(5,2) DEFAULT NULL,

  performance_pct_6m DECIMAL(5,2) DEFAULT NULL,
  performance_pct_1y DECIMAL(5,2) DEFAULT NULL,

  analyst_rating VARCHAR(64) DEFAULT NULL,
  
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_stock_days_price_stock (priceDate,stock_id),
  CONSTRAINT unique_stock_days_price_stock UNIQUE (priceDate,stock_id),
  CONSTRAINT fk_stock_days_price_stock FOREIGN KEY (stock_id) REFERENCES stock (id)
) ENGINE=InnoDB;

CREATE TABLE `flag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(124) NOT NULL,
  `name` varchar(512) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB;

CREATE TABLE `flag_stock` (
  `flag_id` bigint unsigned NOT NULL,
  `stock_id` bigint unsigned NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY `idx_flag_stock` (`flag_id`,`stock_id`),
  CONSTRAINT unique_fs_flag UNIQUE (`flag_id`,`stock_id`),
  CONSTRAINT `fk_fs_flag` FOREIGN KEY (`flag_id`) REFERENCES `flag` (`id`),
  CONSTRAINT `fk_fs_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`id`)
) ENGINE=InnoDB;


/* UPLOAD DATA TABLE */
CREATE TABLE `upload_stock_sector` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `sector` varchar(128) NOT NULL,
  `stockTicker` varchar(128) NOT NULL,
  UNIQUE KEY uq_tmp_stock_sector (sector, stockTicker),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `upload_watchlist_stock` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `watchlist` varchar(128) DEFAULT NULL,
  `stock` varchar(128) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  CONSTRAINT unique_name_cik UNIQUE (`watchlist`,`stock`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `upload_earnings_upcoming_date` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticker` varchar(255) DEFAULT NULL,
  `quarter` varchar(64) DEFAULT NULL,
  `time` varchar(64) DEFAULT NULL,
  `earnings_date` date DEFAULT NULL,
  `missingStock` tinyint(1) DEFAULT '0',
  `missingStock_updateDate` date default NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT unique_earnings_upcoming UNIQUE (`ticker`,`quarter`,`time`,`earnings_date`)
) ENGINE=InnoDB;

DELIMITER $$

CREATE TRIGGER trg_ueud_missingStock_updateDate
BEFORE UPDATE ON upload_earnings_upcoming_date
FOR EACH ROW
BEGIN
  IF NEW.missingStock = 1 AND OLD.missingStock <> 1 THEN
    SET NEW.missingStock_updateDate = CURRENT_DATE;
  END IF;
END$$

DELIMITER ;


CREATE TABLE `upload_earnings_data` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ticker` varchar(255) DEFAULT NULL,
  `earnings_date` date DEFAULT NULL,
  `stockPriceSOD` decimal(20,2) DEFAULT NULL,
  `stockPriceEOD` decimal(20,2) DEFAULT NULL,
  `stockPriceEO2Ds` decimal(20,2) DEFAULT NULL,
  `stockPriceEOW` decimal(20,2) DEFAULT NULL,
  `stockPriceEO2Ws` decimal(20,2) DEFAULT NULL,
  `movement` varchar(64) DEFAULT NULL,
  `supriseEarning` varchar(32) DEFAULT NULL,
  `supriseRevenue` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT unique_earnings_upcoming UNIQUE (`ticker`,`earnings_date`)
) ENGINE=InnoDB;

CREATE TABLE `upload_split_data` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ticker` varchar(255) DEFAULT NULL,
  `earnings_date` date DEFAULT NULL,
  `priceBeforeSplit` decimal(20,2) DEFAULT NULL,
  `priceAfterSplit` decimal(20,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT unique_earnings_upcoming UNIQUE (`ticker`,`earnings_date`)
) ENGINE=InnoDB;

CREATE TABLE `upload_stock_inside_details` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ticker` varchar(255) NOT NULL,
  `seqNo` bigint unsigned NOT NULL,
  `newsDate` date NOT NULL,
  `code` varchar(64) NOT NULL,
  `stockPrice` decimal(20,2) DEFAULT NULL,
  `percentage` decimal(20,2) DEFAULT NULL,
  `details` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT upload_stock_inside_details UNIQUE (`ticker`,`seqNo`,`newsDate`,`code`)
) ENGINE=InnoDB;

CREATE TABLE `upload_fund_manager_stock_inside_details` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ticker` varchar(255) NOT NULL,
  `seqNo` bigint unsigned NOT NULL,
  `newsDate` date NOT NULL,
  `code` varchar(64) NOT NULL,
  `manager_search_code` varchar(128) DEFAULT NULL,
  `manager_people_search_code` varchar(128) NOT NULL,
  `details` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT upload_fund_manager_stock_inside_details UNIQUE (`ticker`, `seqNo`, `newsDate`,`code`,`manager_search_code`,`manager_people_search_code`)
) ENGINE=InnoDB;

CREATE TABLE `upload_13f_stock_data` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `manager` varchar(256) DEFAULT NULL,
  `cik` varchar(20) DEFAULT NULL,
  `reportDate` date NOT NULL,
  `value` decimal(20,2) NOT NULL,
  `shares` bigint unsigned NOT NULL,
  `optionType` varchar(32) DEFAULT NULL,
  `cusip` varchar(20) DEFAULT NULL,
  `ticker` varchar(32) DEFAULT NULL,
  `quarter` varchar(32) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT unique_13f_stock_data UNIQUE (`cik`,`optionType`,`cusip`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `upload_13f_manager_data` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `ticker` varchar(32) NOT NULL,
  `stockName` varchar(256) DEFAULT NULL,
  `cl` varchar(20) DEFAULT NULL,
  `cusip` varchar(20) DEFAULT NULL,
  `value` decimal(20,2) NOT NULL,
  `percentage` decimal(20,2) NOT NULL,
  `shares` bigint unsigned NOT NULL,
  `principal` decimal(20,2) DEFAULT NULL,
  `optionType` varchar(32) DEFAULT NULL,
  `manager` varchar(256) DEFAULT NULL,
  `cik` varchar(20) DEFAULT NULL,
  `quarter` varchar(32) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT unique_name_cik UNIQUE (`cik`,`optionType`,`cusip`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `upload_13f_manager_stock_movement` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `ticker` varchar(32) NOT NULL,
  `stockName` varchar(256) DEFAULT NULL,
  `cl` varchar(20) DEFAULT NULL,
  `cusip` varchar(20) DEFAULT NULL,
  `optionType` varchar(32) DEFAULT NULL,
  `share_pre_quarter` decimal(20,2) NOT NULL,
  `share_cur_quarter` decimal(20,2) NOT NULL,
  `share_diff_quarter` decimal(20,2) NOT NULL,
  `share_change` varchar(32) NOT NULL,
  `value_pre_quarter` decimal(20,2) NOT NULL,
  `value_cur_quarter` decimal(20,2) NOT NULL,
  `value_diff_quarter` decimal(20,2) NOT NULL,
  `value_change` varchar(32) NOT NULL,
  `quarter_to_quarter` varchar(32) NOT NULL,
  `manager` varchar(256) DEFAULT NULL,
  `cik` varchar(20) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT unique_name_cik UNIQUE (`ticker`,`quarter_to_quarter`,`cusip`,`cik`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;
  
CREATE TABLE `upload_manager_people_ticker` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `managerName` varchar(256) DEFAULT NULL,
  `search_code` varchar(128) DEFAULT NULL,
  `ticker` varchar(32) NOT NULL,
  `missingStock` tinyint(1) DEFAULT '0',
  `missingStock_updateDate` date default NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT unique_upload_manager_people_ticker UNIQUE (`managerName`,`search_code`,`ticker`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

DELIMITER $$

CREATE TRIGGER trg_umpt_missingStock_updateDate
BEFORE UPDATE ON upload_manager_people_ticker
FOR EACH ROW
BEGIN
  IF NEW.missingStock = 1 AND OLD.missingStock <> 1 THEN
    SET NEW.missingStock_updateDate = CURRENT_DATE;
  END IF;
END$$

DELIMITER ;



CREATE TABLE upload_stock_days_price (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

  ticker VARCHAR(32) NOT NULL,

  price DECIMAL(5,2) DEFAULT NULL,
  price_currency CHAR(3) DEFAULT NULL,

  price_change_1d DECIMAL(5,2) DEFAULT NULL,
  price_change_pct_1d DECIMAL(5,2) DEFAULT NULL,

  price_change_1w DECIMAL(5,2) DEFAULT NULL,
  performance_pct_1w DECIMAL(5,2) DEFAULT NULL,

  price_change_1m DECIMAL(5,2) DEFAULT NULL,
  performance_pct_1m DECIMAL(5,2) DEFAULT NULL,

  performance_pct_6m DECIMAL(5,2) DEFAULT NULL,
  performance_pct_1y DECIMAL(5,2) DEFAULT NULL,

  sector VARCHAR(128) DEFAULT NULL,
  analyst_rating VARCHAR(64) DEFAULT NULL,
  
  missingStock tinyint(1) DEFAULT '0',

  created_date DATE GENERATED ALWAYS AS (DATE(created_at)) STORED,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_upload_stock_days_price (ticker, created_date)
) ENGINE=InnoDB;


