# 会议室预约与资源协调系统

一个基于前后端分离架构的会议室预约系统，支持用户登录、会议室查询、日历视图预约、我的预约管理与管理员审批。

## 技术栈

- 后端：Spring Boot 3.x、MyBatis-Plus、MySQL 8、Redis、Knife4j
- 前端：Vue 3、Vite、Element Plus、FullCalendar

## 项目结构

- `backend/`：后端服务（REST API）
- `frontend/`：前端 Web 应用
- `start-dev.bat`：Windows 一键启动脚本（同时启动前后端）

## 角色与权限矩阵（默认）

| 功能模块 | 普通用户 | 管理员 | 超级管理员 |
| --- | --- | --- | --- |
| 登录/注册 | ✅ | ✅ | ✅ |
| 日历视图（查询） | ✅ | ✅ | ✅ |
| 我的预约（创建/取消/删除） | ✅ | ✅ | ✅ |
| 会议室管理（新增/编辑/删除/维护） | ❌ | ❌ | ✅ |
| 预约审批（待审批列表） | ❌ | ✅ | ✅ |
| 预约审批（撤销已审批结果） | ❌ | ❌ | ✅ |
| 用户与权限管理 | ❌ | ✅（不可授予超级管理员） | ✅ |
| 运营看板（统计概览） | ✅ | ✅ | ✅ |

> 可通过 `backend/src/main/java/com/example/mrs/domain/Role.java` 调整角色定义，权限控制在各 Controller 的 `@PreAuthorize` 中。

## 运行前准备

请先确保本机已安装并可用：

- JDK 17+
- Maven 3.8+
- Node.js 18+（含 npm）
- MySQL 8（创建数据库：`meeting_room`）
- Redis（默认 `127.0.0.1:6379`）

> 说明：脚本会尝试自动启动 Redis（需 `redis-server` 在 PATH 中）；MySQL 仍需手动提前启动。

## 快速启动（推荐）

在项目根目录双击或执行：

```bat
start-dev.bat
```

脚本会自动：

1. 校验 `backend/` 与 `frontend/` 目录是否存在
2. 校验 `mvn` 与 `npm` 命令是否可用
3. 如前端缺少 `node_modules`，自动执行 `npm install`
4. 尝试自动启动 Redis，并进行可选的 `PING` 连通性校验
5. 分别打开窗口启动后端与前端

## 手动启动

### 1) 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 2) 启动前端

```bash
cd frontend
npm install
npm run dev
```

## 默认访问地址

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- API 文档（Knife4j）：`http://localhost:8080/doc.html`

## 常见问题

- 前端启动失败（端口占用）：修改 `frontend/vite.config.ts` 或释放 5173 端口。
- 后端连接数据库失败：检查 `backend/src/main/resources/application.yml` 中数据库配置与 MySQL 状态。
- Redis 连接失败：确认 Redis 已启动且端口配置正确。
