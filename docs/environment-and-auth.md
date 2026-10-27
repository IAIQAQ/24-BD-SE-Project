# 环境与认证验证记录

2026-10-27。阶段：T01 与 T03 开工探针。原有个人报告和交互原型独立保留。

## 已锁定环境

| 项目 | 本轮实际使用 |
| --- | --- |
| Node | 24.20.0，复用 |
| Vue / TypeScript / Vite | 3.5.44 / 5.9.x / 8.3.5，package-lock 锁定 |
| Capacitor | core / android / cli 均 8.5.3 |
| Java | Temurin 21.0.12.1+1，便携隔离 |
| Maven | 3.9.16；官方 wrapper 3.3.4，only-script |
| Spring Boot / MyBatis Starter | 3.5.16 / 3.0.5 |
| MySQL | 8.4.13，127.0.0.1:13306，独立开发库与测试库 |
| Android | compile/target 36，min 24，build-tools 36.0.0 |
| Gradle / AGP | 8.14.3 / 8.13.0 |
| 真机 | 用户称 iQOO Z10 Turbo；系统型号 V2452A，Android16/API36 |

JDK、Maven、SDK 组件和 MySQL 取自官方渠道并校验下载摘要。沿用原 SDK 的已有许可文件，未修改 DevEco、全局 PATH 或其他项目。完整运行路径从 `.local/toolchain.json` 读取。工具与数据库当前放在项目专用临时目录，后续正式交付前需要迁移到稳定目录并备份。

## 实际网络与认证路线

- 后端仅监听 `127.0.0.1:18443`，HTTPS，证书包含 localhost 和 127.0.0.1，60 天有效。
- APP 页面为 APK 内打包资源，来源为 `https://localhost`；通过 USB `adb reverse tcp:18443 tcp:18443` 访问电脑服务。
- debug 网络配置只为 localhost 信任本项目证书，未放开明文、未关闭证书验证、未安装系统根证书；release 无此额外信任。
- 浏览器本机调试来源 `http://localhost:5173`，由 Vite 严格校验证书的代理访问 HTTPS 后端。这不是公网 Web 部署验收。
- 使用 Spring Security 标准密码编码与 Spring Session JDBC 持久会话。Cookie 为 `MATHDRILL_SESSION`，Secure、HttpOnly、SameSite=None；客户端不读取 Cookie，不把密码或会话凭证存入 localStorage/Preferences。
- Cookie 最长保存 1 天，服务端闲置 30 分钟失效；请求按 Spring Session 更新最后访问时间。所有 POST 均先获取 CSRF token。来源采用精确白名单，未使用 `*`。

## 已完成验证

| 检查 | 结果与边界 |
| --- | --- |
| 前端单测 | 3/3：请求携带凭据、CSRF 登录、保留 401 状态；请求使用替身，不单独证明真实服务 |
| 后端集成测试 | 6/6，连接专用 MySQL 测试库：健康、错误/未知账号、缺 CSRF、会话轮换、退出撤销、隔离、哈希、CORS、过期；最后一次执行无失败 |
| 前端生产构建 | 类型检查与 Vite 构建通过 |
| Android 构建 | APP 与独立测试 APK 构建成功 |
| 浏览器实际点击 | 连接、student_a 登录、me 恢复、退出、退出后恢复得到“请先登录” |
| 真机 full | 1/1，通过实际 WebView 检查 health200、无CSRF403、错误密码401、A登录200/me200、HttpOnly不可读、退出204/原会话401、B登录200/访问A资料403，最后重新登录A200 |
| 真机 ui | 1/1，通过实际 Vue 表单和按钮执行登录、恢复、退出、退出后恢复拒绝，再登录A；覆盖生产 client.ts |
| 真机 resume | 1/1：关闭 Vite、强停 APP、重启后端后，冷启动 WebView health200，me200 且仍为 student_a |
| 启动证据 | 冷启动后 APP 进程存在、MainActivity 前台，页面截图 `evidence/android-cold-start.png`；首张过早截图为启动过渡，已替换为渲染完成截图 |
| 临时凭据 | 真机及本机的 auth-probe.json 已清除；密码和私钥未进入源代码或 APK |
| 构建残留回归 | 在暂存目录放入无害旧资源后构建，镜像同步将其清除；源码已删除文件不会继续留在下一包 |

后端过期检查通过调整专用测试库中同一会话的一致时间字段模拟过期；未等待 30 分钟实测，未宣称完成真机过期提示、长时间续期或多机兼容。浏览器验证了当前会话恢复，尚未覆盖重开整个浏览器后的会话恢复。

## 本轮问题与处理

- 首轮空壳后端没有健康和认证实现，测试确实失败；实现后转绿。新增过期用例最初仅改 EXPIRY_TIME 不足以模拟 Spring Session 判断，随后同时调整最后访问、闲置时长和到期时间，测试通过。
- CORS Bean 最初命名不匹配自动检测，允许来源的测试返回403；按框架约定使用 corsConfigurationSource 后通过。
- Windows 下原生数据库路径含中文造成启动失败；使用项目独立 ASCII 数据目录。保留旧初始化目录，没有删除其他数据库。
- Android 测试 API 中 Files.readString 不可用，改用 Android 可用的 Scanner。无关插件示例测试的 Kotlin 依赖冲突，通过只构建本 APP 的测试任务消除，未强行覆盖全局依赖版本。
- 此手机测试进程的后台 Activity 启动等待，需要 shell 显式启动同一目标 Activity。测试脚本处理这一点，不修改手机安全设置。
- 独立只读审查未发现明确认证绕过或资料越权；发现固定构建目录残留风险，改为严格校验专用目标目录后镜像同步，并完成残留资源回归。审查指出独立 fetch 探针不等于页面流程，另补 ui 模式验证；补充最终恢复 A 登录的断言。
- Flyway 提示 MySQL8.4 新于其已验证范围；本机迁移与测试已通过，仍需后续新库恢复验证。专用测试库先前由健康测试创建会话表，曾一次性以 baseline0 建立迁移历史；开发库从空库正常执行 V1，未在全局开启自动 baseline。

## 交付边界

APK 为环境验证 debug 包，SHA256：`8c9f250f834dfd19518383fc078ed255f2bcd3a2a926952c6e82f6254f5c5a07`。

本轮仅完成最小连接与认证通路。尚未完成教材题库审核、业务接口/表结构、组卷作答、保存恢复、结果错题、公网 HTTPS、Web 正式域名、生产签名和最终验收。T03 Cookie 路线可继续使用，原生安全存储备选未触发；D08 的浏览器重启和长时验证留到首版回归，不把本轮探针冒充首版完成。
