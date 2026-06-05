# 阅读伴侣全程对话式导读 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将阅读伴侣宏观导读改为全程对话式，LLM 控流程，用户随时插话提问。

**Architecture:** 新增统一对话端点 `POST /companion/guide/chat`，贯穿前端 → Java网关 → Python AI 三层。Python 端用 System Prompt 引导 LLM 分段导读、出题、点评、启发提问，前端去掉状态机改为纯聊天界面。

**Tech Stack:** Python FastAPI + LangChain ChatOpenAI, Java Spring Boot (DTO + Client + Service + Controller), Vue 3 + TypeScript

---

## 文件变更清单

| 层 | 文件 | 操作 |
|---|---|---|
| Python | `D:\CijianAgent\app\models\schemas.py` | 新增 GuideChatRequest/Response/Message |
| Python | `D:\CijianAgent\app\agents\companion_agent.py` | 新增 GUIDE_CHAT_PROMPT + guide_chat() |
| Python | `D:\CijianAgent\app\main.py` | 新增 POST /api/companion/guide/chat |
| Java | `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\dto\CompanionDto.java` | 新增 GuideChatRequest/Response/Message |
| Java | `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\client\AiServiceClient.java` | 新增 guideChat() |
| Java | `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\service\CompanionService.java` | 新增 guideChat() |
| Java | `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\controller\CompanionController.java` | 新增 @PostMapping("/guide/chat") |
| Frontend | `D:\Cijian-frontend\src\api\ai.ts` | 新增 guideChatCompanion() |
| Frontend | `D:\Cijian-frontend\src\views\WorkDetail.vue` | 删除状态机、简化消息处理 |

---

### Task 1: Python - 新增数据模型

**Files:**
- Modify: `D:\CijianAgent\app\models\schemas.py` (末尾追加)

- [ ] **Step 1: 在 schemas.py 末尾追加新模型**

在文件末尾追加以下代码：

```python
# ── 阅读伴侣全程对话 ────────────────────────────────────────────

class GuideChatMessage(BaseModel):
    role: str = Field(description="user 或 assistant")
    content: str = Field(description="消息内容")


class GuideChatRequest(BaseModel):
    title: str = Field(..., min_length=1, description="作品标题")
    author: str = Field(default="", description="作者姓名")
    is_classic: bool = Field(default=True, description="是否为经典名作")
    work_content: str = Field(default="", description="作品全文")
    message: str = Field(default="", description="用户当前消息，首轮为空触发导读")
    history: list[GuideChatMessage] = Field(default_factory=list, description="对话历史")


class GuideChatResponse(BaseModel):
    reply: str = Field(default="", description="AI 回复文本")
```

- [ ] **Step 2: 验证 Python 语法无报错**

```bash
cd D:\CijianAgent && C:/Users/26056/AppData/Local/Programs/Python/Python313/python -c "from app.models.schemas import GuideChatRequest, GuideChatResponse, GuideChatMessage; print('OK')"
```

Expected: `OK`

---

### Task 2: Python - Agent 新增 guide_chat 方法

**Files:**
- Modify: `D:\CijianAgent\app\agents\companion_agent.py`

- [ ] **Step 1: 新增 System Prompt 常量**

在 `EVALUATE_PROMPT` 常量之后（第 123 行 `"""` 之后）、`class CompanionAgent` 之前插入：

```python
GUIDE_CHAT_PROMPT = """\
你是文学阅读导师。请带领读者完成一部作品的深度阅读成长流程。

## 作品信息
- 标题：{title}
- 作者：{author}
- 类型：{type_desc}
- 作品全文：
{work_content}

## 你的任务

按以下流程引导读者，自然推进，不要出现"阶段1""阶段2"等字眼：

1. **导读**：分2-3段介绍剧情梗概、创作背景、技法标签、情感基调。每次1-2个方面，不超过200字。每段后停顿问读者想法。全讲完后询问读者是否读完。
2. **出题检验**：读者表示读完后，逐题出2题。等读者答一题后简短点评再出下一题。
3. **点评总结**：全部答完后简短点评（50字内）。
4. **引导提问**：从写作技法、结构、语言等角度提1-2个问题，激发创作思考。

## 核心规则
- 读者随时可插话提问（如"欧亨利式结尾是什么意思？"），必须先回答再自然回到当前流程
- 通过理解读者意图判断是否进入下一阶段（如"读完了""看好了""可以了"），不靠关键词硬匹配
- 导读分段短小，语气亲切，像有见识的朋友聊文学
- 始终围绕这部作品展开，不脱离文本空谈"""
```

