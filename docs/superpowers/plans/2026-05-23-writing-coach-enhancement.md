# 创作教练 Agent 完善 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 优化 inspire/polish/evaluate 三个端点，新增 /api/coach/chat 自由对话端点，前端编辑器新增 AI 对话框。

**Architecture:** 修改 Python writing_coach_agent.py 的 4 个 Prompt 和方法签名，Java 层和前端对应调整参数。chat 端点复用 companion guide chat 的对话式架构（System Prompt + 历史 + 作品全文上下文）。

**Tech Stack:** Python FastAPI + LangChain, Java Spring Boot, Vue 3 + TypeScript

---

## 文件变更清单

| 层 | 文件 | 操作 |
|---|---|---|
| Python | `D:\CijianAgent\app\agents\writing_coach_agent.py` | 改 3 个 prompt + 3 个方法签名；新增 chat prompt + chat() |
| Python | `D:\CijianAgent\app\models\schemas.py` | 扩 InspireRequest/PolishRequest/EvaluateDimension；新增 CoachChat 模型 |
| Python | `D:\CijianAgent\app\main.py` | 改 inspire/polish 端点；新增 /api/coach/chat；更新 import |
| Java | `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\dto\CoachDto.java` | 扩 InspireRequest/PolishRequest/EvaluateDimension；新增 CoachChat 类 |
| Java | `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\client\AiServiceClient.java` | 改 coachInspire/coachPolish；新增 coachChat() |
| Java | `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\service\CoachService.java` | 改 inspire/polish 方法签名；新增 chat() |
| Java | `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\controller\CoachController.java` | 改端点参数；新增 /coach/chat |
| Frontend | `D:\Cijian-frontend\src\api\ai.ts` | 改 inspireCoach/polishCoach；新增 coachChat() |
| Frontend | `D:\Cijian-frontend\src\views\WorkEditor.vue` | 新增侧边栏 AI 对话框 + 重构右键菜单逻辑 |

---

### Task 1: Python schemas — 扩模型

**Files:** Modify `D:\CijianAgent\app\models\schemas.py`

- [ ] **Step 1: 扩 InspireRequest — 加 context + mode**

找到 `class InspireRequest`（约第 36 行），替换为：

```python
class InspireRequest(BaseModel):
    input: str = Field(..., min_length=1, description="用户输入（关键词或写作指令）")
    context: str = Field(default="", description="当前编辑器中的作品片段，续写时用于上下文衔接")
    mode: str = Field(default="", description="inspire（发散灵感）| generate（定向续写）| 空（自动判断）")
```

- [ ] **Step 2: 扩 PolishRequest — 加 level**

找到 `class PolishRequest`（约第 49 行），替换为：

```python
class PolishRequest(BaseModel):
    text: str = Field(..., min_length=1, description="待修正的文本")
    style: str = Field(default="", description="目标风格，如：古风、悬疑、海明威、张爱玲等")
    reference: str = Field(default="", description="参考范文（Few-Shot，可选）")
    level: str = Field(default="medium", description="润色级别：light（仅语法错字）| medium（+用词句式）| heavy（+风格重写）")
```

- [ ] **Step 3: 扩 EvaluateDimension — 加 suggestion**

找到 `class EvaluateDimension`（约第 64 行），替换为：

```python
class EvaluateDimension(BaseModel):
    name: str = Field(description="维度名")
    score: int = Field(description="0-100 分")
    comment: str = Field(description="简短评语（20字）")
    suggestion: str = Field(default="", description="具体可操作的改进建议（50字内）")
```

- [ ] **Step 4: 新增 CoachChat 模型**

在 `EvaluateResponse` 之后、`# ── 阅读伴侣` 之前插入：

```python
# ── 创作教练自由对话 ──────────────────────────────────────────

class CoachChatMessage(BaseModel):
    role: str = Field(description="user 或 assistant")
    content: str = Field(description="消息内容")


class CoachChatRequest(BaseModel):
    work_content: str = Field(default="", description="编辑器当前全文")
    message: str = Field(..., min_length=1, description="用户消息")
    history: list[CoachChatMessage] = Field(default_factory=list, description="对话历史")


class CoachChatResponse(BaseModel):
    reply: str = Field(default="", description="AI 回复")
```

- [ ] **Step 5: 验证语法**

