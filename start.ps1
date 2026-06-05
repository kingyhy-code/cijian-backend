# 辞间全栈启动脚本
# 用法: 以管理员身份在 PowerShell 中运行 .\start.ps1
# 如果只查状态: .\start.ps1 -Status

param([switch]$Status)

$ErrorActionPreference = "Continue"
$JAVA = "C:\Users\26056\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\java"
$PYTHON = "C:\Users\26056\AppData\Local\Programs\Python\Python313\python"
$BASE = "D:\Cijian"

# ====== 服务定义 ======
$DOCKER_CONTAINERS = @(
    "cijian-redis", "cijian-nacos", "cijian-rocketmq-namesrv",
    "cijian-rocketmq-broker", "cijian-elasticsearch", "cijian-seata"
)

$MICRO_SERVICES = @(
    @{Name="user";        Port=8081},
    @{Name="content";     Port=8082},
    @{Name="interaction"; Port=8083},
    @{Name="profile";     Port=8084},
    @{Name="operation";   Port=8086},
    @{Name="search";      Port=8087}
)

$GATEWAY = @{Name="gateway"; Port=8080}
$AGENT_PORT = 8000
$FRONTEND_PORT = 5173

# ====== 工具函数 ======
function Write-Step($msg) { Write-Host "`n>>> $msg" -ForegroundColor Cyan }
function Write-OK($msg)   { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-WARN($msg) { Write-Host "  [!]  $msg" -ForegroundColor Yellow }
function Write-FAIL($msg) { Write-Host "  [X]  $msg" -ForegroundColor Red }
function Write-INFO($msg) { Write-Host "  ...  $msg" -ForegroundColor Gray }

function Test-Port($port) {
    $r = netstat -ano 2>$null | Select-String "LISTENING" | Select-String ":$port "
    return ($r -ne $null)
}

# ====== 状态检查模式 ======
if ($Status) {
    Write-Host "===== 辞间服务状态 =====" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "--- Docker 容器 ---"
    foreach ($c in $DOCKER_CONTAINERS) {
        $s = docker inspect -f '{{.State.Status}}' $c 2>$null
        if ($s -eq "running") { Write-OK "$c" } else { Write-FAIL "$c ($s)" }
    }
    Write-Host ""
    Write-Host "--- Java 微服务 ---"
    foreach ($s in $MICRO_SERVICES) {
        if (Test-Port $s.Port) { Write-OK "$($s.Name) :$($s.Port)" }
        else { Write-FAIL "$($s.Name) :$($s.Port)" }
    }
    if (Test-Port $GATEWAY.Port) { Write-OK "gateway :$($GATEWAY.Port)" }
    else { Write-FAIL "gateway :$($GATEWAY.Port)" }
    Write-Host ""
    if (Test-Port $AGENT_PORT) { Write-OK "Python Agent :$AGENT_PORT" }
    else { Write-FAIL "Python Agent :$AGENT_PORT" }
    if (Test-Port $FRONTEND_PORT) { Write-OK "Frontend :$FRONTEND_PORT" }
    else { Write-FAIL "Frontend :$FRONTEND_PORT" }
    exit 0
}

# ====== 1. Docker 容器 ======
Write-Step "1/5 启动 Docker 容器"
foreach ($c in $DOCKER_CONTAINERS) {
    $running = (docker inspect -f '{{.State.Status}}' $c 2>$null) -eq "running"
    if ($running) {
        Write-OK "$c 已在运行"
    } else {
        docker start $c 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { Write-OK "$c 已启动" }
        else { Write-FAIL "$c 启动失败" }
    }
}

# ====== 2. 前置检查 ======
Write-Step "2/5 前置检查"

# 检查 JAR 文件
$missingJars = @()
foreach ($s in $MICRO_SERVICES) {
    $jar = "$BASE\cijian-$($s.Name)\target\cijian-$($s.Name)-1.0.0-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) { $missingJars += $jar }
}
$gatewayJar = "$BASE\cijian-$($GATEWAY.Name)\target\cijian-$($GATEWAY.Name)-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path $gatewayJar)) { $missingJars += $gatewayJar }

if ($missingJars.Count -gt 0) {
    Write-FAIL "缺少 JAR 文件:"
    $missingJars | ForEach-Object { Write-FAIL "  $_" }
    Write-WARN "请先运行 Maven 构建: cd D:\Cijian && mvn clean install -DskipTests -q"
    exit 1
}
Write-OK "所有 JAR 文件存在"

# 检查 MySQL
Write-INFO "检查 MySQL 连接..."
$mysqlOk = $true
try {
    $tcp = New-Object Net.Sockets.TcpClient
    $tcp.Connect("localhost", 3306)
    $tcp.Close()
    Write-OK "MySQL :3306 可达"
} catch {
    Write-FAIL "MySQL :3306 无法连接"
    $mysqlOk = $false
}

# ====== 3. 启动微服务 ======
Write-Step "3/5 启动 Java 微服务（每个最多等 90 秒）"

$startedServices = @()

foreach ($s in $MICRO_SERVICES) {
    $jar = "$BASE\cijian-$($s.Name)\target\cijian-$($s.Name)-1.0.0-SNAPSHOT.jar"

    if (Test-Port $s.Port) {
        Write-OK "$($s.Name) :$($s.Port) 已在运行，跳过"
        $startedServices += $s.Name
        continue
    }

    Write-INFO "启动 $($s.Name) ..."
    Start-Process -FilePath $JAVA -ArgumentList "-jar", $jar -WorkingDirectory $BASE -WindowStyle Minimized

    # 轮询端口直到就绪
    $ready = $false
    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Port $s.Port) { $ready = $true; break }
    }
    if ($ready) {
        Write-OK "$($s.Name) :$($s.Port) 已就绪"
        $startedServices += $s.Name
    } else {
        Write-FAIL "$($s.Name) :$($s.Port) 超时（90 秒未响应）"
        Write-WARN "  查看日志: Get-Content D:\Cijian\logs\$($s.Name).log -Tail 20"
    }
}

