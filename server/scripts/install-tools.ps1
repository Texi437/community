$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$downloadDir = Join-Path $root '.tools\downloads'
$toolsDir = Join-Path $root '.tools'
New-Item -ItemType Directory -Force -Path $downloadDir,$toolsDir | Out-Null
$manifest = Get-Content "$PSScriptRoot\config\tools.json" -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($tool in $manifest) {
    if (Test-Path (Join-Path $toolsDir $tool.executable)) {
        Write-Output "$($tool.name) already installed."
        continue
    }
    $zip = Join-Path $downloadDir ($tool.name + '.zip')
    if (!(Test-Path $zip)) {
        Write-Output "Downloading $($tool.name)..."
        & "$env:SystemRoot\System32\curl.exe" -fL --retry 2 --connect-timeout 20 --max-time 600 --silent --show-error $tool.url -o "$zip.partial"
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $($tool.name)" }
        Move-Item -LiteralPath "$zip.partial" -Destination $zip
    }
    if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash -ne $tool.sha256) { throw "Checksum mismatch: $zip" }
    Expand-Archive -LiteralPath $zip -DestinationPath $toolsDir -Force
}
if ($root -match '[^\x00-\x7F]') {
    $alias = Join-Path ([System.IO.Path]::GetPathRoot($root)) 'linli-dev'
    if (Test-Path $alias) {
        $item = Get-Item -LiteralPath $alias
        if ($item.LinkType -ne 'Junction' -or $item.Target -ne $root) { throw "Path $alias already belongs to something else; no changes made." }
    } else {
        New-Item -ItemType Junction -Path $alias -Target $root | Out-Null
    }
}
Write-Output 'Tools ready. Next: scripts/init-db.ps1, then scripts/configure-ide.ps1.'
