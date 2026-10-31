param([ValidatePattern('^[a-zA-Z0-9:._-]+$')][string]$Serial='emulator-5554',[switch]$Install,[ValidateSet('i1','i2','i3','s01')][string]$Version='s01',[switch]$IncludeRoles,[switch]$IncludeLearning,[switch]$IncludeNormal)
. "$PSScriptRoot/runtime.ps1"
$adb='D:/Huawei/ArkuiXToolchain/android-sdk/platform-tools/adb.exe'
$credentialFile=Join-Path $PrivateRoot 'i1-probe.json'
$remoteFile='/sdcard/Android/data/cn.edu.hust.mathdrill/files/i1-probe.json'
try {
    if($Install){
        & $adb -s $Serial install -r "$ProjectRoot/delivery/mathdrill-$Version-debug.apk"
        if($LASTEXITCODE -ne 0){throw 'APP 安装失败。'}
        & $adb -s $Serial install -r "$ProjectRoot/delivery/mathdrill-$Version-test.apk"
        if($LASTEXITCODE -ne 0){throw '独立测试包安装失败。'}
    }
    $probe=@{password=$runtime.seedPassword;adminPassword=$runtime.adminPassword}
    if($IncludeNormal){$fixture=Get-Content -LiteralPath (Join-Path $PrivateRoot 's01-ui-fixture.json') -Raw | ConvertFrom-Json;if($fixture.database -ne 'mathdrill_test'){throw 'S01样例必须属于专用测试库。'};$probe.s01Points=$fixture.points;$probe.expectedAvailable=$fixture.expectedAvailable}
    $probe | ConvertTo-Json -Depth 5 -Compress | Set-Content -LiteralPath $credentialFile -Encoding utf8NoBOM
    & $adb -s $Serial reverse tcp:18443 tcp:18443
    if($LASTEXITCODE -ne 0){throw '设备端口转发失败。'}
    & $adb -s $Serial shell mkdir -p /sdcard/Android/data/cn.edu.hust.mathdrill/files
    & $adb -s $Serial push $credentialFile $remoteFile | Out-Null
    if($LASTEXITCODE -ne 0){throw '测试配置传输失败。'}
    $classNames=@('cn.edu.hust.mathdrill.I1FlowTest');if($IncludeRoles){$classNames+='cn.edu.hust.mathdrill.I2RolesFlowTest'};if($IncludeLearning){$classNames+='cn.edu.hust.mathdrill.I3ReplayFlowTest';$classNames+='cn.edu.hust.mathdrill.I3LearningFlowTest'};if($IncludeNormal){$classNames+='cn.edu.hust.mathdrill.S01NormalFlowTest'}
    $classes=$classNames -join ','

    $command='am instrument -w -r -e class cn.edu.hust.mathdrill.I1FlowTest cn.edu.hust.mathdrill.test/androidx.test.runner.AndroidJUnitRunner & sleep 4; am start -n cn.edu.hust.mathdrill/.MainActivity; wait'
    $command=$command.Replace('cn.edu.hust.mathdrill.I1FlowTest',$classes)
    $result=& $adb -s $Serial shell $command
    $result | Set-Content -LiteralPath (Join-Path $PrivateRoot "$Version-device-flow.log")
    $expected="OK \($($classNames.Count) tests?\)"
    if(($result -join "`n") -notmatch $expected){throw '设备完整流程未通过，请查看 .local 中对应版本的设备日志。'}
    Write-Output "DEVICE_FLOW $Version PASS (roles=$IncludeRoles)"
} finally {
    & $adb -s $Serial shell rm -f $remoteFile
    if(Test-Path -LiteralPath $credentialFile){Remove-Item -LiteralPath $credentialFile}
}
