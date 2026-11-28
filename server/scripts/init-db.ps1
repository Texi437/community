param([int]$DbPort = 3307, [int]$ServerPort = 8080)
. "$PSScriptRoot\env.ps1"
New-Item -ItemType Directory -Force -Path $RuntimeRoot,$LocalRoot | Out-Null
$ini = Join-Path $RuntimeRoot 'my.ini'
$dataDir = Join-Path $RuntimeRoot 'mysql-data'
$marker = Join-Path $RuntimeRoot 'database-initialized'
if (Test-Path $marker) {
    if (!(Test-Path $LocalConfig)) { throw 'Database exists but credentials are missing. Restore .local/environment.json; do not reinitialize.' }
    Write-Output 'Database already initialized. Existing data preserved.'
    return
}
if (Test-Path $dataDir) { throw 'An uncompleted database initialization exists. Inspect .runtime/mysql-init.log before proceeding; data will not be overwritten.' }
if (Test-LocalPort $DbPort) { throw "Port $DbPort is in use. Choose another port with -DbPort." }
if (Test-Path $LocalConfig) { throw 'Local config already exists without an initialized database. Inspect it before proceeding.' }
function New-Password {
    $bytes = New-Object byte[] 24
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
}
$config = [ordered]@{ dbPort=$DbPort; serverPort=$ServerPort; serverAddress='127.0.0.1'; appPassword=(New-Password); rootPassword=(New-Password) }
Write-Utf8 $LocalConfig ($config | ConvertTo-Json)
$base = $MysqlHome.Replace('\','/')
$data = (Join-Path $MysqlProjectRoot '.runtime/mysql-data').Replace('\','/')
$log = (Join-Path $MysqlProjectRoot '.runtime/mysql-error.log').Replace('\','/')
Write-Utf8 $ini @"
[mysqld]
basedir="$base"
datadir="$data"
port=$DbPort
bind-address=127.0.0.1
mysqlx=0
character-set-server=utf8mb4
collation-server=utf8mb4_0900_ai_ci
default-time-zone='+08:00'
log-error="$log"
innodb-buffer-pool-size=128M
max-connections=50
"@
$initProcess = Start-Process -FilePath $MysqldExe -ArgumentList @("--defaults-file=`"$MysqlProjectRoot\.runtime\my.ini`"", '--initialize-insecure', '--console') -WorkingDirectory $MysqlProjectRoot -WindowStyle Hidden -Wait -PassThru -RedirectStandardOutput "$RuntimeRoot\mysql-init.log" -RedirectStandardError "$RuntimeRoot\mysql-init-error.log"
if ($initProcess.ExitCode -ne 0) { throw 'MySQL initialization failed. See .runtime/mysql-init-error.log.' }
# Set passwords through init-file before accepting normal client connections.
$bootstrap = Join-Path $LocalRoot 'bootstrap.sql'
Write-Utf8 $bootstrap @"
ALTER USER 'root'@'localhost' IDENTIFIED BY '$($config.rootPassword)';
CREATE DATABASE IF NOT EXISTS linli_governance CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'linli_app'@'127.0.0.1' IDENTIFIED BY '$($config.appPassword)';
GRANT SELECT, INSERT, UPDATE, DELETE ON linli_governance.* TO 'linli_app'@'127.0.0.1';
"@
$dbProcess = Start-Process -FilePath $MysqldExe -ArgumentList @("--defaults-file=`"$MysqlProjectRoot\.runtime\my.ini`"", "--init-file=`"$MysqlLocalRoot\bootstrap.sql`"") -WorkingDirectory $MysqlProjectRoot -WindowStyle Hidden -PassThru
$dbProcess.Id | Set-Content (Join-Path $RuntimeRoot 'mysql-process.id')
foreach ($account in @('root','app')) {
    $user = if ($account -eq 'root') { 'root' } else { 'linli_app' }
    $password = if ($account -eq 'root') { $config.rootPassword } else { $config.appPassword }
    Write-Utf8 (Join-Path $LocalRoot "mysql-$account.cnf") "[client]`nhost=127.0.0.1`nport=$DbPort`nuser=$user`npassword=$password`nprotocol=TCP`ndefault-character-set=utf8mb4`n"
}
$ready = $false
for ($i=0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    if ($dbProcess.HasExited) { throw 'MySQL exited. Check .runtime/mysql-error.log.' }
    if (Test-LocalPort $DbPort) {
        & $MysqlExe "--defaults-extra-file=$MysqlLocalRoot\mysql-app.cnf" --batch --skip-column-names -e 'SELECT 1' linli_governance 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready=$true; break }
    }
}
if (!$ready) { throw 'MySQL did not become ready. Check .runtime/mysql-error.log.' }
Remove-Item -LiteralPath $bootstrap
Write-Utf8 $marker 'Initialized; do not delete while mysql-data exists.'
Write-Output "MySQL ready on 127.0.0.1:$DbPort; database linli_governance. Credentials in .local (ignored by Git)."
