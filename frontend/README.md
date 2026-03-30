# 前端说明

本目录是会议室预约与资源协调系统的前端工程，基于 Vue 3、TypeScript 和 Vite，负责登录注册、工作台导航、运营看板、会议预约、会议室管理、审批、通知和用户管理等界面。

## 技术栈

- Vue 3
- TypeScript
- Vite
- Vue Router
- Element Plus
- FullCalendar
- Axios
- `@paper-design/shaders-react`、React 19、React DOM 19（用于动态 logo）

## 常用命令

启动开发环境：

```bash
npm run dev
```

恢复开发环境：

```bash
npm run dev:recover
```

适用场景：

- 登录后白屏
- 页面切换后内容消失
- 控制台出现 `504 Outdated Optimize Dep`
- 控制台出现 `Failed to fetch dynamically imported module`

强制重新优化依赖：

```bash
npm run dev:force
```

说明：仅建议在纯英文路径环境下使用。

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

- [package.json](package.json)：前端依赖与脚本
- [package-lock.json](package-lock.json)：依赖锁文件
- [vite.config.ts](vite.config.ts)：Vite 配置
- [README.md](README.md)：当前文档
- [.gitignore](.gitignore)：前端忽略规则

### `src/` 目录

- [main.ts](src/main.ts)：前端入口
- [App.vue](src/App.vue)：应用壳与顶层路由视图
- [style.css](src/style.css)：全局样式与组件视觉规则
- [components.d.ts](src/components.d.ts)：自动生成的组件类型声明
- `views/`：页面目录
- `components/`：复用组件
- `api/`：接口封装与类型
- `store/`：状态管理
- `router/`：路由与守卫
- `utils/`：工具函数
- `assets/`：静态资源

### 页面目录 `src/views/`

- [LoginView.vue](src/views/LoginView.vue)：登录页
- [RegisterView.vue](src/views/RegisterView.vue)：注册页
- [LayoutView.vue](src/views/LayoutView.vue)：登录后工作台外壳、顶部导航、通知抽屉
- [DashboardView.vue](src/views/DashboardView.vue)：运营看板
- [CalendarView.vue](src/views/CalendarView.vue)：会议预约日历页
- [RoomsView.vue](src/views/RoomsView.vue)：会议室管理页
- [MyReservationsView.vue](src/views/MyReservationsView.vue)：我的预约页
- [NotificationsView.vue](src/views/NotificationsView.vue)：通知页
- [AdminApprovalsView.vue](src/views/AdminApprovalsView.vue)：预约审批页
- [UserManagementView.vue](src/views/UserManagementView.vue)：用户管理页

### 组件与工具

- [AuthMeshLogo.vue](src/components/AuthMeshLogo.vue)：动态 logo 组件
- [PageStatusPanel.vue](src/components/PageStatusPanel.vue)：统一状态反馈组件
- [http.ts](src/api/http.ts)：Axios 实例与错误处理
- [mrs.ts](src/api/mrs.ts)：业务 API
- [types.ts](src/api/types.ts)：接口类型
- [auth.ts](src/store/auth.ts)：登录态与角色持久化
- [serviceStatus.ts](src/store/serviceStatus.ts)：后端服务状态
- [index.ts](src/router/index.ts)：路由表与守卫
- [authRoute.ts](src/utils/authRoute.ts)：默认首页解析
- [chunkRecovery.ts](src/utils/chunkRecovery.ts)：懒加载恢复逻辑

## 关键行为

### 登录与权限

- 登录成功后会持久化 `token`、`userId`、`username`、`role`
- 所有角色登录后默认进入 `/dashboard`
- 路由守卫负责登录校验、权限校验和默认跳转

相关文件：

- [src/store/auth.ts](src/store/auth.ts)
- [src/router/index.ts](src/router/index.ts)
- [src/utils/authRoute.ts](src/utils/authRoute.ts)

### 接口请求

- 所有接口统一通过 [http.ts](src/api/http.ts) 发出
- 开发环境下，`/api` 会代理到 `http://127.0.0.1:8080`
- 登录态请求会自动带上 `Authorization: Bearer <token>`

### 页面状态反馈

高频页面统一使用一套状态反馈：

- 加载中
- 空态
- 错误态
- 后端服务未就绪提示

相关文件：

- [src/components/PageStatusPanel.vue](src/components/PageStatusPanel.vue)
- [src/store/serviceStatus.ts](src/store/serviceStatus.ts)

### 路由懒加载恢复

为应对开发态依赖缓存异常，项目增加了动态模块加载失败恢复逻辑：

- 首次失败时尝试自动恢复
- 避免无限刷新循环
- 必要时回退到登录页

相关文件：

- [src/router/index.ts](src/router/index.ts)
- [src/utils/chunkRecovery.ts](src/utils/chunkRecovery.ts)

## 开发注意事项

### 1. Windows 中文路径

如果项目位于 Windows 的中文路径下：

- `npm run dev` 适合作为日常开发入口
- `npm run dev:recover` 适合作为异常恢复入口
- `npm run dev:force` 不适合作为默认开发命令

### 2. `components.d.ts` 不要手改

[components.d.ts](src/components.d.ts) 为自动生成文件。
如果组件自动导入发生变化，应通过插件配置或重新构建生成。

## 常见维护入口

1. 想改全局样式：看 [src/style.css](src/style.css)
2. 想改登录流程：看 [src/views/LoginView.vue](src/views/LoginView.vue) 和 [src/store/auth.ts](src/store/auth.ts)
3. 想改导航和登录后壳层：看 [src/views/LayoutView.vue](src/views/LayoutView.vue)
4. 想改接口与错误处理：看 [src/api/http.ts](src/api/http.ts) 和 [src/api/mrs.ts](src/api/mrs.ts)
5. 想改路由或默认首页：看 [src/router/index.ts](src/router/index.ts) 和 [src/utils/authRoute.ts](src/utils/authRoute.ts)
