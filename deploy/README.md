# Code Blog 部署指南

## 单镜像架构

`
┌──────────────────────────────────────────┐
│  Docker Container (code-blog)            │
│                                          │
│  Nginx :80                               │
│    ├── /            → 博客前台 (Vue SPA) │
│    ├── /admin/      → 管理后台 (Vue SPA) │
│    ├── /blog/api/*  → proxy :28083       │
│    ├── /admin/api/* → proxy :28083       │
│    ├── /admin/xxx/* → proxy :28083 (CRUD)│
│    ├── /common/*    → proxy :28083       │
│    └── /upload/*    → proxy :28083       │
│                                          │
│  Spring Boot :28083 (internal)           │
│  Supervisor → 管理 nginx + java 进程     │
└──────────────────────────────────────────┘
`

## 构建镜像

`ash
docker build -f deploy/Dockerfile -t code-blog:latest .
`

## Docker Compose 部署

`ash
docker compose -f deploy/docker-compose.yml up -d
`

通过环境变量传入数据库凭据：

`ash
DB_URL="jdbc:mysql://your-db:3306/my_blog_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC" \
DB_USER=root \
DB_PASS=your_password \
docker compose -f deploy/docker-compose.yml up -d
`

## 配置说明

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| DB_URL | 数据库连接地址 | jdbc:mysql://host.docker.internal:3306/my_blog_db... |
| DB_USER | 数据库用户名 | 
oot |
| DB_PASS | 数据库密码 | your_password |
| TZ | 时区 | Asia/Shanghai |
| SPRING_PROFILES_ACTIVE | Spring Profile | prd |

## 访问地址

- 博客前台：http://your-host/
- 管理后台：http://your-host/admin/

## 文件说明

| 文件 | 说明 |
|------|------|
| Dockerfile | 多阶段构建：Node 构建前端 + Maven 构建后端 + Alpine 运行时 |
| 
ginx.conf | 前端静态服务 + API 反代 + Gzip + 缓存策略 |
| supervisord.conf | 进程管理：同时运行 Nginx 和 Java |
| entrypoint.sh | 容器启动脚本 |
| docker-compose.yml | 编排文件，含数据库环境变量 |
| .dockerignore | 构建上下文排除规则 |