```bash
cd D:\CijianAgent && C:/Users/26056/AppData/Local/Programs/Python/Python313/python -c "from app.models.schemas import InspireRequest, PolishRequest, EvaluateDimension, CoachChatRequest, CoachChatResponse; print('OK')"
```

Expected: `OK`

---

### Task 2: Python agent — 改 3 个 Prompt + 方法 + 新增 chat

**Files:** Modify `D:\CijianAgent\app\agents\writing_coach_agent.py`

- [ ] **Step 1: 替换 INSPIRE_PROMPT**

找到 `INSPIRE_PROMPT`（第 16-38 行），替换整个常量：

```python
INSPIRE_PROMPT = """\
你是一位专业的创作教练，擅长帮写作者激发灵感和生成文本。

{context_section}

## 用户输入
{input}

## 模式指示
{mode_instruction}

按 JSON 格式输出（不要包含其他文字）：
{{
  "mode": "inspire 或 generate",
  "results": [
    {{"angle": "角度标题", "content": "具体内容"}}
  ]
}}
注意：inspire模式返回3-5条，generate模式返回1条。续写时确保结果能自然衔接到已有文字后面。"""
```

- [ ] **Step 2: 替换 POLISH_PROMPT**

找到 `POLISH_PROMPT`（第 40-63 行），替换整个常量：

```python
POLISH_PROMPT = """\
你是一位资深文学编辑，擅长文本润色、风格迁移和去水词。

## 待修正文本
{text}

## 润色级别：{level_desc}
{level_requirements}

## 目标风格
{style_desc}

## 参考范文
{reference_desc}

## 要求
{level_requirements}

按 JSON 格式输出：
{{
  "polished": "修正润色后的完整文本",
  "changes": "简要说明做了哪些修改（50字以内）"
}}"""
```

- [ ] **Step 3: 替换 EVALUATE_PROMPT**

找到 `EVALUATE_PROMPT`（第 65-88 行），替换整个常量：

```python
EVALUATE_PROMPT = """\
你是一位严格的文学评论家，需要从五个维度评估文本质量。

## 待评估文本
{text}

## 评估维度（每项0-100分）

1. **逻辑连贯性**：叙事是否通顺，逻辑自洽，段落衔接自然
2. **词汇精准度**：用词是否准确、具体、生动，有无贫乏或用词不当
3. **情感感染力**：文字的情绪冲击力、画面感、共鸣度
4. **结构节奏**：段落长短变化、句式节奏、详略分布是否得当
5. **表达简洁性**：有无冗余修饰、绕圈话、可删的废话

请在每个维度打分后，给出具体的可操作建议（而非泛泛而谈）。

按 JSON 格式输出：
{{
  "dimensions": [
    {{"name": "逻辑连贯性", "score": 85, "comment": "简短评语", "suggestion": "具体改进建议"}},
    {{"name": "词汇精准度", "score": 72, "comment": "简短评语", "suggestion": "具体改进建议"}},
    {{"name": "情感感染力", "score": 68, "comment": "简短评语", "suggestion": "具体改进建议"}},
    {{"name": "结构节奏", "score": 70, "comment": "简短评语", "suggestion": "具体改进建议"}},
    {{"name": "表达简洁性", "score": 80, "comment": "简短评语", "suggestion": "具体改进建议"}}
  ],
  "highlight": "核心亮点（一句话）",
  "improvement": "最大改进空间（一句话）"
}}"""
```

- [ ] **Step 4: 新增 COACH_CHAT_PROMPT**

在 `EVALUATE_PROMPT` 之后、`class WritingCoachAgent` 之前插入：

```python
COACH_CHAT_PROMPT = """\
你是专业写作教练，正在帮作者打磨一篇作品。你已经读过作品全文。

## 作品全文
{work_content}

## 你能做的事
- 结构诊断：分析段落衔接、节奏、详略分布
- 风格建议：指出风格特点或偏差，给出调整方向
- 局部打磨：对指定句子给出具体修改方案（直接输出改写后文字）
- 思路突破：给几个往下发展的可能
- 创作决策：讨论人称、视角、时态等选择

## 规则
- 始终基于作品全文作答，引用原文具体句子
- 每次给具体建议，不说"建议多读多写"之类的空话
- 局部问题直接给出修改方案
- 回复短小精准，200字以内
- 语气像编辑改稿——直接、建设性、不敷衍
- 如果用户问的事超出写作教练范围（如文学史知识），简短指路后回到作品"""
```

