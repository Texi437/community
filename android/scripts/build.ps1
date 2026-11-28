param([string]$JavaHome = $env:JAVA_HOME, [string]$BackendUrl = '', [string]$BuildRoot = [IO.Path]::GetTempPath())
$ErrorActionPreference = 'Stop'
if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
$projectPath = Split-Path $PSScriptRoot -Parent
$buildPath = $projectPath
if ($projectPath -match '[^\x00-\x7F]') {
    # View binding resolves drive aliases back to Chinese paths; use physical ASCII paths.
    $hash = [System.Security.Cryptography.SHA256]::Create()
    try { $id = ([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes($projectPath)))).Replace('-', '').Substring(0, 12) }
    finally { $hash.Dispose() }
    $buildPath = [IO.Path]::GetFullPath((Join-Path $BuildRoot "linli-android-$id"))
    if ($buildPath -match '[^\x00-\x7F]') { throw 'Use -BuildRoot with an ASCII directory for Android builds.' }
    New-Item -ItemType Directory -Path $buildPath -Force | Out-Null
    foreach ($relative in 'app\src', 'gradle') {
        $target = [IO.Path]::GetFullPath((Join-Path $buildPath $relative))
        if (!$target.StartsWith([IO.Path]::GetFullPath($buildPath) + '\')) { throw 'Invalid build staging path.' }
        if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
        New-Item -ItemType Directory -Path (Split-Path $target -Parent) -Force | Out-Null
        Copy-Item -LiteralPath (Join-Path $projectPath $relative) -Destination $target -Recurse
    }
    foreach ($relative in 'gradlew', 'gradlew.bat', 'build.gradle', 'settings.gradle', 'gradle.properties', 'local.properties', 'app\build.gradle') {
        $source = Join-Path $projectPath $relative
        $target = Join-Path $buildPath $relative
        if (Test-Path -LiteralPath $source) { Copy-Item -LiteralPath $source -Destination $target -Force }
        elseif ($relative -eq 'local.properties') {
            if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Force }
        } else { throw "Missing build input: $relative" }
    }
}
Push-Location $buildPath
try {
    $tasks = @(':app:assembleDebug', ':app:lintDebug', '--console=plain', '--no-daemon')
    if ($BackendUrl) { $tasks += "-PbackendUrl=$BackendUrl" }
    & .\gradlew.bat @tasks
    $buildExit = $LASTEXITCODE
} finally { Pop-Location }
if ($buildPath -ne $projectPath) {
    $outputRoot = Join-Path $projectPath 'app\build'
    New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null
    foreach ($relative in 'outputs', 'reports') {
        $source = Join-Path $buildPath "app\build\$relative"
        if (($relative -eq 'reports' -or $buildExit -eq 0) -and (Test-Path -LiteralPath $source)) {
            Copy-Item -LiteralPath $source -Destination $outputRoot -Recurse -Force
        }
    }
}
exit $buildExit
