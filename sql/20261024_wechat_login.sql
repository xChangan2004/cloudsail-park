-- 微信登录：为 customer.openid 增加唯一索引，避免并发首次登录产生重复账号。
-- 执行前建议先检查重复数据（NULL 不参与唯一约束）：
-- SELECT openid, COUNT(*) FROM `customer` WHERE openid IS NOT NULL GROUP BY openid HAVING COUNT(*) > 1;
USE `cloudsail-park`;

-- 微信首次登录允许先建 openid 账号、稍后绑定手机号，因此 phone 需要允许为空。
ALTER TABLE `customer`
    MODIFY COLUMN `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '手机号';

SET @index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'customer'
      AND index_name = 'uk_openid'
);

SET @ddl = IF(@index_exists = 0,
    'ALTER TABLE `customer` ADD UNIQUE KEY `uk_openid` (`openid`)',
    'SELECT ''uk_openid already exists'' AS result');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
