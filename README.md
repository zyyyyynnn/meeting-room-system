# 会议室预约与资源协调系统

基于前后端分离架构的会议室预约系统，覆盖登录注册、运营看板、会议预约日历、会议室管理、我的预约、预约审批、用户管理和通知抽屉等能力。

## 技术栈

- 后端：Spring Boot 3、Spring Security、MyBatis-Plus、MySQL 8、Redis、JWT
- 前端：Vue 3、TypeScript、Vite、Element Plus、FullCalendar
- 附加依赖：React 19、React DOM 19、`@paper-design/shaders-react`（仅用于动态 logo）

## 当前功能

- 登录、注册与基于角色的权限控制
- 统一登录落点：所有角色登录后默认进入 `/dashboard`
- 运营看板：管理员与普通用户共用骨架，不同角色展示不同内容
- 会议预约日历：房间筛选、时段查看、冲突检测、替代建议
- 会议室管理：列表、状态、容量、设备、维护信息管理
- 我的预约：查看、取消、删除个人预约
- 预约审批：管理员审批待处理预约
- 用户管理：管理员管理账号与角色
- 通知抽屉：从右侧抽屉集中查看与会议相关的通知

## 角色说明

| 角色 | 说明 | 默认首页 |
| --- | --- | --- |
| `USER` | 普通用户，可预约并管理自己的会议 | `/dashboard` |
| `ADMIN` | 管理员，可审批预约并查看运营数据 | `/dashboard` |
| `SUPER_ADMIN` | 超级管理员，额外具备用户与权限管理能力 | `/dashboard` |

## 环境要求

- Java 21
- Maven 3.8+
- Node.js 18+ 与 npm
- MySQL 8
- Redis

后端默认配置见 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)。

## 快速开始

### 一键启动

在项目根目录执行：

```bat
start-dev.bat
```

脚本会自动检查依赖并依次启动 Redis、后端和前端。

### 手动启动

启动后端：

```bash
cd backend
mvn spring-boot:run
```

启动前端：

```bash
cd frontend
npm install
npm run dev
```

## 默认访问地址

- 前端：[http://localhost:5173](http://localhost:5173)
- 后端：[http://localhost:8080](http://localhost:8080)
- 接口文档：[http://localhost:8080/doc.html](http://localhost:8080/doc.html)

## 默认账号

默认账号由后端启动配置自动注入：

- 管理员：`admin / admin123`
- 超级管理员：`root / Root@123456`
- 普通用户：通过注册页自行创建

## 前端开发说明

如果项目位于 Windows 中文路径下，Vite 的依赖优化可能偶发异常，建议按以下方式使用：

- 常规开发：`npm run dev`
- 异常恢复：`npm run dev:recover`
- 强制重建依赖缓存：`npm run dev:force`

常见异常表现：

- 登录后空白页
- 页面切换后内容消失
- 控制台出现 `504 Outdated Optimize Dep`
- 控制台出现 `Failed to fetch dynamically imported module`

## 项目结构

### 根目录

- [README.md](README.md)：项目说明
- [.gitignore](.gitignore)：根目录忽略规则
- [.impeccable.md](.impeccable.md)：前端设计基线
- [start-dev.bat](start-dev.bat)：Windows 一键启动脚本
- `frontend/`：Vue 前端工程
- `backend/`：Spring Boot 后端工程
- `.github/`：GitHub 配置

### 前端目录

前端主工程位于 `frontend/`：

- [frontend/package.json](frontend/package.json)：依赖与脚本
- [frontend/vite.config.ts](frontend/vite.config.ts)：Vite 配置
- [frontend/README.md](frontend/README.md)：前端补充说明

`frontend/src/` 主要目录：

- [frontend/src/main.ts](frontend/src/main.ts)：前端入口
- [frontend/src/App.vue](frontend/src/App.vue)：应用壳与顶层路由容器
- [frontend/src/style.css](frontend/src/style.css)：全局样式与共享布局规则
- `frontend/src/views/`：页面目录
- `frontend/src/components/`：复用组件
- `frontend/src/api/`：接口封装与类型
- `frontend/src/store/`：状态管理
- `frontend/src/router/`：路由与守卫
- `frontend/src/utils/`：工具函数
- `frontend/src/assets/`：静态资源

当前页面：

- [frontend/src/views/LoginView.vue](frontend/src/views/LoginView.vue)
- [frontend/src/views/RegisterView.vue](frontend/src/views/RegisterView.vue)
- [frontend/src/views/LayoutView.vue](frontend/src/views/LayoutView.vue)
- [frontend/src/views/DashboardView.vue](frontend/src/views/DashboardView.vue)
- [frontend/src/views/CalendarView.vue](frontend/src/views/CalendarView.vue)
- [frontend/src/views/RoomsView.vue](frontend/src/views/RoomsView.vue)
- [frontend/src/views/MyReservationsView.vue](frontend/src/views/MyReservationsView.vue)
- [frontend/src/views/AdminApprovalsView.vue](frontend/src/views/AdminApprovalsView.vue)
- [frontend/src/views/UserManagementView.vue](frontend/src/views/UserManagementView.vue)

关键组件：

- [frontend/src/components/AuthMeshLogo.vue](frontend/src/components/AuthMeshLogo.vue)
- [frontend/src/components/PageStatusPanel.vue](frontend/src/components/PageStatusPanel.vue)
- [frontend/src/components/TypedText.vue](frontend/src/components/TypedText.vue)

### 后端目录

后端主工程位于 `backend/`：

- [backend/pom.xml](backend/pom.xml)
- [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)
- [backend/src/main/java/com/example/mrs/MeetingRoomSystemApplication.java](backend/src/main/java/com/example/mrs/MeetingRoomSystemApplication.java)

主要模块：

- `auth/`：登录注册
- `security/`：认证与权限
- `room/`：会议室管理
- `reservation/`：预约、审批、通知
- `stats/`：运营看板聚合统计
- `user/`：用户管理
- `bootstrap/`：管理员与演示数据初始化
- `entity/`、`mapper/`、`common/`、`config/`、`domain/`、`redis/`

## 常用修改入口

1. 登录、权限与默认跳转：
   - [frontend/src/store/auth.ts](frontend/src/store/auth.ts)
   - [frontend/src/router/index.ts](frontend/src/router/index.ts)
   - [frontend/src/utils/authRoute.ts](frontend/src/utils/authRoute.ts)
   - `backend/src/main/java/com/example/mrs/auth/`
   - `backend/src/main/java/com/example/mrs/security/`
2. 前端界面与交互：
   - [frontend/src/style.css](frontend/src/style.css)
   - `frontend/src/views/*.vue`
3. 预约、审批与会议室业务：
   - [frontend/src/api/mrs.ts](frontend/src/api/mrs.ts)
   - `backend/src/main/java/com/example/mrs/reservation/`
   - `backend/src/main/java/com/example/mrs/room/`
4. 启动流程与环境配置：
   - [start-dev.bat](start-dev.bat)
   - [frontend/vite.config.ts](frontend/vite.config.ts)
   - [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)

## 忽略与生成物

以下内容属于构建产物、调试缓存或本地运行时文件，不应提交到仓库：

- `backend/target/`
- `frontend/dist/`
- `frontend/node_modules/`
- `.playwright-cli/`
- `.superpowers/`
- `output/`
- `dump.rdb`

统一忽略规则见 [.gitignore](.gitignore)。