- [ ] **Step 5: 改 import — 加 CoachChatResponse**

找到文件顶部 import（第 7-11 行），替换为：

```python
from app.models.schemas import (
    InspireResponse, InspireItem,
    PolishResponse,
    EvaluateResponse, EvaluateDimension,
    CoachChatResponse,
)
```

- [ ] **Step 6: 改 inspire() 方法**

找到 `inspire()` 方法（第 97 行），替换整个方法：

```python
    def inspire(self, user_input: str, context: str = "", mode: str = "") -> InspireResponse:
        """根据用户输入激发灵感或直接生成文本。"""
        context_section = f"## 作品上下文（续写时请自然衔接到此文字之后）\n```\n{context}\n```" if context else ""
        if mode == "inspire":
            mode_instruction = "请按「灵感激发模式」：围绕用户的关键词，从3-5个不同角度展开情节构思或场景意象。每个角度给标题和描述。"
        elif mode == "generate":
            mode_instruction = "请按「定向生成模式」：直接输出一段完整文字。如有上下文，确保自然衔接。"
        else:
            mode_instruction = "请判断用户意图：零散关键词→灵感激发、明确写作指令→定向生成。"
        prompt = INSPIRE_PROMPT.format(input=user_input, context_section=context_section, mode_instruction=mode_instruction)
        raw = self._llm.chat(prompt, user_input, temperature=0.1, json_mode=True)
        if not raw:
            return InspireResponse(mode="inspire", results=[])
        data = extract_json(raw)
        if not isinstance(data, dict):
            return InspireResponse(mode="inspire", results=[])
        results = [
            InspireItem(angle=item.get("angle", ""), content=item.get("content", ""))
            for item in data.get("results", []) if isinstance(item, dict)
        ]
        return InspireResponse(mode=data.get("mode", "inspire"), results=results)
```

- [ ] **Step 7: 改 polish() 方法**

找到 `polish()` 方法（第 112 行），替换整个方法：

```python
    def polish(self, text: str, style: str = "", reference: str = "", level: str = "medium") -> PolishResponse:
        """修正润色：分级处理——light(仅语法) / medium(+用词句式) / heavy(+风格重写)。"""
        level_map = {
            "light": ("轻修", "仅修复错别字、的得地、标点、基础语法错误。不要改动用词和句式的原有风味。"),
            "medium": ("润色", "修复语法错误，优化用词精准度，调整句式节奏，去除冗余水词。保持原文风格不走样。"),
            "heavy": ("重写", "执行全部修正，并按目标风格深度重写。深入分析参考范文的用词、句式、修辞和情感基调，做到"神似"。"),
        }
        level_desc, level_requirements = level_map.get(level, level_map["medium"])
        style_desc = f"目标风格：「{style}」" if style else ""
        reference_desc = f"参考范文：\n{reference}" if reference else ""

        prompt = POLISH_PROMPT.format(
            text=text, level_desc=level_desc, level_requirements=level_requirements,
            style_desc=style_desc, reference_desc=reference_desc,
        )
        raw = self._llm.chat(prompt, text, temperature=0.1, json_mode=True)
        if not raw:
            return PolishResponse(polished="", changes="处理失败")
        data = extract_json(raw)
        if isinstance(data, dict):
            return PolishResponse(
                polished=data.get("polished", ""),
                changes=data.get("changes", ""),
            )
        return PolishResponse(polished=raw[:2000], changes="")
```

- [ ] **Step 8: 改 evaluate() 方法**

找到 `evaluate()` 方法（第 128 行），替换整个方法：

```python
    def evaluate(self, text: str) -> EvaluateResponse:
        """五维度评估文本质量。"""
        prompt = EVALUATE_PROMPT.format(text=text)
        raw = self._llm.chat(prompt, text, temperature=0.1, json_mode=True)
        if not raw:
            return EvaluateResponse()
        data = extract_json(raw)
        if not isinstance(data, dict):
            return EvaluateResponse()
        dimensions = [
            EvaluateDimension(
                name=d.get("name", ""),
                score=d.get("score", 0),
                comment=d.get("comment", ""),
                suggestion=d.get("suggestion", ""),
            )
            for d in data.get("dimensions", []) if isinstance(d, dict)
        ]
        return EvaluateResponse(
            dimensions=dimensions,
            highlight=data.get("highlight", ""),
            improvement=data.get("improvement", ""),
        )
```

