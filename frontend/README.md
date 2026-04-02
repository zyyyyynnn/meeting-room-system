# 前端说明

本目录是会议室预约与资源协调系统的前端工程，基于 Vue 3、TypeScript 和 Vite，负责登录注册、运营看板、会议预约、会议室管理、我的预约、审批、通知抽屉和用户管理等界面。

## 技术栈

- Vue 3
- TypeScript
- Vite
- Vue Router
- Element Plus
- FullCalendar
- Axios
- React 19、React DOM 19、`@paper-design/shaders-react`（仅用于动态 logo）

## 常用命令

启动开发环境：

```bash
npm run dev
```

异常恢复：

```bash
npm run dev:recover
```

强制重建依赖缓存：

```bash
npm run dev:force
```

生产构建：

```bash
npm run build
```

本地预览构建结果：

```bash
npm run preview
```

## 目录结构

### 根文件

- [package.json](package.json)：依赖与脚本
- [vite.config.ts](vite.config.ts)：Vite 配置
- [README.md](README.md)：当前文档

### `src/`

- [main.ts](src/main.ts)：入口
- [App.vue](src/App.vue)：应用壳与顶层过渡
- [style.css](src/style.css)：全局样式与共享布局系统
- [components.d.ts](src/components.d.ts)：自动生成的组件类型声明
- `views/`：页面
- `components/`：复用组件
- `api/`：接口与类型
- `store/`：状态管理
- `router/`：路由与守卫
- `utils/`：工具函数
- `assets/`：静态资源

### 当前页面

- [src/views/LoginView.vue](src/views/LoginView.vue)
- [src/views/RegisterView.vue](src/views/RegisterView.vue)
- [src/views/LayoutView.vue](src/views/LayoutView.vue)
- [src/views/DashboardView.vue](src/views/DashboardView.vue)
- [src/views/CalendarView.vue](src/views/CalendarView.vue)
- [src/views/RoomsView.vue](src/views/RoomsView.vue)
- [src/views/MyReservationsView.vue](src/views/MyReservationsView.vue)
- [src/views/AdminApprovalsView.vue](src/views/AdminApprovalsView.vue)
- [src/views/UserManagementView.vue](src/views/UserManagementView.vue)

通知不再保留独立页面，统一由 [src/views/LayoutView.vue](src/views/LayoutView.vue) 中的右侧通知抽屉承载。

### 关键组件

- [src/components/AuthMeshLogo.vue](src/components/AuthMeshLogo.vue)
- [src/components/PageStatusPanel.vue](src/components/PageStatusPanel.vue)
- [src/components/TypedText.vue](src/components/TypedText.vue)

## 开发说明

### 登录与权限

- 登录成功后会持久化 `token`、`userId`、`username`、`role`
- 所有角色登录后默认进入 `/dashboard`
- 路由守卫负责登录校验、权限校验和默认跳转

相关文件：

- [src/store/auth.ts](src/store/auth.ts)
- [src/router/index.ts](src/router/index.ts)
- [src/utils/authRoute.ts](src/utils/authRoute.ts)

### 接口请求

- 接口统一通过 [src/api/http.ts](src/api/http.ts) 发出
- 开发环境下 `/api` 代理到 `http://127.0.0.1:8080`
- 登录态请求会自动携带 `Authorization: Bearer <token>`

### 状态反馈

高频页面统一使用一套状态面板处理：

- 加载中
- 空状态
- 错误状态
- 服务不可用提示

相关文件：

- [src/components/PageStatusPanel.vue](src/components/PageStatusPanel.vue)
- [src/store/serviceStatus.ts](src/store/serviceStatus.ts)

### 注意事项

1. 项目位于 Windows 中文路径下时，依赖优化偶发异常可优先使用 `npm run dev:recover`
2. [components.d.ts](src/components.d.ts) 为自动生成文件，不建议手动修改
