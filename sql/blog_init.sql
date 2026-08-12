-- ============================================
-- Code-Blog 数据库初始化脚本
-- MySQL 5.7+
-- 使用方法: mysql -u root -p < blog_init.sql
-- ============================================

CREATE DATABASE IF NOT EXISTS `my_blog_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `my_blog_db`;

-- ----------------------------
-- 管理员表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_admin_user` (
  `admin_user_id` INT NOT NULL AUTO_INCREMENT,
  `login_user_name` VARCHAR(50) NOT NULL,
  `login_password` VARCHAR(64) NOT NULL,
  `nick_name` VARCHAR(50) NOT NULL,
  `locked` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`admin_user_id`),
  UNIQUE KEY `uk_login_user_name` (`login_user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 默认管理员: admin / admin123 (MD5)
INSERT INTO `tb_admin_user` (`login_user_name`, `login_password`, `nick_name`, `locked`)
SELECT 'admin', 'c93ccd78b204f890fc1b3a9d8c0c0a5c', 'admin', 0
WHERE NOT EXISTS (SELECT 1 FROM `tb_admin_user`);

-- ----------------------------
-- 分类表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_blog_category` (
  `category_id` INT NOT NULL AUTO_INCREMENT,
  `category_name` VARCHAR(50) NOT NULL,
  `category_icon` VARCHAR(255) DEFAULT '',
  `category_rank` INT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 标签表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_blog_tag` (
  `tag_id` INT NOT NULL AUTO_INCREMENT,
  `tag_name` VARCHAR(50) NOT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 文章表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_blog` (
  `blog_id` BIGINT NOT NULL AUTO_INCREMENT,
  `blog_title` VARCHAR(200) NOT NULL,
  `blog_sub_url` VARCHAR(200) DEFAULT '',
  `blog_cover_image` VARCHAR(500) DEFAULT '',
  `blog_category_id` INT NOT NULL DEFAULT 0,
  `blog_category_name` VARCHAR(50) DEFAULT '默认分类',
  `blog_tags` VARCHAR(200) DEFAULT '',
  `blog_status` TINYINT NOT NULL DEFAULT 0,
  `blog_views` BIGINT NOT NULL DEFAULT 0,
  `enable_comment` TINYINT NOT NULL DEFAULT 1,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `blog_content` LONGTEXT,
  PRIMARY KEY (`blog_id`),
  KEY `idx_sub_url` (`blog_sub_url`),
  KEY `idx_category_id` (`blog_category_id`),
  KEY `idx_status_deleted` (`blog_status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 文章-标签关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_blog_tag_relation` (
  `relation_id` BIGINT NOT NULL AUTO_INCREMENT,
  `blog_id` BIGINT NOT NULL,
  `tag_id` INT NOT NULL,
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`relation_id`),
  KEY `idx_tag_blog` (`tag_id`, `blog_id`),
  KEY `idx_blog_tag` (`blog_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 评论表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_blog_comment` (
  `comment_id` BIGINT NOT NULL AUTO_INCREMENT,
  `blog_id` BIGINT NOT NULL,
  `commentator` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) DEFAULT '',
  `website_url` VARCHAR(200) DEFAULT '',
  `comment_body` VARCHAR(500) NOT NULL,
  `comment_create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `commentator_ip` VARCHAR(50) DEFAULT '',
  `reply_body` VARCHAR(500) DEFAULT '',
  `reply_create_time` TIMESTAMP NULL DEFAULT NULL,
  `comment_status` TINYINT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`comment_id`),
  KEY `idx_blog_id` (`blog_id`),
  KEY `idx_blog_status` (`blog_id`, `comment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 站点配置表 (key-value)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_config` (
  `config_name` VARCHAR(100) NOT NULL,
  `config_value` VARCHAR(500) DEFAULT '',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 默认站点配置
INSERT INTO `tb_config` (`config_name`, `config_value`)
SELECT 'websiteName', '程军高的个人品牌网站'
WHERE NOT EXISTS (SELECT 1 FROM `tb_config` WHERE `config_name` = 'websiteName');

INSERT INTO `tb_config` (`config_name`, `config_value`)
SELECT 'websiteDescription', '搜索架构师 / AI 搜索系统工程师'
WHERE NOT EXISTS (SELECT 1 FROM `tb_config` WHERE `config_name` = 'websiteDescription');

INSERT INTO `tb_config` (`config_name`, `config_value`)
SELECT 'yourName', '程军高'
WHERE NOT EXISTS (SELECT 1 FROM `tb_config` WHERE `config_name` = 'yourName');

INSERT INTO `tb_config` (`config_name`, `config_value`)
SELECT 'yourAvatar', ''
WHERE NOT EXISTS (SELECT 1 FROM `tb_config` WHERE `config_name` = 'yourAvatar');

INSERT INTO `tb_config` (`config_name`, `config_value`)
SELECT 'yourEmail', 'chengjungao@foxmail.com'
WHERE NOT EXISTS (SELECT 1 FROM `tb_config` WHERE `config_name` = 'yourEmail');

-- ----------------------------
-- 友情链接表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_link` (
  `link_id` INT NOT NULL AUTO_INCREMENT,
  `link_type` TINYINT NOT NULL DEFAULT 1,
  `link_name` VARCHAR(50) NOT NULL,
  `link_url` VARCHAR(200) NOT NULL,
  `link_description` VARCHAR(200) DEFAULT '',
  `link_rank` INT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`link_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 微信消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_messages` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `fromUser` VARCHAR(100) DEFAULT '',
  `toUser` VARCHAR(100) DEFAULT '',
  `content` TEXT,
  `msgType` VARCHAR(20) DEFAULT '',
  `createTime` DATETIME DEFAULT NULL,
  `msgId` VARCHAR(100) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_msg_id` (`msgId`),
  KEY `idx_create_time` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 留言板表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tb_message` (
  `message_id` BIGINT NOT NULL AUTO_INCREMENT,
  `nickname` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) DEFAULT '',
  `avatar` VARCHAR(500) DEFAULT '',
  `message_body` VARCHAR(500) NOT NULL,
  `message_create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `message_ip` VARCHAR(50) DEFAULT '',
  `reply_body` VARCHAR(500) DEFAULT '',
  `reply_create_time` TIMESTAMP NULL DEFAULT NULL,
  `message_status` TINYINT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`message_id`),
  KEY `idx_message_status` (`message_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
