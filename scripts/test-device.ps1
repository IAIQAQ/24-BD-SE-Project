param([ValidateSet('full','ui','resume')] [string]$Mode='full')
. "$PSScriptRoot/runtime.ps1"
$adb='D:/Huawei/ArkuiXToolchain/android-sdk/platform-tools/adb.exe'
$credentialFile=Join-Path $PrivateRoot 'auth-probe.json'
$remoteFile='/sdcard/Android/data/cn.edu.hust.mathdrill/files/auth-probe.json'
try {
    @{password=$runtime.seedPassword} | ConvertTo-Json -Compress | Set-Content -LiteralPath $credentialFile -Encoding utf8NoBOM
    & $adb reverse tcp:18443 tcp:18443
    if($LASTEXITCODE -ne 0){throw 'USB 调试连接失败。'}
    & $adb shell mkdir -p /sdcard/Android/data/cn.edu.hust.mathdrill/files
    & $adb push $credentialFile $remoteFile | Out-Null
    if($LASTEXITCODE -ne 0){throw '测试配置传输失败。'}
    # 此手机阻止测试的后台 Activity 启动；用 shell 显式启动同一目标，不改变安全设置。
    $command="am instrument -w -r -e class cn.edu.hust.mathdrill.AuthProbeTest -e mode $Mode cn.edu.hust.mathdrill.test/androidx.test.runner.AndroidJUnitRunner & sleep 4; am start -n cn.edu.hust.mathdrill/.MainActivity; wait"
    $result=& $adb shell $command
    $result | Set-Content -LiteralPath (Join-Path $PrivateRoot "android-probe-$Mode.log")
    if(($result -join "`n") -notmatch 'OK \(1 test\)'){throw "真机 $Mode 测试未通过，请查看 .local 中的日志。"}
    Write-Output "ANDROID_$Mode PASS"
} finally {
    & $adb shell rm -f $remoteFile
    if(Test-Path -LiteralPath $credentialFile){Remove-Item -LiteralPath $credentialFile}
}
