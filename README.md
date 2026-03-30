# 会议室预约与资源协调系统

基于前后端分离架构的会议室预约系统，提供登录注册、运营看板、会议室管理、日历预约、我的预约、通知、审批和用户管理等能力。

## 技术栈

- 后端：Spring Boot 3、Spring Security、MyBatis-Plus、MySQL 8、Redis、JWT
- 前端：Vue 3、TypeScript、Vite、Element Plus、FullCalendar
- 前端附加依赖：`@paper-design/shaders-react`、React 19、React DOM 19（用于动态 logo）
- 开发辅助：Windows 一键启动脚本、前端运行时恢复脚本、统一状态反馈组件

## 功能概览

- 用户登录、注册与基于角色的权限控制
- 统一登录落点：所有角色登录后默认进入 `/dashboard`
- 运营看板
- 会议室列表、状态管理、维护时段管理
- 日历视图预约、批量周预约、冲突建议与占用查看
- 我的预约、取消预约、删除预约、审计日志
- 管理员预约审批、撤销审批
- 用户管理与权限调整
- 通知查看
- 统一的加载、空态、错误和服务未就绪提示

## 角色说明

| 角色 | 说明 | 默认首页 |
| --- | --- | --- |
| `USER` | 普通用户，可预约、查看和管理自己的预约 | `/dashboard` |
| `ADMIN` | 管理员，可处理预约审批、查看看板，并进入管理页面 | `/dashboard` |
| `SUPER_ADMIN` | 超级管理员，可管理用户与权限，同时具备管理员能力 | `/dashboard` |

## 环境要求

启动项目前请确认本机已安装并可用：

- Java 21
- Maven 3.8+
- Node.js 18+ 与 npm
- MySQL 8
- Redis

默认后端配置见 [application.yml](/E:/私有云/Personal%20project/backend/src/main/resources/application.yml)。

## 快速开始

### 1. 一键启动

在项目根目录执行：

```bat
start-dev.bat
```

脚本会自动：

1. 检查 `backend/` 与 `frontend/` 是否存在
2. 检查 `mvn`、`npm`、`redis-server` 是否可用
3. 当前端依赖缺失时执行 `npm install`
4. 启动 Redis、Spring Boot 后端、Vite 前端
5. 等待后端 `8080` 可达后再启动前端

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

## 前端开发说明

当前仓库路径包含中文字符：`E:/私有云/Personal project`。
在 Windows 下，Vite 8 的 Rolldown 在这类路径上进行依赖优化时可能出现异常，建议按下面方式处理：

- 常规开发使用 `npm run dev`
- 出现空白页、路由模块加载失败时使用 `npm run dev:recover`
- 仅在纯英文路径环境下使用 `npm run dev:force`

恢复命令：

```bash
cd frontend
npm run dev:recover
```

常见症状：

- 登录后白屏
- 切换页面后内容消失
- 控制台出现 `504 Outdated Optimize Dep`
- 控制台出现 `Failed to fetch dynamically imported module`

## 项目结构

### 根目录

- [README.md](/E:/私有云/Personal%20project/README.md)：项目说明
- [.gitignore](/E:/私有云/Personal%20project/.gitignore)：忽略规则
- [.impeccable.md](/E:/私有云/Personal%20project/.impeccable.md)：前端设计基线
- [start-dev.bat](/E:/私有云/Personal%20project/start-dev.bat)：Windows 一键启动脚本
- `frontend/`：Vue 前端工程
- `backend/`：Spring Boot 后端工程
- `.github/`：GitHub 配置

### 前端目录

前端主工程位于 `frontend/`。

- [package.json](/E:/私有云/Personal%20project/frontend/package.json)：前端依赖与脚本
- [vite.config.ts](/E:/私有云/Personal%20project/frontend/vite.config.ts)：Vite 配置
- [README.md](/E:/私有云/Personal%20project/frontend/README.md)：前端补充说明

`frontend/src/` 主要目录：

- [main.ts](/E:/私有云/Personal%20project/frontend/src/main.ts)：前端入口
- [App.vue](/E:/私有云/Personal%20project/frontend/src/App.vue)：应用壳与顶层路由视图
- [style.css](/E:/私有云/Personal%20project/frontend/src/style.css)：全局样式与组件视觉规则
- `views/`：页面目录
- `components/`：复用组件目录
- `api/`：接口封装与类型
- `store/`：状态管理
- `router/`：路由与守卫
- `utils/`：工具函数
- `assets/`：静态资源

页面目录 `frontend/src/views/`：

