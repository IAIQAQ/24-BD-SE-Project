# 小芽口算 · 正式工程

Android 首发，Vue + TypeScript + Capacitor 共用前端，Spring Boot + MyBatis + MySQL 提供统一后端。现阶段实现服务健康检查、学生登录、服务端持久会话、退出与资料隔离；组卷、练习和结果功能按上级目录《首版正式开发实施计划.md》继续实施。

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
2. 首次编译或修改后端后，运行 `. ./scripts/runtime.ps1`，再运行 `./backend/mvnw.cmd -f backend/pom.xml -s backend/.mvn/settings.xml package`。
3. 另开终端运行 `./scripts/start-backend.ps1`，服务位于 `https://localhost:18443`。
4. 浏览器调试：进入 `frontend`，运行 `npm ci`、`npm run dev`，打开 `http://localhost:5173`。代理校验本机后端证书，未关闭证书校验。
5. Android 构建：回到工程根目录，运行 `./scripts/build-android.ps1`，得到 `delivery/mathdrill-foundation-debug.apk` 和独立测试 APK。
6. 手机打开 USB 调试、允许电脑调试与 APK 安装；通过现有 adb 安装两包，再运行 `./scripts/test-device.ps1 -Mode full`。可选模式 `ui` 验证实际页面按钮，`resume` 验证保留的登录会话。

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

这是本机环境验证包，APK 内含打包后的页面，连接电脑的 HTTPS 测试服务，需 USB 连接和端口转发。debug 配置仅为 localhost 信任本项目证书，release 不包含此信任。自签名证书有效期 60 天；不把证书安装到系统信任库。

便携工具和数据库数据目前位于临时目录，系统清理后需恢复，正式交付前须迁移到稳定路径并做备份。这份工程没有包含公共服务器、公开域名或正式签名配置；浏览器 localhost 测试不代表公网 Web 已验收。原有交互原型与个人报告保持独立。
