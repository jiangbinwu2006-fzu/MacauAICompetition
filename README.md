# MacauAICompetition - 澳门文旅智联 v2.0.0

澳门公益文旅智慧助手第二版，完成从匿名游客偏好采集、当前位置路线生成、自然点位推荐、实时事件提醒、动态改线到运营反馈处置的非商业闭环。

## 仓库结构

- `ai-tourism-frontend`：Vue 3 + Vite 游客端与运营看板
- `ai-tourism-backend`：Spring Boot + MyBatis + Sa-Token 业务服务
- `ai-tourism-agent`：FastAPI Agent 服务（保留原项目技术栈）
- `scripts`：Windows 项目启停与后端守护脚本
- `docs`：T01-T16 开发日志、BUG 修复日志及发布说明

## v2 主要能力

- 免登录匿名游客会话与会话级偏好
- 简中、繁中、英文、葡文四语界面
- 高德澳门地图、点位检索、步行与智能公交规划
- 60 个澳门四语分类点位，使用高德真实位置解析与本地缓存
- 按用户时间窗、当前位置、必去点和步行上限生成路线
- 从用户实际当前位置绘制首段路线，展示唯一行程起终点
- 关闸口岸及酒店出发的两条一日往返固定演示路线
- 自然 POI 推荐，不含广告、赞助或付费权重
- SSE 事件提醒、局部改线、整程重排、撤回与恢复
- 无障碍显示、纯文字路线导出、地图故障降级
- 反馈工单与公益运营看板

## 环境要求

- Node.js 18+
- Java 17+
- Maven 3.9+（也可使用后端自带 `mvnw.cmd`）
- Python 3.11+
- MySQL 8+
- 高德地图 JavaScript API Key、Security JS Code 和 Web API Key
- 可选：DeepSeek/OpenAI 兼容 API Key

## 配置

1. 在 `ai-tourism-frontend` 中将 `.env.example` 复制为 `.env.local`，填写高德配置。
2. 在 `ai-tourism-backend/src/main/resources` 中将 `application-example.yml` 复制为 `application.yml`，填写 MySQL 和模型服务配置。
3. 在 `ai-tourism-agent` 中将 `.env.example` 复制为 `.env`，填写 Agent 需要的模型配置。
4. 执行 `ai-tourism-backend/sql/migrations` 中的 SQL 迁移。

> 仓库不包含任何实际 API Key、数据库密码、本机虚拟环境或运行数据。

## 安装与启动

```powershell
cd ai-tourism-frontend
npm install
npm run build

cd ..\ai-tourism-backend
.\mvnw.cmd test
.\mvnw.cmd package -DskipTests

cd ..\ai-tourism-agent
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

完成配置和后端打包后，在仓库根目录运行：

```powershell
.\start-project.cmd
```

访问地址：

- 游客端：`http://localhost:3001/explore`
- 公益运营端：`http://localhost:3001/ops`
- 后端：`http://localhost:8290`

停止项目：

```powershell
.\stop-project.cmd
```

## 开发与 BUG 日志

- [v2.0 项目对接文档](docs/PROJECT_HANDOFF_V2.md)
- [BUG 修复日志](docs/BUGFIX_CHANGELOG.md)
- [T01 日志](docs/T01_CHANGELOG.md)
- [T02 日志](docs/T02_CHANGELOG.md)
- [T03 日志](docs/T03_CHANGELOG.md)
- [T04 日志](docs/T04_CHANGELOG.md)
- [T05-T16 日志](docs/T05_T16_CHANGELOG.md)
- [澳门点位地址核对表](docs/MACAU_POI_ADDRESS_REVIEW.md)
- [v2.0.0 发布说明](docs/RELEASE_NOTES_v2.0.0.md)
- [v1.0.0 发布说明](docs/RELEASE_NOTES_v1.0.0.md)

## 当前边界

v2 仍为非商业公益闭环版本。商户自助入驻、广告投放、赞助、结算、商业流量公平算法和支付功能未包含在本版中。