- [ ] **Step 2: 新增 guide_chat() 方法**

在 `evaluate()` 方法之后、`_parse_questions()` 方法之前插入：

```python
    def guide_chat(self, title: str, author: str, is_classic: bool,
                   work_content: str, message: str,
                   history: list) -> str:
        """全程对话式导读：LLM 控流程，用户随时可插话。"""
        type_desc = "经典名作，你充分了解这部作品" if is_classic else "用户原创作品，请基于作品全文分析"
        system_prompt = GUIDE_CHAT_PROMPT.format(
            title=title,
            author=author or "佚名",
            type_desc=type_desc,
            work_content=work_content or "（无全文）",
        )

        history_text = ""
        for msg in history:
            role_label = "读者" if getattr(msg, 'role', '') == "user" else "助手"
            history_text += f"{role_label}：{getattr(msg, 'content', '')}\n"

        if not message and not history_text:
            user_msg = "请开始为我导读这部作品"
        elif history_text:
            user_msg = f"【对话历史】\n{history_text}\n【当前消息】\n{message or '请继续'}"
        else:
            user_msg = message

        reply = self._llm.chat(system_prompt, user_msg, temperature=0.5)
        return reply or "抱歉，处理请求时出现问题，请重试。"
```

- [ ] **Step 3: 验证导入无报错**

```bash
cd D:\CijianAgent && C:/Users/26056/AppData/Local/Programs/Python/Python313/python -c "from app.agents.companion_agent import CompanionAgent, GUIDE_CHAT_PROMPT; print('OK')"
```

Expected: `OK`

---

### Task 3: Python - main.py 新增端点

**Files:**
- Modify: `D:\CijianAgent\app\main.py`

- [ ] **Step 1: 导入新模型**

修改 `from app.models.schemas import (` 导入块，在 `CompanionEvaluateResponse` 之后追加 `GuideChatRequest, GuideChatResponse`：

找到约第 34-40 行的导入块，修改为：

```python
from app.models.schemas import (
    TextAnalysisRequest,
    AnalysisResponse,
    InspireRequest,
    InspireResponse,
    PolishRequest,
    PolishResponse,
    EvaluateRequest,
    EvaluateResponse,
    CompanionRequest,
    CompanionResponse,
    CompanionChatRequest,
    CompanionChatResponse,
    CompanionOverviewRequest,
    CompanionOverviewResponse,
    CompanionEvaluateRequest,
    CompanionEvaluateResponse,
    GuideChatRequest,
    GuideChatResponse,
    TutorChatRequest,
    TutorChatResponse,
    TutorMessage,
)
```

- [ ] **Step 2: 新增路由**

在 `companion_evaluate` 端点之后、`# ── 文法导师 ──` 注释之前（约第 191 行）插入：

```python

@app.post("/api/companion/guide/chat", response_model=GuideChatResponse)
async def companion_guide_chat(request: GuideChatRequest):
    """全程对话式导读：LLM 控流程，用户随时可插话提问。"""
    try:
        reply = companion_agent.guide_chat(
            title=request.title,
            author=request.author,
            is_classic=request.is_classic,
            work_content=request.work_content,
            message=request.message,
            history=request.history,
        )
        return GuideChatResponse(reply=reply)
    except Exception:
        logger.exception("阅读伴侣全程对话失败")
        return GuideChatResponse(reply="抱歉，处理请求时出现问题，请重试。")
```

- [ ] **Step 3: 验证 FastAPI 启动无报错**

