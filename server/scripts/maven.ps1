. "$PSScriptRoot\env.ps1"
& $MavenExe -B "-Dmaven.repo.local=$ProjectRoot\.m2\repository" -f "$ProjectRoot\backend\pom.xml" @args
if ($LASTEXITCODE -ne 0) { throw "Maven failed: exit $LASTEXITCODE" }
