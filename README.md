# Code Blog

基于 Spring Boot + Vue 3 的前后端分离个人品牌网站。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7 + MyBatis + MySQL + Solr (HanLP 中文分词) |
| 管理后台 | Vue 3 + Vite + Element Plus + md-editor-v3 |
| 博客前台 | Vue 3 + Vite + Vue Router + markdown-it |
| AI 能力 | 阿里云 DashScope (通义千问) |
| 部署 | Docker + Nginx + Supervisor (单镜像) |
| CI/CD | GitHub Actions → 阿里云 ACR |

## 项目结构

```
code-blog/
├── src/main/java/              # 后端 Java 源码
│   └── com/site/blog/my/core/
│       ├── controller/
│       │   ├── admin/          # 管理后台 API
│       │   ├── blog/           # 博客前台 API
│       │   ├── common/         # 验证码、微信消息、AI 健康
│       │   └── vo/             # 视图对象
│       ├── dao/                # MyBatis Mapper
│       ├── entity/             # 实体类
│       ├── service/            # 业务逻辑层
│       ├── solr/               # Solr 全文搜索
│       ├── interceptor/        # 登录拦截器
│       └── util/               # 工具类
├── src/main/resources/
│   ├── mapper/                 # MyBatis XML
│   ├── blog/                   # Solr 配置 (managed-schema, solrconfig.xml)
│   ├── application.yml         # 主配置 (无凭据)
│   ├── application-dev.yml     # 开发环境配置
│   ├── application-local.yml   # 本地凭据 (gitignored)
│   └── application-prd.yml     # 生产环境配置 (环境变量注入)
├── frontend/
│   ├── admin/                  # 管理后台 Vue 项目
│   └── web/                    # 博客前台 Vue 项目
├── deploy/                     # 部署配置
│   ├── Dockerfile              # 多阶段构建
│   ├── nginx.conf              # Nginx 反代配置
│   ├── supervisord.conf        # 进程管理
│   ├── entrypoint.sh           # 容器入口脚本
│   └── docker-compose.yml      # Docker Compose 编排
├── sql/                        # 数据库初始化脚本
│   └── blog_init.sql
├── .github/workflows/          # CI/CD
└── pom.xml
```

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- Node.js 18+
- MySQL 5.7+

### 1. 初始化数据库

```bash
mysql -u root -p < sql/blog_init.sql
```

### 2. 配置本地凭据

创建 `src/main/resources/application-local.yml` (已 gitignore):

```yaml
spring:
  datasource:
    url: "${DB_URL:jdbc:mysql://localhost:3306/my_blog_db?useUnicode=true&characterEncoding=utf8&autoReconnect=true&useSSL=false&serverTimezone=UTC}"
    username: "${DB_USERNAME:root}"
    password: "${DB_PASSWORD:your_password}"

token: "${BLOG_TOKEN:your_token}"
wechat: "${WECHAT_TOKEN:your_wechat_token}"
chat:
  server:
    url: "${CHAT_SERVER_URL:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}"
  model: "${CHAT_MODEL:qwen3.7-plus}"
vision:
  server:
    url: "${VISION_SERVER_URL:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}"
  token: "${VISION_TOKEN:your_vision_token}"
  model: "${VISION_MODEL:qwen3.7-plus}"
```

Solr 配置 (开发环境默认使用项目内 `target/classes/blog` 目录):

```yaml
solr:
  home: D:/Projects/code-blog/target/classes/blog
```

### 3. 启动后端

```bash
mvn spring-boot:run
```

后端运行在 http://localhost:28083

### 4. 启动前端 (开发模式)

管理后台:

```bash
cd frontend/admin
npm install
npm run dev    # → http://localhost:3001
```

博客前台:

```bash
cd frontend/web
npm install
npm run dev    # → http://localhost:3000
```

### 5. 访问

| 入口 | 地址 |
|------|------|
| 博客前台 | http://localhost:3000 |
| 管理后台 | http://localhost:3001 |
| 后端 API | http://localhost:28083 |

## Docker 部署

### 架构

```
┌──────────────────────────────────────────────┐
│  Docker Container                            │
│                                              │
│  Nginx :80 / :443                            │
│    ├── /            → 博客前台 dist           │
│    ├── /admin/      → 管理后台 dist           │
│    ├── /blog/api/*  → Spring Boot :28083     │
│    ├── /admin/*     → Spring Boot :28083     │
│    └── /common/*    → Spring Boot :28083     │
│                                              │
│  Spring Boot :28083 (internal)               │
│  Supervisor → 管理两个进程                    │
│  Solr 配置首次启动自动初始化                   │
└──────────────────────────────────────────────┘
```

### Docker Compose 部署

```bash
# 创建 .env 文件或直接传入环境变量
docker compose -f deploy/docker-compose.yml up -d \
  -e DB_URL="jdbc:mysql://your-db-host:3306/my_blog_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC" \
  -e DB_USER=root \
  -e DB_PASS=your_password \
  -e BLOG_TOKEN=your_blog_token \
  -e WECHAT_TOKEN=your_wechat_token \
  -e VISION_TOKEN=your_vision_token
```

### 手动构建镜像

```bash
docker build -f deploy/Dockerfile -t code-blog:latest .
```

### 验证部署

- 博客前台: http://your-host/
- 管理后台: http://your-host/admin/
- 健康检查: http://your-host/admin/ (容器内置 HEALTHCHECK)

## CI/CD

推送到 master 分支时自动触发 GitHub Actions:

1. 构建前后端 + 后端 jar
2. 推送镜像到阿里云 ACR

需要配置以下 Secrets:

| Secret | 说明 |
|--------|------|
| ACR_REGISTRY | 阿里云 ACR 地址 |
| ACR_USERNAME | ACR 用户名 |
| ACR_PASSWORD | ACR 密码 |
| ACR_NAMESPACE | ACR 命名空间 |

## 配置说明

| 文件 | 用途 | 是否提交 |
|------|------|---------|
| application.yml | 基础配置 (端口、MyBatis 等) | 提交 |
| application-dev.yml | 开发环境 (默认数据源地址) | 提交 |
| application-local.yml | 本地凭据 (数据库密码、API Token) | gitignore |
| application-prd.yml | 生产环境 (环境变量注入，无硬编码凭据) | 提交 |

开发环境激活: `dev,local` → 开发配置 + 本地凭据
生产环境激活: `prd` → 生产配置 + 环境变量注入

## License

[MIT](LICENSE)