```bash
cd D:\CijianAgent && C:/Users/26056/AppData/Local/Programs/Python/Python313/python -c "from app.main import app; print('OK')"
```

Expected: `OK`

---

### Task 4: Java - DTO 新增 GuideChat 类

**Files:**
- Modify: `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\dto\CompanionDto.java`

- [ ] **Step 1: 在 CompanionDto 内部追加 DTO 类**

在 `CompanionDto.java` 文件末尾（`EvaluateResponse` 类之后、最后一个 `}` 之前）追加：

```java
    // ── guide chat ──

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuideChatMessage {
        private String role;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuideChatRequest {
        private String title;
        private String author;
        private boolean isClassic = true;
        private String workContent;
        private String message;
        private List<GuideChatMessage> history;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuideChatResponse {
        private String reply;
    }
```

---

### Task 5: Java - AiServiceClient 新增方法

**Files:**
- Modify: `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\client\AiServiceClient.java`

- [ ] **Step 1: 在 AiServiceClient 末尾（healthCheck 之前）新增方法**

在 `companionEvaluate` 方法之后、`healthCheck` 方法之前插入：

```java
    public CompanionDto.GuideChatResponse guideChat(String title, String author, boolean isClassic,
                                                     String workContent, String message,
                                                     List<CompanionDto.GuideChatMessage> history) {
        Map<String, Object> request = new HashMap<>();
        request.put("title", title);
        request.put("author", author);
        request.put("is_classic", isClassic);
        request.put("work_content", workContent);
        request.put("message", message != null ? message : "");
        if (history != null) {
            request.put("history", history);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers());
        try {
            ResponseEntity<CompanionDto.GuideChatResponse> response = aiRestTemplate.postForEntity(
                    url("/api/companion/guide/chat"), entity, CompanionDto.GuideChatResponse.class);
            return response.getBody();
        } catch (RestClientException e) {
            CompanionDto.GuideChatResponse fallback = new CompanionDto.GuideChatResponse();
            fallback.setReply("抱歉，处理请求时出现问题，请重试。");
            return fallback;
        }
    }
```

---

### Task 6: Java - CompanionService 新增方法

**Files:**
- Modify: `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\service\CompanionService.java`

- [ ] **Step 1: 新增 guideChat 方法**

在 `evaluate()` 方法之后、类结束 `}` 之前插入：

```java
    public CompanionDto.GuideChatResponse guideChat(String title, String author, boolean isClassic,
                                                     String workContent, String message,
                                                     List<CompanionDto.GuideChatMessage> history) {
        return aiServiceClient.guideChat(title, author, isClassic, workContent, message, history);
    }
```

---

### Task 7: Java - CompanionController 新增端点

**Files:**
- Modify: `D:\Cijian\cijian-ai\src\main\java\com\cijian\ai\controller\CompanionController.java`

- [ ] **Step 1: 新增端点**

在 `chat()` 方法之后、类结束 `}` 之前插入：

```java
    @PostMapping("/guide/chat")
    public R<CompanionDto.GuideChatResponse> guideChat(@RequestBody CompanionDto.GuideChatRequest request) {
        CompanionDto.GuideChatResponse result = companionService.guideChat(
                request.getTitle(), request.getAuthor(), request.isClassic(),
                request.getWorkContent(), request.getMessage(), request.getHistory());
        return R.success(result);
    }
```

---

### Task 8: 构建并重启后端服务

- [ ] **Step 1: 重启 Python AI 服务**

先停旧进程，再启动新服务：

```bash
powershell -Command "Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id \$_.OwningProcess -Force -ErrorAction SilentlyContinue }"
sleep 2
cd D:/CijianAgent && C:/Users/26056/AppData/Local/Programs/Python/Python313/python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 > D:/CijianAgent/server.log 2>&1 &
sleep 5 && tail -3 D:/CijianAgent/server.log
```

Expected: `Uvicorn running on http://0.0.0.0:8000`

- [ ] **Step 2: Maven 构建 cijian-ai**

