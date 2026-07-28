# T02 匿名游客访问修改日志

更新时间：2026-07-27

## 本轮修改

### 1. 临时游客令牌

- 新增 `POST /auth/guest`。
- 使用现有 Sa-Token JWT 技术栈签发临时游客令牌。
- 游客身份格式为 `guest:<随机会话 ID>`，有效期为 7200 秒。
- 每次新浏览器会话生成独立会话 ID，不要求手机号、密码或昵称输入。
- 响应明确返回 `guest: true` 和 `persistence_authorized: false`。

### 2. 浏览器会话隔离

- 游客令牌、会话 ID、游客 ID 和过期时间仅写入 `sessionStorage`。
- 不把游客令牌写入 `localStorage`。
- 会话过期后前端自动清除旧游客数据并申请新会话。
- 同一标签页刷新时复用现有游客会话，不重复签发令牌。
- 普通用户登录成功后自动清除临时游客会话。

### 3. 免登录游客工作台

- 根路径 `/` 改为直接进入 `/explore`。
- `/explore` 增加游客会话守卫，无会员账号时自动创建临时游客会话。
- 桌面端显示“临时访客 · 登录保存”。
- 移动端显示“登录保存”。
- 登录入口携带 `redirect=/explore`，用于用户主动登录后返回游客工作台。
- 原会员工作台 `/home` 继续要求正式登录，不使用游客令牌绕过。

## 隐私与数据边界

- 游客会话不写入 `t_user`，数据库检查结果为 0 条 `guest:` 用户记录。
- T02 不采集手机号、精确身份、位置或偏好。
- T03 后的偏好与位置将默认沿用当前会话级存储边界。
- 跨会话保存仍必须由用户主动登录并授权，T02 未自动开启持久化。

## 涉及文件

### 后端

- `ai-tourism-backend/src/main/java/com/example/aitourism/controller/AuthController.java`
- `ai-tourism-backend/src/main/java/com/example/aitourism/dto/user/GuestSessionResponse.java`
- `ai-tourism-backend/src/main/java/com/example/aitourism/service/AuthService.java`
- `ai-tourism-backend/src/main/java/com/example/aitourism/service/impl/AuthServiceImpl.java`
- `ai-tourism-backend/src/test/java/com/example/aitourism/controller/AuthGuestControllerTest.java`

### 前端

- `ai-tourism-frontend/src/router/index.js`
- `ai-tourism-frontend/src/utils/api.js`
- `ai-tourism-frontend/src/utils/guestSession.js`
- `ai-tourism-frontend/src/views/Explore.vue`
- `ai-tourism-frontend/src/views/Login.vue`

## 验证结果

- 后端测试：3 个通过，0 失败。
- 前端生产构建：通过。
- 无凭据创建游客会话：通过。
- 两次新建游客会话 ID 不同：通过。
- 游客令牌有效期：7200 秒。
- 持久化授权默认值：false。
- MySQL `t_user` 中 `guest:` 用户数量：0。
- 无账号访问 `/` 自动进入 `/explore`：通过。
- 刷新页面复用当前游客会话：通过。
- 无账号访问 `/home` 跳转 `/login?redirect=/home`：通过。
- 390 × 844 移动端横向溢出：无。
- 浏览器控制台错误：0。

## T02 状态

用户已验收通过，T03 已开始。