- [ ] **Step 9: 新增 chat() 方法**

在 `evaluate()` 方法之后、类结束之前插入：

```python
    def chat(self, work_content: str, message: str, history: list) -> str:
        """自由对话：基于作品全文回答写作相关问题。"""
        system_prompt = COACH_CHAT_PROMPT.format(work_content=work_content or "（无全文）")

        history_text = ""
        for msg in history:
            role_label = "作者" if getattr(msg, 'role', '') == "user" else "教练"
            history_text += f"{role_label}：{getattr(msg, 'content', '')}\n"

        if history_text:
            user_msg = f"【对话历史】\n{history_text}\n【当前消息】\n{message}"
        else:
            user_msg = message

        reply = self._llm.chat(system_prompt, user_msg, temperature=0.5)
        return reply or "抱歉，处理请求时出现问题，请重试。"
```

- [ ] **Step 10: 验证语法**

```bash
cd D:\CijianAgent && C:/Users/26056/AppData/Local/Programs/Python/Python313/python -c "from app.agents.writing_coach_agent import WritingCoachAgent; print('OK')"
```

Expected: `OK`

---

### Task 3: Python main.py — 改端点 + 新增

**Files:** Modify `D:\CijianAgent\app\main.py`

- [ ] **Step 1: 更新 import**

在 schemas import 块中追加 `CoachChatRequest, CoachChatResponse`（在 `EvaluateRequest, EvaluateResponse` 之后）。

- [ ] **Step 2: 改 inspire 端点 — 解构新参数**

找到 `async def coach_inspire`（约第 81 行），替换为：

```python
@app.post("/api/coach/inspire", response_model=InspireResponse)
async def coach_inspire(request: InspireRequest):
    """帮写模块：灵感激发或定向生成。"""
    try:
        return writing_coach_agent.inspire(request.input, request.context, request.mode)
    except Exception:
        logger.exception("帮写模块失败")
        return InspireResponse(mode="inspire", results=[])
```

- [ ] **Step 3: 改 polish 端点 — 传 level**

找到 `async def coach_polish`（约第 91 行），替换为：

```python
@app.post("/api/coach/polish", response_model=PolishResponse)
async def coach_polish(request: PolishRequest):
    """修正润色：风格迁移、语法修正、去水词。"""
    try:
        return writing_coach_agent.polish(request.text, request.style, request.reference, request.level)
    except Exception:
        logger.exception("润色模块失败")
        return PolishResponse(polished="", changes="处理失败")
```

- [ ] **Step 4: 新增 chat 端点**

在 `coach_evaluate` 端点之后、`# ── 阅读伴侣` 之前插入：

```python

@app.post("/api/coach/chat", response_model=CoachChatResponse)
async def coach_chat(request: CoachChatRequest):
    """创作教练自由对话：基于作品全文回答写作问题。"""
    try:
        reply = writing_coach_agent.chat(
            work_content=request.work_content,
            message=request.message,
            history=request.history,
        )
        return CoachChatResponse(reply=reply)
    except Exception:
        logger.exception("创作教练对话失败")
        return CoachChatResponse(reply="抱歉，处理请求时出现问题，请重试。")
```

- [ ] **Step 5: 验证**

```bash
cd D:\CijianAgent && C:/Users/26056/AppData/Local/Programs/Python/Python313/python -c "from app.main import app; print('OK')"
```

Expected: `OK`

---

### Task 4: Java — DTO 扩 + 新增

**Files:** Modify `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\dto\CoachDto.java`

- [ ] **Step 1: 扩 InspireRequest**

替换第 51 行的 `InspireRequest`：

```java
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class InspireRequest {
        private String input;
        private String context;
        private String mode;
    }
```

- [ ] **Step 2: 扩 PolishRequest**

替换第 61 行的 `PolishRequest`：

```java
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PolishRequest {
        private String text;
        private String style;
        private String reference;
        private String level;
    }
```

- [ ] **Step 3: 扩 EvaluateDimension — 加 suggestion**

替换第 70 行的 `EvaluateDimension`：

```java
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class EvaluateDimension {
        private String name;
        private int score;
        private String comment;
        private String suggestion;
    }
```

- [ ] **Step 4: 新增 CoachChat 类**

