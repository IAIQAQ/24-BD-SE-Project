# I2 三角色接口补充

2026-10-28。统一前缀 `/api/v1`、响应外层、会话Cookie、CSRF规则和学生I1接口沿用现有契约。所有写操作需要 `X-CSRF-TOKEN`；注册也需要先获取CSRF。编号按字符串传给前端，禁止把BIGINT转换为JavaScript Number。

## 账号

- `POST /auth/register`：loginName、password、nickname、role、grade。公众role仅STUDENT/PARENT；学生grade为1—3；登录名3—64位字母数字/下划线/短横线，admin保留。重复409，错误输入400。注册不直接登录。
- `PUT /auth/profile`：学生修改nickname、grade。历史练习保留创建时的年级和知识点名。
- 管理员只在本地启用种子初始化且有独立私有密码时创建；已有账号不重置密码。

## 家庭关联与任务

- `GET /family/links`：按当前学生或家长返回本人的关系及申请。
- `POST /family/requests`：家长提交studentLoginName，进入PENDING。同一对账号复用申请记录。
- `POST /family/links/{id}/respond`：学生传accept，同意ACTIVE或拒绝REJECTED。
- `POST /family/links/{id}/unbind`：当事人传confirm=true，变UNBOUND。解绑保留学生已有任务，立即取消家长后续读取和操作权限。
- `POST /family/children/{child}/previews`：pointId、total、operationKey；生成固定题干快照，不含标准答案，学生列表不显示预览。
- `POST /family/previews/{id}/send`：confirm=true。15分钟有效期；重新检查当前关系、学生年级、知识点版本和题目版本/审核/启用，发送同一份快照为PENDING。重复发送返回已有任务。
- `POST /family/practices/{id}/withdraw`：confirm=true，仅原发送家长且尚未开始可撤回。学生open与撤回共用练习锁。
- `GET /family/children/{child}/practices`：最近100条任务和total。
- `GET /family/children/{child}/practices/{task}`：家长只读进度/正式结果。
- `GET /family/children/{child}/mistakes`：当前孩子错误记录，只读。
- `GET /family/children/{child}/stats?start=YYYY-MM-DD&end=YYYY-MM-DD`：默认最近7个北京时间自然日，仅已完成记录；汇总正确率按总答对题数/总题数计算，包含每日和知识点统计。

学生I1列表新增PENDING、WITHDRAWN、VOID状态和source/senderId等信息；首次open将PENDING转为IN_PROGRESS。VOID/WITHDRAWN不允许写答案、计时或提交；提交核对不重新赋予编辑权。数学错误标记通过contentError/errorNote显示。

## 管理员

- `GET /admin/overview`：题目总数、可用题数、学生数和完成练习数。
- `GET/POST /admin/points`，`PUT /admin/points/{id}`：分类名称、年级、学期、教材及依据、启用状态。修改递增分类版本，旧家长预览需重新生成。
- `GET /admin/questions?query=&page=1`：每页30条。
- `POST /admin/questions`，`PUT /admin/questions/{id}`：pointId、stem、standardAnswer、explanation。当前仅非负整数标准答案；规范化题干与答案去重；创建/修改后待审核且停用，修改保留历史版本。
- `POST /admin/questions/{id}/review`：confirm=true、expectedRevision。当前版本与管理员所见不一致返回409；已确认错误的版本不能重新审核。
- `POST /admin/questions/{id}/enabled`：enabled。启用必须审核通过且分类启用；普通停用不改已有快照。
- `GET /admin/questions/{id}/versions`：历史版本。
- `GET /admin/questions/{id}/versions/{version}/impact`：相关未完成/已完成练习数。
- `POST /admin/questions/{id}/versions/{version}/error`：confirm=true、reason。精确定位版本；相关待完成/进行中练习整份VOID，预览EXPIRED；完成成绩保留并标记，不提供改分。
- `GET /admin/import-template`：下载xlsx模板。
- `POST /admin/imports`：multipart的file，单表.xlsx、5MB以内、最多500行；业务列为题干/年级/学期/知识点/答案类型/标准答案/解析。当前答案类型填整数，禁用公式。返回批次id、READY/REJECTED及逐行问题，存在问题时整批不导入。
- `POST /admin/imports/{id}/confirm`：confirm=true，整批事务写入；重复确认返回已有结果，导入题待审核且停用。
- `GET /admin/imports/{id}`：查询本人导入批次的持久结果。

无关联/非本人/角色越权403，失去会话401，业务状态冲突409，锁竞争暂时不可用503。前端错误提示不能替代服务端鉴权。
