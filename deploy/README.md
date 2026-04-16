# My-Blog 部署指南

## 单镜像架构

```
┌─────────────────────────────────────────────┐
│  Docker Container (my-blog)                │
│                                             │
│  Nginx :80                                  │
│    ├── /            → 博客前台 (Vue SPA)    │
│    ├── /admin/      → 管理后台 (Vue SPA)    │
│    ├── /blog/api/*  → proxy :28083          │
│    ├── /admin/api/* → proxy :28083          │
│    ├── /admin/xxx/* → proxy :28083 (CRUD)   │
│    ├── /common/*    → proxy :28083          │
│    └── /blog/default/* → proxy :28083       │
│                                             │
│  Spring Boot :28083 (internal)              │
│    └── my-blog-4.0.0-SNAPSHOT.jar           │
│                                             │
│  Supervisor → 管理 nginx + java 进程        │
└─────────────────────────────────────────────┘
```

## 构建命令

```bash
cd D:\Projects
docker build -f My-Blog/deploy/Dockerfile -t my-blog .
```

## Docker Compose 部署

```bash
cd D:\Projects
docker compose -f My-Blog/deploy/docker-compose.yml up -d
```

## 配置数据库连接

方式一：环境变量（推荐）
```bash
docker compose -f My-Blog/deploy/docker-compose.yml up -d \
  -e DB_URL=jdbc:mysql://your-mysql-host:3306/my_blog_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC \
  -e DB_USER=root \
  -e DB_PASS=your_password
```

方式二：修改 docker-compose.yml 中的 environment 段

## 访问地址

- 博客前台：`http://your-host/`
- 管理后台：`http://your-host/admin/`

## 本地开发 vs 部署

| 环境 | 博客前台 | 管理后台 |
|------|---------|---------|
| 本地开发 | localhost:3000 | localhost:3000/admin/ |
| Docker 部署 | your-host/ | your-host/admin/ |

## 文件说明

| 文件 | 说明 |
|------|------|
| Dockerfile | 多阶段构建：Node构建前端 + Maven构建后端 + Alpine运行时 |
| nginx.conf | 前端静态服务 + API 反代 + Gzip + 缓存 |
| supervisord.conf | 进程管理：同时运行 Nginx 和 Java |
| entrypoint.sh | 容器启动脚本 |
| docker-compose.yml | 编排文件，含数据库环境变量 |
| .dockerignore | 构建上下文排除规则（减小镜像体积） |
