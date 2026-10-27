. "$PSScriptRoot/runtime.ps1" -Database mathdrill_dev
& "$env:JAVA_HOME/bin/java.exe" -jar (Join-Path $ProjectRoot 'backend/target/mathdrill-0.1.0.jar')
if($LASTEXITCODE -ne 0){throw '测试后端未能正常运行。'}
