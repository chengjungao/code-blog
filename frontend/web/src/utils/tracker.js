/**
 * 极简访问埋点（前台）。
 *
 * <p><b>设计原则：前端只负责「如实上报一次」，不做队列、不做批量、不做重试。</b>
 * 削峰是服务端的事——后端有一个小容量的有界内存队列 + 单消费者线程负责写文件，
 * 队列满时丢包并计数，请求会立刻返回，不会把服务端拖住。
 *
 * <p>为什么前端越简单越好：浏览器端多出来的每一点复杂度（本地排队、失败重试、
 * 定时批量）都会变成新的丢数据场景：用户关页面、切网络、切前后台，
 * 攒在内存里的那批数据就没了，而且没人知道丢了多少。
 * 单发 + `keepalive` / `sendBeacon` 反而是最可靠的一条路径。
 */
const ENDPOINT = '/blog/api/stat'
const VID_KEY = 'cj_vid'
const SID_KEY = 'cj_sid'
const SID_TIME_KEY = 'cj_sid_t'
/** 会话滑动窗口：30 分钟无访问算新会话 */
const SESSION_GAP = 30 * 60 * 1000

let vid = ''
let sid = ''
let lastPath = ''
let enterAt = 0
let off = false
let installed = false

function disabled() {
  if (typeof window === 'undefined' || typeof navigator === 'undefined') {
    return true
  }
  // 预渲染用 Puppeteer 渲染全站（日更数十页）。它一挂载 Vue 就会触发上报，
  // 等于每天往统计里灌几十条幽灵 PV；1.4.8 起渲染直连容器内 nginx，
  // 这些请求还会以 127.0.0.1 混进 UV。三道判据任意命中即彻底关闭。
  if (navigator.webdriver === true) {
    return true
  }
  if (window.__PRERENDER__ === true) {
    return true
  }
  const ua = navigator.userAgent || ''
  return /HeadlessChrome|Puppeteer|Lighthouse/i.test(ua)
}

function rand() {
  return Math.random().toString(36).slice(2, 10) + Date.now().toString(36)
}

/** localStorage / sessionStorage 在隐私模式下会抛异常，全部包一层 */
function readStore(store, key) {
  try {
    return store.getItem(key) || ''
  } catch (e) {
    return ''
  }
}

function writeStore(store, key, value) {
  try {
    store.setItem(key, value)
  } catch (e) {
    /* 存不下就退化成内存值，不影响本次会话 */
  }
}

function ensureVisitor() {
  if (vid) {
    return
  }
  vid = readStore(window.localStorage, VID_KEY)
  if (!vid) {
    vid = rand()
    writeStore(window.localStorage, VID_KEY, vid)
  }
}

/** 返回 true 表示这是本次会话的落地页（首次进入或超过 30 分钟） */
function ensureSession() {
  const now = Date.now()
  const stored = readStore(window.sessionStorage, SID_KEY)
  const at = Number(readStore(window.sessionStorage, SID_TIME_KEY) || 0)
  let entry = false
  if (!stored || !at || now - at > SESSION_GAP) {
    sid = rand()
    writeStore(window.sessionStorage, SID_KEY, sid)
    entry = true
  } else {
    sid = stored
  }
  writeStore(window.sessionStorage, SID_TIME_KEY, String(now))
  return entry
}

/**
 * 发送一条事件。
 * @param {object} body 事件体
 * @param {boolean} beacon 页面正在关闭时用 sendBeacon（浏览器会在后台把请求发完）
 */
function send(body, beacon) {
  if (off) {
    return
  }
  const json = JSON.stringify(body)
  if (beacon && navigator.sendBeacon) {
    try {
      // 注意 Content-Type：sendBeacon 默认发 text/plain，后端 @RequestBody 只认 application/json，
      // 必须自己包一层 Blob 指定类型，否则会被 415 拒掉而静默丢数据。
      if (navigator.sendBeacon(ENDPOINT, new Blob([json], { type: 'application/json' }))) {
        return
      }
    } catch (e) {
      /* 落到 fetch 兜底 */
    }
  }
  try {
    fetch(ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: json,
      keepalive: true,
      credentials: 'same-origin'
    }).catch(() => {
      /* 埋点失败绝不影响页面 */
    })
  } catch (e) {
    /* 同上 */
  }
}

/** 补一条停留时长。用 sendBeacon 而不是 fetch，页面卸载时才能确保发出去 */
function flushLeave() {
  if (off || !lastPath || !enterAt) {
    return
  }
  const dur = Math.round((Date.now() - enterAt) / 1000)
  const path = lastPath
  // 清掉，避免 visibilitychange/route 切换重复上报同一次停留
  lastPath = ''
  enterAt = 0
  if (dur < 1 || dur > 3600) {
    return
  }
  send({ type: 'leave', pagePath: path, dur, vid, sid }, true)
}

/**
 * 路由切换时调用（router.afterEach）。
 * @param {{path: string, meta?: object}} to
 */
export function trackPageview(to) {
  if (off || !to || !to.path) {
    return
  }
  install()
  // 先给上一个页面结掉停留时长，再开始新页面
  flushLeave()
  const entry = ensureSession()
  ensureVisitor()
  lastPath = to.path
  enterAt = Date.now()

  send({
    type: 'pv',
    pagePath: to.path,
    title: (to.meta && to.meta.title) || document.title || '',
    // document.referrer 在 SPA 内不会随路由变化，只有落地页才代表真实来源，
    // 站内跳转一律留空，否则「来源」报表会被自己的站内链接灌满。
    ref: entry ? (document.referrer || '') : '',
    entry,
    vid,
    sid,
    scr: window.screen ? window.screen.width + 'x' + window.screen.height : '',
    lang: navigator.language || '',
    ts: new Date().toISOString()
  }, false)
}

/** 注册页面离开/切后台的兜底上报，只装一次 */
export function install() {
  if (installed || typeof window === 'undefined') {
    return
  }
  installed = true
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
      flushLeave()
    } else {
      // 回到前台重新计时，这样停留时长不会把挂机时间算进去
      if (lastPath) {
        enterAt = Date.now()
      }
    }
  })
  // pagehide 比 beforeunload 在移动端更可靠（iOS 上 beforeunload 常不触发）
  window.addEventListener('pagehide', flushLeave)
  window.addEventListener('beforeunload', flushLeave)
}

if (disabled()) {
  off = true
} else {
  install()
}