```bash
JAVA="C:/Users/26056/AppData/Local/Programs/Eclipse Adoptium/jdk-17.0.18.8-hotspot/bin/java"
MVN="C:/Program Files/apache-maven-3.9.9"
"$JAVA" -classpath "$MVN/boot/*" -Dclassworlds.conf="$MVN/bin/m2.conf" -Dmaven.home="$MVN" -Dmaven.multiModuleProjectDirectory="D:/Cijian" org.codehaus.plexus.classworlds.launcher.Launcher clean install -DskipTests -q -pl cijian-ai -am
```

Expected: 无输出（-q 模式，成功静默）

- [ ] **Step 3: 重启 cijian-ai Java 服务**

```bash
# 杀掉旧 cijian-ai 进程 (端口 8085)
powershell -Command "Stop-Process -Id (Get-NetTCPConnection -LocalPort 8085 -ErrorAction SilentlyContinue | Select-Object -First 1).OwningProcess -Force -ErrorAction SilentlyContinue"
sleep 3
JAVA="C:/Users/26056/AppData/Local/Programs/Eclipse Adoptium/jdk-17.0.18.8-hotspot/bin/java"
BASE="D:/Cijian"
"$JAVA" -jar "$BASE/cijian-ai/target/cijian-ai-1.0.0-SNAPSHOT.jar" > "$BASE/logs/ai.log" 2>&1 &
echo "cijian-ai PID: $!"
```

- [ ] **Step 4: 验证 Java 服务启动**

```bash
sleep 60 && grep "Started CijianAiApplication" D:/Cijian/logs/ai.log
```

Expected: `Started CijianAiApplication in X seconds`

- [ ] **Step 5: 验证新端点可达**

```bash
curl -s -X POST http://localhost:8000/api/companion/guide/chat \
  -H "Content-Type: application/json" \
  -d '{"title":"测试","author":"测试","is_classic":true,"work_content":"测试正文","message":"","history":[]}'
```

Expected: 返回 JSON 包含 `reply` 字段（AI 导读回复）

---

### Task 9: Frontend - API 层新增方法

**Files:**
- Modify: `D:\Cijian-frontend\src\api\ai.ts`

- [ ] **Step 1: 新增 guideChatCompanion**

在 `evaluateCompanion` 函数之后、`chatTutor` 之前插入：

```typescript
/** 阅读伴侣 - 全程对话式导读 */
export function guideChatCompanion(data: {
  title: string
  author: string
  isClassic: boolean
  workContent: string
  message: string
  history: { role: string; content: string }[]
}) {
  return post<{ reply: string }>('/api/ai/companion/guide/chat', data)
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

```bash
cd D:/Cijian-frontend && "C:/Program Files/nodejs/node" node_modules/.bin/vue-tsc --noEmit --skipLibCheck 2>&1 | head -5
```

Expected: 无新增错误（可能已有预存错误）

---

### Task 10: Frontend - WorkDetail.vue 去除状态机

**Files:**
- Modify: `D:\Cijian-frontend\src\views\WorkDetail.vue`

- [ ] **Step 1: 更新 import**

将第 157 行的：
```typescript
import { overviewCompanion, evaluateCompanion, chatCompanion } from '@/api/ai'
```
改为：
```typescript
import { guideChatCompanion, chatCompanion } from '@/api/ai'
```

- [ ] **Step 2: 删除状态变量**

删除第 184-188 行：
```typescript
const step = ref<'overview' | 'waiting_read' | 'quiz' | 'evaluate' | 'done'>('overview')
const quizData = ref<{ question: string; hint: string }[]>([])
const inspData = ref<{ question: string; category: string }[]>([])
const quizIndex = ref(0)
const userAnswers = ref<string[]>([])
```

- [ ] **Step 3: 删除 loadingStep 的默认值依赖**

保留 `const loadingStep = ref(false)`，无需改动。

- [ ] **Step 4: 简化 inputPlaceholder**

将第 190-197 行的 computed 替换为：
```typescript
const inputPlaceholder = '输入消息...'
```

- [ ] **Step 5: 重新实现 toggleSidebar 和 genOverview**

将第 208-240 行的 `toggleSidebar` 和 `genOverview` 替换为：

```typescript
function toggleSidebar() {
  aiSidebar.value = !aiSidebar.value
  if (aiSidebar.value && !sbMessages.value.length) startGuide()
}

