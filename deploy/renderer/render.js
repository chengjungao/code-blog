#!/usr/bin/env node

/**
 * Puppeteer 静态页面渲染脚本
 * 用于将 Vue SPA 页面渲染为静态 HTML，提升 SEO 和首屏加载速度
 * 
 * 用法：node render.js <url1> [url2] [url3] ...
 * 或从 stdin 读取 URL 列表（每行一个）
 */

const puppeteer = require('puppeteer-core');
const fs = require('fs');
const path = require('path');

// 静态页面输出目录（SPA root 内的 pages 子目录，避免 nginx try_files 重定向循环）
const OUTPUT_DIR = process.env.STATIC_PAGES_DIR || '/usr/share/nginx/html/blog/pages';

// 渲染配置
const CONFIG = {
    // 等待 Vue 渲染完成的选择器
    readySelector: '#app',
    // 额外等待时间（毫秒），确保异步数据加载完成
    extraWait: 2000,
    // 页面加载超时（毫秒）
    timeout: 30000,
    // 并发渲染数
    concurrency: 1,
    // 浏览器参数
    browserArgs: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--disable-gpu',
        '--disable-software-rasterizer'
    ]
};

/**
 * 确保输出目录存在
 */
function ensureOutputDir() {
    if (!fs.existsSync(OUTPUT_DIR)) {
        fs.mkdirSync(OUTPUT_DIR, { recursive: true });
    }
}

/**
 * 将 URL 路径转换为输出文件路径（保持目录结构）
 * 例如：/notes/123 -> notes/123.html
 *       /springboot-mybatis -> springboot-mybatis.html
 *       / -> index.html
 */
function urlToFilePath(urlPath) {
    // 移除开头的斜杠
    let filePath = urlPath.replace(/^\//, '');
    // 如果为空（首页），使用 index
    if (!filePath) {
        filePath = 'index';
    }
    return filePath + '.html';
}

/**
 * 从页面读取预加载数据。
 * 关键：Vue 写入的 __PRELOADED_DATA__ 可能是 reactive proxy，Puppeteer/CDP 直接序列化
 * proxy 会得到空壳 {}。因此在页面上下文内先 JSON.stringify 再 parse（触发 proxy get trap
 * 正常取值），返回普通对象后再交给 CDP 序列化，保证注入数据完整。
 */
async function readPreloadedData(page) {
    return await page.evaluate(() => {
        const d = window.__PRELOADED_DATA__ || null;
        if (!d) return null;
        try { return JSON.parse(JSON.stringify(d)); } catch (e) { return null; }
    });
}

/**
 * 等待页面预加载数据就绪（供水合注入）。
 * 首次超时后自动重载页面重试一次，应对渲染时后端偶发未就绪 / API 慢导致的注入丢失。
 * @returns {Promise<object|null>} 有效的 {blog, comments}；两次尝试均失败返回 null
 */
async function waitForPreloadedData(page, url) {
    for (let attempt = 1; attempt <= 2; attempt++) {
        let data = await readPreloadedData(page);
        if (data && data.blog && data.blog.blogId) return data;

        try {
            await page.waitForFunction(
                () => window.__PRELOADED_DATA__ && window.__PRELOADED_DATA__.blog && window.__PRELOADED_DATA__.blog.blogId,
                { timeout: 15000, polling: 500 }
            );
            data = await readPreloadedData(page);
            if (data && data.blog && data.blog.blogId) return data;
        } catch (e) { /* 本轮超时，进入重试或放弃 */ }

        if (attempt === 1) {
            console.log(`[重试] ${url} 第 1 次未等到预加载数据，重新加载页面重试...`);
            await page.reload({ waitUntil: 'networkidle0', timeout: CONFIG.timeout }).catch(() => {});
            await page.waitForSelector(CONFIG.readySelector, { timeout: 10000 }).catch(() => {});
            await new Promise(resolve => setTimeout(resolve, CONFIG.extraWait));
        }
    }
    console.log(`[提示] ${url} 两次尝试均未等到预加载数据，页面将无注入数据`);
    return null;
}

/**
 * 渲染单个 URL
 */
async function renderUrl(browser, url) {
    const page = await browser.newPage();
    
    try {
        console.log(`[渲染] ${url}`);
        
        // 设置视口
        await page.setViewport({ width: 1280, height: 800 });
        
        // 访问页面
        await page.goto(url, {
            waitUntil: 'networkidle0',
            timeout: CONFIG.timeout
        });
        
        // 等待 Vue 应用挂载
        await page.waitForSelector(CONFIG.readySelector, {
            timeout: 10000
        });
        
        // 额外等待，确保异步数据加载完成
        await new Promise(resolve => setTimeout(resolve, CONFIG.extraWait));
        
        // 读取 Vue 写入的预加载数据；未就绪则轮询等待，首次失败自动重载重试（防注入丢失）
        const preloadedData = await waitForPreloadedData(page, url);
        
        // 检查是否有 404 标记
        const is404 = await page.evaluate(() => {
            const notFoundEl = document.querySelector('.empty-state, .not-found-page');
            return notFoundEl && notFoundEl.textContent.includes('不存在');
        });
        
        if (is404) {
            console.log(`[跳过] ${url} - 页面不存在`);
            await page.close();
            return null;
        }
        
        // 获取渲染后的 HTML
        const html = await page.content();
        
        // 提取 Vue 存储的博客数据，注入到 HTML 中供客户端水合使用（消除闪烁）
        let finalHtml = html;
        // 仅在数据有效时注入（防止空对象污染预渲染文件，导致客户端白屏）
        if (preloadedData && preloadedData.blog && preloadedData.blog.blogId) {
            // 转义 < 防止 </script> 破坏 HTML 结构
            const jsonStr = JSON.stringify(preloadedData).replace(/</g, '\\u003c');
            const dataScript = `<script id="__PRELOADED_DATA__" type="application/json">${jsonStr}</script>`;
            // 用 indexOf 定位拼接，避免 String.replace 对替换串中 $ 符号的特殊解释（博客内容可能含 $）
            const bodyCloseIdx = finalHtml.lastIndexOf('</body>');
            if (bodyCloseIdx !== -1) {
                finalHtml = finalHtml.slice(0, bodyCloseIdx) + dataScript + '\n' + finalHtml.slice(bodyCloseIdx);
            }
        }
        
        // 提取 <head> 中的 meta 标签（用于 SEO）
        const metaInfo = await page.evaluate(() => {
            const title = document.title;
            const description = document.querySelector('meta[name="description"]')?.content || '';
            const ogTitle = document.querySelector('meta[property="og:title"]')?.content || '';
            const ogDescription = document.querySelector('meta[property="og:description"]')?.content || '';
            const ogImage = document.querySelector('meta[property="og:image"]')?.content || '';
            return { title, description, ogTitle, ogDescription, ogImage };
        });
        
        // 生成输出文件路径（保持目录结构）
        const urlPath = new URL(url).pathname;
        const filePath = urlToFilePath(urlPath);
        const outputPath = path.join(OUTPUT_DIR, filePath);
        
        // 确保子目录存在
        const outputDir = path.dirname(outputPath);
        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }
        
        // 写入文件（使用注入了数据的 HTML）
        fs.writeFileSync(outputPath, finalHtml, 'utf-8');
        
        console.log(`[完成] ${url} -> ${filePath}`);
        console.log(`       标题: ${metaInfo.title}`);
        
        await page.close();
        return { url, filePath, metaInfo };
        
    } catch (error) {
        console.error(`[错误] ${url}: ${error.message}`);
        await page.close();
        return null;
    }
}

