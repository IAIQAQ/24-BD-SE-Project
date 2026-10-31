# P3完整互动版 Implementation Plan

> 最终用户确认（2026-10-31）：第四批体验无问题，四批P3用户体验通过，授权同步GitHub。下文工程执行状态保留历史；设备/弱网等专项没有追加通过声明。

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. 用户授权优先：不自动提交、上传、部署或运行模拟器验收。

**Goal:** 在已通过用户体验的P2上完成角色局部动作、本次练习成长、完成反馈、可关闭轻音效和操作动效，不包含长期养成。

**Architecture:** 复用Vue组件、真实draft/answered及原保存提交流程。仅补展示组件、轻音效模块和明确成功事件；不改API/schema、不新增业务请求、不复制计时/保存逻辑。

**Tech Stack:** 现有Vue 3/TypeScript/CSS、浏览器内置音频能力、Capacitor、Vitest及现有构建脚本；无新运行时依赖。

**Spec:** [已确认的P3大纲及范围](../../ui-p3-interaction-proposal.md)。本文件最新状态（2026-10-31）：批1、批2视觉修订及批3均已由用户体验通过；批4整体工程检查、独立打包和启动完成，最终用户体验待反馈。勾选框仅表示已取得对应证据。最终记录见[第四批完成记录](../../evidence/ui-p3/final/completion.md)。末尾历史执行备注保留当时状态，以本段和最新记录为准。

## Global Constraints

- 四项受保护内容：当前完整未提交源码、390题及11知识点与原账号/关系/历史、全部旧包、私有配置和备份。保留P2作为可演示版本，不reset/clean，不从HEAD覆盖工作区。
- 不开发长期养成、小花园、积分、签到、徽章、排行榜、兑换或语音讲题；不扩展S02、教学覆盖和答案类型。
- 家长和管理员沿用P2业务与管理风格；不新增角色动画或声音到管理操作。
- 本次成长只代表非空答案数量，不表示正确数或保存成功；首页Summary.currentSeq不能用作已答数量。
- 声音默认关闭、设备本地偏好；不联网取音频、不收集声音偏好、不做跨账号设备同步。静音、后台、播放失败均不能阻止业务。
- 装饰不挡操作，不延迟答题；主要按钮最小48px，键盘焦点保留；系统减少动态效果时静态可用。
- 不自动暂存/提交/上传/配置远端/公网部署/发其他窗口消息；课程窗口文件和docs/coursework只读。工程事实只记docs/dev-window-progress.md。
- 不运行模拟器验收。APP只安装打开供用户体验，不能将编译/启动成功写作音效、原生生命周期或真机验收通过。
- 每批测试失败先修复，不把缺项改称完成。无需向用户逐个询问可逆实现细节；重大效果变化或范围变化才调整方案。

## Review Focus

1. 空白、空格、清空答案、1/5/100题、恢复及切换练习，成长与真实数量一致；批2覆盖。
2. 保存失败/冲突/后台、提交超时后的重复核对，成功反馈不得误报/重播；批3覆盖。
3. 音频被限制、存储不可用、关闭声音、快速点击和减少动态效果，学习操作不受阻；批3覆盖。
4. 窄屏/长昵称/长知识点、焦点和已有头像导航保护，装饰不能挤走核心内容；批1/2/4覆盖。
5. 换账号、旧结果重看、历史内容已确认错误，反馈不得串到另一人/练习或掩盖错误提示；批3/4覆盖。

## 批次总览与交付节奏

| 批次 | 用户可体验结果 | 工时初估 | 体验重点 |
|---|---|---|---|
| 1 | 首页小芽局部动作及按钮/卡片操作反馈 | 2—3小时 | 动作幅度、风格、是否抢眼 |
| 2 | 三阶段本次成长，填写及保存恢复同步 | 3—5小时 | 看懂进度，算式和输入是否仍清楚 |
| 3 | 成功庆祝、可关闭轻音效与异常处理 | 3—5小时 | 是否及时、舒服、不烦扰、不误报 |
| 4 | 整体回归、适配、完整独立交付 | 4—5小时 | 从学生到家长/管理员完整使用 |

实现与工程检查合计12—18有效小时，另留3—5小时反馈/素材返工；总安排15—23小时。每日工时未定，不承诺完成日期。每批仅报告该批已完成内容，前三批不标记“P3完整版完成”。每批结束启动网页供体验；若安装阶段APP，使用独立`ui-p3-b1-v1`/`b2-v1`/`b3-v1`名称，先检查同名，拒绝覆盖。最终使用`ui-p3-v1`，同名存在则升新批次。阶段包按用户实际需要提供，不能覆盖P2。

## Task 1 / 批1：局部角色动作与基础交互

**Files**
- 修改frontend/src/views/GardenHero.vue、SproutCompanion.vue、IllustratedPracticeCard.vue及styles/learning-ui.css。
- 如素材需要，新增frontend/src/assets/sprout/p3-interaction/，保留P2素材不替换原图。
- 测试复用frontend/tests/garden-hero.test.ts、sprout-companion.test.ts、illustrated-practice-card.test.ts。
- 记录docs/assets-sprout-p3.md、docs/evidence/ui-p3/batch-1/、docs/dev-window-progress.md。

**Interfaces**：GardenHero保留busy/start；卡片保留record/busy/open；SproutCompanion保留scene/slot。可增加可选active:boolean（默认true）以暂停装饰；不改变事件含义或增加请求。

### 实施步骤

- [x] 核对Git、P2 manifest及实际工作区差异；备份当前源码、旧包哈希和数据库保护快照。沿用P2只读快照方法，另存P3目录，绝不覆盖P2基线。
- [x] 确认角色与分层资产：以P2欢迎/完成形象为基准，准备可独立摆动叶片和成长阶段所需局部素材；用imagegen生成语义素材，压缩转换在本机进行。无正文/按钮/题目烘焙；原图、提示和资源清单留存，新增资源目标不超过约1MB。
- [x] 先补active=false不带动作态、资源失败保留CTA、busy禁用和原事件仍生效的检查；运行对应测试确认新要求先失败，再实现。
- [x] 首页入场动作限定一次约2—3秒、叶片小幅摆动；不移动花园标题和按钮，不做整页持续循环。不以整张图片反复上下跳替代局部动作。
- [x] 学生按钮按压和卡片选中约100—180ms；禁用态不响应。焦点样式保留；不添加延迟等待动画结束的逻辑。答题输入/算式不做飞入或抖动。
- [x] 加入prefers-reduced-motion静态退化；失图时仍可开始、继续和看结果；active=false暂停/撤下动作，重新active不补播欢迎动作。
- [x] 运行：`npm.cmd --prefix frontend test -- --run tests/garden-hero.test.ts tests/sprout-companion.test.ts tests/illustrated-practice-card.test.ts`，预期全通过；`npm.cmd --prefix frontend run build`，预期类型及Web构建成功。
- [x] 390px实际页面截图＋能体现局部动作的短录屏（若录制能力不可用则用户现场查看动作，静态截图不能证明动画）；核对320/768布局。启动网页供体验并记录结果。

### 批1验收

- 首页仍接近用户认可的P2构图，叶片与角色边缘没有裂缝、重影或明显风格变化。
- 动作自行结束；开始练习立即响应；卡片、头像、资料与退出入口不丢失。
- 减少动态效果时静态可用；图片失败保留按钮。此批还没有成长、音效和交卷庆祝，明确标注。

## Task 2 / 批2：本次练习成长与保存恢复

**Files**
- 新增frontend/src/views/PracticeGrowth.vue及frontend/tests/practice-growth.test.ts。
- 修改LearningApp.vue、styles/learning-ui.css及frontend/tests/sprout-pages.test.ts。
- 复用frontend/tests/draft-save.test.ts、lifecycle.test.ts，不改保存/计时流程。
- 记录docs/evidence/ui-p3/batch-2/与dev-window-progress.md。

**Interfaces**：PracticeGrowth接收answered:number,total:number,active:boolean，展示层无请求、无完成事件。LearningApp传入已有answered与draft.total；active由答题页面、前台状态与减少动态效果约束。不得向业务输入反写数量。

