

drop table firm;

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

/*  Earnings Data */
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

/*  Day Stock Movement */

drop table day_stock_movement_map;

drop table day_stock_movement_entry;

select code from day_stock_movement_key;

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


