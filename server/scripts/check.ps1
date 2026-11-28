. "$PSScriptRoot\env.ps1"
& $JavaExe -version
& $MavenExe --version
& $MysqlExe --version
& $MysqlExe "--defaults-extra-file=$MysqlLocalRoot\mysql-app.cnf" --table -e 'SELECT VERSION() AS mysql_version, DATABASE() AS db, CURRENT_USER() AS account, @@session.time_zone AS timezone, @@character_set_database AS charset;' linli_governance
if ($LASTEXITCODE -ne 0) { throw 'Database verification failed.' }
$url = "http://127.0.0.1:$($DevConfig.serverPort)"
$health = Invoke-RestMethod "$url/actuator/health" -TimeoutSec 10
if ($health.status -ne 'UP') { throw 'Backend unhealthy.' }
try { Invoke-WebRequest "$url/api/users/me" -UseBasicParsing -TimeoutSec 10 | Out-Null; throw 'Unprotected business route.' }
catch {
    if (!$_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 401) { throw }
}
Write-Output 'PASS: MySQL connected; health UP; anonymous profile request returns 401.'
