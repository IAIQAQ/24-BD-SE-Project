. "$PSScriptRoot/runtime.ps1" -Database mathdrill_demo
$env:MATHDRILL_SEED_ENABLED='false'
& "$env:JAVA_HOME/bin/java.exe" -jar (Join-Path $ProjectRoot 'delivery/mathdrill-s01-backend.jar')
if($LASTEXITCODE -ne 0){throw '课堂体验后端未能正常运行。'}
