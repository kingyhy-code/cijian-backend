# 创作教练Agent完善

## 现状

创作教练由 `WritingCoachAgent` 提供三个独立端点：`inspire`（灵感/续写）、`polish`（润色）、`evaluate`（评估）。三者单轮、无上下文、碎片化。前端（WorkEditor）通过右键菜单、评估面板按钮调用。

## 目标

1. 优化三个现有端点——不改前端交互方式，提升 prompt 质量和参数粒度
2. 新增自由对话端点——编辑器侧边栏 AI 对话框，LLM 基于作品全文回答问题

## 设计

### 1. inspire — 加 context + mode

**端点：** `POST /api/coach/inspire`

```python
class InspireRequest:
    input: str           # 关键词或指令
    context: str = ""    # 当前编辑器中的作品片段（可选）
    mode: str = ""       # "inspire"（发散灵感）| "generate"（定向续写）| ""（自动判断）
```

- `context` 传入后，Prompt 要求 AI 的生成结果自然衔接到作品上下文中
- `mode` 传 "inspire" 时强制发散 3-5 个角度，传 "generate" 时直接续写
- 不传 mode 时保持原有自动判断逻辑

### 2. polish — 加 level 分级

**端点：** `POST /api/coach/polish`

```python
class PolishRequest:
    text: str
    style: str = ""
    reference: str = ""
    level: str = "medium"  # "light" | "medium" | "heavy"
```

分级 Prompt 行为：

| level | 做的事 | 不改的事 |
|-------|--------|----------|
| light | 错别字、的得地、标点、基础语法 | 用词、句式、风格 |
| medium | light + 优化用词精准度、调整句式节奏、去水词/冗余 | 整体风格 |
| heavy | medium + 按目标风格重写（海明威/古风/悬疑等） | 核心意思和情节 |

### 3. evaluate — 扩维度 + 可操作建议

**端点：** `POST /api/coach/evaluate`（路径和响应类型不变）

维度从 3 扩到 5：

| 维度 | 说明 |
|------|------|
| 逻辑连贯性 | 叙事是否通顺，逻辑自洽，段落衔接 |
| 词汇精准度 | 用词是否准确、具体、生动（原"词汇丰富度"改为精准优先） |
| 情感感染力 | 文字的情绪冲击力、画面感、共鸣度 |
| 结构节奏 | 段落长短变化、句式节奏、详略分布 |
| 表达简洁性 | 有无冗余修饰、绕圈话、可删的废话 |

每个维度：
- score: 0-100
- comment: 简短评语（20字）
- suggestion: 具体可操作的改进建议（新增字段，50字内）

### 4. 新增 chat — 自由对话

**端点：** `POST /api/coach/chat`

```python
class CoachChatRequest:
    work_content: str = ""    # 编辑器当前全文
    message: str              # 用户消息
    history: list[{role, content}] = []  # 对话历史

class CoachChatResponse:
    reply: str
```

**定位：** 知道作品全文的写作教练，不是万能聊天机器人。

**System Prompt 核心定义：**

```
你是专业写作教练，正在帮作者打磨一篇作品。你已经读过作品全文。

## 你能做的事
- 结构诊断："开头会不会太拖？""转折突兀吗？"
- 风格建议："怎么增加张力？""离鲁迅那种冷峻感差在哪？"
- 局部打磨："这句帮我改""结尾太仓促了"
- 思路突破："写到这卡住了，给几个走向"
- 创作决策："第三人称会不会比第一人称好？"

## 规则
- 始终基于作品全文作答，引用原文具体句子
- 每次给具体建议，不说"建议多读多写"之类的空话
- 局部问题直接给出修改方案（改写后文字）
- 回复短小精准，200字以内
- 语气像编辑改稿——直接、有建设性、不敷衍
- 如果用户问的事超出写作教练范围（如文学史知识），简短指路后回到作品
```

## 改动清单

| 层 | 文件 | 改动 |
|---|---|---|
| Python agent | `writing_coach_agent.py` | 改 INSPIRE_PROMPT/POLISH_PROMPT/EVALUATE_PROMPT；新增 COACH_CHAT_PROMPT + chat() |
| Python schema | `schemas.py` | 改 InspireRequest/PolishRequest；EvaluateDimension 加 suggestion；新增 CoachChatRequest/CoachChatResponse/CoachChatMessage |
| Python route | `main.py` | 改 inspire/polish 端点参数；新增 /api/coach/chat |
| Java DTO | `CompanionDto.java`→`CoachDto.java` | 改 InspireRequest/PolishRequest；新增 CoachChatRequest/Response |
| Java Client | `AiServiceClient.java` | invoke/continue/chat 参数调整；新增 coachChat() |
| Java Service | `CoachService.java` | 参数调整；新增 chat() |
| Java Controller | `CoachController.java` | 端点参数调整；新增 /coach/chat |
| 前端 API | `ai.ts` | 改 inspireCoach/polishCoach 参数；新增 coachChat() |
| 前端 View | `WorkEditor.vue` | 新增侧边栏 AI 对话框组件 |
