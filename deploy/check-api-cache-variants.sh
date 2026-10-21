#!/bin/sh
# 检查 CDN 上的「编码分叉脏对象」：同一个接口，不同 Accept-Encoding 必须返回同样的内容。
#
# 为什么需要它：本站 /blog/api/* 的响应带 Vary: Accept-Encoding（gzip_vary 产生），
# 阿里云 CDN 会按编码各存一份。只要某一档存下旧数据、而目录刷新漏掉那一档，
# 用户就会看到「第 1 页正常、第 2 页文章变少」这种鬼故事（2026-10-21 真实故障）。
#
# 用法：
#   sh deploy/check-api-cache-variants.sh                       # 查默认路径
#   sh deploy/check-api-cache-variants.sh https://域名 /blog/api/index/1 /blog/api/index/2
#
# 退出码：0 = 全部一致；1 = 发现不一致（需要去控制台做目录刷新）

BASE=${1:-https://www.chengjungao.cn}
shift 2>/dev/null || true
if [ "$#" -gt 0 ]; then
    PATHS="$@"
else
    PATHS="/blog/api/index/1 /blog/api/index/2 /blog/api/index/3 /blog/api/config"
fi

fail=0
printf '%-34s %-10s %-10s %s\n' "路径" "identity" "br/zstd" "判定"
printf '%s\n' "--------------------------------------------------------------------------"

for p in $PATHS; do
    a=$(curl -s --noproxy '*' -m 20 -H 'Accept-Encoding: identity' "$BASE$p" | cksum | awk '{print $1}')
    b=$(curl -s --noproxy '*' -m 20 --compressed -H 'Accept-Encoding: gzip, deflate, br, zstd' "$BASE$p" | cksum | awk '{print $1}')
    if [ "$a" = "$b" ]; then
        printf '%-34s %-10s %-10s %s\n' "$p" "$a" "$b" "一致"
    else
        printf '%-34s %-10s %-10s %s\n' "$p" "$a" "$b" "不一致 ← 有一档是旧对象"
        fail=1
    fi
done

if [ "$fail" -ne 0 ]; then
    echo
    echo "❗ 存在编码分叉：去阿里云 CDN 控制台做「目录刷新」，输入 $BASE/blog/api/ （目录级，别只刷单个 URL）。"
    echo "   刷新后重跑本脚本应全部一致；若仍不一致，检查该路径是否被控制台缓存规则设成了非 0 秒。"
fi
exit "$fail"