/**
 * 批量渲染 URL（控制并发）
 */
async function renderUrls(urls) {
    ensureOutputDir();
    
    console.log(`========================================`);
    console.log(`  开始渲染 ${urls.length} 个页面`);
    console.log(`  输出目录: ${OUTPUT_DIR}`);
    console.log(`  并发数: ${CONFIG.concurrency}`);
    console.log(`========================================`);
    
    const startTime = Date.now();
    
    // 启动浏览器（puppeteer-core 需要显式指定 Chromium 路径）
    const chromiumPath = process.env.PUPPETEER_EXECUTABLE_PATH || '/usr/bin/chromium-browser';
    const browser = await puppeteer.launch({
        headless: 'new',
        executablePath: chromiumPath,
        args: CONFIG.browserArgs
    });
    
    const results = [];
    const errors = [];
    
    // 分批处理，控制并发
    for (let i = 0; i < urls.length; i += CONFIG.concurrency) {
        const batch = urls.slice(i, i + CONFIG.concurrency);
        const batchResults = await Promise.all(
            batch.map(url => renderUrl(browser, url))
        );
        
        batchResults.forEach(result => {
            if (result) {
                results.push(result);
            } else {
                errors.push(batch[results.length]);
            }
        });
        
        console.log(`[进度] ${Math.min(i + CONFIG.concurrency, urls.length)}/${urls.length}`);
    }
    
    await browser.close();
    
    const elapsed = ((Date.now() - startTime) / 1000).toFixed(2);
    
    console.log(`========================================`);
    console.log(`  渲染完成`);
    console.log(`  成功: ${results.length}`);
    console.log(`  失败: ${errors.length}`);
    console.log(`  耗时: ${elapsed}s`);
    console.log(`========================================`);
    
    return { results, errors };
}

/**
 * 从命令行参数或 stdin 读取 URL
 */
async function getUrls() {
    const args = process.argv.slice(2);
    
    if (args.length > 0) {
        // 从命令行参数读取
        return args;
    }
    
    // 从 stdin 读取
    return new Promise((resolve) => {
        let data = '';
        process.stdin.setEncoding('utf-8');
        process.stdin.on('data', chunk => data += chunk);
        process.stdin.on('end', () => {
            const urls = data.split('\n')
                .map(line => line.trim())
                .filter(line => line && !line.startsWith('#'));
            resolve(urls);
        });
        process.stdin.on('error', () => resolve([]));
        
        // 如果没有输入，3秒后超时
        setTimeout(() => {
            if (!data) resolve([]);
        }, 3000);
    });
}

/**
 * 主函数
 */
async function main() {
    try {
        const urls = await getUrls();
        
        if (urls.length === 0) {
            console.error('错误: 未提供 URL');
            console.error('用法: node render.js <url1> [url2] ...');
            console.error('或: echo "url1\\nurl2" | node render.js');
            process.exit(1);
        }
        
        console.log(`收到 ${urls.length} 个 URL:`);
        urls.forEach((url, i) => console.log(`  ${i + 1}. ${url}`));
        console.log('');
        
        const { results, errors } = await renderUrls(urls);
        
        // 输出结果摘要
        if (results.length > 0) {
            console.log('\n渲染成功的页面:');
            results.forEach(r => console.log(`  - ${r.filePath}: ${r.metaInfo.title}`));
        }
        
        if (errors.length > 0) {
            console.log('\n渲染失败的页面:');
            errors.forEach(url => console.log(`  - ${url}`));
        }
        
        process.exit(errors.length > 0 ? 1 : 0);
        
    } catch (error) {
        console.error('致命错误:', error);
        process.exit(1);
    }
}

// 运行
main();
