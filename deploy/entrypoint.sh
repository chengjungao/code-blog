#!/bin/sh
set -e

echo "========================================="
echo "  Code-Blog Starting..."
echo "========================================="

# 确保数据目录存在
mkdir -p /var/blog/data /var/blog/solr

# 首次启动时从模板初始化 Solr 配置（挂载卷为空时）
if [ ! -f /var/blog/solr/solr.xml ]; then
  echo "Initializing Solr config from template..."
  cp -r /opt/app/solr-template/* /var/blog/solr/
fi

# Start supervisor (nginx + java)
exec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf
