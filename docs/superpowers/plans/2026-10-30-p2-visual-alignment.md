# P2示意图视觉对齐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 将P2从“P1旁加小图”修订为接近B1/B2的完整静态儿童视觉，保留真实功能与数据。

**Architecture:** Vue/CSS实现布局和交互，分层本地素材提供场景与装饰；保留原composables/API。首页先对照参考图验收构图，再协调错题/结果页。

**Tech Stack:** Vue 3、TypeScript、现有CSS/UiIcon、内置imagegen、现有Vitest及Capacitor；无新运行时依赖。

**Spec:** [P2视觉修订idea V2](../../ui-p2-visual-revision.md)

## Global Constraints

- 以当前完整未提交工作区为基线，先备份，不reset/clean或只从HEAD新建项目，不覆盖P1/ui-p2-v1及更早包。
- 不改题库、账号、家庭关系、历史、后端/API/schema；不为截图伪造记录或进度。
- 当前Summary不提供已答数；currentSeq只表示上次题号，不是完成比例。
- 不进入P3；不自动模拟器验收、提交、上传或联系课程窗口。
- 图片不含正文、按钮、题目/成绩；操作和文字为真实HTML，点击区至少48px。

## Review Focus

1. 320px长昵称/长知识点/三位数题量，内容不挤出或遮挡；T2/T3负责。
2. 记录状态和currentSeq被误写成完成数；T3用状态夹具检查。
3. 静态插画失效、加载迟缓导致按钮消失或布局跳动；T1/T2负责。
4. 顶部重排导致退出、改级或辅助登录入口丢失；T2/T4检查可达性。
5. 只靠少量截图或绿色风格宣称视觉完成；T4按spec七项逐项对照，并交用户确认。

## Task 1：素材分层及布局骨架

文件：新增`frontend/src/assets/sprout/p2-scene/{garden,task-notebook,continue-book,avatar}.webp`；保留现有素材和组件；新增`frontend/src/views/GardenHero.vue`。

接口：GardenHero仅接收`busy:boolean`，发出`start:[]`；不读取账号/接口，父层继续绑定app.configure。装饰图片alt为空。必要的失败回退由组件本地状态处理。

- [x] 备份当前源码、全部delivery哈希与数据库保护快照；记录当前真实工作区指纹。
- [x] 基于B1生成四份独立素材，复用已有小芽角色；保留生成原图，进行尺寸压缩/WebP转码，核对透明边缘和无内嵌文字。
- [x] 对GardenHero的start事件、busy禁用、图像错误仍保留主按钮先写测试RED，再实现真实HTML标题/短句/按钮和分层场景。
- [x] 固定背景区域比例；角色在独立网格占位中，不用覆盖主按钮的绝对定位。背景裁切不影响标题和按钮。
- [x] 跑新增组件测试GREEN，记录实际资源体积。先做这一组件的390px实际截图对照，不生成新的整页概念图代替真实页面。

## Task 2：首页header与花园横幅

文件：`LearningApp.vue`、`styles/learning-ui.css`、`StudentHeader.vue`、必要时`SproutCompanion.vue`；测试复用`tests/render-ui.ts`、`sprout-pages.test.ts`及`student-header.test.ts`、`garden-hero.test.ts`。顶部栏组件只负责展示和发出openProfile事件，避免在主页面重复图片回退与布局，不引入账号状态或请求。

接口：消费原profile/page/busy/app.configure，不新增profile API。学生头像按钮设置page='profile'；年级只读；退出仍使用app.signOut且个人页可达。家长/管理员保持原header及退出入口。

- [x] 先补学生主页头像进入个人页、年级取真实profile、开始按钮事件和忙碌状态测试RED。
- [x] 调整学生顶部品牌/标签/头像，保留登录页及家长/管理员行为；实现真实昵称问候。
- [x] 用GardenHero替换当前纵向小芽hero，标题“一起练口算”在320/390px不出现尾字孤立换行，左侧CTA、右侧角色和背景达到spec比例。
- [x] 核对图片失败时仍能开始、键盘焦点可见，320/390/768px不横向溢出；必要时短标题随宽度缩字号，不截断用户名。
- [x] 保存与B1同宽实际对照截图，记录七项中目前实现项及差距，再继续Task 3。

