USE edu_order;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'edu_order' AND TABLE_NAME = 't_order' AND COLUMN_NAME = 'provider_trade_no'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE t_order ADD COLUMN provider_trade_no VARCHAR(64) NULL COMMENT ''Alipay trade number'' AFTER pay_method, ADD INDEX idx_provider_trade_no (provider_trade_no)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
