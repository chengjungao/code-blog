# ===========================================
# Multi-stage Dockerfile for Code-Blog
# Build context: project root
# Usage: docker build -f deploy/Dockerfile -t code-blog:latest .
# ===========================================

# ---------- Stage 1: Build Admin Frontend ----------
FROM node:20-alpine AS build-admin
WORKDIR /app
COPY frontend/admin/package.json frontend/admin/package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com
COPY frontend/admin/ ./
RUN npm run build

# ---------- Stage 2: Build Blog Frontend ----------
FROM node:20-alpine AS build-blog
WORKDIR /app
COPY frontend/web/package.json frontend/web/package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com
COPY frontend/web/ ./
RUN npm run build

# ---------- Stage 3: Maven Build Backend ----------
FROM maven:3.8-openjdk-8 AS build-backend
WORKDIR /app
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>\n\
<settings>\n\
  <mirrors>\n\
    <mirror>\n\
      <id>aliyun</id>\n\
      <mirrorOf>central</mirrorOf>\n\
      <name>Aliyun Maven Mirror</name>\n\
      <url>https://maven.aliyun.com/repository/public</url>\n\
    </mirror>\n\
  </mirrors>\n\
</settings>' > /root/.m2/settings.xml
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn package -DskipTests -B

# ---------- Stage 4: Final Image ----------
FROM eclipse-temurin:8-jre-alpine

LABEL org.opencontainers.image.authors="chengjungao"
LABEL org.opencontainers.image.description="Code-Blog: Nginx + Spring Boot single image"

# Alpine Aliyun mirror + install packages
RUN sed -i 's|https://dl-cdn.alpinelinux.org|https://mirrors.aliyun.com|g' /etc/apk/repositories \
 && apk add --no-cache nginx supervisor curl

# Create directories
RUN mkdir -p /opt/app/blog \
    /var/log/supervisor \
    /var/blog/data

# Copy Spring Boot JAR
COPY --from=build-backend /app/target/my-blog-4.0.0-SNAPSHOT.jar /opt/app/blog/app.jar

# Copy frontend builds
COPY --from=build-admin /app/dist /usr/share/nginx/html/admin
COPY --from=build-blog /app/dist /usr/share/nginx/html/blog

# Copy config files
COPY deploy/nginx.conf /etc/nginx/http.d/default.conf
COPY deploy/supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Fix line endings for config files (CRLF + BOM)
RUN sed -i '1s/^\xEF\xBB\xBF//' /etc/nginx/http.d/default.conf /etc/supervisor/conf.d/supervisord.conf \
 && sed -i 's/\r$//' /etc/nginx/http.d/default.conf /etc/supervisor/conf.d/supervisord.conf

# Write entrypoint.sh directly inside container (avoids BOM/CRLF from Windows COPY)
RUN printf '#!/bin/sh\nset -e\necho "========================================="\necho "  Code-Blog Starting..."\necho "========================================="\nmkdir -p /var/blog/data /var/blog/solr\nexec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf\n' > /opt/entrypoint.sh \
 && chmod +x /opt/entrypoint.sh

# Remove default nginx config
RUN rm -f /etc/nginx/http.d/default.conf.default

EXPOSE 80 443

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost/admin/ || exit 1

WORKDIR /opt/app/blog

ENTRYPOINT ["/opt/entrypoint.sh"]
