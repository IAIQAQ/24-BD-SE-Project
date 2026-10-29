# I3 错题原题重练与同知识点强化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking. 当前会话沿用主代理顺序实施；本计划不另行要求并行开发。不得自动Git提交、推送、部署。

**Goal:** 学生从历史错题创建独立的原题重练或同知识点强化，家长为当前关联孩子安排强化；APP与Web共用页面、统一组卷快照、作答保存、计时、提交判分和统计。

**Architecture:** 给现有practice增加练习类型和历史来源；原题重练复制practice_item历史内容，强化按历史归属从当前可用题库抽题。两者均创建普通业务练习，继续使用现有open/draft/answers/segments/submit/submission-status/result；家长强化复用PREVIEW→PENDING及15分钟固定预览发送/撤回。

**Tech Stack:** 现有Vue/TypeScript、Capacitor Android、Spring Boot/MyBatis/JdbcTemplate、MySQL/Flyway、Vitest/JUnit/Android instrumentation。不新增运行依赖。

**Spec:** 已确认的[需求分析报告](../../../../需求分析报告_U202414804_陈虞林.md) F-05、F-10、F-11、F-12、F-13及[APP页面与操作流程](../../../../APP页面结构与完整操作流程.md) W03/W04、U07。本计划细化既有功能，不重新选择平台。I2实际基线见[acceptance-i2.md](../../acceptance-i2.md)。

## 当前状态与本轮边界

2026-10-28：用户已授权推进I3，指定先做原题重练和同知识点强化、复用现有流程、两端共用。此前只有阶段方向，本文件是首次具体实施计划；当前仅完成代码/需求核对和计划，不表示功能已经实现或验收。

- 当前Git工作区仍有I2未提交代码、迁移、截图和文档。不得reset/clean或建立不含这些内容的新checkout作为起点。
- 上轮记录为后端28项、前端11项、Android14两条真实UI通过；本轮尚未重跑，不把旧结果自动算作I3结果。
- 现有错题仅查询错误事件，历史中还没有包含后来答对的全部尝试；组卷、家长发送和知识点统计仍按单知识点假设实现。必须一并处理这些调用点。
- 本批不扩充正式教材题库、不新增答案类型、不新增教师/班级、AI出题或掌握度算法，不在本批自动发布公网。多知识点支持用于完成本次强化；普通练习的多知识点产品入口留后续，原单知识点普通接口保持兼容。

## Global Constraints

- Android首发，APP/Web共用Vue组件与后端；不再写一套作答、计时或判分实现。
- 每份普通、原题重练或强化练习均为1—100题，同份无重复题；同份不混年级，跨年级在界面明确分组并逐组创建。
- source保留SELF/PARENT，practiceKind另用NORMAL/REPLAY/REINFORCE；来源与练习类型不能混用。
- 公众不能选择管理员；学生只读写本人，家长每次操作重查当前有效关联；原题重练仅学生操作，家长只查看和安排强化。
- 历史内容、答案、错误和成绩不可被后来重练覆盖；答对不删除旧错题，不显示“已掌握”。所有新练习初始答案为空，提交前的答题接口不提供标准答案和解析。
- 已确认数学错误的历史版本禁止原题重练。普通停用、正常改题或停用分类不自动禁止旧内容重练；重练沿用旧题干、标准答案、解析和原归属。
- 强化只用当前启用且核对通过的题及启用分类；按所选错题的历史年级/知识点选题，不悄悄换成学生当前年级。历史分类已改到其他年级、已停用或可用题不足时明确拒绝并提示调整。
- 同类强化按当前可用池随机无放回抽取，可能遇到原题；本批不增加“保证排除所有原题”的要求，不能把强化承诺为全部新题。
- 所有写操作保留CSRF、服务端校验、事务、幂等键与锁；新增迁移从V7起，不改已成功执行的V1—V6。
- 原I1/I2交付包保留；I3单独生成产物与验收记录。私有配置、会话、备份和密码不进入源码及APK。

## 产品流程与确定规则

### 原题重练

学生错题列表/详情→选择历史错题→确认原年级、知识点、实际不同题数及不可用原因→创建REPLAY→复用答题页→保存/恢复→提交→独立结果→历史增加本次记录。

服务端只接受该学生已完成练习中真实错答/漏答的itemId，不接受客户端提交题干、标准答案、解析或年级。重复itemId属于非法输入；同年级所选不同错误事件若对应同一fingerprint，只重练一次，选择其中最近的所选历史快照。确认页面先通过服务端options显示去重后的数量；创建时再校验，不静默跳过错误版本。