- [LoginView.vue](/E:/私有云/Personal%20project/frontend/src/views/LoginView.vue)：登录页
- [RegisterView.vue](/E:/私有云/Personal%20project/frontend/src/views/RegisterView.vue)：注册页
- [LayoutView.vue](/E:/私有云/Personal%20project/frontend/src/views/LayoutView.vue)：登录后工作台外壳、顶部导航、通知抽屉
- [DashboardView.vue](/E:/私有云/Personal%20project/frontend/src/views/DashboardView.vue)：运营看板
- [CalendarView.vue](/E:/私有云/Personal%20project/frontend/src/views/CalendarView.vue)：会议预约日历页
- [RoomsView.vue](/E:/私有云/Personal%20project/frontend/src/views/RoomsView.vue)：会议室管理页
- [MyReservationsView.vue](/E:/私有云/Personal%20project/frontend/src/views/MyReservationsView.vue)：我的预约页
- [NotificationsView.vue](/E:/私有云/Personal%20project/frontend/src/views/NotificationsView.vue)：通知页
- [AdminApprovalsView.vue](/E:/私有云/Personal%20project/frontend/src/views/AdminApprovalsView.vue)：预约审批页
- [UserManagementView.vue](/E:/私有云/Personal%20project/frontend/src/views/UserManagementView.vue)：用户管理页

组件目录 `frontend/src/components/`：

- [AuthMeshLogo.vue](/E:/私有云/Personal%20project/frontend/src/components/AuthMeshLogo.vue)：动态 logo 组件
- [PageStatusPanel.vue](/E:/私有云/Personal%20project/frontend/src/components/PageStatusPanel.vue)：统一状态反馈组件

工具目录：

- [frontend/src/api/http.ts](/E:/私有云/Personal%20project/frontend/src/api/http.ts)：Axios 实例与错误处理
- [frontend/src/api/mrs.ts](/E:/私有云/Personal%20project/frontend/src/api/mrs.ts)：业务 API
- [frontend/src/api/types.ts](/E:/私有云/Personal%20project/frontend/src/api/types.ts)：接口类型
- [frontend/src/store/auth.ts](/E:/私有云/Personal%20project/frontend/src/store/auth.ts)：登录态与角色持久化
- [frontend/src/store/serviceStatus.ts](/E:/私有云/Personal%20project/frontend/src/store/serviceStatus.ts)：后端服务状态
- [frontend/src/router/index.ts](/E:/私有云/Personal%20project/frontend/src/router/index.ts)：路由表与守卫
- [frontend/src/utils/authRoute.ts](/E:/私有云/Personal%20project/frontend/src/utils/authRoute.ts)：默认首页解析
- [frontend/src/utils/chunkRecovery.ts](/E:/私有云/Personal%20project/frontend/src/utils/chunkRecovery.ts)：懒加载恢复逻辑

### 后端目录

后端主工程位于 `backend/`。

- [pom.xml](/E:/私有云/Personal%20project/backend/pom.xml)：Maven 配置
- [application.yml](/E:/私有云/Personal%20project/backend/src/main/resources/application.yml)：运行配置
- [MeetingRoomSystemApplication.java](/E:/私有云/Personal%20project/backend/src/main/java/com/example/mrs/MeetingRoomSystemApplication.java)：启动入口

`backend/src/main/java/com/example/mrs/` 主要模块：

- `auth/`：登录注册
- `security/`：认证与权限
- `room/`：会议室管理
- `reservation/`：预约、审批、审计日志、通知
- `stats/`：运营看板统计
- `user/`：用户管理
- `entity/`：实体类
- `mapper/`：数据访问层
- `common/`：统一返回与异常处理
- `config/`：配置类
- `bootstrap/`：初始化管理员账号
- `domain/`：角色与状态枚举
- `redis/`：Redis 相关封装

## 常见修改入口

1. 登录、权限、默认跳转：
   - [frontend/src/store/auth.ts](/E:/私有云/Personal%20project/frontend/src/store/auth.ts)
   - [frontend/src/router/index.ts](/E:/私有云/Personal%20project/frontend/src/router/index.ts)
   - [frontend/src/utils/authRoute.ts](/E:/私有云/Personal%20project/frontend/src/utils/authRoute.ts)
   - `backend/src/main/java/com/example/mrs/auth/`
   - `backend/src/main/java/com/example/mrs/security/`
2. 前端界面与交互：
   - [frontend/src/style.css](/E:/私有云/Personal%20project/frontend/src/style.css)
   - [.impeccable.md](/E:/私有云/Personal%20project/.impeccable.md)
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

统一忽略规则见 [.gitignore](/E:/私有云/Personal%20project/.gitignore)。
