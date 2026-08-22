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



select * from stock;

MU = 1

select * from day_stock_movement_entry;

select * from options_interval_analyse;

select * from option_snapshot_flatfile;

select * from option_contract
where stock_id = 1 and expiration_date = '2026-07-31' ;

select * from option_snapshot where stock_id = 1;

LOAD DATA LOCAL INFILE '/Users/I753307/Downloads/stockmovement_data_mu.csv'
INTO TABLE day_stock_movement_entry
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(
  stock_id,
  earnings_date_id,
  pre_day_close,
  cur_day_open,
  cur_day_close,
  cur_day_high,
  cur_day_low,
  cur_day_vol_weight,
  cur_day_volume,
  change_percent,
  earnings,
  day_opening_change_percent,
  day_stock_movement_date
);




SELECT
  dsme.id,
  dsme.stock_id,
  DATE(dsme.day_stock_movement_date) AS movement_day,
  dsme.cur_day_high,
  dsme.cur_day_low,
  (
    SELECT GROUP_CONCAT(DISTINCT DATE_FORMAT(os.option_quote_time, '%H:%i')
                        ORDER BY DATE_FORMAT(os.option_quote_time, '%H:%i') SEPARATOR ',')
    FROM option_snapshot os
    WHERE os.stock_id = dsme.stock_id
      AND DATE(os.option_quote_time) = DATE(dsme.day_stock_movement_date)
      AND ROUND(os.underlying_price, 2) = dsme.cur_day_high
  ) AS calc_cur_day_high_snap_shot_datetime,
  (
    SELECT GROUP_CONCAT(DISTINCT DATE_FORMAT(os.option_quote_time, '%H:%i')
                        ORDER BY DATE_FORMAT(os.option_quote_time, '%H:%i') SEPARATOR ',')
    FROM option_snapshot os
    WHERE os.stock_id = dsme.stock_id
      AND DATE(os.option_quote_time) = DATE(dsme.day_stock_movement_date)
      AND ROUND(os.underlying_price, 2) = dsme.cur_day_low
  ) AS calc_cur_day_low_snap_shot_datetime,
  (
    SELECT GROUP_CONCAT(DISTINCT DATE_FORMAT(osf.local_time, '%H:%i')
                        ORDER BY DATE_FORMAT(osf.local_time, '%H:%i') SEPARATOR ',')
    FROM option_snapshot_flatfile osf
    WHERE osf.stock_id = dsme.stock_id
      AND DATE(osf.local_time) = DATE(dsme.day_stock_movement_date)
      AND ROUND(osf.stock_open, 2) = dsme.cur_day_high
  ) AS calc_cur_day_high_flat_file_datetime,
  (
    SELECT GROUP_CONCAT(DISTINCT DATE_FORMAT(osf.local_time, '%H:%i')
                        ORDER BY DATE_FORMAT(osf.local_time, '%H:%i') SEPARATOR ',')
    FROM option_snapshot_flatfile osf
    WHERE osf.stock_id = dsme.stock_id
      AND DATE(osf.local_time) = DATE(dsme.day_stock_movement_date)
      AND ROUND(osf.stock_open, 2) = dsme.cur_day_low
  ) AS calc_cur_day_low_flat_file_datetime
FROM day_stock_movement_entry dsme
WHERE DATE(dsme.day_stock_movement_date) = '2026-07-31'
  AND dsme.stock_id IN (1, 2, 3, 4, 5, 6);




  SELECT GROUP_CONCAT(DISTINCT DATE_FORMAT(os.option_quote_time, '%H:%i')
                        ORDER BY DATE_FORMAT(os.option_quote_time, '%H:%i') SEPARATOR ',')
    FROM option_snapshot os
    WHERE os.stock_id IN (1, 2, 3, 4, 5, 6)
      AND DATE(os.option_quote_time) = DATE('2026-07-31')
      AND TRUNCATE(os.underlying_price, 0) = TRUNCATE(836.3400, 0);

      AND ROUND(os.underlying_price, 2) = 930.88;

select * from option_snapshot 
order by option_quote_time asc;



SELECT os.snapshot_time, os.option_quote_time, os.underlying_price, os.bid, os.ask, os.midpoint, os.implied_volatility, os.delta, os.gamma, os.theta, os.vega, os.open_interest, os.day_volume 
FROM option_snapshot os 
WHERE os.option_contract_id = 187 
ORDER BY COALESCE(os.option_quote_time, os.snapshot_time) DESC;


select * from option_contract oc 
join option_snapshot os on oc.id = os.option_contract_id
where oc.contract_ticker like "O:MU260731%";



select * from option_snapshot os 
join stock s on os.stock_id = s.id
where s.ticker = "MU" ;