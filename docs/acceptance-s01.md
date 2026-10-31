# S01 普通多知识点组卷与家长发送验收

日期：2026-10-29。状态：软件实现、本机Web与Android14模拟器工程自验完成，用户最终验收待确认。I3历史验收保持独立，不覆盖旧包或旧记录。

## 范围与环境

- 学生当前年级普通多点组卷；家长当前有效关联孩子普通多点固定预览/发送；共用保存、恢复、计时、提交、结果和逐题知识点统计。
- 无schema迁移，保留V1—V7。原非负整数题型不变；未实施S02、教材覆盖、其他答案类型、家长完整历史。
- 后端Java21/MySQL8.4.13，13306专用实例，全部新增测试与页面样例在 `mathdrill_test`，未在开发库造数或故障注入。
- 页面服务明确以 `runtime.ps1 -Database mathdrill_test` 启动。另只读查询MySQL实际连接列表：6条连接均为mathdrill_test，没有mathdrill_dev连接；迁移记录至V7。证据 `.local/s01-database-target-check.log`。fixture的库名声明不是此项的证明。
- Web为本机内置浏览器、Vite严格HTTPS代理；APP为emulator-5554，Android14/API34。实体Android16、其他浏览器组合不在本批通过结论中。

## 实际结果

| 检查 | 独立预期与实际结果 | 证据 |
| --- | --- | --- |
| 后端完整回归 | 51/51，失败0、错误0、跳过0；含原45项及新增6个S01测试方法 | `.local/s01-backend-all.log` |
| 前端完整回归 | 29/29，8个文件通过；含分配、同值重填幂等、学生重开、家长迟到响应与初始会话恢复互斥 | `.local/s01-frontend-final.log` |
| 类型/Web/Android构建 | vue-tsc、Vite及Android debug/test构建实际通过 | `.local/s01-build-android-verified.log` |
| Android完整页面回归 | 5/5：I1、I2角色、I3重练、I3学生/家长强化、S01普通多点 | `.local/s01-device-flow.log`、`.local/s01-device-probes.log` |
| 实际安装版本 | 从设备读取base.apk，SHA256与delivery中S01主APK一致；APK内主Web资源与frontend/dist一致 | `.local/s01-installed-verified.apk`；manifest-s01验证字段 |
| 精确分配与统计 | A2+B3恰5题、question_id不重复；A对1/2、B对2/3，汇总3/5=60.00%；按题快照统计而非每点重复记全卷 | S01NormalPracticeIntegrationTest、Web/Android断言 |
| 不足与非法输入 | 任一点不足整份无practice/item残留；所有不足项均列所需/可用；空、0、小数、重复点、混年级、合计101及双表示拒绝；1和100通过 | S01NormalPracticeIntegrationTest |
| 当前可用池 | 停用分类、跨年级拒绝；未审核/停用题不进入当前池，不跨点补齐 | 同上 |
| 重试兼容 | 旧单点默认30、旧哈希结构保持；单点/一项allocations同键同卷；多点顺序变化同卷、不同分配同键409 | 同上；normal-composition/lifecycle测试 |
| 家长权限/固定预览 | 题干与顺序一致，重复发送不重复；任一分类、题版本、年级变化、过期、解绑拒绝；家长不能代答 | 后端用例、Web及Android实际流程 |
| 异步页面边界 | 改孩子或配置后旧预览不进入当前页面；同配置刷新保留不明操作键；分类不可用时有明确取消入口 | normal-parent测试、共享配置实现 |

后端51项在17:06取得；之后后端产品与测试未再改变，后来变更仅前端、设备测试、构建脚本和记录。前端29项及最终包均覆盖后来两项前端修复，未用旧I3的45/20/4冒充新版本结果。

## Web实际流程

全部使用隔离账号与5道可独立计算的加法样例，不计作教材扩充。

1. 学生配置A3+B2，因A仅2题显示需要3/可用2，仍保留配置；调整A2+B3，返回首页再打开仍为2/3/总5。
2. 新卷首题填5、手动保存、保存退出，再进入并明确接管，恢复同一题与答案5。按题干标记独立安排A对1/2、B对2/3，5道不同题，正式结果60.00%。
3. 家长按A2+B3预览5题，取消确认仍保留同一预览，再确认发送。学生接收的5题逐项、顺序均与预览相同，提交60.00%。
4. 家长学情为2次/10题/6对/60.00%，A共4题对2、B共6题对4。最终前端修复后再次完整创建提交五题，仍A2+B3、5题不重复、60.00%。

截图：`evidence/s01/web-student-result.png`、`web-parent-stats.png`、`web-student-final.png`。Android截图 `android-s01-normal.png`只记录末页顶部，不用截图本身证明未显示区域的数据；完整步骤依据脚本断言和日志。

## 发现与修复

- 新后端6例先真实失败（不接受allocations），实现后完整51例通过。第一次受限环境连接失败属于环境失败，不算业务RED；可访问环境下取得业务失败日志 `.local/s01-backend-red.log`。
- 独立审查发现同值setter也会换键；先通过 `.local/s01-same-value-red.log` 复现，再修为Object.is同值早退，回归通过。真正变更配置仍换键。
- 补初始会话恢复期间禁用登录，避免恢复与登录并发；`.local/s01-auth-restore-red.log`先复现，修后通过。没有改变服务端鉴权或关闭HTTPS校验。
- 设备首轮旧I3脚本未等待记录按钮可用，旧I1脚本又只识别学生/未登录初始页面；另有冷启动授权失败观察。修正按钮等待与任意角色退出前提后，最终5例完整通过。失败日志保留attempt1/2/3，未删除失败记录。
- 独立复核最终未发现明确阻断问题；复核范围为静态代码和无数据库写入的组合逻辑自检，完整测试与设备流程由软件窗口执行。

## 交付与复现

独立产物：`delivery/mathdrill-s01-debug.apk`、`mathdrill-s01-test.apk`、`mathdrill-s01-backend.jar`、`mathdrill-s01-web.zip`，SHA256、源码快照及验证信息见 `delivery/manifest-s01.json`。均为本机debug测试候选，不是公网或正式签名发布。

- 后端：`./scripts/test-backend.ps1`，先核实专用实例可用，脚本选择mathdrill_test。
- 前端：frontend中 `npm test -- --run`；`npm run build`。Windows临时目录权限受限时，把TEMP/TMP设为项目 `.local/test-tmp`。
- 构建：`./scripts/build-android.ps1 -Version s01`。脚本默认s01，拒绝覆盖已存在的历史版本APK。
- 设备：核实18443实际连接mathdrill_test、隔离账号/家庭及 `.local/s01-ui-fixture.json` 中的真实点ID和从SQL独立核对的expectedAvailable，然后运行 `./scripts/test-i1-device.ps1 -Serial emulator-5554 -Version s01 -Install -IncludeRoles -IncludeLearning -IncludeNormal`。本轮测试库可用题171，管理员界面精确核对此数；旧I2默认120仅适用于原120题基线。
- 设备脚本finally已清除本机与模拟器i1-probe.json，均实际确认不存在。私有运行配置及备份不进入公开源码包。

## 保留与未测

I2/I3共13个既有产物/清单与S01前基线逐项哈希一致；I3源码ZIP和私有SQL备份仍匹配原manifest。源码继续位于原未提交工作区，无暂存、提交、remote配置、推送、上传、部署或课程平台操作。

新版实体设备、原生文件跨应用、秒表±2秒、全面弱网双端、隔离还原/稳定存储、性能及公网/真实试用仍未验；S01本机验收不代替这些项目，也不改变既有需求承诺。
