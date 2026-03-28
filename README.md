# 会议室预约与资源协调系统

一个基于前后端分离架构的会议室预约系统，支持登录注册、会议室查询、日历视图预约、我的预约管理、通知查看，以及管理员审批和用户管理。

## 技术栈

- 后端：Spring Boot 3、Spring Security、MyBatis-Plus、MySQL 8、Redis、JWT
- 前端：Vue 3、TypeScript、Vite、Element Plus、FullCalendar
- 开发辅助：Windows 一键启动脚本、前端运行时容错、统一状态反馈组件

## 核心能力

- 用户登录、注册与基于角色的权限控制
- 会议室列表、状态管理、维护时段管理
- 日历视图预约、批量周预约、冲突建议与占用查看
- 我的预约、取消预约、删除预约、审计日志
- 管理员预约审批、撤销审批、运营看板、用户管理
- 统一的页面状态反馈：加载中、空态、错误态、后端未就绪提示

## 角色说明

| 角色 | 说明 | 默认首页 |
| --- | --- | --- |
| `USER` | 普通用户，可预约、查看和管理自己的预约 | `/dashboard` |
| `ADMIN` | 管理员，可处理预约审批和查看看板 | `/admin/approvals` |
| `SUPER_ADMIN` | 超级管理员，可管理用户与权限，同时具备管理员能力 | `/admin/users` |

## 环境要求

启动项目前，请先确认本机已安装并可用：

- Java 21
- Maven 3.8+
- Node.js 18+ 与 npm
- MySQL 8
- Redis

默认配置见 [application.yml](/E:/私有云/Personal%20project/backend/src/main/resources/application.yml)。

## 快速开始

### 1. 一键启动

在项目根目录执行：

```bat
start-dev.bat
```

脚本会自动完成这些工作：

1. 检查 `backend/` 与 `frontend/` 是否存在
2. 检查 `mvn`、`npm`、`redis-server` 是否可用
3. 当前端依赖缺失时自动执行 `npm install`
4. 启动 Redis、Spring Boot 后端、Vite 前端
5. 等待后端 `8080` 可达后再启动前端，减少刚打开页面就遇到 `502` 的情况

### 2. 手动启动

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

- 前端：http://localhost:5173
- 后端：http://localhost:8080
- 接口文档：http://localhost:8080/doc.html

## 默认账号

默认账号由后端启动配置自动注入：

- 管理员：`admin / admin123`
- 超级管理员：`root / Root@123456`
- 普通用户：通过注册页自行创建

## 前端开发稳定性说明

当前工作目录位于包含中文字符的路径下：`E:/私有云/Personal project`。
在 Windows 下，Vite 8 的 Rolldown 对这类路径执行强制依赖优化时可能出现异常，因此建议遵循以下规则：

- 常规开发使用 `npm run dev`
- 出现空白页、路由模块加载失败时使用 `npm run dev:recover`
- 仅在纯英文路径环境下使用 `npm run dev:force`

恢复命令：

```bash
cd frontend
npm run dev:recover
```

适用症状：

- 登录后白屏
- 切换页面后内容消失
- 控制台出现 `504 Outdated Optimize Dep`
- 控制台出现 `Failed to fetch dynamically imported module`

## 项目结构

### 根目录

- [README.md](/E:/私有云/Personal%20project/README.md)：项目总说明、启动方式和结构说明
- [.gitignore](/E:/私有云/Personal%20project/.gitignore)：项目级忽略规则，屏蔽构建产物、运行日志和缓存目录
- [start-dev.bat](/E:/私有云/Personal%20project/start-dev.bat)：Windows 一键启动脚本
- `frontend/`：Vue 前端工程
- `backend/`：Spring Boot 后端工程
- `.github/`：仓库协作与 GitHub 配置
- `.vscode/`：VS Code 工作区设置
- `.idea/`：JetBrains IDE 工程配置
- `.cursor/`：本地编辑器辅助目录

### 前端目录

前端主工程位于 `frontend/`。

- [package.json](/E:/私有云/Personal%20project/frontend/package.json)：前端依赖与脚本入口
- [vite.config.ts](/E:/私有云/Personal%20project/frontend/vite.config.ts)：Vite 配置、Element Plus 自动导入、开发代理和依赖优化策略
- [README.md](/E:/私有云/Personal%20project/frontend/README.md)：前端补充说明

`frontend/src/` 是前端源码主目录：

- [main.ts](/E:/私有云/Personal%20project/frontend/src/main.ts)：前端入口文件
- [App.vue](/E:/私有云/Personal%20project/frontend/src/App.vue)：最外层应用壳，承载路由视图
- [style.css](/E:/私有云/Personal%20project/frontend/src/style.css)：全局视觉样式，包括颗粒背景、卡片、表格、弹窗、按钮等统一设计语言

`frontend/src/views/` 页面目录：

- [LoginView.vue](/E:/私有云/Personal%20project/frontend/src/views/LoginView.vue)：登录页
- [RegisterView.vue](/E:/私有云/Personal%20project/frontend/src/views/RegisterView.vue)：注册页
- [LayoutView.vue](/E:/私有云/Personal%20project/frontend/src/views/LayoutView.vue)：登录后的整体工作台外壳、顶部导航和通知抽屉
- [DashboardView.vue](/E:/私有云/Personal%20project/frontend/src/views/DashboardView.vue)：运营看板首页
- [CalendarView.vue](/E:/私有云/Personal%20project/frontend/src/views/CalendarView.vue)：会议预约日历页
- [RoomsView.vue](/E:/私有云/Personal%20project/frontend/src/views/RoomsView.vue)：会议室管理页
- [MyReservationsView.vue](/E:/私有云/Personal%20project/frontend/src/views/MyReservationsView.vue)：我的预约页
- [NotificationsView.vue](/E:/私有云/Personal%20project/frontend/src/views/NotificationsView.vue)：通知页
- [AdminApprovalsView.vue](/E:/私有云/Personal%20project/frontend/src/views/AdminApprovalsView.vue)：预约审批页
- [UserManagementView.vue](/E:/私有云/Personal%20project/frontend/src/views/UserManagementView.vue)：用户管理页