async function startGuide() {
  if (!work.value) return
  loadingStep.value = true
  try {
    const r = await guideChatCompanion({
      title: work.value.title,
      author: work.value.originalAuthor || work.value.author.nickname,
      isClassic: !!work.value.isMasterpiece,
      workContent: work.value.content || '',
      message: '',
      history: [],
    })
    sbMessages.value.push({ role: 'assistant', content: toChatHtml(r.data?.reply || '') })
  } catch { ElMessage.error('启动导读失败') } finally { loadingStep.value = false; scrollSbChat() }
}
```

- [ ] **Step 6: 新增 toChatHtml 工具函数**

在 `startGuide` 函数后面追加：

```typescript
function toChatHtml(text: string) {
  return text.replace(/\n/g, '<br/>')
}
```

- [ ] **Step 7: 重写 sendSbMessage**

将第 242-297 行的 `sendSbMessage` 替换为：

```typescript
async function sendSbMessage() {
  const text = sbInput.value.trim()
  if (!text || loadingStep.value) return
  sbInput.value = ''
  sbMessages.value.push({ role: 'user', content: text })
  loadingStep.value = true
  scrollSbChat()
  try {
    const r = await guideChatCompanion({
      title: work.value!.title,
      author: work.value!.originalAuthor || work.value!.author.nickname,
      isClassic: !!work.value!.isMasterpiece,
      workContent: work.value!.content || '',
      message: text,
      history: sbMessages.value.slice(0, -1).map(m => ({ role: m.role, content: m.content })),
    })
    sbMessages.value.push({ role: 'assistant', content: toChatHtml(r.data?.reply || '') })
  } catch { ElMessage.error('发送失败') } finally { loadingStep.value = false; scrollSbChat() }
}
```

- [ ] **Step 8: 简化发送按钮 template**

将第 140-141 行的：
```html
<el-button type="primary" :loading="loadingStep"
  :disabled="!sbInput.trim() || step === 'done'" @click="sendSbMessage">发送</el-button>
```
改为：
```html
<el-button type="primary" :loading="loadingStep"
  :disabled="!sbInput.trim()" @click="sendSbMessage">发送</el-button>
```

---

### Task 11: 端到端验证

- [ ] **Step 1: 确认所有服务运行中**

```bash
powershell -Command "netstat -ano | Select-String ':8085 |:8000 |:5173 ' | Select-String LISTENING"
```

Expected: 显示 8085 (cijian-ai)、8000 (Python AI)、5173 (Vite) 均在 LISTENING

- [ ] **Step 2: 通过网关端到端测试新端点**

```bash
curl -s -X POST http://localhost:8080/companion/guide/chat \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"背影\",\"author\":\"朱自清\",\"isClassic\":true,\"workContent\":\"我与父亲不相见已二年余了，我最不能忘记的是他的背影。那年冬天，祖母死了，父亲的差使也交卸了，正是祸不单行的日子...\",\"message\":\"\",\"history\":[]}"
```

Expected: 返回 JSON 含 `code: 200` 和 `data.reply` 字段，reply 内容为 AI 导读文本

- [ ] **Step 3: 测试追问能力**

```bash
curl -s -X POST http://localhost:8080/companion/guide/chat \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"背影\",\"author\":\"朱自清\",\"isClassic\":true,\"workContent\":\"...\",\"message\":\"朱自清是什么时代的作家？\",\"history\":[{\"role\":\"assistant\",\"content\":\"你好！让我带你读朱自清的《背影》...\"}]}"
```

Expected: 返回 JSON 含 `code: 200`，`data.reply` 回答关于朱自清的问题

- [ ] **Step 4: 浏览器打开前端验证**

打开 `http://localhost:5173/`，进入一篇作品详情页，打开阅读伴侣侧边栏：
- AI 应开始分段导读
- 输入框始终可用
- 输入问题后 AI 回答并自然回到流程
