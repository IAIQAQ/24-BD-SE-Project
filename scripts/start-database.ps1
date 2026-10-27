. "$PSScriptRoot/runtime.ps1"
& "$($toolchain.mysqlHome)/bin/mysqld.exe" "--basedir=$($toolchain.mysqlHome)" "--datadir=$($toolchain.mysqlData)" --bind-address=127.0.0.1 --port=13306 --mysqlx=0 --console
if($LASTEXITCODE -ne 0){throw '项目隔离数据库未能启动。'}
