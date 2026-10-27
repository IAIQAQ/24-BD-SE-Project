. "$PSScriptRoot/runtime.ps1" -Database mathdrill_test
Push-Location (Join-Path $ProjectRoot 'backend')
try {
    & "$($toolchain.mavenHome)/bin/mvn.cmd" -s .mvn/settings.xml -B test
    if($LASTEXITCODE -ne 0){throw '后端测试未通过。'}
} finally {Pop-Location}
