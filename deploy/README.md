# ============================================
# Code-Blog 阿里云部署完整指南
# ============================================

## 一、本地构建 & 推送到 ACR

### 1.1 登录阿里云 ACR

docker login --username=你的阿里云账号 registry.cn-hangzhou.aliyuncs.com
# 输入密码（ACR 独立密码，非阿里云登录密码）

### 1.2 构建镜像

cd D:\Projects\code-blog
docker build -f deploy/Dockerfile -t code-blog:latest .

### 1.3 打标签 & 推送

docker tag code-blog:latest registry.cn-hangzhou.aliyuncs.com/chengjungao/code-blog:latest
docker push registry.cn-hangzhou.aliyuncs.com/chengjungao/code-blog:latest

# 如需带版本号：
docker tag code-blog:latest registry.cn-hangzhou.aliyuncs.com/chengjungao/code-blog:1.0.0
docker push registry.cn-hangzhou.aliyuncs.com/chengjungao/code-blog:1.0.0

---

## 二、服务器部署

### 2.1 SSH 登录服务器

ssh root@你的服务器IP

### 2.2 创建必要目录

mkdir -p /var/blog/data /var/blog/solr /etc/ssl/nginx

### 2.3 上传 SSL 证书

# 本地执行（把证书文件传到服务器）
scp chengjungao.cn.pem  root@你的服务器IP:/etc/ssl/nginx/
scp chengjungao.cn.key  root@你的服务器IP:/etc/ssl/nginx/

### 2.4 登录 ACR（服务器端）

docker login --username=你的阿里云账号 registry.cn-hangzhou.aliyuncs.com

### 2.5 拉取镜像

docker pull registry.cn-hangzhou.aliyuncs.com/chengjungao/code-blog:latest

### 2.6 启动容器

docker run -d \
  --name code-blog \
  --restart=always \
  -p 80:80 \
  -p 443:443 \
  -e TZ=Asia/Shanghai \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://172.17.0.1:3306/my_blog_db?useUnicode=true&characterEncoding=utf8&autoReconnect=true&useSSL=false&serverTimezone=UTC" \
  -e SPRING_DATASOURCE_USERNAME="root" \
  -e SPRING_DATASOURCE_PASSWORD="你的数据库密码" \
  -e FILE_ATTACHMENT=/var/blog/data/ \
  -v /var/blog/data:/var/blog/data \
  -v /var/blog/solr:/var/blog/solr \
  -v /etc/ssl/nginx:/etc/ssl/nginx:ro \
  registry.cn-hangzhou.aliyuncs.com/chengjungao/code-blog:latest

> 注意：172.17.0.1 是 Docker 默认网桥 IP，用于容器访问宿主机 MySQL。
> 如果 MySQL 不在本机，改为 MySQL 实际 IP。

### 2.7 查看日志

docker logs -f code-blog

# 分别查看
docker exec code-blog tail -f /var/log/supervisor/java.log     # Spring Boot 日志
docker exec code-blog tail -f /var/log/supervisor/nginx.log    # Nginx 日志

---

## 三、后续更新部署

### 本地（一条龙）

cd D:\Projects\code-blog
docker build -f deploy/Dockerfile -t code-blog:latest . && ^
docker tag code-blog:latest registry.cn-hangzhou.aliyuncs.com/chengjungao/code-blog:latest && ^
docker push registry.cn-hangzhou.aliyuncs.com/chengjungao/code-blog:latest

### 服务器

docker pull registry.cn-hangzhou.aliyuncs.com/chengjungao/code-blog:latest && ^
docker stop code-blog && ^
docker rm code-blog && ^
docker run -d \
  --name code-blog \
  --restart=always \
  -p 80:80 \
  -p 443:443 \
  -e TZ=Asia/Shanghai \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://172.17.0.1:3306/my_blog_db?useUnicode=true&characterEncoding=utf8&autoReconnect=true&useSSL=false&serverTimezone=UTC" \
  -e SPRING_DATASOURCE_USERNAME="root" \
  -e SPRING_DATASOURCE_PASSWORD="你的数据库密码" \
  -e FILE_ATTACHMENT=/var/blog/data/ \
  -v /var/blog/data:/var/blog/data \
  -v /var/blog/solr:/var/blog/solr \
  -v /etc/ssl/nginx:/etc/ssl/nginx:ro \
  registry.cn-hangzhou.aliyuncs.com/chengjungao/code-blog:latest

