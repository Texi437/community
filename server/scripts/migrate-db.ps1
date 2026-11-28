param([string]$Database = 'linli_governance', [switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\env.ps1"
& "$PSScriptRoot\start-db.ps1"
$driver = Get-ChildItem "$ProjectRoot\.m2\repository\com\mysql\mysql-connector-j" -Filter '*.jar' -Recurse | Select-Object -First 1
if (!$driver) { throw 'MySQL JDBC driver missing; build the backend first.' }
$mode = if ($CheckOnly) { 'check' } else { 'apply' }
$env:LINLI_MIGRATION_PASSWORD = $DevConfig.rootPassword
try {
    & $JavaExe '-Dfile.encoding=UTF-8' --class-path $driver.FullName "$PSScriptRoot\java\SchemaMigration.java" ([string]$DevConfig.dbPort) $Database "$ProjectRoot\database" $mode
    if ($LASTEXITCODE -ne 0) { throw 'Migration stopped; inspect the reported version and preserve all existing data.' }
} finally { Remove-Item Env:LINLI_MIGRATION_PASSWORD -ErrorAction SilentlyContinue }

