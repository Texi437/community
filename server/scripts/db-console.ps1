param([switch]$Admin)
. "$PSScriptRoot\env.ps1"
$account = if ($Admin) { 'root' } else { 'app' }
& $MysqlExe "--defaults-extra-file=$MysqlLocalRoot\mysql-$account.cnf" linli_governance
