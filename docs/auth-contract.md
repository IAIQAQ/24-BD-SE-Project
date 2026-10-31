# 已实现认证接口契约

基地址 `/api/v1`。健康检查之外，成功响应为 `{code:"OK",data:...,traceId:"..."}`；错误为 `{code:"...",message:"...",traceId:"..."}`。traceId 目前是响应标识，尚未接入日志关联。

| 方法与路径 | 请求 | 成功响应 | 拒绝 |
| --- | --- | --- | --- |
| GET /health | 无 | `{status:"UP"}` | 服务不可用 |
| GET /auth/csrf | 浏览器/APP cookie | data: token、headerName | 服务不可用 |
| POST /auth/login | JSON loginName/password；CSRF头 | data: UserProfile；服务端轮换会话 | 400格式，401凭据，403CSRF |
| GET /auth/me | 会话Cookie | data: UserProfile | 401未登录/失效 |
| POST /auth/logout | 会话Cookie；CSRF头 | 204，无响应体；撤销会话 | 403CSRF |
| GET /students/{id}/profile | 会话Cookie | 当前学生的 UserProfile | 401未登录，403他人资料 |

UserProfile 字段：id（字符串）、loginName、nickname、role（当前 STUDENT）、grade（整数）。不包含密码哈希或会话凭证。

调用方统一通过 `frontend/src/api/client.ts` 的 health/login/me/logout，使用 `credentials:include`。POST 前获取当前会话的 CSRF token，登录轮换会话并废弃旧 token；下次 POST 重新获取。业务接口尚未实现，不把设计中的字段当实际服务。
