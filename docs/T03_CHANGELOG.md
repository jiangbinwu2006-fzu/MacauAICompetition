# T03 游客偏好采集修改日志

## 完成内容

- 在游客工作台新增偏好表单，采集兴趣、出发时间、最晚结束时间、最大步行距离、必去地点、交通偏好、语言和无障碍需求。
- 兴趣默认选择文化与美食，时间默认 `09:00` 至 `14:00`，最大步行距离默认 5 公里，交通默认组合方式，语言默认简体中文。
- 必去地点直接使用 T01 澳门目录点位，可多选并逐项移除。
- 添加必填校验、时间先后校验、选项白名单校验、数量限制和步行距离范围校验。
- 添加“一键重置”，恢复全部默认值并清除当前会话已保存的偏好。
- 保存成功后显示当前会话状态；刷新页面会从当前匿名游客会话重新读取并回显。
- 偏好只写入当前 Sa-Token 游客会话，不写入用户表，也不会跨游客会话持久化。
- 游客工作台新增“偏好 / 点位”视图切换，保留 T01 高德地图和点位浏览能力。

## 新增接口

- `GET /api/preferences`：读取当前游客会话偏好；未保存时返回默认值。
- `PUT /api/preferences`：校验并保存当前游客会话偏好。
- `DELETE /api/preferences`：删除已保存偏好并返回默认值。

以上接口均要求有效的游客令牌或正式用户令牌。

## 主要文件

- `ai-tourism-backend/src/main/java/com/example/aitourism/controller/PreferencesController.java`
- `ai-tourism-backend/src/main/java/com/example/aitourism/dto/preferences/VisitorPreferencesRequest.java`
- `ai-tourism-backend/src/main/java/com/example/aitourism/dto/preferences/VisitorPreferencesResponse.java`
- `ai-tourism-backend/src/main/java/com/example/aitourism/service/PreferencesService.java`
- `ai-tourism-backend/src/main/java/com/example/aitourism/service/impl/PreferencesServiceImpl.java`
- `ai-tourism-backend/src/test/java/com/example/aitourism/controller/PreferencesControllerTest.java`
- `ai-tourism-frontend/src/components/PreferencesPanel.vue`
- `ai-tourism-frontend/src/utils/api.js`
- `ai-tourism-frontend/src/views/Explore.vue`

## 验证结果

- 后端自动化测试：5 个通过，0 个失败，其中 T03 新增 2 个控制器测试。
- 数据库集成检查：偏好保存在当前 Sa-Token 会话，不新增数据库表，不写入 `t_user`。
- 前端生产构建：通过，共转换 51 个模块。
- 真实接口链路：默认读取、保存、再次读取和重置均通过。
- 浏览器保存后刷新回显：兴趣、时间、6.5 公里步行上限、必去点、公交偏好和无台阶需求均正确保留。
- 冲突时间校验：`10:30` 出发、`09:30` 结束被前端准确拦截。
- 一键重置：恢复文化/美食、`09:00` 至 `14:00`、5 公里、组合交通，并清空必去点和无障碍需求。
- 390 x 844 移动端横向溢出：无；表单按钮和地图均正常显示。

## T03 状态

用户已验收通过，T04 已开始。

## 2026-07-27 所在地偏好增强

- 新增主动定位、手动选择澳门起点及清除位置能力，不在页面加载时自动请求定位权限。
- 新增会话级字段：`current_longitude`、`current_latitude`、`current_location_name`、`location_source`。
- 路线生成优先使用游客所在地匹配最近的有效澳门点位，并以真实位置计算首段距离。
- 所在地仍遵守 T03 会话级隐私原则，不进入数据库持久化。
- 验证结果：后端 9 项测试通过（含所在地路线回归测试），前端生产构建通过，运行态氹仔定位与附近路线匹配通过。