在 `EvaluateResponse` 之后、类结束 `}` 之前插入：

```java
    // ── 自由对话 ──
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CoachChatMessage { private String role; private String content; }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CoachChatRequest {
        private String workContent;
        private String message;
        private List<CoachChatMessage> history;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CoachChatResponse { private String reply; }
```

---

### Task 5: Java — AiServiceClient 改 + 新增

**Files:** Modify `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\client\AiServiceClient.java`

- [ ] **Step 1: 改 coachInspire — 传 context + mode**

替换 `coachInspire` 方法（约第 80 行）：

```java
    public CoachDto.InspireResponse coachInspire(String input, String context, String mode) {
        Map<String, Object> body = new HashMap<>();
        body.put("input", input);
        if (context != null && !context.isEmpty()) body.put("context", context);
        if (mode != null && !mode.isEmpty()) body.put("mode", mode);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers());
        try {
            ResponseEntity<CoachDto.InspireResponse> response = aiRestTemplate.postForEntity(
                    url("/api/coach/inspire"), entity, CoachDto.InspireResponse.class);
            return response.getBody();
        } catch (RestClientException e) {
            return new CoachDto.InspireResponse("inspire", java.util.Collections.emptyList());
        }
    }
```

- [ ] **Step 2: 改 coachPolish — 传 level**

替换 `coachPolish` 方法（约第 92 行）：

```java
    public CoachDto.PolishResponse coachPolish(String text, String style, String reference, String level) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        if (style != null && !style.isEmpty()) body.put("style", style);
        if (reference != null && !reference.isEmpty()) body.put("reference", reference);
        if (level != null && !level.isEmpty()) body.put("level", level);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers());
        try {
            ResponseEntity<CoachDto.PolishResponse> response = aiRestTemplate.postForEntity(
                    url("/api/coach/polish"), entity, CoachDto.PolishResponse.class);
            return response.getBody();
        } catch (RestClientException e) {
            return new CoachDto.PolishResponse("", "处理失败");
        }
    }
```

- [ ] **Step 3: 新增 coachChat**

在 `coachEvaluate` 之后、`companionAnalyze` 之前插入：

```java
    public CoachDto.CoachChatResponse coachChat(String workContent, String message,
                                                 List<CoachDto.CoachChatMessage> history) {
        Map<String, Object> body = new HashMap<>();
        body.put("work_content", workContent != null ? workContent : "");
        body.put("message", message);
        if (history != null) body.put("history", history);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers());
        try {
            ResponseEntity<CoachDto.CoachChatResponse> response = aiRestTemplate.postForEntity(
                    url("/api/coach/chat"), entity, CoachDto.CoachChatResponse.class);
            return response.getBody();
        } catch (RestClientException e) {
            CoachDto.CoachChatResponse fallback = new CoachDto.CoachChatResponse();
            fallback.setReply("抱歉，处理请求时出现问题，请重试。");
            return fallback;
        }
    }
```

---

### Task 6: Java — CoachService 改签名

**Files:** Modify `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\service\CoachService.java`

- [ ] **Step 1: 改 inspire 方法**

替换第 50 行：

```java
    public CoachDto.InspireResponse inspire(String input, String context, String mode) {
        return aiServiceClient.coachInspire(input, context, mode);
    }
```

- [ ] **Step 2: 改 polish 方法**

替换第 51 行：

```java
    public CoachDto.PolishResponse polish(String text, String style, String reference, String level) {
        return aiServiceClient.coachPolish(text, style, reference, level);
    }
```

- [ ] **Step 3: 新增 chat 方法**

在 `evaluate()` 之后插入：

```java
    public CoachDto.CoachChatResponse chat(String workContent, String message,
                                            List<CoachDto.CoachChatMessage> history) {
        return aiServiceClient.coachChat(workContent, message, history);
    }
```

---

### Task 7: Java — CoachController 改端点

**Files:** Modify `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\controller\CoachController.java`

- [ ] **Step 1: 改 inspire 端点**

替换第 32-35 行：

```java
    @PostMapping("/inspire")
    public R<CoachDto.InspireResponse> inspire(@RequestBody CoachDto.InspireRequest request) {
        return R.success(coachService.inspire(request.getInput(), request.getContext(), request.getMode()));
    }
```

- [ ] **Step 2: 改 polish 端点**

