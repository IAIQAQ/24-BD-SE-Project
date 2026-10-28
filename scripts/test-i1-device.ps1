param([ValidatePattern('^[a-zA-Z0-9:._-]+$')][string]$Serial='emulator-5554',[switch]$Install)
. "$PSScriptRoot/runtime.ps1"
$adb='D:/Huawei/ArkuiXToolchain/android-sdk/platform-tools/adb.exe'
$credentialFile=Join-Path $PrivateRoot 'i1-probe.json'
$remoteFile='/sdcard/Android/data/cn.edu.hust.mathdrill/files/i1-probe.json'
try {
    if($Install){
        & $adb -s $Serial install -r "$ProjectRoot/delivery/mathdrill-i1-debug.apk"
        if($LASTEXITCODE -ne 0){throw 'APP 安装失败。'}
        & $adb -s $Serial install -r "$ProjectRoot/delivery/mathdrill-i1-test.apk"
        if($LASTEXITCODE -ne 0){throw '独立测试包安装失败。'}
    }
    @{password=$runtime.seedPassword} | ConvertTo-Json -Compress | Set-Content -LiteralPath $credentialFile -Encoding utf8NoBOM
    & $adb -s $Serial reverse tcp:18443 tcp:18443
    if($LASTEXITCODE -ne 0){throw '设备端口转发失败。'}
    & $adb -s $Serial shell mkdir -p /sdcard/Android/data/cn.edu.hust.mathdrill/files
    & $adb -s $Serial push $credentialFile $remoteFile | Out-Null
    if($LASTEXITCODE -ne 0){throw '测试配置传输失败。'}
    $command='am instrument -w -r -e class cn.edu.hust.mathdrill.I1FlowTest cn.edu.hust.mathdrill.test/androidx.test.runner.AndroidJUnitRunner & sleep 4; am start -n cn.edu.hust.mathdrill/.MainActivity; wait'
    $result=& $adb -s $Serial shell $command
    $result | Set-Content -LiteralPath (Join-Path $PrivateRoot 'i1-device-flow.log')
    if(($result -join "`n") -notmatch 'OK \(1 test\)'){throw '设备完整流程未通过，请查看 .local/i1-device-flow.log。'}
    Write-Output 'I1_DEVICE_FLOW PASS'
} finally {
    & $adb -s $Serial shell rm -f $remoteFile
    if(Test-Path -LiteralPath $credentialFile){Remove-Item -LiteralPath $credentialFile}
}