`frontend/src/components/` 复用组件目录：

- [AuthMeshLogo.vue](/E:/私有云/Personal%20project/frontend/src/components/AuthMeshLogo.vue)：动态 logo 组件，内部使用 React shader 渲染效果
- [PageStatusPanel.vue](/E:/私有云/Personal%20project/frontend/src/components/PageStatusPanel.vue)：统一的加载中、错误、空态、服务未就绪提示组件

`frontend/src/api/` 接口层：

- [http.ts](/E:/私有云/Personal%20project/frontend/src/api/http.ts)：Axios 实例、请求拦截器、401 处理、基础设施错误翻译
- [mrs.ts](/E:/私有云/Personal%20project/frontend/src/api/mrs.ts)：业务 API 封装
- [types.ts](/E:/私有云/Personal%20project/frontend/src/api/types.ts)：前端接口类型定义

`frontend/src/store/` 状态层：

- [auth.ts](/E:/私有云/Personal%20project/frontend/src/store/auth.ts)：登录态、角色、token 持久化
- [serviceStatus.ts](/E:/私有云/Personal%20project/frontend/src/store/serviceStatus.ts)：后端服务状态，用于页面内统一反馈

`frontend/src/router/` 路由层：

- [index.ts](/E:/私有云/Personal%20project/frontend/src/router/index.ts)：路由表、登录守卫、角色跳转和懒加载恢复逻辑

`frontend/src/utils/` 工具层：

- [authRoute.ts](/E:/私有云/Personal%20project/frontend/src/utils/authRoute.ts)：根据角色计算默认首页
- [chunkRecovery.ts](/E:/私有云/Personal%20project/frontend/src/utils/chunkRecovery.ts)：处理路由懒加载失败后的恢复逻辑

`frontend/src/assets/` 静态素材目录：

- [linen-noise.svg](/E:/私有云/Personal%20project/frontend/src/assets/linen-noise.svg)：当前登录页与登录后页面共用的亚麻颗粒背景素材

### 后端目录

后端主工程位于 `backend/`。

- [pom.xml](/E:/私有云/Personal%20project/backend/pom.xml)：Maven 配置与依赖声明
- [application.yml](/E:/私有云/Personal%20project/backend/src/main/resources/application.yml)：数据库、Redis、JWT、默认管理员、预约规则等运行配置
- [MeetingRoomSystemApplication.java](/E:/私有云/Personal%20project/backend/src/main/java/com/example/mrs/MeetingRoomSystemApplication.java)：Spring Boot 启动入口

`backend/src/main/java/com/example/mrs/` 为后端核心源码：

- `auth/`：登录注册控制器、服务与 DTO
- `security/`：JWT、认证过滤器、Spring Security 配置
- `room/`：会议室管理相关业务
- `reservation/`：预约、审批、审计日志、通知等核心业务
- `stats/`：运营看板统计逻辑
- `user/`：用户管理与权限调整
- `entity/`：数据库实体类
- `mapper/`：MyBatis-Plus 数据访问层
- `common/`：统一返回体、业务异常、全局异常处理
- `config/`：MyBatis、OpenAPI、预约规则等配置类
- `bootstrap/`：启动时初始化管理员账号
- `domain/`：角色、预约状态等领域枚举
- `redis/`：Redis key 与分布式锁相关封装

## 常见修改入口

如果你后续要自己维护项目，通常可以从这些入口开始：

1. 登录、权限、默认跳转：
   - [frontend/src/store/auth.ts](/E:/私有云/Personal%20project/frontend/src/store/auth.ts)
   - [frontend/src/router/index.ts](/E:/私有云/Personal%20project/frontend/src/router/index.ts)
   - `backend/src/main/java/com/example/mrs/auth/`
   - `backend/src/main/java/com/example/mrs/security/`
2. 全站视觉与页面样式：
   - [frontend/src/style.css](/E:/私有云/Personal%20project/frontend/src/style.css)
   - `frontend/src/views/*.vue`
3. 预约、审批、会议室业务：
   - [frontend/src/api/mrs.ts](/E:/私有云/Personal%20project/frontend/src/api/mrs.ts)
   - `backend/src/main/java/com/example/mrs/reservation/`
   - `backend/src/main/java/com/example/mrs/room/`
4. 启动流程与本地环境：
   - [start-dev.bat](/E:/私有云/Personal%20project/start-dev.bat)
   - [frontend/vite.config.ts](/E:/私有云/Personal%20project/frontend/vite.config.ts)
   - [backend/src/main/resources/application.yml](/E:/私有云/Personal%20project/backend/src/main/resources/application.yml)

## 生成物与忽略规则

以下内容属于构建产物、缓存或运行时文件，不建议提交到仓库：

- `backend/target/`
- `frontend/dist/`
- `dump.rdb`
- `.playwright-cli/`
- `output/`

这些规则已经收敛到根目录 [.gitignore](/E:/私有云/Personal%20project/.gitignore) 中统一管理。
