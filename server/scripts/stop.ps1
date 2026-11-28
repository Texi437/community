param([switch]$KeepDatabase)
. "$PSScriptRoot\env.ps1"
$record = Join-Path $RuntimeRoot 'backend-process.json'
if (Test-Path $record) {
    $saved = Get-Content $record -Raw | ConvertFrom-Json
    $p = Get-Process -Id $saved.id -ErrorAction SilentlyContinue
    if ($p) {
        if ($p.Path -ne $JavaExe -or $p.StartTime.ToUniversalTime() -ne ([datetime]$saved.startTime).ToUniversalTime()) { throw 'Process identity changed; refusing to stop another process.' }
        Stop-Process -Id $p.Id
        $p.WaitForExit(10000) | Out-Null
    }
    Remove-Item -LiteralPath $record
    Write-Output 'Backend stopped.'
}
if (!$KeepDatabase -and (Test-Path $LocalConfig) -and (Test-LocalPort $DevConfig.dbPort)) {
    $dbRecord = Join-Path $RuntimeRoot 'mysql-process.id'
    $dbProcess = $null
    if (Test-Path $dbRecord) {
        $dbProcess = Get-Process -Id ([int](Get-Content $dbRecord)) -ErrorAction SilentlyContinue
        if ($dbProcess -and $dbProcess.Path -ne $MysqldExe) { throw 'Database process identity changed; inspect before stopping.' }
    }
    & $MysqlAdminExe "--defaults-extra-file=$MysqlLocalRoot\mysql-root.cnf" shutdown
    if ($LASTEXITCODE -ne 0) { throw 'Database shutdown failed.' }
    if ($dbProcess -and !$dbProcess.WaitForExit(15000)) { throw 'MySQL is still shutting down; wait before restarting.' }
    if (Test-Path $dbRecord) { Remove-Item -LiteralPath $dbRecord }
    Write-Output 'MySQL shut down cleanly.'
}
