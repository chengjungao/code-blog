export const profile = {
  name: '程军高',
  title: '搜索架构师 / AI 应用架构师',
  summary:
    '11 年后端开发与架构经验，近一年深耕 Agent 与 MCP 平台。在某电商公司主导搜索架构五轮迭代（单体 → 分布式 → 微服务 → 双塔 + 多路召回 → AI Search），日均 1500 万+ PV，并主导 Search Agent 与 MCP 平台建设。最近聚焦 Agent 编排、RAG 与 MCP 协议落地。',
  focus: ['AI 搜索系统', 'Agent 与 MCP 平台', '搜索引擎架构', '大模型微调与 RAG'],
  location: '上海',
  email: 'chengjungao@foxmail.com',
  links: [
    { label: 'GitHub', href: 'https://github.com/chengjungao' },
    { label: '拾光集', href: 'http://www.chengjungao.cn/' },
    { label: '技术笔记', href: '/notes' },
    { label: '作品集', href: '/portfolio' }
  ],
  stats: [
    { label: '后端开发经验', value: '11 年' },
    { label: '架构设计经验', value: '6 年' },
    { label: '团队管理经验', value: '6 年' },
    { label: '搜索平台日均 PV', value: '1500 万' }
  ],
  capabilities: [
    {
      group: '搜索引擎架构',
      level: '核心能力',
      items: ['Solr / Elasticsearch / Lucene', '倒排索引与多路召回', 'LTR 粗排 + GBDT / 深度学习精排', 'Query 理解（实体识别、类目预测、拼写纠错、同义词）', '全量 + 增量索引系统（ETL / MapReduce / 流式）'],
      description: '某电商公司搜索平台的架构主线。索引、召回、排序、Query 理解——日均 1500 万 PV 背后的每一层都摸过。'
    },
    {
      group: 'AI 搜索',
      level: '近期主线',
      items: ['双塔模型与语义召回', '向量检索与 RAG', '大模型微调（Gemma）', 'Milvus / Embedding / Sentence Transformer'],
      description: '从语义召回到会话式搜索，主攻让搜索从关键词匹配走向真正理解意图。'
    },
    {
      group: 'Agent 与 MCP 平台',
      level: '平台实践',
      items: ['LangGraph / ReAct 推理链路', 'MCP 协议落地与工具封装', 'Agent 编排与多轮上下文', 'DSPy / 意图理解 DSL'],
      description: '琢磨怎么让大模型安全、高效地调用企业系统——工具协议、权限边界、执行追踪，一个都不能少。'
    },
    {
      group: '后端与大数据架构',
      level: '工程底盘',
      items: ['Java / Scala / Python', 'Spring Cloud / Spring Boot 微服务', 'Hadoop / Spark / Flink / Kafka', 'Redis / HBase / ClickHouse', 'Docker / K8S / CI/CD'],
      description: '搜索和广告业务背后的工程底盘。微服务、大数据、缓存和容器化，撑起了上面所有业务逻辑。'
    }
  ]
}

