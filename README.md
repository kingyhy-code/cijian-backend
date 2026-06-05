# 辞间 · 后端服务 (cijian-backend)

短篇叙事创作平台的 Spring Cloud 微服务后端，提供用户认证、作品管理、社交互动、数据统计、搜索和管理功能。AI 能力由独立的 Python Agent 服务（`D:/CijianAgent`）提供。

## 项目目标

- **用户系统** — 邮箱注册/登录，JWT 鉴权，Redis 会话管理，个人资料编辑
- **作品管理** — 短篇/灵感 CRUD，标签分类，专题聚合，灵感引用追踪
- **社交互动** — 点赞/评论/收藏/关注/划词批注，嵌套评论回复
- **通知系统** — 互动通知（点赞/评论/收藏/关注/私信），分类筛选，未读计数
- **私信系统** — 互相关注后私聊，会话管理，消息已读
- **AI 写作辅助** — 由 Python Agent 提供：创作教练（规则检测/评估/润色/帮写）、阅读伴侣（文学分析/导读/对话）、Agent 对话（自主工具调用/画像/出题/批改）
- **个人数据** — 用户画像统计、标签分布、收藏导出
- **异步消息** — RocketMQ 事件驱动（发布/互动/审核/搜索）
- **搜索** — Elasticsearch 全文检索，无 ES 时优雅降级
- **运营管理** — 内容审核、敏感词库、用户管理、名家管理

## 技术栈

| 层 | 技术 |
|---|---|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.2.5 + Spring Cloud 2023.0.2 + Spring Cloud Alibaba 2023.0.1.0 |
| 服务发现/配置 | Nacos 2.3.2 |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.3 |
| 缓存 | Redis 7 |
| 鉴权 | JJWT 0.12.5 + BCrypt |
| 消息队列 | RocketMQ 2.3.1 (Spring Boot Starter) |
| 搜索引擎 | Elasticsearch 8.13.2 (已配置，未实现) |
| 分布式事务 | Seata 1.8.0 (AT 模式，已配置) |
| 服务调用 | OpenFeign + Spring Cloud LoadBalancer |
| 工具库 | Hutool 5.8.27, Lombok |
| 构建 | Maven 3.9 + spring-boot-maven-plugin |
| 容器 | Docker + docker-compose |
| AI 服务 | 独立 Python Agent 服务（LangGraph + LangChain + FastAPI + SQLite，见 `D:/CijianAgent`） |

## 目录结构

```
D:\Cijian\
├── pom.xml                                    # 根 POM，依赖版本管理
├── docker-compose.yml                         # 开发环境：Redis + Nacos + RocketMQ + ES + Seata
├── Dump20260503.sql                           # 数据库初始化脚本（13 张表，无数据）
├── CLAUDE.md                                  # AI 辅助开发规范
├── cijian-common/                             # 公共模块
│   └── src/main/java/com/cijian/common/
│       ├── result/R.java                      # 统一响应包装
│       ├── enums/ResultCode.java              # 状态码枚举
│       ├── exception/BizException.java        # 业务异常
│       ├── exception/GlobalExceptionHandler.java  # 全局异常处理
│       ├── entity/BaseEntity.java             # MyBatis-Plus 基础实体
│       ├── config/MyMetaObjectHandler.java    # 自动填充时间戳
│       ├── page/PageParam.java, PageResult.java  # 分页 DTO
│       ├── utils/JwtUtil.java                 # JJWT 令牌生成/解析/验证
│       ├── utils/RedisUtil.java               # StringRedisTemplate 工具
│       ├── util/BeanCopyUtils.java            # Hutool Bean 拷贝
│       ├── mq/RocketMQTopics.java             # 6 个消息主题常量
│       └── feign/UserFeignClient.java         # 用户服务 Feign 接口
├── cijian-gateway/                            # API 网关
│   └── port 8080 | JWT 认证/路由转发/CORS
├── cijian-user/                               # 用户认证服务
│   └── port 8081 | 注册/登录/登出/JWT/资料编辑/密码修改
├── cijian-content/                            # 内容服务
│   └── port 8082 | 作品 CRUD/多 Feed 流/标签/专题/灵感引用/Feign 跨服务
├── cijian-interaction/                        # 互动服务
│   └── port 8083 | 点赞/评论(嵌套)/收藏/批注/关注/通知/私信 + 计数器同步
├── cijian-profile/                            # 用户画像服务
│   └── port 8084 | 统计聚合/标签分布/收藏导出
├── cijian-search/                             # 搜索服务
│   └── port 8087 | ES 全文检索 + 索引同步 + 容错降级
├── cijian-operation/                          # 运营服务
│   └── port 8086 | 看板/审核/敏感词/用户管理/名家管理
├── .claude/                                   # Claude Code 配置
└── logs/                                      # 各模块日志文件
```