替换第 37-40 行：

```java
    @PostMapping("/polish")
    public R<CoachDto.PolishResponse> polish(@RequestBody CoachDto.PolishRequest request) {
        return R.success(coachService.polish(request.getText(), request.getStyle(), request.getReference(), request.getLevel()));
    }
```

- [ ] **Step 3: 新增 chat 端点**

在 `evaluate` 之后插入：

```java
    @PostMapping("/chat")
    public R<CoachDto.CoachChatResponse> chat(@RequestBody CoachDto.CoachChatRequest request) {
        return R.success(coachService.chat(request.getWorkContent(), request.getMessage(), request.getHistory()));
    }
```

---

### Task 8: 构建并重启后端

- [ ] **Step 1: 重启 Python AI 服务**

```bash
powershell -Command "Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id \$_.OwningProcess -Force -ErrorAction SilentlyContinue }"
sleep 3
cd D:/CijianAgent && C:/Users/26056/AppData/Local/Programs/Python/Python313/python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 > D:/CijianAgent/server.log 2>&1 &
sleep 5 && tail -2 D:/CijianAgent/server.log
```

Expected: `Uvicorn running on http://0.0.0.0:8000`

- [ ] **Step 2: 构建 + 重启 Java cijian-ai**

```bash
powershell -Command "Stop-Process -Id (Get-NetTCPConnection -LocalPort 8085 -ErrorAction SilentlyContinue | Select-Object -First 1).OwningProcess -Force -ErrorAction SilentlyContinue"
sleep 3
JAVA="C:/Users/26056/AppData/Local/Programs/Eclipse Adoptium/jdk-17.0.18.8-hotspot/bin/java"
MVN="C:/Program Files/apache-maven-3.9.9"
"$JAVA" -classpath "$MVN/boot/*" -Dclassworlds.conf="$MVN/bin/m2.conf" -Dmaven.home="$MVN" -Dmaven.multiModuleProjectDirectory="D:/Cijian" org.codehaus.plexus.classworlds.launcher.Launcher clean install -DskipTests -q -pl cijian-ai -am
"$JAVA" -jar D:/Cijian/cijian-ai/target/cijian-ai-1.0.0-SNAPSHOT.jar > D:/Cijian/logs/ai.log 2>&1 &
sleep 60 && grep "Started CijianAiApplication" D:/Cijian/logs/ai.log
```

Expected: `Started CijianAiApplication in X seconds`

- [ ] **Step 3: 验证新端点**

```bash
# 测试 chat 端点
echo '{"work_content":"今天天气很好。","message":"我的开头会不会太普通了？","history":[]}' | curl -s -X POST http://localhost:8085/coach/chat -H "Content-Type: application/json" -d @- 2>&1 | python -c "import sys,json; d=json.load(sys.stdin); print(d['data']['reply'][:200])"

# 测试 inspire 带 context
echo '{"input":"继续写下去","context":"他推开门，发现屋里空无一人。","mode":"generate"}' | curl -s -X POST http://localhost:8085/coach/inspire -H "Content-Type: application/json" -d @- 2>&1 | python -c "import sys,json; d=json.load(sys.stdin); print(d['data']['results'][0]['content'][:200])"
```

Expected: 均返回有意义的文本

---

### Task 9: Frontend — API + WorkEditor AI 对话框

**Files:**
- Modify: `D:\Cijian-frontend\src\api\ai.ts`
- Modify: `D:\Cijian-frontend\src\views\WorkEditor.vue`

- [ ] **Step 1: API 层 — 改 inspireCoach 参数 + 新增 coachChat**

在 `ai.ts` 中，改 `inspireCoach` 签名：

```typescript
export function inspireCoach(input: string, context?: string, mode?: string) {
  return post<{ mode: string; results: { angle: string; content: string }[] }>('/api/ai/coach/inspire', { input, context, mode })
}
```

改 `polishCoach` 签名：

```typescript
export function polishCoach(data: { text: string; style?: string; reference?: string; level?: string }) {
  return post<{ polished: string; changes: string }>('/api/ai/coach/polish', data)
}
```

新增 `coachChat`：

```typescript
/** 创作教练 - 自由对话 */
export function coachChat(data: {
  workContent: string
  message: string
  history: { role: string; content: string }[]
}) {
  return post<{ reply: string }>('/api/ai/coach/chat', data)
}
```