原题重练不增加题量填空：选中几道不同原题就生成几道；不足100题时不能复制同题凑数。超过100道或跨年级提交一份请求时拒绝；界面按年级提示分批。学生升年级后可重练自己低年级历史错题。

### 同知识点强化

学生：选择错题→服务端按历史年级/知识点归组→选择一个年级组→配置各知识点题量和总题量→展示所需/可用数→创建REINFORCE→复用答题页与结果。

家长：选当前孩子→孩子错题→选择错题→相同强化配置→预览实际题干→确认孩子及练习类型→复用既有发送→学生开始→家长只读结果/学情，可撤回未开始的本人任务。

- 同年级允许多个知识点，配置必须覆盖该组所选知识点，每项至少1题，合计1—100题。初始总量max(30,所选知识点数)题，最多100题；按知识点ID稳定排序均分余数，用户可调整；不能因某项不足自动挪给别项。
- 学生和家长均可围绕历史低年级错题安排对应低年级强化；普通练习仍按当前年级。只有经本人/关联权限校验的历史错题可开启这个例外，不能仅靠传grade绕过普通限制。
- options可返回多个年级组，但一次创建/发送只处理界面明确选择的一个组。切孩子、选择或题量变化后清除旧配置、operationKey及预览；没有“一次确认暗中创建多份任务”。
- 错题旧版本被确认数学错误时，禁止REPLAY；若原知识点仍有效，可明确选择当前正确题库的REINFORCE，不能把修正后的题伪称历史原题。
- 家长预览记录孩子当时的当前年级recipientGradeAtPreview，发送时检验孩子是否在预览后变更年级；不能把历史练习年级originalGrade强制等同孩子当前年级。继续校验每项分类版本、启用状态、源题版本及当前关联。

### 历史、类型与统计

练习列表、家长任务、结果、错题历史显示普通/原题重练/同类强化；家长任务同时保留发送人。跨页选择按itemId保存、最多100项；切账号/孩子清空选择。历史详情包含同一fingerprint的已完成作答，包括后来答对的尝试；错误次数仍只数原有错误事件，不把所有尝试算错，也不因答对清零。

所有已完成类型计入现有汇总与每日统计。多知识点的知识点总题数/答对数必须按题目归属汇总，不能把整份练习的总分计入每个知识点；不虚构每个知识点用时。原成绩只读规则保持。

## 数据与复用位置

新增`V7__review_and_reinforcement.sql`，先在测试库验证，再备份开发库升级：

| 数据 | 变化及旧数据处理 |
| --- | --- |
| practice | practice_kind默认NORMAL；seed_item_ids JSON记录来源错题ID；recipient_grade_at_preview记录家长预览时孩子年级。旧预览按原original_grade回填，不能据现年级追认旧预览有效。 |
| practice_point | 新表，复合主键(practice_id,point_id,point_revision)，保存该份练习的原年级、知识点名、分类版本和题量。旧练习从原practice快照回填一行。 |
| practice_item | 增加point_id、point_revision及可空origin_item_id。旧题目从所属practice快照回填；REPLAY的origin_item_id指向选中的历史item，题目question_id/source_revision保持旧版本，以继续适用管理员错误处置。 |

practice原point_id等字段保留以兼容I1/I2，但多知识点练习以practice_point和item归属为准；旧字段为排序后第一项，不能再拿它做多知识点计分依据。header显示名从各项历史名称生成，不改写旧记录。原题重练可能选到同一知识点的不同历史分类版本，practice_point不能只按point_id去重。

调整PracticeService中的“持久化练习和快照”一段为包内共享入口；普通、重练、强化各自完成来源校验后调用。open/save/timing/submit/reconcile不复制实现；新增题目ID，raw为空，outcome为空。

## 接口和内部契约

统一`/api/v1`前缀、现有响应外层、字符串编号及CSRF。以下为计划接口，尚未实现：

| 路径 | 输入与输出 |
| --- | --- |
| POST /students/{id}/review-options | `{itemIds:string[]}`，最多100项；返回去重原题清单、replayAllowed/reason及按原年级归组的知识点、当前可用数。仅本人。 |
| POST /family/children/{child}/review-options | 同上，当前有效家长；原题清单只用于查看，不能代学生创建REPLAY。 |
| POST /practices/replays | `{itemIds:string[],operationKey:string}`→Summary；仅学生，创建一份REPLAY/IN_PROGRESS。 |
| POST /practices/reinforcements | `{seedItemIds:string[],allocations:{pointId:string,total:number}[],operationKey:string}`→Summary；仅学生，一份单年级REINFORCE/IN_PROGRESS。 |
| POST /family/children/{child}/reinforcement-previews | 同强化输入→固定Summary+实际题干列表；parent、child由服务端验证后确定。其后继续用原send/withdraw接口。 |
| GET /students/{id}/mistakes/{item} | 扩展现有详情，增加历史分类、原版本、replayAllowed/reason及全部已完成尝试。旧字段保留。 |
| GET /family/children/{child}/mistakes/{item} | 相同只读详情，当前关联检查；家长原列表接口保持现有数组形状，新增字段兼容。 |

Summary增加practiceKind、points[]和senderName，原字段保持；ResultItem/Mistake增加grade、pointId、pointName、pointRevision及来源信息。前端不再写死“20以内加法”；知识点显示来自历史快照。

内部新增`ReviewPracticeService`和`ReviewMapper`，只承担错题来源校验/归组/历史查询/重练与强化组卷。避免把全部逻辑继续塞进useLearningApp或重构无关模块。

- `options(Authentication,long studentId,List<String> itemIds)`：学生路径校验本人；家长入口由FamilyService先验证当前关联，再调用包内授权参数入口`optionsFor(long studentId,List<String>)`。
- `replay(Authentication,ReplayInput)`、`reinforce(Authentication,ReinforceInput)`返回Summary。
- `parentPreview(long parentId,long studentId,ReinforceInput)`只由已鉴权的FamilyService调用；不从控制器直接信任parentId。
- `PracticeService.persistSnapshot(SnapshotInput)`为包内共享入口。SnapshotInput包含studentId、senderId、practiceKind、operationKey、requestHash、recipientGradeAtPreview、pointSnapshots、itemSnapshots、seedItemIds；不包含客户端答案。

幂等哈希覆盖练习类型、接收学生、发送人、规范排序的来源ID/分配和实际题量；同键同输入返回同一练习，同键换内容409。去重展示和实际创建都以服务端计算为准。

## Review Focus

1. 旧题被正常改题/停用/停用分类，重练仍复制旧内容；确认错误版本后必须禁止，不能偷换新标准答案。（Task 1/4）
2. 升年级历史强化例外不能成为越权或任意选年级的入口；分类移年级、跨年级或解绑必须准确拒绝。（Task 3/4）
3. 正在创建重练/强化时管理员确认源题错误：只能先创建后被处置，或直接拒绝，不能在处置扫描后漏出仍可作答的新任务。（Task 1/4）
4. 同知识点不同历史分类版本、同题多次错误、同题后来答对：归属、去重、次数和历史展示不得混淆。（Task 1/2/3）
5. 保存/提交超时或连点不得重复建卷/判分；切孩子/选题/配置后不能发送旧预览，取消不能丢输入。（Task 2/4/5）

## 文件职责

- 新增：迁移V7、`practice/ReviewPracticeService.java`、`practice/ReviewMapper.java`、`practice/ReviewController.java`；新增`I3ReviewIntegrationTest.java`、`I3ReviewConcurrencyIntegrationTest.java`。
- 修改：PracticeMapper/PracticeService的快照持久化、历史元信息和查询；PracticeController原接口保持兼容。
- 修改：FamilyService/FamilyController强化入口、详情、发送时逐分类校验与知识点统计；AdminService错误处置继续按question_id/source_revision并与创建锁配合。
- 新增：共享`ReviewPracticePanel.vue`（选择后的确认/配置），`frontend/tests/review-flow.test.ts`；按需要提取共享类型，禁止为两端再建一套页面。
- 修改：client.ts/contracts.ts、useLearningApp.ts、LearningApp.vue、ParentWorkspace.vue；沿用已有ConfirmationDialog/SaveQueue/ForegroundClock。
- 新增Android `I3LearningFlowTest.java`；修改构建/测试脚本支持独立i3包与完整回归；不覆盖i1/i2产物。
- 更新：README、api-i3.md、acceptance-i3.md、progress.md及PROJECT_WORKFLOW.md，记录实际结果和未测项。

## Task 0：保留基线与建立测试入口

