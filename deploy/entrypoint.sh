#!/bin/sh
set -e

echo "========================================="
echo "  Code-Blog Starting..."
echo "========================================="

# 确保数据目录存在
mkdir -p /var/blog/data /var/blog/solr

# Start supervisor (nginx + java)
exec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf
