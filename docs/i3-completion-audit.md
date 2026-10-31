# I3 完成审计

日期：2026-10-29。对照批准的 `superpowers/plans/2026-10-28-i3-review-reinforce.md`，核对当前实现、实际运行与交付。范围：原题重练、学生同知识点强化、家长强化；APP/Web共享，保留I2。真机、公网、教材全覆盖和全量压力/弱网不属于本批通过声明。

## 需求与证据

| 计划/约束 | 当前权威依据 | 结论 |
| --- | --- | --- |
| Task0保留I2和恢复原环境 | `.local/archive/i2-source-before-i3.zip`；`delivery/manifest-i2.json`四原包SHA256与当前文件一致；DB13306、HTTPS18443、原模拟器；初始基线日志i3-baseline-backend.log | 通过；无reset/clean/自动提交 |
| Task1独立历史快照 | V7、ReviewPracticeService.replay/persistSnapshot、I3ReviewIntegrationTest.replayCopiesOldSnapshotAfterNormalEditAndDisable | 旧题干/答案/解析不变，新练习新ID、空白raw，草稿无答案及解析，原成绩不变 |
| Task1来源与边界 | replayRejectsForeignCorrectDuplicateAndErrorMarkedItems、replayDeduplicatesHistoryButRejectsMixedGrades、reviewBoundariesRejectOversizedSelectionsAndFractionalAllocations | 他人/家长/正确题拒绝，0/101/重复ID拒绝，错误版本拒绝，同fingerprint去重，跨年级拒绝，同键不重复创建 |
| Task1锁与错误处置 | ReviewPracticeService稳定锁序、ReviewMapper当前锁；I3ReviewConcurrencyIntegrationTest原2项 | 原题先创建后错误会VOID，错误先确认后重练不创建；V1—V6未改写，旧数据字段由V7追加兼容 |
| Task2共享重练界面 | LearningApp、ReviewPracticePanel、useReviewSelection、client/contracts；review-flow.test.ts；Android I3ReplayFlowTest及Web历史证据 | 取消保留选择、同键重试、变更重建键；保存恢复/提交/旧错保留及含正确尝试的历史通过 |
| Task3历史年级及当前可用池 | reinforcementUsesHistoricalGradeAndCurrentReviewedPool、ReviewMapper.available；Web隔离三年级账号练二年级C | 采用历史年级；仅当前启用/审核且分类启用的题；分类移年级或停用拒绝 |
| Task3精确分配与原子性 | multiPointReinforcementHasExactAllocationAndItemBasedStats、reviewBoundaries...；Web A/B各1题与可用不足提示 | 后端A2+B3=5，按1/2和2/3统计、总3/5=60%；UI1+1=2；不足整份无写入；0/小数/重复/缺少分配/合计101/混年级拒绝 |
| 审查：历史分类不能丢失 | reinforcementPreservesHistoricalCategoriesWhenReplayDeduplicates；前端all historical sources回归 | 同内容A→B后重练仍1题，强化保留全部A/B来源及分类 |
| 审查：重开配置可靠重试 | review-flow.test.ts重开分配7及重练/强化同键回归；Android重开1、Web学生重开2/家长重开1 | 相同选择保留配置和键；真实配置变化才重置 |
| Task4家长当前关联和角色 | parentHistoricalReinforcementUsesFixedPreviewAndCurrentAssociation及RoleFlowIntegrationTest；FamilyService.active | 待确认/解绑无权限，跨孩子来源403，家长重练/调用学生强化/代答403，学生自己任务不因解绑消失 |
| Task4固定预览及发送校验 | parentMultiPointPreviewRejectsEveryChangedCategoryAndLaterGradeChange；FamilyService.send逐practice_point校验 | 任一分类变化、预览后升年级、超过15分钟拒绝；历史低年级合法；重复发送同任务；预览题干/顺序固定 |
| Task4状态竞争 | I3ReviewConcurrencyIntegrationTest新增6项，用事务+CountDownLatch确定交错 | 创建/发送与管理员错误处置两个顺序均安全；开始先完成则撤回409；撤回先完成则打开无编辑权 |
| Task4切孩子及旧响应 | ParentWorkspace清理预览/详情；review-flow parent endpoints/account generation回归 | 选择或配置变化清旧预览；旧孩子异步响应不能打开当前孩子练习 |
| Task5完整回归 | `.local/i3-final-regression.log`最终45/45；前端20/20；Vue类型/Web构建；`.local/i3-build-android.log` | 全绿；测试均使用专用dev/test库 |
| Task5 Android真实流程 | `.local/i3-device-flow.log`，I1Flow/I2Roles/I3Replay/I3Learning四项 `OK (4 tests)`；已安装APK读回与交付SHA256一致 | 原流程/原题重练/学生强化保存恢复/家长固定预览发题/学生提交/家长结果历史学情通过 |
| Task5 Web真实流程 | 本轮CUA页面操作；web-student-reinforce-final.jpg、web-parent-reinforce-final.jpg、web-multipoint-final.jpg、web-historical-disabled-replay.jpg、web-error-version-blocked.jpg | 学生2题保存恢复50%、家长1题固定预览100%、多点、升年级、普通停用可重练、错误版本不可重练通过 |
| Task5独立交付与文档 | delivery四个i3产物及manifest-i3.json；README、api-i3.md、acceptance-i3.md、progress及PROJECT_WORKFLOW | I2及冻结i3-replay包未覆盖；源码及私有SQL快照、哈希、秘密扫描和压缩完整性检查完成 |

## 证据口径与执行差异

- 计划中的示意测试名由最终用例合并覆盖，以断言及运行结果为准。新增的并发/边界验证用于证明既有实现，没有因此新增产品逻辑；第一遍通过就记录通过，不虚构RED。两项真实审查缺陷均先复现失败再修复。
- 早期暂停交接和38/38记录保留为历史；最新结果45/45替代当前状态。产品代码自38项通过后的两个审查修复以来未变，后来新增测试与Web证据不会使已安装Android包失配。
- Web多点UI用A1+B1，后端精确分配和统计用A2+B3，两个检查共同证明入口、组卷和统计，不把其中一个夸大成另一种实际输入。
- Android角色回归使用本机已有并已确认的演示家庭；原题重练另注册独立学生，强化用演示学生新建错误。答题按显示题目独立算加法，不依赖抽到某个固定题。Web边界样例为明确隔离分类/账号，完成后分类和题目停用；没有新增实际家庭权限。
- 不为验证修改原始课程报告或编造分工/用户试用。没有自动Git提交、推送、部署或课程平台提交。

## 本批出口

历史快照与原错误/成绩保留；按历史年级和全部知识点创建强化；家长固定预览/权限/发送安全；重练、强化共用作答保存、计时、提交判分及统计；APP与Web实测；I2保留。I3本机实现与本批验收已满足。

后续另列：Android16实体手机、原生分享、备份还原与稳定数据目录、长时间/弱网/压力、教材与教师审核、真实儿童家长试用、正式签名和公网发布。它们未被标为本批通过。