### 阶段规则

| 条件 | 展示 |
|---|---|
| total<=0或练习尚未加载 | 不显示成长比例，不除以零 |
| answered=0 | 小苗初始静态，“已填写0/M题” |
| 0<answered<total/2 | 第一阶段，幼苗 |
| total/2<=answered<total | 第二阶段，舒展 |
| answered=total | 第三阶段，长成；显示“已填写M/M题”，不显示交卷成功 |

0题与第一阶段复用同一形象，故仍为三个成长形象；五题边界为0/1/2、3/4、5。非空答案计数遵循现有trim逻辑，重复编辑不累计成长，清空会回退。不得新增“已答对”判断或立即判题。

### 实施步骤

- [x] 写夹具检查：total=5的answered=0/1/2/3/4/5，total=1与100、total=0；同阶段改答案不重播展开、跨阶段变化后静止、active=false不播放。执行`tests/practice-growth.test.ts`观察RED。
- [x] 实现紧凑三阶段图形，放在原答题进度附近，整合现有已答信息，避免同屏重复三次报数。切换练习按practiceId重新建立显示，恢复时直接显示实际阶段，不从0开始播满成长过程。
- [x] 数字对辅助阅读工具可读，阶段图片装饰性alt为空；不逐个输入字符高频播报。保存文案继续由原saved/dirty/conflict呈现，与成长分开表达。
- [x] 页面夹具检查：练习内无大幅花园背景，仍有输入、手动保存、结束并检查；所有学生页头像保护保持；首页继续卡仍不使用currentSeq作为完成比例。
- [x] 执行`npm.cmd --prefix frontend test -- --run tests/practice-growth.test.ts tests/sprout-pages.test.ts tests/draft-save.test.ts tests/lifecycle.test.ts`，预期全通过；再运行Web构建。
- [x] 仅在mathdrill_demo用虚构学生新建5题，依次填写0/1/3/5题、清空一题，记录阶段；手动保存退出、重新登录恢复，比对已保存答案和阶段。失败/冲突使用现有单测mock，不在唯一开发库断网造故障。
- [x] 检查320/390/768及长知识点，输入框、软键盘可视区域由用户APP体验反馈；启动本批网页供用户确认。

### 批2验收

- 阶段与已填写数量一致；保存恢复和切换练习后不串状态。
- 填完未提交仍不会庆祝；答错也不会被误写为答对。
- 图形紧凑、不挤压算式和主按钮；原自动保存、冲突接管、计时与交卷入口可用。

## Task 3 / 批3：完成庆祝、音效与生命周期

**Files**
- 修改frontend/src/composables/useLearningApp.ts、views/LearningApp.vue、SproutCompanion.vue及styles/learning-ui.css。
- 新增frontend/src/composables/useInteractionFeedback.ts及frontend/tests/interaction-feedback.test.ts。
- 新增frontend/tests/practice-feedback.test.ts，复用draft-save.test.ts、lifecycle.test.ts与sprout-pages.test.ts。
- 更新素材说明及docs/evidence/ui-p3/batch-3/。

**Interfaces**
- useLearningApp新增只读消费的feedback ref：`{sequence:number,kind:'saved'|'completed',practiceId:string}|null`，以及foreground ref。sequence单调递增；只在真实成功路径写入，不监听页面名/保存文案猜成功。
- manualSave确认本次有效练习成功保存，且没有新修改尚待保存，才发saved；自动保存不发。无queue、冲突、失败时不得发成功。
- submit直接成功以及pending核对首次确认COMPLETED才发completed；按本会话练习ID去重；showResult历史查看不发。内容已确认错误的练习保留错误提示，不做庆祝。
- useInteractionFeedback返回enabled ref、`setEnabled(value:boolean):Promise<void>`、`play(kind:'tap'|'saved'|'completed'):Promise<boolean>`、`stop():void`；失败回false或设置可用提示，不向业务抛音频错误。内部管理本机偏好与音频资源，不发业务请求。
- foreground沿用现有Web/Capacitor生命周期入口提供，不另起一套计时或重复订阅native App监听；退出/换账号清理本轮反馈标记，设备音效偏好保留。

### 实施步骤

- [x] 写RED：成功手动保存发一次、自动保存不发、失败/冲突/保存时又改答案不误报；提交成功一次、核对成功一次、重复核对/旧结果不重复；退出换账号与切换练习不串反馈。
- [x] 在现有成功分支补事件；保持run/save/submit/reconcile/leavePractice错误处理与返回行为。不得为了播放效果等待音效完成再提交或跳页。
- [x] 写RED：默认关闭、本机开关恢复、存储读写失败可用、播放被拒绝无未处理异常、关闭立即停止、后台不发声/返回不补播、快速点击不叠加多轨。音频测试注入/模拟平台能力，不依赖扬声器才能跑测试。
- [x] 用内置音频能力生成三类短提示，不引入外链或背景音乐。tap约40—80ms、saved约100—180ms、completed约300—600ms；低音量无尖锐高响度，实际听感由用户确认。快速tap合并/限制在每120ms最多一次，成功反馈优先，停止旧音避免叠加。
- [x] 学生个人页及答题页非干扰位置提供“提示音”开关，建议用带aria-pressed的按钮；默认关。开启必须来自用户点击，初始化失败显示简短提示，不虚标能播放；后续播放被限制时同样安静退化。
- [x] 成功庆祝使用已有完成角色＋有限叶片/星点，1—2秒自动结束，图片装饰不拦点击；后台停止并消费本次事件，返回不补播。减少动态效果仅静态鼓励。成绩、错误和未答数不因庆祝变化。
- [x] tap仅绑定明确学生操作（开始/继续/翻题/选择），不对输入每个字符或自动保存发声；交卷/保存按钮避免同一动作tap与success叠响。声音开关本身不发tap。
- [x] 执行`npm.cmd --prefix frontend test -- --run tests/interaction-feedback.test.ts tests/practice-feedback.test.ts tests/draft-save.test.ts tests/lifecycle.test.ts tests/sprout-pages.test.ts`，预期全通过；类型/Web构建成功。
- [x] 网页实际体验默认静音、主动开启、手动保存、提交成功、关闭后操作及历史成绩重看；通过本机mock测试失败边界，不在开发库做故障注入。用户在APP实际体验声音与减少动态效果，未确认项单独列出。

### 批3验收

- 只在确认成功后反馈，同一完成不重播；保存失败/冲突/核对中安静保留提示。
- 声音默认关、随时可关，播放失败不影响保存答题；关闭与后台立即停止，不积压补播。
- 庆祝短暂且不遮挡成绩，不引入正确率奖励门槛；零分仍真实显示，鼓励不羞辱或错误表扬全对。

## Task 4 / 批4：整体检查、独立打包与体验交付

**Files**：scripts/build-android.ps1、docs/startup-classroom.md、docs/assets-sprout-p3.md、docs/dev-window-progress.md；新增docs/evidence/ui-p3/final/及独立delivery产物。其他启动脚本不因P3无关调整。

### 实施步骤

- [x] 跑完整`npm.cmd --prefix frontend test -- --run`，预期全部通过，记录实际数量，不预写通过数；跑`npm.cmd --prefix frontend run build`，预期vue-tsc/Vite均成功。
- [x] 浏览器用虚构账号走5题填写—保存退出—恢复—确认交卷—结果—错题再练，核对成长及完成去重；家长预览发送及查看结果；管理员只读查看390题。记录新增工程检查练习，不冒充真实儿童试用。
- [x] 做320/390/768布局、长昵称/知识点/100题夹具、减少动态效果、图片失败、音频不可用检查。截图证明布局，实际录屏或用户现场体验证明动作与听感，不能用静态图证明音效通过。
- [x] 一次独立代码检查，关注本计划Review Focus；新发现重要问题补复现检查、修复并重跑相关及完整测试。记录未测原生/真机项，不擅自执行模拟器验收。
- [x] 扩充build-android.ps1独立批次白名单，默认最终P3批次，保留同名拒绝覆盖；执行`./scripts/build-android.ps1 -Version ui-p3-v1`，预期Android app/test编译成功。同名已存在时先确定新名称，不能删除旧包重跑。
- [x] 创建独立Web ZIP与manifest；核对dist、WebZIP、APK对应资源字节一致，音频若为生成式则核对控制模块同版，不虚构音频文件。记录HEAD、未提交工作区指纹、资源字节数和所有产物SHA256。
- [x] 只读比对本轮保护快照与全部旧包SHA256，旧题库/账号/档案/关系/原练习/开发和验收库保持。只对虚构体验记录做正常操作，不清理旧数据。
- [x] 更新启动/素材/工程记录；保留当前软件服务供体验。若模拟器已离线，启动既有实例，安装独立新版并打开，不跑设备测试；真实包名从当前配置读取，不凭记忆。
- [x] 提供学生—家长—管理员简短体验流程与本轮实际截图、变更和未测项。用户体验反馈另记，不提前标为用户通过。

### 最终交付清单与完成门槛

| 交付 | 完成证据 |
|---|---|
| 五项完整互动范围 | 批1—3逐项检查及用户可操作页面；不能只做首页动效就称完整版 |
| 必要自动检查 | 当前测试日志、类型/Web/Android编译日志 |
| 视觉/交互证据 | 三宽布局截图、真实成长步骤、动作展示、音效开关实际检查；用户APP听感未确认则明确待确认 |
| 独立包 | debug.apk、test.apk、web.zip、manifest及实际哈希，同版资源核对 |
| 数据与前版保留 | 基线/结束比对与旧包哈希；P2仍可回用 |
| 工程记录 | 每批范围、真实改动/验证/产物、工作区指纹、限制和用户反馈 |

工程实施完成与用户体验通过分开；真机、弱网专项、原生文件专项、另一台电脑交接、公网试用不随P3自动完成。若声音/动作在APP出现差异，先如实报告并修正，不能用Web检查替代APP实际效果。

## 计划制定时自检结论（历史，仅文档检查）

五项范围均有所属批次；三阶段边界、成功事件来源、默认静音/失败退化和交付隔离均有明确规则与验收。新增接口仅为展示与反馈，不引入第二份业务状态。旧包/数据保留、无模拟器验收与无上传约束覆盖四批。所有任务尚未执行，无测试/构建结果声明。


批1执行备注：采用原P2图片CSS裁切分层，不新增语义图片；成长素材按用户指令留在批2。Web动画实时样式记录和静态截图已保存，录屏未制作，动画观感由用户现场体验；原生后台行为未验收。批2—4未实施。

批2执行备注：三个成长形象复用P2同源图，叶片尺寸为52%/78%/100%，无新位图。网页仅新建一份五题检查练习，填/清空/重新登录恢复已查；同期旧练习41交卷变化保留，严格逐行不变快照未通过，已做细项比对并单独记录，不能写作全库逐行无变化。原生后台、软键盘实际体验待用户确认，未做模拟器验收。

成长视觉v2修订：用户反馈v1辨识度不足，另按docs/ui-p3-growth-visual-revision.md实现三张独立两/四/六叶插画与持续阶段名，80px占位；替代此前同图叶片尺寸方案，阶段规则不变。旧v1历史记录保留，第三批仍未开始。

第三批执行说明：独立ui-p3-b3-v2阶段包、72/72前端检查、Web/Android编译与网页新五题流程已完成。音效用内置音频能力生成，不新增音频文件；原生回调接线有单元检查，实际扬声器听感/真机后台仍由用户体验，不将其写作设备验收通过。第四批整体回归/最终交付尚未执行。


第四批最终执行说明（2026-10-31）：完整19文件75/75、类型/Web/Android app/test编译、三身份网页流程及三宽布局检查完成；独立ui-p3-v1包与manifest已核对同版资源，60份旧交付哈希和严格数据保护快照保持。独立检查无阻塞项，两项覆盖建议已补齐。APP仅安装启动，无模拟器验收；P3工程实施完成，最终用户体验待反馈。详见docs/evidence/ui-p3/final/completion.md与docs/ui-p3-experience.md。
