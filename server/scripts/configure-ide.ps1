. "$PSScriptRoot\env.ps1"
if (!(Test-Path $LocalConfig)) { throw 'Run init-db.ps1 first.' }
$jre = $env:JAVA_HOME.Replace('\','/')
New-Item -ItemType Directory -Force "$ProjectRoot\.vscode","$ProjectRoot\.run" | Out-Null
$settings = [ordered]@{
    'files.encoding' = 'utf8'
    'java.configuration.runtimes' = @(@{name='JavaSE-17';path=$env:JAVA_HOME;default=$true})
    'maven.executable.path' = $MavenExe
    'java.configuration.maven.userSettings' = "$LocalRoot\maven-settings.xml"
    'java.import.maven.userSettings' = "$LocalRoot\maven-settings.xml"
}
Write-Utf8 "$ProjectRoot\.vscode\settings.json" ($settings | ConvertTo-Json -Depth 5)
Write-Utf8 "$LocalRoot\dev.env" "DB_URL=$env:DB_URL`nDB_USERNAME=$env:DB_USERNAME`nDB_PASSWORD=$env:DB_PASSWORD`nSERVER_PORT=$env:SERVER_PORT`nSERVER_ADDRESS=$env:SERVER_ADDRESS`nUPLOAD_DIR=$env:UPLOAD_DIR`n"
Write-Utf8 "$LocalRoot\application-local.properties" "spring.datasource.url=$env:DB_URL`nspring.datasource.username=$env:DB_USERNAME`nspring.datasource.password=$env:DB_PASSWORD`nserver.port=$env:SERVER_PORT`nserver.address=$env:SERVER_ADDRESS`n"
# Use the verified ASCII junction to avoid Java properties encoding differences.
$uploadPropertiesPath = (Join-Path $MysqlProjectRoot '.runtime\uploads').Replace('\','/')
Add-Content -LiteralPath "$LocalRoot\application-local.properties" -Encoding UTF8 -Value ("linli.storage.root=" + $uploadPropertiesPath)
$repo = [System.Security.SecurityElement]::Escape("$ProjectRoot\.m2\repository")
Write-Utf8 "$LocalRoot\maven-settings.xml" "<settings xmlns=`"http://maven.apache.org/SETTINGS/1.2.0`"><localRepository>$repo</localRepository></settings>"
$jreXml = [System.Security.SecurityElement]::Escape($jre)
$ideaTemplate = @'
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Linli Backend" type="Application" factoryName="Application">
    <option name="ALTERNATIVE_JRE_PATH" value="__JRE__" />
    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="true" />
    <option name="MAIN_CLASS_NAME" value="cn.linli.LinliApplication" />
    <module name="linli-backend" />
    <option name="VM_PARAMETERS" value="-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai" />
    <option name="WORKING_DIRECTORY" value="$PROJECT_DIR$" />
    <envs><env name="SPRING_CONFIG_ADDITIONAL_LOCATION" value="file:.local/application-local.properties" /></envs>
    <method v="2"><option name="Make" enabled="true" /></method>
  </configuration>
</component>
'@
Write-Utf8 "$ProjectRoot\.run\Linli Backend.run.xml" ($ideaTemplate.Replace('__JRE__',$jreXml))
Write-Output 'Configured IDEA run target, VS Code JDK/Maven, and private local environment files.'