**Files:** 现有.local/archive、docs/progress.md；测试脚本沿用scripts/test-backend.ps1。

- [x] 记录当前分支和未提交I2文件清单，保存不含.local/密码的源码快照及哈希，保留I2产物；不自动提交。
- [x] 核对实际服务/数据库/模拟器状态，复用既有环境；按需启动项目专用服务，不重装工具。
- [x] 执行`./scripts/test-backend.ps1`及frontend的`npm run test -- --run`，与记录的28/11比较；有失败先定位，不能建立I3成功结论。

## Task 1：历史来源与原题重练后端

**Files:** V7、ReviewMapper/ReviewPracticeService/ReviewController、PracticeMapper/PracticeService、I3ReviewIntegrationTest。
**Interfaces:** 产出options、replay和persistSnapshot；原open/draft/save/submit等保持原签名。

- [x] 先写用例`replayCopiesOldSnapshotAfterNormalEditAndDisable`：保存旧题干/答案/解析后正常改题和停用，重练内容仍为旧值，新itemId、新raw为空；当前题标准答案不得泄漏至新草稿。
- [x] 写`replayRejectsForeignCorrectOrErrorMarkedItems`：其他学生/非错题拒绝、重复ID400、已确认错误409、同fingerprint去重、0及101项拒绝、跨年级拒绝；同键重试id不变。
- [x] 跑`./scripts/test-backend.ps1`，记录新接口缺失或断言不满足的RED；随后追加迁移，提取最小共享持久化代码并实现上述接口。
- [x] 按题目ID升序取得源question和对应question_revision当前锁，再创建并写快照；历史内容校验不可依赖鉴权时建立的旧事务快照。不能先锁practice再反向锁question。创建入口按当前关联（家长）→学生账号→当前分类ID升序（强化）→question/版本ID升序→目标practice取锁；persistSnapshot不得在已持有question锁后才首次请求学生账号锁。
- [x] 用确定性两事务用例`errorDispositionAndReplayCreationCannotMissEachOther`覆盖Review Focus 3；完成GREEN和I1/I2回归。迁移验证旧练习/旧预览/旧分数不变。

## Task 2：学生重练完整纵向流程

**Files:** ReviewPracticePanel.vue、client.ts/contracts.ts、useLearningApp.ts、LearningApp.vue、review-flow.test.ts、ReviewMapper历史查询。
**Interfaces:** 消费options/replay，复用现有openPractice(record.id)；详情history包含已完成正确与错误尝试。

- [x] 先写交互用例`replayConfirmationPreservesSelectionOnCancelAndRetryKey`：取消不建卷；请求结果不明重试使用原键；改选择重建键；新练习进入原答题页，草稿无标准答案。
- [x] 实现错题列表勾选和详情单题入口，按年级分组确认；服务端不可用原因显示在确认页，创建时二次校验失败保留选择。
- [x] 实现同fingerprint全部正式尝试的历史查询；测试先错后重练答对，错误次数仍1、不同错题仍1、历史有2次且新成绩独立。
- [x] 运行`npm run test -- --run`、`npm run build`及后端测试；浏览器实际重练→保存退出恢复→提交→回看两次记录，形成第一个可交付增量。

## Task 3：学生同知识点强化与统计归属

**Files:** ReviewPracticeService/ReviewMapper、共享配置面板、PracticeMapper、FamilyService.stats、I3ReviewIntegrationTest、review-flow.test.ts。
**Interfaces:** options.groups提供历史年级和知识点/currentAvailable；reinforce接受seedItemIds+allocations，不接受客户端自选年级或答案。

- [x] RED用例`reinforcementUsesHistoricalGradeAndCurrentReviewedPool`：学生已升年级仍允许合法历史低年级；当前停用/未审核题不能入卷，分类移年级或停用准确拒绝。
- [x] RED用例`allocationIsExactAndInsufficientPoolWritesNothing`：两个知识点分配2+3=5，错误总量/缺少分配/0/小数/重复知识点/超过100拒绝；一项不足时整份不写，响应含所需与可用数。
- [x] 实现归组、当前分类/源题按稳定ID锁定、随机无放回取题及practice_point/item归属，调用persistSnapshot；保留普通create当前年级限制。
- [x] 校正多知识点统计：固定样例A两题答对1、B三题答对2，汇总3/5=60.00%，知识点A=1/2、B=2/3，不能给A/B各记五题；同一知识点跨历史年级/名称时展示原归属。PracticeMapper.mistakes现有SELECT i.*,p.point_id必须去掉会覆盖新item.point_id的头字段，用题目快照归属。类型可辨，已有单知识点统计不变。
- [x] UI从options选择一个年级组并明确配置题量，学生创建后进入原作答页；完成测试和浏览器实际强化流程。

