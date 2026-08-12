export const profile = {
  name: '程军高',
  title: '搜索架构师 / AI 搜索系统工程师',
  summary:
    '9 年 Java，6 年架构和团队管理。在 Newegg 搭了六年搜索平台，日均扛着 1500 万 PV。现在专注 AI 搜索方向——双塔召回、大模型微调、Agent 和 MCP 平台。这里是我的数字花园，记录技术，也记录生活。',
  focus: ['AI 搜索系统', '搜索引擎架构', '双塔模型与向量检索', 'Agent 与 MCP 平台'],
  location: '上海',
  email: 'chengjungao@foxmail.com',
  links: [
    { label: 'GitHub', href: 'https://github.com/chengjungao' },
    { label: '拾光集', href: 'http://www.chengjungao.cn/' },
    { label: '技术笔记', href: '/notes' },
    { label: '作品集', href: '/portfolio' }
  ],
  stats: [
    { label: 'Java 开发经验', value: '9 年' },
    { label: '架构设计经验', value: '6 年' },
    { label: '团队管理与 Scrum Master', value: '6 年' },
    { label: '搜索平台日均 PV', value: '1500 万' }
  ],
  capabilities: [
    {
      group: '搜索引擎架构',
      level: '核心能力',
      items: ['Solr / Elasticsearch / Lucene', '倒排索引与召回链路', '排序与相关性调优', 'Solr 插件开发（自定义评分与分词器）', 'Query 理解（实体识别、类目预测、拼写检查、同义词）'],
      description: 'Newegg 搜索平台的六年主架构。索引、召回、排序、Query 理解——日均 1500 万 PV 背后的每一层都摸过。'
    },
    {
      group: 'AI 搜索',
      level: '近期主线',
      items: ['双塔模型与语义召回', '向量检索与 RAG 知识库', '大模型微调', 'Milvus / Embedding'],
      description: '从 AI 购物助手入坑，现在主攻语义召回和大模型微调——让搜索从关键词匹配走向真正理解意图。'
    },
    {
      group: 'Agent 与 MCP 平台',
      level: '平台实践',
      items: ['Agent 开发与工具调用', 'MCP 平台建设', 'Langchain 集成', '工作流编排与执行链路追踪'],
      description: '琢磨怎么让大模型安全地调用企业系统——工具协议、权限边界、执行追踪，一个都不能少。'
    },
    {
      group: '后端与大数据架构',
      level: '工程底盘',
      items: ['Java / Scala / Python', 'Spring Cloud / Spring Boot 微服务', 'Hadoop / Spark / Flink / Kafka', 'Redis / HBase / ClickHouse', 'Docker / K8S'],
      description: '搜索和广告业务背后的工程底盘。微服务、大数据、缓存和容器化，撑起了上面所有业务逻辑。'
    }
  ]
}

