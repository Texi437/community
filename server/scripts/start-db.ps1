. "$PSScriptRoot\env.ps1"
if (!(Test-Path "$RuntimeRoot\database-initialized")) { throw 'Run scripts/init-db.ps1 first.' }
if (Test-LocalPort $DevConfig.dbPort) {
    & $MysqlExe "--defaults-extra-file=$MysqlLocalRoot\mysql-app.cnf" --batch --skip-column-names -e 'SELECT 1' linli_governance
    if ($LASTEXITCODE -ne 0) { throw 'Database port occupied by an unexpected or inaccessible instance.' }
    Write-Output 'Project database is already running.'
    return
}
$p = Start-Process -FilePath $MysqldExe -ArgumentList "--defaults-file=`"$MysqlProjectRoot\.runtime\my.ini`"" -WorkingDirectory $MysqlProjectRoot -WindowStyle Hidden -PassThru
$p.Id | Set-Content "$RuntimeRoot\mysql-process.id"
for ($i=0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    if ($p.HasExited) { throw 'MySQL exited. See .runtime/mysql-error.log.' }
    if (Test-LocalPort $DevConfig.dbPort) {
        & $MysqlExe "--defaults-extra-file=$MysqlLocalRoot\mysql-app.cnf" --batch --skip-column-names -e 'SELECT 1' linli_governance 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { Write-Output 'MySQL ready.'; return }
    }
}
throw 'MySQL startup timed out.'
