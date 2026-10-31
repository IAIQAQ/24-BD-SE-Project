# 小芽口算最终启动说明

> 2026-10-30更新：本机正常体验改用独立mathdrill_demo及新版APP，优先阅读[课堂体验启动说明](startup-classroom.md)。下文保留S01验收库历史启动方式，不作为当前体验默认入口。

适用版本：S01。面向当前已配置的Windows电脑；这不是公网部署说明。请在mathdrill目录打开PowerShell 7。

## 1. 启动前

- 本机保留 `.local/toolchain.json`、`.local/runtime.json`、localhost证书和既有MySQL数据；登录信息见 `.local/测试账号.md`。这些私有文件不上传GitHub。
- 使用现有 `delivery/mathdrill-s01-backend.jar` 和 `mathdrill-s01-debug.apk`，无需重新构建。
- 推荐用 `mathdrill_test` 演示，里面已有S01隔离账号和多点样例。不要同时运行开发库与测试库的两份后端争用18443端口。

## 2. 启动三个窗口

窗口一：启动项目数据库。已有本项目13306实例正常运行时跳过，不重复启动。

```powershell
./scripts/start-database.ps1
```

窗口二：用冻结S01后端连接测试库。

```powershell
. ./scripts/runtime.ps1 -Database mathdrill_test
& "$env:JAVA_HOME/bin/java.exe" -jar ./delivery/mathdrill-s01-backend.jar
```

窗口三：启动Web界面。

```powershell
cd frontend
npm run dev
```

打开终端显示的本机地址，通常是 `http://localhost:5173`。如首次准备本机依赖且node_modules不存在，先在frontend运行 `npm ci`；已有依赖不必重装。正常启动后应出现“小芽口算”登录页，登录后能加载真实记录。

需要查看原开发库记录时，先停止窗口二的本项目后端，再将其Database参数改为 `mathdrill_dev`。不导入SQL，不运行造数脚本，不改原数据目录。

## 3. Android模拟器或手机

APP也依赖窗口一、二；打开本机Web不是APP运行的必要条件。先启动既有模拟器或连接允许调试的手机：

```powershell
$adb='D:/Huawei/ArkuiXToolchain/android-sdk/platform-tools/adb.exe'
& $adb devices -l
# 把下面序列号替换为上一步实际设备；当前雷电实例通常为emulator-5554。
& $adb -s emulator-5554 reverse tcp:18443 tcp:18443
# 已安装同版S01时跳过安装。
& $adb -s emulator-5554 install -r ./delivery/mathdrill-s01-debug.apk
```

打开“小芽口算”，使用与Web相同的账号。当前APK只连接电脑的HTTPS测试服务，不能在离开这台电脑后当作公网应用使用；不要关闭证书校验来绕过连接问题。

## 4. 账号与检查

- S01隔离演示：学生 `s01_ui_student`、家长 `s01_ui_parent`；已建立演示关联，沿用本机 `.local/runtime.json` 中的seedPassword。只在私有资料中取用密码，不写入说明或截图。
- 既有基础账号：`student_a`、`student_b`、`parent_demo`、`admin`。家庭关联以当前实际状态为准，管理员使用独立密码。
- 测试库S01样例为一年级“ S01验收A-093352 ”与“ S01验收B-d0a0e6 ”，可用2题和3题；名称/ID也保存在 `.local/s01-ui-fixture.json`。如果本机环境后来改变，按实际目录核对，不自动重建或清理数据。
- 演示操作会正常产生新的测试练习记录。不要为现场演示运行 `test-backend.ps1` 或设备全量测试脚本，它们另有测试写入用途。

## 5. 结束与常见问题

- 后端连不上：检查窗口一、二及13306/18443目标；APP还要检查设备在线与端口转发。
- 页面正常但登录失败：确认服务连接的是所选库，并从私有资料核对账号；不要重置已有密码或重复造家庭关系。
- 开始新卷提示不足：调整所选知识点分配，不跨知识点补题；S01样例按A2+B3配置。
- 演示结束先退出账号，再在自己启动的Web/后端窗口按Ctrl+C停止；数据库保留原数据，不删除目录。不要批量结束所有Java/MySQL进程。

GitHub只保存源码、测试、脚本及技术文档，`.local`、数据库转储、证书和delivery包均不随源码上传。换电脑需要另行准备这些运行条件，克隆仓库不等于已经完成环境安装。公网部署待服务器确定后单独处理。