## API 端点

所有 AI 端点已迁移到 Python Agent（`/api/agent/**`），Java 后端不再包含 AI 模块。

### 用户服务 (:8081)
| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/user/register` | 注册 | - |
| POST | `/user/login` | 登录，返回 JWT | - |
| GET | `/user/info` | 获取当前用户信息 | Bearer |
| PUT | `/user/info` | 修改昵称/简介/头像 | Bearer |
| POST | `/user/password` | 修改密码 | Bearer |
| POST | `/user/logout` | 登出（删 Redis 令牌） | Bearer |
| POST | `/user/forgot-password` | 发送 6 位验证码到邮箱 | - |
| POST | `/user/reset-password` | 输入验证码 + 新密码重置 | - |
| POST | `/user/token/refresh` | 旧 token 换新 token | Bearer |
| GET | `/user/{id}` | 获取用户公开信息 | - |

### 内容服务 (:8082)
| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/work` | 发布作品 | X-User-Id |
| PUT | `/work/{id}` | 编辑作品 | X-User-Id (作者) |
| DELETE | `/work/{id}` | 删除作品 | X-User-Id (作者) |
| GET | `/work/{id}` | 作品详情 | - |
| GET | `/work/list` | 作品列表 | - |
| GET | `/work/masterpiece` | 名家作品（支持 `?country=` 按国别筛选） | - |
| GET | `/work/inspiration` | 灵感广场 | - |
| GET | `/work/topic/{topicId}` | 专题聚合 | - |
| GET | `/work/tag?tagName=` | 标签聚合 | - |
| POST | `/work/{id}/view` | 浏览计数 | - |
| GET | `/work/count/by-author` | 作者作品数 | - |
| GET | `/work/tags/stat` | 作者标签统计 | - |

### 互动服务 (:8083)

**社交互动：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/like/toggle` | 点赞/取消 |
| GET | `/like/check` | 查点赞状态 |
| GET | `/comment/work/{workId}` | 评论列表（含嵌套回复） |
| POST | `/comment` | 发表评论 |
| PUT | `/comment` | 编辑评论 |
| DELETE | `/comment/{id}` | 删除评论 |
| POST | `/collection` | 收藏 |
| DELETE | `/collection/{id}` | 取消收藏 |
| GET | `/collection/list` | 收藏列表 |
| GET | `/collection/check` | 查收藏状态 |
| POST | `/annotation` | 添加批注 |
| PUT | `/annotation/{id}` | 编辑批注 |
| DELETE | `/annotation/{id}` | 删除批注 |
| GET | `/annotation/work/{workId}` | 作品批注列表 |
| POST | `/follow/toggle` | 关注/取消 |
| GET | `/follow/check` | 查关注状态 |
| GET | `/follow/following/{userId}` | 关注列表 |
| GET | `/follow/followers/{userId}` | 粉丝列表 |

**通知系统：**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/notification/list` | 通知列表（分页） |
| GET | `/notification/unread-count` | 未读通知数 |
| PUT | `/notification/{id}/read` | 标记单条已读 |

**私信系统：**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/message/conversations` | 会话列表 |
| GET | `/message/{conversationId}` | 聊天记录 |
| POST | `/message/send` | 发送私信（需互关） |
| PUT | `/message/{conversationId}/read` | 标记会话已读 |
| GET | `/message/unread-count` | 未读私信数 |

### 用户画像 (:8084)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/profile/stats/{userId}` | 用户统计（作品/获赞/金句/引用） |
| GET | `/profile/tags/{userId}` | 标签分布 |
| GET | `/profile/lianci/{userId}` | 炼词窗报告 |
| GET | `/profile/collections/export/{userId}` | 收藏导出（占位） |

### AI Agent 服务 (:8000)

全部 AI 能力由 Python Agent 提供，Gateway 路由 `/api/agent/**` → Agent `:8000`。

