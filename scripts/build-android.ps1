. "$PSScriptRoot/runtime.ps1"
$frontend=Join-Path $ProjectRoot 'frontend'
Push-Location $frontend
try {
    & npm.cmd run build
    if($LASTEXITCODE -ne 0){throw '前端构建失败。'}
    & npx.cmd cap sync android
    if($LASTEXITCODE -ne 0){throw 'Android 资源同步失败。'}
    $raw=Join-Path $frontend 'android/app/src/debug/res/raw'
    New-Item -ItemType Directory -Path $raw -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $PrivateRoot 'dev-server.pem') -Destination (Join-Path $raw 'dev_server.pem')
    $stage=[IO.Path]::GetFullPath($toolchain.androidStage)
    if($stage -ne [IO.Path]::GetFullPath('C:\Users\LENOVO\AppData\Local\Temp\mathdrill-android')){throw '构建位置不在项目专用临时目录。'}
    New-Item -ItemType Directory -Path $stage -Force | Out-Null
    & robocopy $frontend $stage /MIR /XD node_modules .gradle build /XF local.properties /NFL /NDL /NJH /NJS /NP | Out-Null
    if($LASTEXITCODE -ge 8){throw '构建源码复制失败。'}
    & robocopy "$frontend/node_modules/@capacitor/android" "$stage/node_modules/@capacitor/android" /MIR /XD .gradle build /NFL /NDL /NJH /NJS /NP | Out-Null
    if($LASTEXITCODE -ge 8){throw 'Capacitor 构建依赖复制失败。'}
    Set-Content -LiteralPath "$stage/android/local.properties" -Value "sdk.dir=$($toolchain.sdkRoot)" -Encoding ascii
    & "$stage/android/gradlew.bat" -p "$stage/android" :app:assembleDebug :app:assembleDebugAndroidTest --console=plain --no-daemon
    if($LASTEXITCODE -ne 0){throw 'Android 构建失败。'}
    $delivery=Join-Path $ProjectRoot 'delivery'
    New-Item -ItemType Directory -Path $delivery -Force | Out-Null
    Copy-Item -LiteralPath "$stage/android/app/build/outputs/apk/debug/app-debug.apk" -Destination "$delivery/mathdrill-foundation-debug.apk"
    Copy-Item -LiteralPath "$stage/android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk" -Destination "$delivery/mathdrill-foundation-test.apk"
} finally {Pop-Location}
