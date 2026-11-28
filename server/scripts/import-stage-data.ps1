. "$PSScriptRoot\env.ps1"
$accountFile = Join-Path $LocalRoot 'stage-accounts.json'
if (!(Test-Path -LiteralPath $accountFile)) {
    $accounts = foreach ($name in @('resident','neighbor','worker','admin','authorized')) {
        @{username=$name; password=[Guid]::NewGuid().ToString('N')}
    }
    Write-Utf8 $accountFile ($accounts | ConvertTo-Json)
}
$accounts = Get-Content -LiteralPath $accountFile -Raw -Encoding UTF8 | ConvertFrom-Json
$properties = Join-Path $LocalRoot 'stage-import.properties'
$lines = @("url=$env:DB_URL", "user=$env:DB_USERNAME", "password=$env:DB_PASSWORD")
foreach ($account in $accounts) { $lines += "$($account.username)=$($account.password)" }
Write-Utf8 $properties ($lines -join "`n")
try {
    $jars = foreach ($artifact in @('org\springframework\security\spring-security-crypto','org\springframework\spring-jcl','com\mysql\mysql-connector-j')) {
        $jar = Get-ChildItem (Join-Path "$ProjectRoot\.m2\repository" $artifact) -Filter '*.jar' -Recurse | Where-Object {$_.Name -notmatch '(sources|javadoc)'} | Sort-Object FullName -Descending | Select-Object -First 1
        if (!$jar) { throw 'Build the backend first.' }
        $jar.FullName
    }
    & $JavaExe '-Dfile.encoding=UTF-8' --class-path ($jars -join ';') "$PSScriptRoot\java\ImportStageData.java" $properties "$ProjectRoot\database\seeds\stage-data.sql"
    if ($LASTEXITCODE -ne 0) { throw 'Import failed; transaction rolled back. Existing records were not overwritten.' }
} finally { Remove-Item -LiteralPath $properties -ErrorAction SilentlyContinue }
Write-Output 'Credentials are stored only in .local/stage-accounts.json; do not include that file in source delivery.'