- [ ] **Step 2: WorkEditor.vue — import 改**

将第 162 行：
```typescript
import { analyzeCoach, inspireCoach, polishCoach, evaluateCoach } from '@/api/ai'
```
改为：
```typescript
import { analyzeCoach, inspireCoach, polishCoach, evaluateCoach, coachChat } from '@/api/ai'
```

- [ ] **Step 3: WorkEditor.vue — 新增 AI 对话框状态**

在现有 ref 声明区域（约第 173 行后）追加：

```typescript
// ═══ AI 对话框 ═══
const chatOpen = ref(false)
const chatMessages = ref<{ role: string; content: string }[]>([])
const chatInput = ref('')
const chatLoading = ref(false)
const chatMsgListRef = ref<HTMLElement | null>(null)

function toggleChat() {
  chatOpen.value = !chatOpen.value
}

async function sendChat() {
  const text = chatInput.value.trim()
  if (!text || chatLoading.value) return
  chatInput.value = ''
  chatMessages.value.push({ role: 'user', content: text })
  chatLoading.value = true
  scrollChatList()
  try {
    const r = await coachChat({
      workContent: form.content,
      message: text,
      history: chatMessages.value.slice(0, -1).map(m => ({ role: m.role, content: m.content })),
    })
    chatMessages.value.push({ role: 'assistant', content: r.data?.reply || '' })
  } catch { ElMessage.error('对话失败') } finally { chatLoading.value = false; scrollChatList() }
}

function scrollChatList() {
  setTimeout(() => {
    if (chatMsgListRef.value) chatMsgListRef.value.scrollTop = chatMsgListRef.value.scrollHeight
  }, 50)
}
```

- [ ] **Step 4: WorkEditor.vue — 重构 submitFloatInput 使用新 API**

找到 `submitFloatInput` 函数，根据右键菜单的动作类型调用不同的 API（改 polish 调用传 level，改 inspire 调用传 context）。

具体替换整个函数为：

```typescript
async function submitFloatInput() {
  const prompt = floatInput.prompt.trim()
  if (!prompt && floatInput.label.includes('润色')) return
  floatLoading.value = true
  try {
    let output = ''
    if (floatInput.label.includes('润色')) {
      const level = floatInput.prompt === prompt ? 'medium' : 'medium'
      const r = await polishCoach({ text: ctxMenu.text, level })
      output = r.data?.polished || ''
    } else if (floatInput.label.includes('模仿')) {
      const style = floatInput.prompt || '指定风格'
      const r = await polishCoach({ text: ctxMenu.text, style, level: 'heavy' })
      output = r.data?.polished || ''
    } else if (floatInput.label.includes('续写')) {
      const cursorPos = form.content.length
      const ctx = form.content.slice(Math.max(0, cursorPos - 200), cursorPos)
      const r = await inspireCoach(prompt || '请继续写下去', ctx, 'generate')
      output = r.data?.results?.[0]?.content || ''
    } else {
      const r = await inspireCoach(prompt, form.content, '')
      output = r.data?.results?.[0]?.content || ''
    }
    if (output) {
      floatInput.visible = false
      floatInput.prompt = ''
      const ta = textareaRef.value; if (ta) ta.focus()
    }
  } catch { ElMessage.error('生成失败') } finally { floatLoading.value = false }
}
```

注意：上面是简化的逻辑，实际 `submitFloatInput` 的代码需要根据当前文件重新定位后替换。

- [ ] **Step 5: WorkEditor.vue — 模板新增 AI 对话框按钮 + 侧边栏**

在模板中（约 `<div class="action-bar">` 或工具栏区域）新增一个按钮：

```html
<el-button :type="chatOpen ? 'primary' : 'default'" size="small" @click="toggleChat" :icon="ChatDotRound">AI 对话</el-button>
```

在模板末尾（`</template>` 之前）新增侧边栏：