export const portfolioItems = [
  {
    title: 'AI 搜索 Agent',
    type: 'AI 应用 · 2025.12~2026.06',
    status: '已上线',
    tags: ['LangGraph', 'Milvus', 'GPT', 'Gemma 微调', 'DSPy', 'MCP'],
    description:
      '某电商平台的下一代会话式商品搜索服务。基于 MCP 协议构建搜索 Agent，将用户自然语言查询实时转化为结构化检索指令，精准召回商品并返回个性化推荐。',
    highlights: [
      '意图理解层：基于 Gemma 微调模型解析查询意图，输出结构化 DSL，支持多轮上下文',
      'Agent 编排层：基于 LangGraph 构建 ReAct 推理链路，协调记忆、召回与后端服务',
      '多路召回：关键词 + 语义 + 类目召回融合',
      '上线后会话式搜索转化率较传统关键词搜索提升约 22%，平均交互轮次 3.2 轮'
    ]
  },
  {
    title: 'MCP 平台',
    type: '平台建设 · 2025.12~2026.02',
    status: '已交付',
    tags: ['Spring Boot', 'Redis Cluster', 'PostgreSQL', 'MCP 协议'],
    description:
      '某电商公司内部 AI 基础平台的核心中间件。基于 MCP 协议将公司内部 7000+ REST API 标准化封装为可被发现、可调用的 MCP 工具服务，赋能内外部 Agent 即插即用式调用。',
    highlights: [
      'MCP 服务治理：工具注册、发布、下线全生命周期管理，支持鉴权、流量控制与灰度发布',
      'MCP 执行引擎：多节点 Load Balance 集群部署，实时监控 QPS、延迟、错误率',
      'Agent 平均接入周期从 2 周缩短至 1 天'
    ]
  },
  {
    title: '电商商品搜索平台',
    type: '架构设计 · 2016.11~2026.06',
    status: '日均 PV 1500 万',
    tags: ['Solr', 'Redis', 'Spark', 'Kafka', 'HBase', 'Milvus', 'K8S'],
    description:
      '某电商公司全站商品搜索引擎，覆盖 PC / 移动端全部搜索场景。历经五轮架构演进（单体 → 分布式 → 微服务 → 双塔 + 多路召回 → AI Search），峰值 QPS 达数万级别。',
    highlights: [
      'Query 理解：实体识别、类目预测、拼写纠错、同义词扩展、多意图消歧',
      '多路召回：关键词 + 语义 + 新品 + 个性化协同，召回覆盖率提升约 35%，CTR 2.2% → 2.7%',
      '排序系统：LTR 粗排 + GBDT / 深度学习精排',
      '全量索引 TB 级日更 < 24h，增量索引端到端延迟 < 30s'
    ]
  },
  {
    title: '电商广告平台',
    type: '架构设计 · 2020.5~2021.12',
    status: '已上线',
    tags: ['Spring Boot', 'Solr', 'Redis', 'Spark', 'ClickHouse'],
    description:
      '某电商公司商品广告系统，为商家提供广告投放服务。通过关键字和商品相关性召回广告，二次精排后投放至电商页面，含广告管理、投放和 BI 结算系统。',
    highlights: [
      '负责平台整体架构设计、多部门协调与项目管理',
      '开发核心广告投放服务：召回 + 精排 + 投放',
      '构建广告效益 BI 与结算系统，支持离线 Job 报表'
    ]
  },
  {
    title: 'Redis 数据服务平台',
    type: '平台建设 · 2019.3~2019.9',
    status: '已交付',
    tags: ['Redis', 'Spring Boot', 'Hystrix', 'Eureka'],
    description:
      '为某电商公司集团提供统一的 Redis 服务，集运维、管理、监控、报警与 Rest 服务于一体，单节点 10 万 QPS，日均流量超 10 亿。',
    highlights: [
      'Redis Cluster 管理运营：集群安装、监控报警、日常运维',
      'Redis Executor：支持读写分离、Cluster 熔断',
      '提供 Spring Boot Starter 快速接入客户端'
    ]
  },
  {
    title: '浦发银行信用卡中心数据服务平台',
    type: '大数据开发 · 2016.1~2016.11',
    status: '已交付',
    tags: ['Hadoop', 'Solr', 'HBase', 'MapReduce', 'Zookeeper'],
    description:
      '为浦发银行信用卡中心构建全行级大数据查询平台，支撑风控、运营、客服等多条业务线，日均调用量百万级。',
    highlights: [
      '开发高性能 REST 接口，P99 响应 < 200ms',
      '独立维护大规模 Solr 集群（千万级索引文档），SLA > 99.9%',
      '基于 Zookeeper 实现 Solr 主备切换'
    ]
  },
  {
    title: '个人品牌网站',
    type: '产品化项目 · 持续迭代',
    status: '第一版',
    tags: ['Vue 3', 'Spring Boot', 'Solr', 'AI 对话'],
    description:
      '「拾光集」—— 个人技术博客与品牌网站，集成嵌入式 Solr 搜索、HanLP 中文分词、AI 智能分身和 Markdown 内容管理。',
    highlights: ['Vue 3 双前端 + Spring Boot 后端', '嵌入式 Solr 全文搜索', 'AI 智能分身集成', 'Docker 一体化部署']
  }
]

export const experience = [
  {
    company: '某电商公司',
    role: '技术经理 / 架构师',
    period: '2016.11 ~ 2026.06',
    summary:
      '负责电商搜索平台整体架构设计、技术选型与团队管理。主导搜索架构五轮演进（单体 → 分布式 → 微服务 → 双塔 + 多路召回 → AI Search），团队规模最高 11 人。',
    achievements: [
      '主导搜索服务微服务重构，落地 CI/CD 全流程，交付周期缩短 60%',
      '设计 Redis + Solr 高性能数据服务平台，P99 响应 < 80ms，支撑千万级日请求',
      '主导 PC Builder（自助装机）独立业务线从 0 到 1，成长为最挣钱支线业务',
      '近一年主导 Search Agent 与 MCP 平台建设'
    ]
  },
  {
    company: '上海天玑科技有限公司',
    role: 'Hadoop 大数据开发工程师',
    period: '2016.01 ~ 2016.11',
    summary: '为浦发银行信用卡中心构建并维护全行级大数据查询平台，支撑风控、运营、客服等多条业务线。',
    achievements: ['开发高性能 REST 接口，P99 响应 < 200ms', '独立维护大规模 Solr 集群，SLA > 99.9%']
  },
  {
    company: '上海奔耀信息科技有限公司',
    role: 'Java 开发工程师',
    period: '2015.07 ~ 2016.02',
    summary: '为甲方客户提供文本分析数据处理与可视化报表一站式解决方案。',
    achievements: ['对接文本分析系统，构建 Solr 检索数据管道，日处理文本数十万条']
  }
]

export const education = [
  {
    school: '长江大学',
    major: '计算机科学与技术',
    degree: '本科',
    period: '2011.9 ~ 2015.7'
  }
]

export const certifications = ['PMP 证书', 'Scrum Master', '2017 年度优秀员工']

export const techStack = {
  languages: ['Java', 'Scala', 'Python'],
  microservices: ['Spring Cloud', 'Spring Boot', 'Spring Data', 'Eureka', 'Hystrix', 'Ribbon'],
  bigData: ['Hadoop', 'HBase', 'Hive', 'MapReduce', 'Spark', 'Flink', 'Kafka', 'Zookeeper', 'HDFS'],
  search: ['Solr', 'Elasticsearch', 'Lucene', 'Milvus'],
  dataAnalysis: ['Hive', 'ClickHouse'],
  ai: ['Langchain', 'LangGraph', 'GPT', 'Gemma 微调', 'Embedding', '双塔模型', 'RAG', 'DSPy', 'MCP'],
  devops: ['Docker', 'K8S', 'Grafana', 'ELK']
}
