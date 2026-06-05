# 阅读伴侣：全程对话式导读

## 问题

宏观导读（`/api/companion/overview`）一次性生成全部内容，前端状态机 `overview → waiting_read → quiz → evaluate → done` 锁死了流程，用户看到导读中不懂的概念（如"欧亨利式结尾"）时无法插话提问。

## 目标

将整个阅读伴侣流程变成**全程对话式**：LLM 控流程，用户在导读/提问/评估/引导提问的任何阶段都能自由插话。

## 设计

### 后端：新统一对话端点

**`POST /api/companion/guide/chat`**

```
Request:
  title: string          // 作品标题
  author: string         // 作者
  is_classic: boolean    // 是否经典名作
  work_content: string   // 作品全文（对话上下文）
  message: string        // 用户当前消息（首轮为空则触发导读）
  history: {role, content}[]  // 对话历史

Response:
  reply: string  // AI 回复文本
```

System Prompt 规定四个阶段，LLM 自主推进：

1. **导读** — 分段输出剧情梗概、创作背景、技法、情感基调。每段简短，讲完后问读者想法。讲完全部内容后确认用户是否读完。
2. **出题检验** — 逐题提问，每次1题，等读者回答后点评+出下一题。
3. **点评总结** — 对整体表现简短点评。
4. **引导提问** — 从创作角度提问激发思考。

核心规则：
- 用户可随时插话提问（如"XXX 是什么意思？"），先答问再自然回到流程
- AI 通过理解用户意图判断是否进入下一阶段（如"读完了""看完了"），不靠关键词硬匹配
- 不出现"阶段1""阶段2"字样，自然过渡
- 语气亲切，回复短小精悍

**实现位置：** `app/agents/companion_agent.py` 新增 `guide_chat()` 方法，`app/main.py` 新增路由。

### 前端：去除状态机

**改造 `WorkDetail.vue`：**

- 删除：`step`、`quizData`、`inspData`、`quizIndex`、`userAnswers` 等状态变量
- 删除：`genOverview()`、`sendSbMessage()` 中的 step 分支逻辑
- `sendSbMessage()` 简化为：拼接历史 → 调 `guideChatCompanion()` → 追加回复
- 首条消息由新逻辑自动触发（打开侧边栏时发送空 message）
- `inputPlaceholder` 固定为 `"输入消息..."`

### 改动清单

| 层 | 文件 | 改动 |
|---|---|---|
| Python agent | `companion_agent.py` | 新增 `guide_chat()` + `GUIDE_CHAT_PROMPT` |
| Python route | `main.py` | 新增 `POST /api/companion/guide/chat` + `GuideChatRequest`/`GuideChatResponse` schema |
| Python schema | `schemas.py` | 新增 request/response model |
| Vue API | `ai.ts` | 新增 `guideChatCompanion()` |
| Vue view | `WorkDetail.vue` | 删除状态机 ~40 行，简化消息处理 ~30 行 |

### 不保留兼容

旧 `overview` + `evaluate` 组合不再需要，但不删除——`overview` 端点保留给可能的独立使用场景，前端只不再调用。

### 边界情况

- **网络中断**：前端 catch 异常，显示错误提示，不丢失对话历史
- **LLM 回复非预期格式**：不做格式校验，直接把 reply 文本渲染给用户
- **超长作品**：作品全文超过 token 限制时，截取前 N 字符（后续可考虑分段发送）
- **空消息首轮**：后端检测 message 为空 + history 为空时，自动触发导读
