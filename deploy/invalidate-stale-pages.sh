#!/bin/sh
# ============================================================
# 预渲染页面启动自检（容器启动时、nginx 之前执行）
#
# 背景：
#   1) pages/ 目录挂的是宿主机卷（/var/blog/pages），会跨部署保留；
#   2) Vite 构建产物的文件名带内容 hash（index-<hash>.js、Notes-<hash>.css），
#      前端一改，旧 hash 文件在新镜像里就不存在了；
#   3) 旧的 pages/*.html 里写死的仍是上一次构建的旧 hash，浏览器请求这些
#      CSS/JS 会命中 nginx 的 SPA 兜底，拿到 200 + text/html，样式与脚本
#      全部失效 —— 表现就是「预渲染页面样式错乱」。
#
#   为什么是删掉而不是就地改写引用：Vue scoped 样式的类名（data-v-xxxxxxxx）
#   在 @vitejs/plugin-vue 里是 hash(相对路径 + 源码全文)，源码改一个字符类名就变。
#   旧 HTML 上的 data-v-* 与新 CSS 必然对不上，改引用也救不回来。
#   所以：只要页面引用了已不存在的资源，就判定为过期页面并删除，由 nginx 回退
#   SPA 壳（壳引用的是当前构建，样式与脚本都正确，内容由 Vue 从 API 拉取），
#   随后启动后 5 分钟的全量渲染会把它们重建出来。
#
#   这个判据是充分的：组件源码一改，其 chunk 的内容 hash 必变，页面里引用的
#   文件名必然缺失；因此「无缺失引用」等价于「构建未变、scopeId 仍匹配」。
# ============================================================

WEB_ROOT="${WEB_ROOT:-/usr/share/nginx/html/blog}"
PAGES_DIR="${STATIC_PAGES_DIR:-$WEB_ROOT/pages}"

[ -d "$PAGES_DIR" ] || exit 0
[ -d "$WEB_ROOT/assets" ] || exit 0

LIST=$(mktemp)
find "$PAGES_DIR" -type f -name '*.html' > "$LIST" 2>/dev/null

kept=0
dropped=0

while IFS= read -r f; do
    [ -f "$f" ] || continue

    missing=$(grep -o "/assets/[A-Za-z0-9._-]*" "$f" 2>/dev/null | sort -u | \
        while IFS= read -r ref; do
            [ -e "$WEB_ROOT$ref" ] || echo "$ref"
        done | head -1)

    if [ -n "$missing" ]; then
        echo "[prerender] 过期页面已失效: ${f#"$PAGES_DIR"/}（缺少 $missing）"
        rm -f "$f"
        dropped=$((dropped + 1))
    else
        kept=$((kept + 1))
    fi
done < "$LIST"

rm -f "$LIST"

# 清理因删除而空掉的子目录（pages/notes/ 等），失败不影响启动
find "$PAGES_DIR" -mindepth 1 -type d -empty -delete 2>/dev/null

echo "[prerender] 启动自检完成：有效 ${kept} 个，失效 ${dropped} 个（失效页面回退 SPA 壳，待全量渲染重建）"

exit 0