## Task 3：插画练习卡片与协调页面

文件：`LearningApp.vue`、`styles/learning-ui.css`、`SproutCompanion.vue`；新增`frontend/src/views/IllustratedPracticeCard.vue`和对应`tests/illustrated-practice-card.test.ts`，必要时调整`ReviewPracticePanel.vue`呈现。

接口：IllustratedPracticeCard接收`record:Summary,busy:boolean`，发出`open:[id:string]`；父层按原status调用showResult/resume，不复制请求/重试状态。

- [x] 卡片先写状态行为测试RED：PENDING开始、IN_PROGRESS继续、COMPLETED看结果、WITHDRAWN/VOID真实状态；来源/练习类型仍可读，不渲染“已答currentSeq/total”。
- [x] 实现绘本/铅笔卡片；标题简短、来源和类型醒目、知识点可完整查看，所有records和原分页保留，不静态只展示两张卡。
- [x] 将已保存提示限定于真实IN_PROGRESS记录；显示题号时用“上次看到第N题”，不新增伪完成进度。
- [x] 首页列表末尾增加流内鼓励区，复用complete角色；不固定叠在底栏上，不遮挡后续卡片和焦点。
- [x] 协调错题插画页头、结果和空状态，保留原条件/选择/历史/解析，答题页不加背景插画或角色。
- [x] 执行卡片/页面测试GREEN；覆盖空列表、多条长记录、同页与跨页选择、不同历史年级、低分/零分仍真实显示及图片失败。

## Task 4：视觉对照、必要回归与独立交付

文件：`scripts/build-android.ps1`、`docs/startup-classroom.md`、`docs/dev-window-progress.md`；新证据`docs/evidence/ui-p2-v2/`。

- [x] 执行完整前端测试、类型检查和Web构建；检查管理员/家长未被学生样式影响，个人资料/退出/注册/家庭入口可达。
- [x] 网页完成一条5题练习及保存恢复、结果/错题入口、家长预览发送及结果查看；仅在mathdrill_demo，不运行全量后端造数测试。检查前后核对原数据和旧包，新增检查记录如实记载。
- [x] 390px实际截图与B1/B2按七项逐项对照；检查320/768px、长名称和安全区留白。未达到横幅/角色/卡片构图目标时继续修正，不能用测试通过替代视觉完成。
- [x] 构建脚本增加独立ui-p2-v2且同名拒绝覆盖，输出APK/test.apk/Web ZIP及manifest；若同名存在改新批次并记录。核对包内所有图像本地存在、与dist同字节，旧包哈希保持。
- [x] 独立代码检查本轮差异，修复阻塞问题；不为此执行模拟器验收。记录图片失败回退、资源体积、实际检查及未测项。
- [x] 启动网页/后端，安装打开APP仅供用户体验；更新启动说明和专属工程记录，使用工作区指纹定位，不编造新提交。交用户对照参考图确认。

## 完成边界

实现和代理检查完成不等于用户视觉验收通过。P1及ui-p2-v1保留；P3、上传源码和另一台电脑交接不随本计划自动执行。

## 2026-10-30计划核对状态

本次继续完善计划，未接入页面或执行新包构建。现有工作区已经包含素材及GardenHero、StudentHeader、IllustratedPracticeCard草稿；已有组件日志显示3个测试文件、5项检查通过。LearningApp仍使用旧sprout-hero，尚未消费上述三个组件，因此不能宣称整页视觉完成、功能回归通过或用户验收通过。执行时复用并核对这些草稿，不重复生成素材，不只从HEAD重建。

首页接入后先提交真实390px截图供用户查看，明确与B1的相似处和仍有差距的地方；先解决整体构图，再继续协调其他页面和最后打包。若用户要求停在首页预览，则暂停后续页面实施。6—10有效小时为整轮初估，不能把组件测试耗时当作整轮开发耗时或承诺完成时间。


## 执行结果

ui-p2-v2实现与工程检查完成，用户视觉验收仍待反馈。完成证据、范围和检查限制见`../../evidence/ui-p2-v2/completion.md`及`../../dev-window-progress.md`。前文计划核对状态是实施前历史快照，不代表当前未接入状态。
