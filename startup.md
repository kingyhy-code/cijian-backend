# 辞间全栈启动指南

## 前置条件

```
Java:   C:\Users\26056\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\java
Maven:  C:\Program Files\apache-maven-3.9.9
Node:   C:\Program Files\nodejs\node
Python: C:\Users\26056\AppData\Local\Programs\Python\Python313\python
MySQL:  localhost:3306 (cijian 库)
```

## 架构

```
前端 :5173 (Vite)
  └→ Gateway :8080 (Spring Cloud Gateway + Nacos :8848)
       ├→ /api/agent/**       → Python Agent :8000
       ├→ /api/content/**     → cijian-content :8082
       ├→ /api/user/**        → cijian-user :8081
       ├→ /api/interaction/** → cijian-interaction :8083
       ├→ /api/profile/**     → cijian-profile :8084
       ├→ /api/search/**      → cijian-search :8087
       └→ /api/operation/**   → cijian-operation :8086
```

基础设施: Redis / Nacos / RocketMQ / Elasticsearch / Seata（均为 Docker 容器）

## 一键启动

**以管理员身份打开 PowerShell，运行：**

```powershell
.\start.ps1
```

脚本会按顺序：检查前置条件 → 启动 Docker → 逐个启动 Java 服务并验证端口 → 等 Nacos 注册 → 启动 Gateway → 启动 Agent 和前端 → 打印状态汇总。

每个服务启动后都会**轮询端口直到就绪**，如果超时会有明确提示。

## 查看状态

```powershell
.\start.ps1 -Status
```

## Maven 构建

修改 Java 代码后重新构建：

```powershell
cd D:\Cijian
mvn clean install -DskipTests -q
```

**注意：** `clean` 会删除 target 目录，如果服务正在运行会失败。先停掉所有 Java 进程再构建，或去掉 `clean` 只做增量编译。

## 常见问题

### 某个服务启动失败（start.ps1 报超时）

```
Get-Content D:\Cijian\logs\<服务名>.log -Tail 20
```

### 书库/图片 404

JVM 工作目录不是 `D:\Cijian`。`start.ps1` 已用 `-WorkingDirectory $BASE` 处理。

### Agent 返回"未配置 API Key"

检查 `D:/CijianAgent/.env` 中 `AI_API_KEY` 是否配置。

### 前端 API 请求 500

检查 `D:\Cijian-frontend\.env` 是否存在。`start.ps1` 会自动创建。

### Maven 构建失败

常见原因：Docker 没启动（某些服务依赖 Docker 内的 Nacos 配置）、本地 Maven 仓库缓存损坏。先跑 `docker ps` 确认容器都在。

---
*最后更新：2026-05-30*