export const portfolioItems = [
  {
    title: 'Newegg 商品搜索平台',
    type: '架构设计 · 2016~至今',
    status: '日均 PV 1500 万',
    tags: ['Solr', 'Redis', 'Spark', 'Flink', 'Kafka', 'Spring Cloud', 'K8S'],
    description:
      '为 Newegg 电商网站提供商品搜索服务，包含搜索引擎 Solr 插件、高并发数据服务平台、大数据索引系统、搜索召回分布式微服务、排序子系统和 Query 理解子系统。',
    highlights: [
      '设计搜索平台整体架构，含索引 ETL、召回服务、排序系统和 Query 理解等子系统',
      '开发高性能 Redis 数据服务平台和 Newegg Solr 自定义插件',
      '日均 PV 1500 万，解决 Solr 插件热部署、Spark Kafka 插件、Varnish 负载倾斜等重大风险',
      '技术栈：Solr、Redis、HDFS、Hbase、MR、Kafka、Spark、Flink、Hive、Spring Cloud、Docker、K8S'
    ],
    link: 'https://www.newegg.com/p/pl?d=ddr5'
  },
  {
    title: 'Newegg AI Shopping Assistant',
    type: 'AI 应用 · 2023.5~2023.12',
    status: '已上线',
    tags: ['Langchain', 'Milvus', 'ChatGPT', 'Embedding', 'RAG'],
    description:
      '利用大模型 AIGC 能力为用户提供购物、推荐、咨询、售后等全方位服务的电商 AI 购物助手。包含向量知识库系统、服务代理系统、Assistant Service 和用户行为分析系统。',
    highlights: [
      '担任项目 PM 和 Scrum Master，负责整体架构设计',
      '开发基于向量引擎的知识库系统，提供 Newegg 基础知识库服务',
      '代理搜索、订单、物流等多个内部服务，实现 ChatGPT 驱动的助手服务',
      '技术栈：Langchain、Milvus、ChatGPT、Embedding'
    ],
    link: 'https://www.newegg.com/'
  },
  {
    title: 'AI 搜索系统架构',
    type: '架构实践 · 2024~至今',
    status: '持续完善',
    tags: ['AI Search', 'Two-tower', 'RAG', 'LLM', 'Vector Search'],
    description:
      '面向业务知识检索和问答场景，设计语义召回、关键词召回、重排、上下文组装和生成链路。覆盖双塔模型训练、大模型微调和 Agent 工具链集成。',
    highlights: [
      '双塔召回与向量检索：样本构建、特征治理、向量索引选型',
      '混合召回链路：关键词召回 + 语义召回 + 重排',
      '大模型微调与 RAG 知识增强生成',
      'MCP 平台：工具注册与发现、执行链路追踪、企业系统接入'
    ]
  },
  {
    title: 'Newegg Sponsored 广告平台',
    type: '架构设计 · 2020.5~2021.12',
    status: '已上线',
    tags: ['Spring Boot', 'Solr', 'Redis', 'Spark', 'ClickHouse'],
    description:
      'Newegg 商品广告系统，为商家提供广告投放服务，通过关键字和商品相关性召回广告，二次精排后投放至电商页面。含广告管理、投放系统和 BI 结算系统。',
    highlights: [
      '负责平台整体架构设计、多部门沟通协调和项目管理',
      '开发核心模块广告投放服务：召回 + 精排 + 投放',
      '构建广告效益 BI 系统和结算系统，支持离线 Job 报表',
      '技术栈：AngularJS、Spring Boot、Solr、Redis、HDFS、Spark、ClickHouse'
    ],
    link: 'https://www.newegg.com/p/pl?d=ssd'
  },
  {
    title: 'Newegg Solr SaaS 平台',
    type: '平台建设 · 2019.11~2020.5',
    status: '已交付',
    tags: ['Solr', 'Spring Boot', 'Spark', 'MR', 'Eureka'],
    description:
      '为 Newegg 集团提供统一的 Solr 服务，集运维、管理、监控、报警和 Solr Rest 服务于一体。含 Solr Cloud 管理、服务执行器集群和 ETL 工具。',
    highlights: [
      'Solr Cloud 管理：集群安装初始化、监控报警、自定义插件管理',
      'Solr Service Executor：高并发安全的 Solr 查询执行服务',
      'ETL 工具：MR 和 Spark Streaming 多数据源同步至 Solr',
      '负责项目整体架构设计、管理和推广'
    ]
  },
  {
    title: 'Newegg Redis SaaS 平台',
    type: '平台建设 · 2019.3~2019.9',
    status: '已交付',
    tags: ['Redis', 'Spring Boot', 'Hystrix', 'Eureka'],
    description:
      '为 Newegg 集团提供统一 Redis 服务，单节点 10 万 QPS，日均流量超 10 亿。含 Redis Cluster 管理运营、服务执行器和 Spring Boot Starter 客户端。',
    highlights: [
      '单节点 10 万 QPS，日均流量超过 10 亿',
      'Redis Executor：支持读写分离、Cluster 熔断',
      '提供 Spring Boot Starter 快速接入客户端',
      '负责整体架构设计、Executor 开发和项目推广'
    ]
  },
  {
    title: '浦发银行信用卡中心数据服务平台',
    type: '大数据开发 · 2016.2~2016.11',
    status: '已交付',
    tags: ['Hadoop', 'Solr', 'HBase', 'MapReduce', 'Zookeeper'],
    description:
      '为浦发银行信用卡中心开发基础数据查询平台，使用 MapReduce 批量建立索引，通过 Rest 接口为银行业务提供快速数据查询服务。',
    highlights: [
      '开发 MR 程序批量建立 Solr / HBase 索引',
      '维护大规模 Solr 集群并负责调优',
      '开发高并发 Rest 数据服务接口',
      '基于 Zookeeper 实现 Solr 主备切换'
    ]
  },
  {
    title: '个人品牌网站',
    type: '产品化项目 · 持续迭代',
    status: '第一版',
    tags: ['Vue 3', 'Spring Boot', 'Solr', 'Content System'],
    description:
      '「拾光集」—— 个人技术博客与品牌网站，集成嵌入式 Solr 搜索、HanLP 中文分词、AI 对话和 Markdown 内容管理。',
    highlights: ['Vue 3 双前端 + Spring Boot 后端', '嵌入式 Solr 全文搜索', 'AI 助手集成', 'Docker 一体化部署']
  }
]

