# 小芽口算 · 正式工程

Android 首发，Vue + TypeScript + Capacitor 共用前端，Spring Boot + MyBatis + MySQL 提供统一后端。现已在 Android14 雷电模拟器和浏览器跑通真实登录、120题范围组卷、作答保存恢复、提交、结果和错题详情。当前是内部 I1 主流程版本，完整异常与真机验收状态见 `docs/acceptance-i1.md`。

## 目录

- `frontend/`：Vue 源码和 Capacitor Android 工程。
- `backend/`：认证接口、数据库迁移和集成测试。
- `scripts/`：本机启动、构建和真机验证。
- `docs/`：环境、接口与验证记录。
- `.local/`：本机私有配置、证书和日志，已排除版本管理。
- `delivery/`：生成的 APK，已排除版本管理。

## 在本机继续开发

用 PowerShell 7 在本目录操作。Node 24.20.0 沿用既有安装。JDK21、Maven3.9.16、MySQL8.4.13 和 API36 SDK 使用 `C:/Users/LENOVO/AppData/Local/Temp/mathdrill-toolchain` 的隔离工具，不修改系统 PATH；Android 构建暂存于同级 `mathdrill-android`。具体路径从 `.local/toolchain.json` 读取。

1. 单独打开终端运行 `./scripts/start-database.ps1`。数据库仅监听 `127.0.0.1:13306`；已经启动时不重复启动。
2. 首次编译或修改后端后，先停止本项目旧后端进程，再运行 `. ./scripts/runtime.ps1`，然后运行 `./backend/mvnw.cmd -f backend/pom.xml -s backend/.mvn/settings.xml package`。Windows 下运行中的 JAR 可能被锁定，不能边运行边覆盖打包。
3. 另开终端运行 `./scripts/start-backend.ps1`，服务位于 `https://localhost:18443`。
4. 浏览器调试：进入 `frontend`，运行 `npm ci`、`npm run dev`，打开 `http://localhost:5173`。代理校验本机后端证书，未关闭证书校验。
5. Android 构建：回到工程根目录，运行 `./scripts/build-android.ps1`，得到 `delivery/mathdrill-i1-debug.apk` 和 `mathdrill-i1-test.apk`。旧环境验证包保留。
6. 无USB时使用已安装雷电：运行 `D:/leidian/LDPlayer14/ldconsole.exe launch --index 0`，启动完成后运行 `D:/Huawei/ArkuiXToolchain/android-sdk/platform-tools/adb.exe connect 127.0.0.1:5555`。然后运行 `./scripts/test-i1-device.ps1 -Serial emulator-5554 -Install`，完成真实页面流程。脚本临时传入密码并在结束时清除。
7. 以后手机USB可用时允许调试和安装，使用 `adb devices -l` 中的手机序列号作为 `-Serial` 参数。模拟器通过不代表新版本已在手机验收。

测试账号为 `student_a`、`student_b`，密码保存在 `.local/runtime.json` 的 `seedPassword` 字段。不要把此文件发到公开仓库或嵌入 APK。启动时仅初始化不存在的测试账号，不重置已有密码。

## 验证命令

```powershell
./scripts/test-backend.ps1
cd frontend
npm test -- --run
npm run build
```

后端测试专用库为 `mathdrill_test`，开发库为 `mathdrill_dev`；测试不连接其他项目数据库。真机脚本临时传入测试配置，结束后清除手机中的临时文件，不记录密码。

## 适用范围

这是本机内部测试包，APK 内含打包后的页面，连接电脑的 HTTPS 测试服务；模拟器无需USB，实体手机需调试连接。使用 adb reverse 转发18443端口。debug 配置仅为 localhost 信任本项目证书，release 不包含此信任。自签名证书有效期60天，不安装到系统信任库。

页面操作：登录后点“开始新练习”，选择1—100题；作答可自动/手动保存和“保存退出”；重登后从记录继续，已有编辑会话时明确选择“在此继续”；结束检查漏答后提交，再看正式结果和错题详情。其他端接管后旧端禁止写入。提交结果不明时先核对。

`delivery/manifest-i1.json` 记录APK、测试包、后端JAR与Web静态包的SHA256。最新私有数据库备份为 `.local/archive/20261028-i1-flow.sql`；此文件含会话和密码哈希，不得公开。已导出，尚未完成新库导入恢复验收。

便携工具和数据库数据目前位于临时目录，系统清理后需恢复，正式交付前须迁移到稳定路径并做备份。这份工程没有包含公共服务器、公开域名或正式签名配置；浏览器 localhost 测试不代表公网 Web 已验收。原有交互原型与个人报告保持独立。
