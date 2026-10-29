# I3 错题重练与强化接口

更新：2026-10-29。沿用 `/api/v1`、会话 Cookie、CSRF 和原有作答接口；ID 均以字符串传输。V7 追加历史来源、练习类型与知识点快照，不修改 V1—V6。

| 方法与路径 | 输入与用途 |
| --- | --- |
| POST `/students/{id}/review-options` | 学生本人，`{itemIds:[历史错题itemId]}`，1—100条，不接受重复ID。 |
| POST `/practices/replays` | `{itemIds,operationKey}`，按历史题干、标准答案、解析新建空白原题重练；同内容按历史年级去重，禁止混年级和已确认错误的历史版本。 |
| POST `/practices/reinforcements` | `{seedItemIds,allocations:[{pointId,total}],operationKey}`，学生同类强化；选一个历史年级，覆盖全部所选知识点，各项正整数、合计1—100。 |
| GET `/students/{id}/mistakes/{item}` | 返回历史详情及同内容全部正式尝试，包含后来答对的记录；不清除原错误。 |
| POST `/family/children/{child}/review-options` | 当前有效关联家长，同学生选择结构。 |
| GET `/family/children/{child}/mistakes/{item}` | 家长只读历史，校验有效关联和错题所属孩子。 |
| POST `/family/children/{child}/reinforcement-previews` | 同强化输入，返回固定 PREVIEW 与实际题目，不含标准答案。 |
| POST `/family/previews/{id}/send` | 复用原接口 `{confirm:true}`。15分钟内确认；逐项校验所有知识点快照、题目版本及预览时孩子年级，原题序不变。 |

选择响应包含 `replay:{allowed,reason,total,items}`、`grades`、`groups:[{grade,seedItemIds,points:[{pointId,pointName,available,reason}]}]`。

`replay.items` 是去重后的原题重练展示；强化必须使用所选 `groups[].seedItemIds`，不能从 `replay.items` 推导。相同内容曾从分类A移到B且两次答错时，重练仍可去重为一道，强化必须保留A和B。

强化从当前启用且已审核的题池随机无放回抽取；普通停用题不进入当前强化池。历史分类停用或改变年级时拒绝，题量不足整份不写入。草稿不返回标准答案，提交后后端判分。

`practiceKind` 为 NORMAL / REPLAY / REINFORCE。重练与强化进入原 `open` → `draft` → `answers` → `submit` 流程，保存队列、编辑权、计时、提交核对共用。统计按每道题的知识点快照归属。

创建结果不明时，关闭并重新打开相同选择继续使用原 operationKey、原年级和题量；改变选择、年级或题量后才建立新操作。服务端同键同输入返回同练习，同键不同输入返回冲突。家长预览与学生创建分开授权，家长不能调用学生重练或代答接口。