**Agent 对话与学习：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/agent/chat` | Agent 统一对话（LLM 自主决策，调用工具） |
| POST | `/api/agent/chat/stream` | SSE 流式对话 |
| GET | `/api/agent/history/{sessionId}` | 会话历史 |
| GET | `/api/agent/tasks/{userId}` | 学习任务列表 |
| POST | `/api/agent/plan/create` | 创建学习计划 |
| GET | `/api/agent/profile/{userId}` | 用户学习档案 |
| GET | `/api/agent/progress/scores/{userId}` | 五维能力分数历史 |
| GET | `/api/agent/progress/summary/{userId}` | 综合进度概览 |
| GET | `/api/agent/health` | 健康检查 |

**创作教练：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/agent/coach/analyze` | L1-L3 规则检测（纯正则，毫秒级） |
| POST | `/api/agent/coach/evaluate` | 五维度深度评估 |
| POST | `/api/agent/coach/polish` | 分级润色（light/medium/heavy） |
| POST | `/api/agent/coach/inspire` | 帮写（灵感激发/定向续写） |
| POST | `/api/agent/coach/chat` | 基于作品全文的教练对话 |

**阅读伴侣：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/agent/companion/analyze` | 文学分析（修辞+洞察+引导问题） |
| POST | `/api/agent/companion/overview` | 宏观导读（经典/普通作品区分） |
| POST | `/api/agent/companion/chat` | 选段对话 |
| POST | `/api/agent/companion/evaluate` | 检验点评 |
| POST | `/api/agent/companion/guide/chat` | 全程对话式导读 |

### 搜索服务 (:8087)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/search/works` | 全文搜索 |
| GET | `/api/search/suggest?prefix=` | 搜索建议 |
| GET | `/api/search/health` | ES 连接状态 + 索引创建 |
| POST | `/api/search/reindex` | 从内容服务全量同步索引 |

### 运营服务 (:8086)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/operation/dashboard/stats` | 看板统计 |
| GET | `/api/operation/review/pending` | 待审作品列表 |
| GET | `/api/operation/review?status=` | 全部作品（按状态筛选） |
| PUT | `/api/operation/review/{id}/approve` | 审核通过 |
| PUT | `/api/operation/review/{id}/reject` | 审核驳回 |
| DELETE | `/api/operation/review/{id}` | 软删除作品 |
| DELETE | `/api/operation/review/{id}/force` | **彻底删除**（物理删除，级联清理） |
| GET | `/api/operation/sensitive-word` | 敏感词分页列表 |
| POST | `/api/operation/sensitive-word` | 添加敏感词 |
| DELETE | `/api/operation/sensitive-word/{id}` | 删除 |
| POST | `/api/operation/sensitive-word/check` | 检测文本 |
| POST | `/api/operation/sensitive-word/filter` | 过滤替换 |
| GET | `/api/operation/users` | 用户列表 |
| PUT | `/api/operation/users/{id}/disable` | 禁用用户 |
| PUT | `/api/operation/users/{id}/enable` | 启用用户 |
| DELETE | `/api/operation/users/{id}` | 软删除用户 |
| DELETE | `/api/operation/users/{id}/force` | **彻底删除**（物理删除，级联清理作品/评论等） |
| GET | `/api/operation/masterpiece` | 名家作品列表（支持 `?country=` 筛选） |
| POST | `/api/operation/masterpiece` | 添加名家作品（含 `country`/`tagNames`/`dynasty`） |
| PUT | `/api/operation/masterpiece/{id}` | 编辑（支持更新 `country`/`tagNames`） |
| DELETE | `/api/operation/masterpiece/{id}` | 软删除 |
| DELETE | `/api/operation/masterpiece/{id}/force` | **彻底删除**（物理删除，级联清理） |
| GET | `/api/operation/masterpiece/countries` | 已用国别列表（供下拉选择） |
| GET | `/api/operation/tags` | 标签列表（分页，按使用次数排序） |
| POST | `/api/operation/tags` | 新增标签 |
| PUT | `/api/operation/tags/{id}` | 编辑标签名 |
| DELETE | `/api/operation/tags/{id}` | 删除标签 |

## 数据库

15 张表（不含 Agent 的 SQLite），均使用 InnoDB + utf8mb4_unicode_ci + 逻辑删除模式：

