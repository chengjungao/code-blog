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
