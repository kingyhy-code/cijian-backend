# Cijian 后端启动状态一键验证
# 用法: powershell -ExecutionPolicy Bypass -File check_all.ps1

$BASE = "D:\Cijian"
$services = @(
    @{Name="user";        Port=8081},
    @{Name="content";     Port=8082},
    @{Name="interaction"; Port=8083},
    @{Name="profile";     Port=8084},
    @{Name="ai";          Port=8085},
    @{Name="operation";   Port=8086},
    @{Name="search";      Port=8087},
    @{Name="gateway";     Port=8080}
)

Write-Host "=== 1. Log Check ==="
$ready = 0
foreach ($s in $services) {
    $log = Get-Content "$BASE\logs\$($s.Name).log" -Tail 3 -ErrorAction SilentlyContinue
    if ($log -match 'Started') {
        Write-Host "  $($s.Name) : STARTED" -ForegroundColor Green
        $ready++
    } elseif ($log) {
        Write-Host "  $($s.Name) : still starting..." -ForegroundColor Yellow
    } else {
        Write-Host "  $($s.Name) : no log" -ForegroundColor Gray
    }
}
Write-Host "  ($ready / $($services.Count) running)`n"

Write-Host "=== 2. HTTP Test ==="
foreach ($s in $services) {
    $url = "http://localhost:$($s.Port)/swagger-ui.html"
    try {
        $r = Invoke-WebRequest -Uri $url -TimeoutSec 10 -UseBasicParsing
        Write-Host "  $($s.Name) :$($s.Port) $($r.StatusCode) OK" -ForegroundColor Green
    } catch {
        Write-Host "  $($s.Name) :$($s.Port) FAIL - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n=== 3. Gateway Routes ==="
$routes = @(
    @{Path="/api/user/info";         Desc="User Service"},
    @{Path="/api/content/work/list"; Desc="Content Service"},
    @{Path="/api/operation/dashboard/stats"; Desc="Operation Service"}
)
foreach ($r in $routes) {
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:8080$($r.Path)" -TimeoutSec 10 -UseBasicParsing
        Write-Host "  $($r.Desc) : $($resp.StatusCode) OK" -ForegroundColor Green
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -eq 401) {
            Write-Host "  $($r.Desc) : 401 (auth required, route OK)" -ForegroundColor Green
        } else {
            Write-Host "  $($r.Desc) : FAIL - $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host "`nDone."