## Task 4：家长强化、固定预览与状态竞争

**Files:** FamilyController/FamilyService、ParentWorkspace.vue、共享ReviewPracticePanel、ReviewPracticeService、I3ReviewConcurrencyIntegrationTest。
**Interfaces:** 家长options/detail/parentPreview新增入口；继续消费原`/family/previews/{id}/send`及withdraw。

- [x] RED用例`parentCannotReplayAndUnbindingRevokesReinforcement`：家长不能调用学生重练/作答，待确认及解绑后不能options/预览/发送/旧链接读取，不能混用另一个孩子的itemId。
- [x] RED用例`historicalGradePreviewSendsSameItemsAndRejectsLaterChanges`：孩子现年级与历史练习年级不同也能发合法强化；预览后再次升年级、任一分类版本/题目版本变化、超15分钟分别409；重复确认同任务。
- [x] 将FamilyService.send改为读取所有practice_point逐项校验，并比较recipientGradeAtPreview；旧普通家长任务继续原规则。预览内容及题序不重新抽取，未开始撤回仍仅本人。
- [x] 确定性回归管理员错误处置与强化创建/发送、开始与撤回竞争；不会漏处置、没有错误成绩或部分发卷。
- [x] 家长切孩子/改选择/改配置清除旧预览及key；当前面板展示孩子、年级、各知识点题量和强化类型。联调学生完成后家长结果/错题/学情一致。

## Task 5：完整回归与独立I3交付

**Files:** I3LearningFlowTest、scripts/build-android.ps1、scripts/test-i1-device.ps1、docs/api-i3.md、docs/acceptance-i3.md等。

- [x] 运行完整后端、前端及类型/Web构建；检查I1/I2旧接口与历史数据，无新失败才构建Android。
- [x] 新建i3命名产物，不覆盖旧包；扩展测试脚本Version支持i3，保留i1/i2显式选择。
- [x] Android14模拟器跑原I1/I2及I3：生成错误→原题重练→保存/恢复→答对而旧错仍在→学生强化→家长强化预览/发送→学生完成→家长结果/统计；测试源数据由隔离样例建立，不依赖正式题库恰好抽到某题。
- [x] 浏览器同场景，补多知识点/升年级/普通停用可重练/错误版本不可重练；没有USB不阻塞模拟器，但不能声称实体手机通过。
- [x] 导出本轮私有数据库备份，保存源码快照、APK/JAR/WebZIP哈希和实际截图。扫描源码及产物秘密值，清除模拟器临时凭据；记录备份还原是否实际做过。
- [x] 更新验收表，逐项标通过/失败/未测；原始个人报告不自动改写，课程全量题库、公网、性能/目标用户试用仍另列。

## 验收出口与执行顺序

顺序：Task 0→Task 1/2原题重练完整增量→Task 3学生强化→Task 4家长强化→Task 5双端验收。每个增量先复现测试，再最小实现并实际联调，出现失败先解决，不仅按页面完成数推进。

完成本批需同时满足：历史快照不被换题；错后重练答对不删旧错；强化的年级/知识点/数量符合所选历史范围；家长无越权与旧预览误发；两种练习共用保存判分且成绩统计一致；APP/Web真实操作通过。只有方案文件或测试设计不能勾完成。

计划自检：F-10→Task1/2，F-11与相关F-05→Task3/4，F-12统计一致→Task2/3/4，F-13历史与错误版本→Task1/4，权限及双端→Task4/5。普通多知识点入口和完整F-04教学覆盖不在本批宣称完成。

执行方式沿用当前会话由主代理顺序实施。计划完成后先交用户审阅本批范围与验收标准，审阅前不修改产品代码；这不是重新确认既定技术路线。初始任务均未勾选；2026-10-29按实际证据逐项审计后更新勾选，见../../i3-completion-audit.md。


2026-10-29完成依据：../../i3-completion-audit.md。实现测试名有合并/改名，以最终用例与断言为准；精确2+3分配和统计用隔离后端数据验证，Web多点UI用1+1，均为独立可推算样例。实体手机、全压力/弱网、备份还原和公网从来不纳入本轮通过结论。
