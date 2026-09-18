-- =============================================================================
-- 2026-09-18  my_blog_db 字符集升级：utf8mb3 -> utf8mb4
--
-- 起因：后台保存含 emoji 的文章报
--   java.sql.SQLException: Incorrect string value: '\xF0\x9F\x93\xA6 *...'
--   for column 'blog_content' at row 1   (SQLState HY000, error 1366)
--   \xF0\x9F\x93\xA6 = U+1F4E6 📦，4 字节 UTF-8，utf8mb3 存不下
--
-- 现状（2026-09-18 实测，服务器 MySQL 8.0.32）：
--   * 连接层没问题：JDBC 用 characterEncoding=utf8，Connector/J 8.0.31 实际协商出
--     character_set_client/connection = utf8mb4，collation_connection = utf8mb4_0900_ai_ci
--   * 库默认也没问题：my_blog_db = utf8mb4 / utf8mb4_0900_ai_ci
--   * 问题全在表/列：11 张表仍是 utf8mb3_general_ci（早期建表脚本遗留），
--     共 26 个字符型列；其中 tb_blog.blog_content 是 mediumtext utf8mb3
--   * 同库较新的 4 张表（tb_message / tb_messages / tb_page_stat / tb_wechat_auto_reply）
--     已经是 utf8mb4_0900_ai_ci —— 所以本脚本就是把老表对齐到新表
--
-- 为什么选 utf8mb4_0900_ai_ci 而不是 utf8mb4_general_ci：
--   库默认与 4 张新表都是 0900_ai_ci，混用两种 collation 会让跨表 JOIN / 比较报
--   error 1267 (Illegal mix of collations)。若要改用 general_ci，必须把库默认和
--   那 4 张新表一起改，不能只改这 11 张。
--
-- 执行前已做的验证（本机临时表上跑，真实表零写入）：
--   [复现] CREATE TEMPORARY TABLE t_clone LIKE tb_blog; 同形 INSERT -> error 1366
--   [修复] ALTER TABLE t_clone CONVERT TO ... utf8mb4 -> 同一条 INSERT 成功，标题/正文往返一致
--   [数据] 克隆 tb_blog 全部 20 行 -> ALTER -> 逐行逐列比对，不一致 0 行
--          迁移前后内容 MD5 汇总均为 d7a7d2e6fb6914aec3c9d6227e8e744c
--   [索引] 转 utf8mb4 后字节数 > 3072 的索引：0 个（不会 error 1071）
--   [唯一键] utf8mb3 列上的唯一索引只有 tb_config.PRIMARY(config_name) 1 个，
--          在 0900_ai_ci 和 general_ci 下重复组均为 0（不会 error 1062）
--   [历史] tb_blog 20 行中无一含 4 字节字符 -> 之前是 INSERT 被拦，没有历史内容被截断
--
-- 副作用（已知、可接受）：
--   ALTER ... CONVERT TO 会把 TEXT 系类型自动提一档（字符可能占更多字节），
--   实测 tb_blog.blog_content: mediumtext -> longtext。反而与 sql/blog_init.sql 的 LONGTEXT 一致。
--
-- 回滚：本脚本不可平滑回滚。反向 CONVERT 能退回 utf8mb3，但迁移后新写入的 4 字节字符
--       会静默变成 '?'（实测 HEX 3F）。所以务必先备份，见下方第 0 步。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 第 0 步：备份（强烈建议；在服务器上执行，不要在本机跑）
-- ---------------------------------------------------------------------------
-- mysqldump -h 127.0.0.1 -P 8306 -uroot -p --single-transaction --default-character-set=utf8mb4 \
--     --hex-blob my_blog_db > my_blog_db_before_utf8mb4_20260918.sql

-- ---------------------------------------------------------------------------
-- 第 1 步：库默认字符集（现状已是 utf8mb4/0900，此处为幂等声明）
-- ---------------------------------------------------------------------------
ALTER DATABASE `my_blog_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 第 2 步：11 张 utf8mb3 表逐一转换
--   数据量很小（最大 tb_blog_tag 33 行 / tb_messages 198 行），单表亚秒级完成
-- ---------------------------------------------------------------------------
ALTER TABLE `tb_blog`              CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `tb_blog_category`     CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `tb_blog_tag`          CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `tb_blog_tag_relation` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `tb_blog_comment`      CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `tb_admin_user`        CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `tb_config`            CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `tb_link`              CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 测试遗留表（无生产数据；若打算清理可整段跳过，跳过也不影响业务）
ALTER TABLE `tb_test`              CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `jdbc_test`            CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `generator_test`       CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 第 3 步：验证 1 —— 必须返回 0 行
-- ---------------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'my_blog_db'
  AND CHARACTER_SET_NAME IS NOT NULL
  AND CHARACTER_SET_NAME <> 'utf8mb4';

-- 表级也应全部是 utf8mb4_0900_ai_ci
SELECT TABLE_NAME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'my_blog_db' AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_COLLATION, TABLE_NAME;

-- ---------------------------------------------------------------------------
-- 第 4 步：验证 2 —— 真实往返写入测试（用完即删，不污染业务表）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `_charset_probe`;
CREATE TABLE `_charset_probe` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `v`  LONGTEXT,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO `_charset_probe` (`v`) VALUES ('中文 📦 emoji 😀 结尾');
-- 期望：HEX 含 F09F93A6 与 F09F9880，若出现 3F 即静默丢字
SELECT `v`, HEX(`v`) AS hex_v FROM `_charset_probe`;
DROP TABLE `_charset_probe`;

-- ---------------------------------------------------------------------------
-- 第 5 步：业务侧最终验收（不在此脚本内）
--   后端重启 -> 进后台编辑器，把之前保存失败那篇文章（正文含 📦）重新保存，
--   确认 200 且列表页/详情页 emoji 正常显示。
-- =============================================================================
