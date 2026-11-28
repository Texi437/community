$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ToolRoot = Join-Path $ProjectRoot '.tools'
$RuntimeRoot = Join-Path $ProjectRoot '.runtime'
$LocalRoot = Join-Path $ProjectRoot '.local'
$env:UPLOAD_DIR = Join-Path $RuntimeRoot 'uploads'
$jdkDir = Get-ChildItem $ToolRoot -Directory -Filter 'jdk-*' | Select-Object -First 1
$mavenDir = Get-ChildItem $ToolRoot -Directory -Filter 'apache-maven-*' | Select-Object -First 1
$mysqlDir = Get-ChildItem $ToolRoot -Directory -Filter 'mysql-*-winx64' | Select-Object -First 1
if (!$jdkDir -or !$mavenDir -or !$mysqlDir) { throw 'Missing local tools. Run scripts/install-tools.ps1 first.' }
$env:JAVA_HOME = $jdkDir.FullName
$env:MAVEN_HOME = $mavenDir.FullName
$MysqlProjectRoot = $ProjectRoot
if ($ProjectRoot -match '[^\x00-\x7F]') {
    $MysqlProjectRoot = Join-Path ([System.IO.Path]::GetPathRoot($ProjectRoot)) 'linli-dev'
    $aliasItem = Get-Item -LiteralPath $MysqlProjectRoot -ErrorAction SilentlyContinue
    if (!$aliasItem -or $aliasItem.LinkType -ne 'Junction' -or $aliasItem.Target -ne $ProjectRoot) {
        throw 'MySQL needs an ASCII path. Run install-tools.ps1 to create the linli-dev directory junction.'
    }
}
$MysqlHome = Join-Path $MysqlProjectRoot ('.tools\' + $mysqlDir.Name)
$MysqlLocalRoot = Join-Path $MysqlProjectRoot '.local'
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$MysqlHome\bin;$env:Path"
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai'
$JavaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
$MavenExe = Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'
$MysqlExe = Join-Path $MysqlHome 'bin\mysql.exe'
$MysqldExe = Join-Path $MysqlHome 'bin\mysqld.exe'
$MysqlAdminExe = Join-Path $MysqlHome 'bin\mysqladmin.exe'
$LocalConfig = Join-Path $LocalRoot 'environment.json'
if (Test-Path $LocalConfig) {
    $DevConfig = Get-Content $LocalConfig -Raw -Encoding UTF8 | ConvertFrom-Json
    $env:DB_URL = "jdbc:mysql://127.0.0.1:$($DevConfig.dbPort)/linli_governance?characterEncoding=UTF-8&connectionTimeZone=%2B08:00&forceConnectionTimeZoneToSession=true&sslMode=DISABLED&allowPublicKeyRetrieval=true"
    $env:DB_USERNAME = 'linli_app'
    $env:DB_PASSWORD = $DevConfig.appPassword
    $env:SERVER_PORT = [string]$DevConfig.serverPort
    $env:SERVER_ADDRESS = $DevConfig.serverAddress
}

function Test-LocalPort([int]$Port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $task = $client.ConnectAsync('127.0.0.1', $Port)
        if (!$task.Wait(500)) { return $false }
        return $client.Connected
    } catch { return $false } finally { $client.Dispose() }
}

function Write-Utf8([string]$Path, [string]$Content) {
    [System.IO.File]::WriteAllText($Path, $Content, (New-Object System.Text.UTF8Encoding($false)))
}
