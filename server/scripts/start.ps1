. "$PSScriptRoot\env.ps1"
& "$PSScriptRoot\start-db.ps1"
$url = "http://127.0.0.1:$($DevConfig.serverPort)/actuator/health"
if (Test-LocalPort $DevConfig.serverPort) {
    $record = Join-Path $RuntimeRoot 'backend-process.json'
    $isProjectBackend = $false
    if (Test-Path $record) {
        try {
            $saved = Get-Content $record -Raw | ConvertFrom-Json
            $running = Get-Process -Id $saved.id -ErrorAction Stop
            $listeners = @(Get-NetTCPConnection -LocalPort $DevConfig.serverPort -State Listen -ErrorAction Stop)
            $isProjectBackend = $running.Path -eq $JavaExe -and
                $running.StartTime.ToUniversalTime() -eq ([datetime]$saved.startTime).ToUniversalTime() -and
                @($listeners | Where-Object { $_.OwningProcess -eq $running.Id }).Count -gt 0
        } catch { $isProjectBackend = $false }
    }
    if (!$isProjectBackend) {
        throw "Port $($DevConfig.serverPort) is occupied by another or unrecognized process. No process was stopped."
    }
    try { $health = Invoke-RestMethod $url -TimeoutSec 5 }
    catch { throw 'Project backend is running but its health check failed. Check .runtime/backend.log and run scripts/check.ps1.' }
    if ($health.status -ne 'UP') { throw 'Project backend is running but is not healthy. Run scripts/check.ps1.' }
    Write-Output "Backend already running and healthy: $url"
    return
}
$jar = Join-Path $ProjectRoot 'backend\target\linli-backend-0.0.1-SNAPSHOT.jar'
if (!(Test-Path $jar)) { & "$PSScriptRoot\maven.ps1" clean package }
$app = Start-Process -FilePath $JavaExe -ArgumentList @('-Dfile.encoding=UTF-8','-Duser.timezone=Asia/Shanghai','-jar',"`"$jar`"") -WorkingDirectory $ProjectRoot -WindowStyle Hidden -RedirectStandardOutput "$RuntimeRoot\backend.log" -RedirectStandardError "$RuntimeRoot\backend-error.log" -PassThru
@{id=$app.Id; startTime=$app.StartTime.ToUniversalTime().ToString('o')} | ConvertTo-Json | Set-Content "$RuntimeRoot\backend-process.json"
for ($i=0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    if ($app.HasExited) { throw 'Backend exited. Check .runtime/backend.log and backend-error.log.' }
    try {
        $health = Invoke-RestMethod $url -TimeoutSec 2
        if ($health.status -eq 'UP') { Write-Output "Backend ready: $url"; return }
    } catch { }
}
throw 'Backend readiness timed out. Check .runtime/backend.log; the process remains available for inspection.'
