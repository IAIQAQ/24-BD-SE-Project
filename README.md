# 小芽口算

Android首发的口算学习课程项目，Android与Web共用Vue前端，Spring Boot提供统一后端。

**软件开发已完成，最终功能版本为S01。** 学生本人完整学情S02已按用户决定取消；公网部署等服务器确定后再做。

个人GitHub源码仓库：[zhenzhu666/mathdrill](https://github.com/zhenzhu666/mathdrill)（私有，需要仓库访问权限）。私有运行资料和delivery包保留在本机。

## 已有功能

- 学生：注册登录、普通单点/多知识点组卷、作答、保存恢复、明确接管、计时、提交判分、结果与错题。
- 巩固：历史原题重练、按历史知识点强化，保留原成绩和错误历史。
- 家长：家庭关联确认/解绑、固定预览发题、未开始任务撤回、结果/错题/学情只读。
- 管理员：分类和题库维护、审核启停、错误版本处置、Excel模板及整批校验导入。

## 启动与演示

1. [最终启动说明](docs/startup-final.md)：当前电脑的数据库、S01后端、Web与Android启动步骤。
2. [最终演示流程](docs/demo-final.md)：学生练习、家长安排、错题巩固及管理员补充。
3. [S01验收](docs/acceptance-s01.md)：实际测试、证据入口和未测项。
4. [S01接口](docs/api-s01.md)及[软件窗口记录](docs/dev-window-progress.md)：技术行为与最终范围决定。

启动说明针对已有本机配置。GitHub源码不包含私有运行配置、数据库数据、证书、账号密码和构建包；换机需要准备环境，不能只克隆就直接运行。

## 技术结构

- 前端：Vue 3、TypeScript、Vite；Capacitor复用为Android APP。
- 后端：Java 21、Spring Boot、MyBatis/JdbcTemplate、MySQL 8.4；Flyway迁移V1—V7。
- Android APP内置页面，通过HTTPS访问与Web相同的业务服务；权限、判分、快照和幂等由后端执行。

## 工程验证

S01已取得后端51/51、前端29/29、Android14/API34模拟器5/5，以及类型检查、Web和Android构建通过的记录。它们是对应版本的已有结果，不代表每次整理文档都重新执行了测试。

```powershell
# 后端会连接专用mathdrill_test，运行前核对本机环境。
./scripts/test-backend.ps1
# 前端在frontend目录执行：
npm test -- --run
npm run build
```

本机构建使用 `./scripts/build-android.ps1 -Version s01`。delivery中的I2/I3/S01包和manifest独立保留，历史包不覆盖；不默认运行设备测试脚本作为启动步骤。

## 文件组织

| 目录 | 内容 |
| --- | --- |
| backend | 后端源码、迁移、集成测试 |
| frontend | 共享前端、Android工程与页面测试 |
| scripts | 本机启动、构建及测试脚本 |
| docs | 接口、验收、启动与演示说明、历史记录 |
| .local（不上传） | 私有配置、证书、账号资料、备份、日志 |
| delivery（不上传） | 冻结APK、测试APK、后端JAR、Web ZIP及manifest |

## 当前范围

当前基础内容以120道自编加法为主，不宣称一至三年级教材全覆盖；标准答案为非负整数。学生有练习记录、结果和错题，家长有学情；家长任务列表最近100条。S02不在最终开发范围。

新版真机、原生文件跨应用、独立秒表、专项弱网/压力、备份还原等未单独完成的项目如实保留未测说明，本轮简化收尾不再额外展开。当前为本机运行版本，公网访问尚未部署。

历史记录中的暂停、未提交及旧测试数量属于当时状态，当前软件事实以最新开发记录和S01验收为准。父目录课程材料由课程窗口维护，不作为本仓库自动上传范围。