if ($startedServices.Count -eq 0) {
    Write-FAIL "没有微服务启动成功，退出"
    exit 1
}

# ====== 3.5 等待 Nacos 注册 ======
Write-Step "3.5/5 等待服务注册到 Nacos (最多 60 秒)"
$nacosReady = $false
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    try {
        $check = Invoke-RestMethod -Uri "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=user-service" -Method Get -TimeoutSec 3
        if ($check.hosts.Count -gt 0) { $nacosReady = $true; break }
    } catch {}
}
if ($nacosReady) {
    Write-OK "Nacos 中已有服务注册"
} else {
    Write-WARN "Nacos 检查超时，继续启动 Gateway（可能部分服务未注册）"
}

# ====== 4. 启动 Gateway ======
Write-Step "4/5 启动 Gateway"

if (Test-Port $GATEWAY.Port) {
    Write-OK "Gateway :$($GATEWAY.Port) 已在运行"
} else {
    Write-INFO "启动 gateway ..."
    Start-Process -FilePath $JAVA -ArgumentList "-jar", $gatewayJar -WorkingDirectory $BASE -WindowStyle Minimized

    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Port $GATEWAY.Port) { $ready = $true; break }
    }
    if ($ready) { Write-OK "Gateway :$($GATEWAY.Port) 已就绪" }
    else { Write-FAIL "Gateway 启动超时" }
}

# ====== 5. Python Agent + 前端 ======
Write-Step "5/5 启动 Agent 和前端"

# Python Agent
if (Test-Path "D:\CijianAgent") {
    if (Test-Port $AGENT_PORT) {
        Write-OK "Agent :$AGENT_PORT 已在运行"
    } else {
        Write-INFO "启动 Python Agent ..."
        Start-Process -FilePath $PYTHON -ArgumentList "-m", "uvicorn", "app.main:app", "--reload", "--host", "0.0.0.0", "--port", "8000" -WorkingDirectory "D:\CijianAgent" -WindowStyle Minimized
        $ready = $false
        for ($i = 0; $i -lt 30; $i++) { Start-Sleep -Seconds 1; if (Test-Port $AGENT_PORT) { $ready = $true; break } }
        if ($ready) { Write-OK "Agent :$AGENT_PORT 已就绪" }
        else { Write-WARN "Agent 启动超时（uvicorn 首次加载较慢可忽略）" }
    }
} else {
    Write-WARN "D:\CijianAgent 目录不存在，跳过 Agent"
}

# 前端
if (Test-Path "D:\Cijian-frontend") {
    $envFile = "D:\Cijian-frontend\.env"
    if (-not (Test-Path $envFile)) {
        @"
VITE_USER_SERVICE=http://localhost:8080
VITE_CONTENT_SERVICE=http://localhost:8080
VITE_INTERACTION_SERVICE=http://localhost:8080
VITE_PROFILE_SERVICE=http://localhost:8080
VITE_OPERATION_SERVICE=http://localhost:8080
VITE_SEARCH_SERVICE=http://localhost:8080
VITE_AI_SERVICE=http://localhost:8000
"@ | Out-File $envFile -Encoding UTF8
        Write-OK ".env 文件已创建"
    }

    if (Test-Port $FRONTEND_PORT) {
        Write-OK "前端 :$FRONTEND_PORT 已在运行"
    } else {
        Write-INFO "启动前端 ..."
        Start-Process -FilePath "npx" -ArgumentList "vite", "--host", "0.0.0.0", "--port", "5173" -WorkingDirectory "D:\Cijian-frontend" -WindowStyle Minimized
        $ready = $false
        for ($i = 0; $i -lt 30; $i++) { Start-Sleep -Seconds 1; if (Test-Port $FRONTEND_PORT) { $ready = $true; break } }
        if ($ready) { Write-OK "前端 :$FRONTEND_PORT 已就绪" }
        else { Write-WARN "前端启动超时" }
    }
}

# ====== 汇总 ======
Write-Host "`n==============================" -ForegroundColor Cyan
Write-Host "  启动完成 - 服务状态汇总" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan
Write-Host ""

$services = @(
    @{Label="cijian-user";        Port=8081},
    @{Label="cijian-content";     Port=8082},
    @{Label="cijian-interaction"; Port=8083},
    @{Label="cijian-profile";     Port=8084},
    @{Label="cijian-operation";   Port=8086},
    @{Label="cijian-search";      Port=8087},
    @{Label="cijian-gateway";     Port=8080},
    @{Label="Python Agent";       Port=8000},
    @{Label="前端 (Vite)";         Port=5173}
)

$up = 0; $down = 0
foreach ($svc in $services) {
    $label = $svc.Label.PadRight(22)
    if (Test-Port $svc.Port) {
        Write-Host "  [UP]    $label`t:$($svc.Port)" -ForegroundColor Green
        $up++
    } else {
        Write-Host "  [DOWN]  $label`t:$($svc.Port)" -ForegroundColor Red
        $down++
    }
}
Write-Host ""
Write-Host "  在线: $up / 离线: $down" -ForegroundColor $(if ($down -eq 0) { "Green" } else { "Yellow" })

Write-Host "`n  验证地址:" -ForegroundColor Gray
Write-Host "    前端:  http://localhost:5173/" -ForegroundColor Gray
Write-Host "    Agent: http://localhost:8000/health" -ForegroundColor Gray
Write-Host "    Nacos: http://localhost:8848/nacos" -ForegroundColor Gray
Write-Host ""