---

## 四、常用运维命令

# 停止
docker stop code-blog

# 重启
docker restart code-blog

# 进入容器
docker exec -it code-blog sh

# 查看容器状态
docker ps | grep code-blog

# 清理旧镜像
docker image prune -f

---

## 五、访问统计埋点与明细日志

### 5.1 数据流

```
浏览器 ──单发上报──▶ POST /blog/api/stat
                        │  只做「取字段 + 入队」，无磁盘 IO / 无 DB IO
                        ▼
              有界内存队列（page-stat.queue-capacity，默认 2000）
                        │  offer 非阻塞：队列满就丢弃并计数，绝不拖住 Tomcat 线程
                        ▼
              单消费者线程 page-stat-consumer（批量 drainTo + 写 JSONL）
                        │
        ┌───────────────┴────────────────┐
        ▼                                ▼
/var/blog/data/page-stat/         内存当日 PV/UV
  stat-YYYY-MM-DD.jsonl                 │ 每 5 分钟
  （一行一条事件，保留 90 天）            ▼
        │                        tb_page_stat（旧链路，历史趋势不断档）
        ▼
admin「访问统计」页：明细口径多维报表（缺失区间自动回退 DB 并标注 src=db）
```

> 设计要点：埋点是「高频、低价值、可丢失」的请求。**队列故意做小**，
> 它只是削峰缓冲而不是数据仓库——真有上千条积压说明磁盘已经出问题，
> 这时丢弃并计数（报表「数据健康」可见）比堆积更安全。
> 浏览器端不做队列/批量/重试：多一层缓存就多一种「关页面丢数据」的场景。

### 5.2 相关配置

| 配置项 | 默认 | 说明 |
|---|---|---|
| `page-stat.enabled` | true | 关掉后埋点接口直接忽略上报 |
| `page-stat.log-dir` | prd `/var/blog/data/page-stat` | **必须在挂载卷内**，否则容器重建即丢 |
| `page-stat.queue-capacity` | 2000 | 有界队列容量 |
| `page-stat.batch-size` | 500 | 单批写盘条数 |
| `page-stat.retain-days` | 90 | 明细保留天数（每天 03 点前后清理） |
| `page-stat.ip-mode` | full | `full` 保留完整 IP（便于核对 CDN 链路）；`mask` 只留网段 |

环境变量覆盖：`PAGE_STAT_LOG_DIR`。

### 5.3 常用排查命令

```bash
# 今天的明细里有几条、长什么样（容器内）
docker exec code-blog sh -c 'wc -l /var/blog/data/page-stat/stat-$(date +%F).jsonl'
docker exec code-blog sh -c 'tail -3 /var/blog/data/page-stat/stat-$(date +%F).jsonl'

# 看 CDN 到底往 X-Forwarded-For 里写了什么（ipSrc / xff 字段就是为此保留的）
docker exec code-blog sh -c 'tail -50 /var/blog/data/page-stat/stat-$(date +%F).jsonl | grep -o "\"xff\":\"[^\"]*\"" | sort | uniq -c'

# 消费者线程与队列健康度（也可直接在 admin「访问统计 → 数据健康」查看）
docker exec code-blog sh -c 'ps -o pid,comm,args | grep -c page-stat || true'
docker logs --tail 200 code-blog | grep "\[stat\]"
```

### 5.4 历史上的三个失真源（已处理）

1. **预渲染自己上报**：Puppeteer 渲染全站，页面一挂载 Vue 就触发埋点，
   每天固定注入数十条幽灵 PV。现在前端用 `navigator.webdriver` / `HeadlessChrome` /
   `window.__PRERENDER__` 三重判据直接关闭，后端另有 `internal`（回环地址）兜底。
2. **爬虫计入 PV**：Googlebot 执行 JS，只能靠 UA 拦；已按 UA 黑名单过滤并单独计数。
3. **真实 IP 不可信**：nginx 用 `$proxy_add_x_forwarded_for` 会把 CDN 边缘节点追加到
   XFF **末尾**，原实现取**首段**（客户端可伪造）⇒ 限流与 UV 都可被绕过。
   现统一走 `ClientIpResolver`：剥掉末段后从链尾取第一个公网地址，并保留原始 XFF 供核对。