```html
<!-- AI 对话框 -->
<div class="ai-chat-sidebar" v-if="chatOpen">
  <div class="chat-head">
    <span>写作教练</span>
    <span class="chat-close" @click="chatOpen = false"><el-icon :size="16"><Close /></el-icon></span>
  </div>
  <div class="chat-msgs" ref="chatMsgListRef">
    <div v-if="!chatMessages.length && !chatLoading" class="chat-empty">输入你的问题，比如"开头会不会太拖了？""帮我看看第二段的结构"</div>
    <div v-for="(m, i) in chatMessages" :key="i" class="chat-msg" :class="m.role">
      <div class="msg-content">{{ m.content }}</div>
    </div>
    <div v-if="chatLoading" class="chat-msg assistant"><div class="msg-content typing">...</div></div>
  </div>
  <div class="chat-input-bar">
    <el-input v-model="chatInput" placeholder="输入消息..." size="small" @keyup.enter="sendChat" />
    <el-button size="small" type="primary" :loading="chatLoading" @click="sendChat">发送</el-button>
  </div>
</div>
```

需要导入 `ChatDotRound` 图标（如果尚未导入）：在 import 中加 `ChatDotRound`。

- [ ] **Step 6: WorkEditor.vue — 新增 CSS**

在 `<style scoped>` 末尾追加对话框样式：

```css
.ai-chat-sidebar {
  position: fixed; top: 60px; right: 0; z-index: 950;
  width: 380px; height: calc(100vh - 60px);
  background: var(--bg-card); border-left: 1px solid var(--border-color);
  display: flex; flex-direction: column;
}
.chat-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; border-bottom: 1px solid var(--border-color);
  font-weight: 600; font-size: 15px; color: var(--text-primary); flex-shrink: 0;
}
.chat-close { cursor: pointer; color: var(--text-disabled); }
.chat-close:hover { color: var(--text-primary); }
.chat-msgs { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.chat-empty { color: var(--text-disabled); font-size: 13px; text-align: center; padding: 40px 0; line-height: 1.8; }
.chat-msg { max-width: 92%; }
.chat-msg.user { align-self: flex-end; }
.chat-msg.user .msg-content { background: rgba(167,139,250,0.15); border-radius: 14px 14px 4px 14px; color: var(--text-primary); padding: 10px 14px; font-size: 14px; }
.chat-msg.assistant .msg-content { background: rgba(255,255,255,0.06); border-radius: 14px 14px 14px 4px; color: var(--text-secondary); padding: 10px 14px; font-size: 14px; line-height: 1.7; white-space: pre-wrap; }
.msg-content.typing { color: var(--text-disabled); font-style: italic; }
.chat-input-bar { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid var(--border-color); flex-shrink: 0; }
.chat-input-bar .el-input { flex: 1; }
```

---

### Task 10: 端到端验证

- [ ] **Step 1: 确认所有服务运行**

```bash
powershell -Command "netstat -ano | Select-String ':8085 |:8000 |:5173 ' | Select-String LISTENING"
```

Expected: 8085、8000、5173 均在 LISTENING

- [ ] **Step 2: 测试 chat 端点（通过 Java 代理）**

```bash
echo '{"workContent":"他推开门，发现屋里空无一人。桌上放着一杯还冒着热气的茶。","message":"这个开头如何制造悬念？","history":[]}' | curl -s -X POST http://localhost:8085/coach/chat -H "Content-Type: application/json" -d @- 2>&1 | python -c "import sys,json; d=json.load(sys.stdin); print(d['data']['reply'][:300])"
```

Expected: AI 给出具体建议，引用原文

- [ ] **Step 3: 测试 inspire 带 context**

```bash
echo '{"input":"续写","context":"他推开门，发现屋里空无一人。","mode":"generate"}' | curl -s -X POST http://localhost:8085/coach/inspire -H "Content-Type: application/json" -d @- 2>&1 | python -c "import sys,json; d=json.load(sys.stdin); print(d['data']['results'][0]['content'][:200])"
```

Expected: 续写内容自然衔接到"他推开门..."

- [ ] **Step 4: 测试 polish level=light（只修语法不改文风）**

```bash
echo '{"text":"他觉的很奇怪，明明刚才还在这里的。","level":"light"}' | curl -s -X POST http://localhost:8085/coach/polish -H "Content-Type: application/json" -d @- 2>&1 | python -c "import sys,json; d=json.load(sys.stdin); print(d['data']['polished'])"
```

Expected: "的"被修正为"得"，文风不变

- [ ] **Step 5: 浏览器验证前端**

打开 `http://localhost:5173/`，进入编辑器：
- 点击"AI 对话"按钮 → 侧边栏打开
- 输入问题 → AI 基于全文回复
- 选中文字 → 右键润色 → 确认 level 生效