| 表 | 所属模块 | 简述 |
|---|---|---|
| `user` | cijian-user | 用户账号（email/password/nickname/status/role） |
| `work` | cijian-content | 作品（title/content/status/country/5 个计数器） |
| `tag` | cijian-content | 标签（name 唯一，use_count） |
| `work_tag_rel` | cijian-content | 作品-标签关联 |
| `topic` | cijian-content | 写作话题 |
| `inspiration_ref` | cijian-content | 灵感引用链 |
| `comment` | cijian-interaction | 评论（parent_id 嵌套） |
| `like` | cijian-interaction | 多目标点赞（work/comment/sentence） |
| `collection` | cijian-interaction | 多类型收藏 |
| `annotation` | cijian-interaction | 划词批注 |
| `follow` | cijian-interaction | 关注关系 |
| `notification` | cijian-interaction | 互动通知（类型/目标/已读） |
| `conversation` | cijian-interaction | 私信会话（user1/user2 唯一） |
| `message` | cijian-interaction | 私信消息（会话/发送者/接收者/已读） |
| `sensitive_word` | cijian-operation | 敏感词库 |

> `lianci_log` 表已随 `cijian-ai` 模块移除，炼词记录现在由 Agent 的 SQLite 管理。

## 当前进度

### 已完成

- [x] **用户服务** — 注册/登录/登出，BCrypt 加密，JWT + Redis 会话
- [x] **内容服务** — 作品 CRUD，多 Feed 流，标签/专题/灵感引用，浏览计数
- [x] **互动服务** — 点赞/评论(嵌套)/收藏/批注/关注，计数器跨服务同步
- [x] **通知系统** — 点赞/评论/收藏/关注/私信自动通知，分类筛选，未读计数
- [x] **私信系统** — 互关后私聊，会话管理，消息已读
- [x] **用户画像** — 作品统计/标签分布/收藏导出框架
- [x] **AI Agent 服务** — Python LangGraph Agent（规则检测/评估/润色/帮写/文学分析/导读/对话/画像/出题/批改，7 个工具，LLM 自主决策）
- [x] **搜索服务** — ES 全文检索，索引同步，无 ES 时优雅降级
- [x] **运营服务** — 数据看板、内容审核、敏感词库、用户管理、名家管理
- [x] **公共模块** — R 响应包装/JWT/Redis/BeanCopy/分页/异常处理
- [x] **网关** — 6 个下游服务 + Agent 路由，统一认证，StripPrefix
- [x] **数据库** — 15 张表
- [x] **Docker** — docker-compose 全栈中间件（Redis/Nacos/MySQL/RocketMQ/ES/Seata）
- [x] **网关认证** — JWT 校验 + X-User-Id 注入统一到网关 WebFilter
- [x] **API 文档** — 各模块集成 SpringDoc OpenAPI + Swagger UI
- [x] **测试覆盖** — JwtUtil + UserService 单元测试（17 用例）
- [x] **Seata 分布式事务** — DataSourceProxy 已配置，@GlobalTransactional 已接入核心跨服务方法
- [x] **JWT 续期** — `/user/token/refresh` 端点
- [x] **密码重置** — 6 位数字验证码，Redis 30 分钟有效
- [x] **邮箱注册校验** — 注册时 MX 记录查询 + 60+ 一次性邮箱域名拦截
- [x] **邮件服务** — 原生 SMTP 实现，已接入 QQ 邮箱
- [x] **网关 Swagger 聚合** — 微服务 API 文档聚合到网关统一入口
- [x] **ES 索引初始化** — 启动时自动创建 `cijian_works` 索引
- [x] **经典作品国别分类** — `/work/masterpiece` 支持 `?country=` 筛选
- [x] **后台标签管理** — 独立标签 CRUD，经典作品上传支持 `country`/`tagNames`
- [x] **物理删除** — 后台 `/{id}/force` 端点支持彻底删除


---

## 本地启动

详见 `STARTUP.md`。

### 端口表

| 服务 | 端口 | 说明 |
|------|------|------|
| gateway | 8080 | API 网关 |
| user | 8081 | 用户服务 |
| content | 8082 | 内容服务 |
| interaction | 8083 | 互动服务 |
| profile | 8084 | 画像服务 |
| operation | 8086 | 运营服务 |
| search | 8087 | 搜索服务 |
| Agent | 8000 | Python AI 服务（独立项目） |
| 前端 | 5173 | Vite 开发服务器 |

### 前置依赖

- **MySQL**: 本地 3306 端口，root/1230，数据库 `cijian`
- **Nacos**: Docker 8848 端口
- **Redis**: Docker 6379 端口
- **RocketMQ**: Docker 9876/10911 端口
- **Elasticsearch**: Docker 9200 端口
- **Python Agent**: `D:/CijianAgent`，需配置 `.env` 中的 `AI_API_KEY`

---

*最后更新：2026-05-30 · 7 个 Java 模块 + 1 个 Python Agent · 全栈可运行*
