# 辞间 · 后端服务

短篇叙事创作 AI 教学平台后端，Spring Cloud Alibaba 微服务架构。7 个 Java 服务 + 1 个 Python AI Agent，提供用户认证、作品管理、社交互动、全文搜索、运营管理等能力。

## 技术栈

Java 17 · Spring Boot 3.2 · Spring Cloud 2023.0 · Spring Cloud Alibaba 2023.0 · MyBatis-Plus 3.5 · MySQL 8 · Redis 7 · RocketMQ 5 · Elasticsearch 8 · Nacos 2.3 · Seata 1.8 · Docker

## 模块

| 模块 | 端口 | 说明 |
|------|------|------|
| cijian-common | — | 公共类库（实体/工具/异常/分页/JWT） |
| cijian-gateway | 8080 | API 网关（JWT 鉴权 + 路由转发） |
| cijian-user | 8081 | 用户认证与账号管理 |
| cijian-content | 8082 | 作品管理与发布 |
| cijian-interaction | 8083 | 社交互动（点赞/评论/收藏/私信/通知） |
| cijian-profile | 8084 | 用户画像与统计 |
| cijian-operation | 8086 | 运营管理（审核/敏感词/文件上传） |
| cijian-search | 8087 | 全文检索（Elasticsearch） |

AI 能力由独立 Python Agent 服务提供（见 `cijian-agent` 仓库）。

## 快速启动

```bash
# 1. 启动基础设施
docker-compose up -d

# 2. 构建
mvn clean install -DskipTests

# 3. 启动全部服务
.\start.ps1
```

访问 http://localhost:5173（前端）或 http://localhost:8080（网关）。

## 数据库

15 张 MySQL 表：`user`、`work`、`comment`、`like`、`collection`、`annotation`、`follow`、`notification`、`conversation`、`message`、`tag`、`work_tag_rel`、`topic`、`sensitive_word`、`lianci_log`

均使用 InnoDB + utf8mb4 + MyBatis-Plus 逻辑删除。

## 环境变量

生产环境通过环境变量覆盖默认配置：

| 变量 | 说明 |
|------|------|
| `MYSQL_URL` | 数据库连接 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 数据库凭证 |
| `NACOS_ADDR` | Nacos 地址 |
| `REDIS_HOST` | Redis 地址 |
| `ES_HOST` | Elasticsearch 地址 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | 邮箱 SMTP 凭证 |

## 目录结构

```
├── pom.xml                    # 根 POM
├── docker-compose.yml         # 开发环境中间件
├── start.ps1                  # 一键启动脚本
├── cijian-common/             # 公共模块
├── cijian-gateway/            # API 网关
├── cijian-user/               # 用户服务
├── cijian-content/            # 内容服务
├── cijian-interaction/        # 互动服务
├── cijian-profile/            # 画像服务
├── cijian-operation/          # 运营服务
└── cijian-search/             # 搜索服务
```
