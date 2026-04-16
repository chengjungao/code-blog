# ===========================================
# Multi-stage Dockerfile for My-Blog
# Build context: D:\Projects\code-blog
# Usage: docker build -t my-blog .
# ===========================================

# ---------- Stage 1: Build Admin Frontend ----------
FROM node:18-alpine AS build-admin
WORKDIR /app
COPY frontend/admin/package.json frontend/admin/package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com
COPY frontend/admin/ ./
RUN npm run build

# ---------- Stage 2: Build Blog Frontend ----------
FROM node:18-alpine AS build-blog
WORKDIR /app
COPY frontend/web/package.json frontend/web/package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com
COPY frontend/web/ ./
RUN npm run build

# ---------- Stage 3: Maven Build Backend ----------
FROM maven:3.8-openjdk-8 AS build-backend
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn package -DskipTests -B

# ---------- Stage 4: Final Image ----------
FROM openjdk:8u342-jre-alpine

LABEL org.opencontainers.image.authors="chengjungao"
LABEL org.opencontainers.image.description="My-Blog: Nginx + Spring Boot single image"

# Install nginx + supervisor
RUN apk add --no-cache nginx supervisor curl

# Create directories
RUN mkdir -p /opt/app/blog \
    /var/log/supervisor \
    /var/blog/data \
    /var/blog/solr

# Copy Spring Boot JAR (already contains application-*.yml)
COPY --from=build-backend /app/target/my-blog-4.0.0-SNAPSHOT.jar /opt/app/blog/app.jar

# Copy frontend builds
COPY --from=build-admin /app/dist /usr/share/nginx/html/admin
COPY --from=build-blog /app/dist /usr/share/nginx/html/blog

# Copy nginx config
COPY deploy/nginx.conf /etc/nginx/http.d/default.conf

# Copy supervisor config
COPY deploy/supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Copy entrypoint
COPY deploy/entrypoint.sh /opt/entrypoint.sh
RUN chmod +x /opt/entrypoint.sh

# Remove default nginx config
RUN rm -f /etc/nginx/http.d/default.conf.default

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost/admin/ || exit 1

WORKDIR /opt/app/blog

ENTRYPOINT ["/opt/entrypoint.sh"]
