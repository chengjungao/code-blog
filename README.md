# 🗡️ Code Blog

基于 Spring Boot + Vue 3 的前后端分离个人博客系统。

## 🏗️ 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7 + MyBatis + MySQL + Solr |
| 管理后台 | Vue 3 + Vite + Element Plus + md-editor-v3 |
| 博客前台 | Vue 3 + Vite + Vue Router |
| 部署 | Docker + Nginx + Supervisor（单镜像） |
| CI/CD | GitHub Actions → 阿里云 ACR |

## 📁 项目结构

`
code-blog/
├── src/main/java/              # 后端 Java 源码
│   └── com/site/blog/my/core/
│       ├── controller/
│       │   ├── admin/          # 管理后台 API（AdminApiController + CRUD Controllers）
│       │   ├── blog/           # 博客前台 API（BlogApiController）
│       │   ├── common/         # 通用接口（验证码、健康检查、消息）
│       │   └── vo/             # 视图对象
│       ├── dao/                # MyBatis Mapper
│       ├── entity/             # 实体类
│       ├── service/            # 业务逻辑层
│       ├── solr/               # Solr 全文搜索
│       ├── interceptor/        # 登录拦截器
│       └── util/               # 工具类
├── src/main/resources/
│   ├── mapper/                 # MyBatis XML
│   ├── blog/                   # Solr 配置
│   ├── application.yml         # 主配置（无凭据）
│   ├── application-dev.yml     # 开发环境配置
│   ├── application-local.yml   # 本地凭据（gitignored）
│   └── application-prd.yml     # 生产环境凭据（gitignored）
├── frontend/
│   ├── admin/                  # 管理后台 Vue 项目
│   └── web/                    # 博客前台 Vue 项目
├── deploy/                     # 部署配置
│   ├── Dockerfile              # 多阶段构建
│   ├── nginx.conf              # Nginx 反代配置
│   ├── supervisord.conf        # 进程管理
│   ├── entrypoint.sh           # 容器入口脚本
│   └── docker-compose.yml      # Docker Compose 编排
├── .github/workflows/          # CI/CD
└── pom.xml
`

## 🚀 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- Node.js 18+
- MySQL 5.7+
- Solr 7+（全文搜索，可选）

### 1. 克隆项目

`ash
git clone https://github.com/chengjungao/code-blog.git
cd code-blog
`

### 2. 配置数据库凭据

复制并编辑本地配置文件（已 gitignore，不会提交）：

`ash
cp src/main/resources/application.yml src/main/resources/application-local.yml
`

编辑 pplication-local.yml，填入实际凭据：

`yaml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/my_blog_db?useUnicode=true&characterEncoding=utf8&autoReconnect=true&useSSL=false&serverTimezone=UTC"
    username: root
    password: your_password

solr:
  home: /path/to/solr/home

# API Token
token: "your_token"
vision:
  token: "your_vision_token"
`

### 3. 启动后端

`ash
mvn spring-boot:run
`

后端运行在 http://localhost:28083

### 4. 启动前端（开发模式）

管理后台：

`ash
cd frontend/admin
npm install
npm run dev
`

博客前台：

`ash
cd frontend/web
npm install
npm run dev
`

### 5. 访问

| 入口 | 地址 |
|------|------|
| 博客前台 | http://localhost:5173 |
| 管理后台 | http://localhost:5174 |

## 🐳 Docker 部署（推荐）

### 架构说明

单 Docker 镜像内同时运行 Nginx + Spring Boot：

`
┌──────────────────────────────────────────┐
│  Docker Container                        │
│                                          │
│  Nginx :80                               │
│    ├── /            → 博客前台           │
│    ├── /admin/      → 管理后台           │
│    ├── /blog/api/*  → Spring Boot :28083 │
│    ├── /admin/*     → Spring Boot :28083 │
│    └── /common/*    → Spring Boot :28083 │
│                                          │
│  Spring Boot :28083 (internal)           │
│  Supervisor → 管理两个进程                │
└──────────────────────────────────────────┘
`

### Docker Compose 部署

**方式一：环境变量传入凭据（推荐）**

`ash
docker compose -f deploy/docker-compose.yml up -d \
  -e DB_URL="jdbc:mysql://your-db-host:3306/my_blog_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC" \
  -e DB_USER=root \
  -e DB_PASS=your_password
`

**方式二：编辑 docker-compose.yml**

修改 environment 部分的默认值后直接启动：

`ash
docker compose -f deploy/docker-compose.yml up -d
`

### 手动构建镜像

`ash
docker build -f deploy/Dockerfile -t code-blog:latest .
`

### 验证部署

- 博客前台：http://your-host/
- 管理后台：http://your-host/admin/
- 健康检查：http://your-host/admin/ (容器内置 HEALTHCHECK)

## 🔧 CI/CD

项目配置了 GitHub Actions 自动构建，推送到 master 分支时自动触发：

1. 构建前后端
2. 推送镜像到阿里云 ACR
3. 需要配置以下 Secrets：

| Secret | 说明 |
|--------|------|
| ACR_REGISTRY | 阿里云 ACR 地址 |
| ACR_USERNAME | ACR 用户名 |
| ACR_PASSWORD | ACR 密码 |
| ACR_NAMESPACE | ACR 命名空间 |

## 📝 配置说明

Spring Boot 配置采用多层合并策略：

| 文件 | 用途 | 是否提交 |
|------|------|---------|
| pplication.yml | 基础配置（端口、MyBatis 等） | ✅ 提交 |
| pplication-dev.yml | 开发环境（默认数据源地址） | ✅ 提交 |
| pplication-local.yml | 本地凭据（数据库密码、API Token） | ❌ gitignore |
| pplication-prd.yml | 生产环境凭据 | ❌ gitignore |

激活顺序：pplication.yml → dev → local，后者覆盖前者同名配置。

## 📄 License

[MIT](LICENSE)