export const experience = [
  {
    company: '新蛋信息技术（中国）有限公司',
    role: '技术经理兼架构师',
    period: '2016.11 ~ 至今',
    summary:
      '负责电商搜索平台整体架构设计、业务项目和技术方向规划。管理 Site Search 组项目资源和人力资源，负责技术攻坚和 Code Review。',
    achievements: [
      '重构 Newegg 电商搜索服务，完成微服务化和 CI/CD',
      '设计并开发高性能 Redis 和 Solr 数据服务平台',
      '设计和开发 Newegg 最挣钱的支线业务 PC Builder（自助装机）',
      '开发 Newegg Java Framework 中众多组件'
    ]
  },
  {
    company: '上海天玑科技有限公司',
    role: 'Hadoop 大数据开发工程师',
    period: '2016.1 ~ 2016.11',
    summary: '为浦发银行信用卡中心大数据开发部门开发基础数据查询平台。',
    achievements: ['开发高性能 Rest 接口提供基础数据查询服务', '对接银联数据导入 Hadoop 大数据平台', '独立维护银行大规模 Solr 集群']
  },
  {
    company: '上海奔耀信息科技有限公司',
    role: 'Java 开发工程师',
    period: '2015.7 ~ 2016.2',
    summary: '从事文本分析程序开发和报表展示系统开发，主要为公司开发 POC。',
    achievements: ['对接文本分析系统数据导入搜索引擎', '根据甲方需求开发可视化报表', 'Solr 运维']
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

export const certifications = ['PMP 证书', 'Scrum Master', '2017 新蛋优秀员工']

export const techStack = {
  languages: ['Java', 'Scala', 'Python'],
  microservices: ['Spring Cloud', 'Spring Boot', 'Spring Framework', 'Spring Data', 'Eureka', 'Hystrix', 'Ribbon'],
  bigData: ['Hadoop', 'HBase', 'Hive', 'MapReduce', 'Spark', 'Flink', 'Kafka', 'Zookeeper', 'HDFS'],
  search: ['Solr', 'Elasticsearch', 'Lucene', 'Milvus'],
  dataAnalysis: ['Hive', 'ClickHouse'],
  ai: ['Langchain', 'ChatGPT', 'Embedding', '双塔模型', '大模型微调'],
  devops: ['Docker', 'K8S', 'Grafana', 'ELK']
}

export const lifeEntries = {
  cooking: [
    {
      title: '番茄牛腩',
      meta: '周末慢炖 / 酸甜汤底',
      note: '番茄分两次下锅：第一次熬汤底，第二次保留新鲜酸甜味。'
    },
    {
      title: '葱油拌面',
      meta: '工作日晚餐 / 15 分钟',
      note: '小火把葱香逼出来，酱油和糖的比例决定最后的层次。'
    }
  ],
  books: [
    {
      title: '系统设计相关读书笔记',
      meta: '架构 / 长期更新',
      note: '重点记录系统边界、演进路径和工程取舍，而不是只做摘抄。'
    },
    {
      title: 'AI 工程实践阅读',
      meta: 'LLM / Agent / Search',
      note: '关注模型能力如何进入真实业务，以及评估、成本和稳定性问题。'
    }
  ]
}
