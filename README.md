# 辞间 · 后端服务

短篇叙事创作 AI 教学平台后端，Spring Cloud Alibaba 微服务架构。7 个 Java 服务 + 1 个 Python AI Agent，提供用户认证、作品管理、社交互动、全文搜索、运营管理等能力。

## 技术栈

Java 17 · Spring Boot 3.2 · Spring Cloud 2023.0 · Spring Cloud Alibaba 2023.0 · MyBatis-Plus 3.5 · MySQL 8 · Redis 7 · RocketMQ 5 · Elasticsearch 8 · Nacos 2.3 · Seata 1.8 · Docker

## 架构

```
Gateway (:8080) — 统一入口，JWT 鉴权，路由转发
  ├── /user/**          → cijian-user (:8081)       # 用户认证
  ├── /work/**          → cijian-content (:8082)     # 作品管理
  ├── /like/** /comment/** /follow/** ...
  │                      → cijian-interaction (:8083) # 社交互动
  ├── /profile/**       → cijian-profile (:8084)     # 用户画像
  ├── /api/operation/** → cijian-operation (:8086)   # 运营管理
  ├── /api/search/**    → cijian-search (:8087)      # 全文检索
  └── /api/agent/**     → cijian-agent (:8000)       # AI 服务 (Python)
```

Nacos 服务注册发现 + OpenFeign 跨服务调用 + RocketMQ 异步解耦 + Seata AT 分布式事务。

## 功能

**用户系统** — 邮箱注册/登录，BCrypt 加密，JWT + Redis 会话，Token 自动续期，密码重置（6 位验证码），60+ 一次性邮箱拦截

**作品管理** — 短篇/灵感 CRUD，多 Feed 流（最新/最热/关注/名家/话题/标签），标签分类，专题聚合，灵感引用追踪，时间衰减排序算法

**社交互动** — 多目标点赞（作品/句子/评论），嵌套评论回复，多类型收藏，划词批注，关注/取关，计数器跨服务同步

**通知私信** — 互动通知自动生成 + 分类筛选 + 未读计数，互关后私聊，会话管理，消息已读

**全文搜索** — Elasticsearch 全文检索，启动全量同步 + RocketMQ 增量更新，reindex 手动重建，ES 不可用降级

**运营管理** — 数据看板，内容审核，敏感词库，用户管理（禁用/启用/删除），名家作品管理（按国别筛选），标签管理

## API 端点

### 用户服务 (:8081)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /user/register | 注册 |
| POST | /user/login | 登录，返回 JWT |
| GET | /user/info | 获取当前用户信息 |
| POST | /user/token/refresh | Token 续期 |
| POST | /user/forgot-password | 发送验证码 |
| POST | /user/reset-password | 重置密码 |

### 内容服务 (:8082)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /work | 发布作品 |
| PUT | /work/{id} | 编辑作品 |
| DELETE | /work/{id} | 删除作品 |
| GET | /work/{id} | 作品详情 |
| GET | /work/list | 作品列表（支持 sortBy=NEWEST/HOTTEST） |
| GET | /work/masterpiece | 名家作品（支持 ?country= 筛选） |
| GET | /work/inspiration | 灵感广场 |
| GET | /work/tag?tagName= | 标签聚合 |
| GET | /work/tags/list?q= | 标签搜索（自动补全用） |

### 互动服务 (:8083)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /like/toggle | 点赞/取消 |
| GET | /like/check | 查询点赞状态 |
| GET | /comment/work/{workId} | 评论列表（含嵌套） |
| POST | /comment | 发表评论 |
| POST | /collection | 收藏 |
| POST | /follow/toggle | 关注/取消 |
| GET | /notification/list | 通知列表 |
| GET | /message/conversations | 会话列表 |
| POST | /message/send | 发送私信（需互关） |

### 搜索服务 (:8087)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/search/works?keyword=&tag= | 全文搜索 |
| GET | /api/search/suggest?prefix= | 搜索建议 |
| POST | /api/search/reindex | 从内容服务全量重建索引 |

## 数据库

15 张 MySQL 表，InnoDB + utf8mb4 + MyBatis-Plus 逻辑删除：

| 表 | 模块 | 说明 |
|---|---|---|
| user | cijian-user | 用户账号 |
| work | cijian-content | 作品（含 5 个计数器/灵感引用） |
| tag | cijian-content | 标签（全局共享，use_count） |
| work_tag_rel | cijian-content | 作品-标签关联 |
| topic | cijian-content | 写作话题（含 work_count） |
| comment | cijian-interaction | 评论（parent_id 嵌套） |
| like | cijian-interaction | 多目标点赞 |
| collection | cijian-interaction | 多类型收藏 |
| annotation | cijian-interaction | 划词批注 |
| follow | cijian-interaction | 关注关系 |
| notification | cijian-interaction | 互动通知 |
| conversation | cijian-interaction | 私信会话 |
| message | cijian-interaction | 私信消息 |
| sensitive_word | cijian-operation | 敏感词库 |
| lianci_log | cijian-profile | 炼词记录 |

## 快速启动

```bash
# 1. 启动基础设施
docker-compose up -d

# 2. 构建
mvn clean install -DskipTests

# 3. 一键启动
.\start.ps1
```

访问 http://localhost:5173（前端）或 http://localhost:8080（网关）。

## 目录

```
├── pom.xml                    # 根 POM，依赖版本管理
├── docker-compose.yml         # 基础设施（Redis/Nacos/MySQL/RocketMQ/ES/Seata）
├── start.ps1                  # 一键启动脚本
├── cijian-common/             # 公共模块（R响应/JWT/工具/异常/分页/Feign）
├── cijian-gateway/            # API 网关（JWT 鉴权 + 路由转发）
├── cijian-user/               # 用户服务
├── cijian-content/            # 内容服务
├── cijian-interaction/        # 互动服务
├── cijian-profile/            # 画像服务
├── cijian-operation/          # 运营服务
└── cijian-search/             # 搜索服务
```
