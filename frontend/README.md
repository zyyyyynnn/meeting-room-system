# 前端说明

本目录是会议室预约与资源协调系统的前端工程，基于 Vue 3、TypeScript 和 Vite 构建，负责登录注册、工作台布局、会议预约、会议室管理、审批、通知和用户管理等界面。

## 技术栈

- Vue 3
- TypeScript
- Vite
- Vue Router
- Element Plus
- FullCalendar
- Axios

## 常用命令

### 启动开发环境

```bash
npm run dev
```

用于日常本地开发，是当前仓库默认推荐方式。

### 恢复开发环境

```bash
npm run dev:recover
```

会先清理 `node_modules/.vite` 缓存，再重新启动开发服务。
适合这些情况：

- 登录后白屏
- 页面切换后内容消失
- 控制台出现 `504 Outdated Optimize Dep`
- 控制台出现 `Failed to fetch dynamically imported module`

### 强制重新优化依赖

```bash
npm run dev:force
```

只建议在纯英文路径环境下使用。
当前项目路径包含中文字符，在 Windows 下直接强制优化依赖可能触发 Vite 8 / Rolldown 的路径问题。

### 生产构建

```bash
npm run build
```

### 本地预览构建结果

```bash
npm run preview
```

## 目录结构

### 根文件

- [package.json](/E:/私有云/Personal%20project/frontend/package.json)：前端依赖与脚本配置
- [package-lock.json](/E:/私有云/Personal%20project/frontend/package-lock.json)：依赖锁文件
- [vite.config.ts](/E:/私有云/Personal%20project/frontend/vite.config.ts)：Vite 配置、开发代理、组件自动导入、依赖优化策略
- [README.md](/E:/私有云/Personal%20project/frontend/README.md)：当前前端说明文档
- [.gitignore](/E:/私有云/Personal%20project/frontend/.gitignore)：前端目录级忽略规则

### `src/` 目录

- [main.ts](/E:/私有云/Personal%20project/frontend/src/main.ts)：前端入口，挂载应用并注册全局运行时处理
- [App.vue](/E:/私有云/Personal%20project/frontend/src/App.vue)：应用最外层壳，仅承载路由视图
- [style.css](/E:/私有云/Personal%20project/frontend/src/style.css)：全局样式入口，控制背景颗粒、卡片、表格、弹窗、按钮等统一设计语言
- [components.d.ts](/E:/私有云/Personal%20project/frontend/src/components.d.ts)：由 `unplugin-vue-components` 自动生成的组件类型声明文件

### `src/views/` 页面视图

- [LoginView.vue](/E:/私有云/Personal%20project/frontend/src/views/LoginView.vue)：登录页
- [RegisterView.vue](/E:/私有云/Personal%20project/frontend/src/views/RegisterView.vue)：注册页
- [LayoutView.vue](/E:/私有云/Personal%20project/frontend/src/views/LayoutView.vue)：登录后工作台布局、顶部导航、通知抽屉
- [DashboardView.vue](/E:/私有云/Personal%20project/frontend/src/views/DashboardView.vue)：运营看板
- [CalendarView.vue](/E:/私有云/Personal%20project/frontend/src/views/CalendarView.vue)：会议预约日历页
- [RoomsView.vue](/E:/私有云/Personal%20project/frontend/src/views/RoomsView.vue)：会议室管理
- [MyReservationsView.vue](/E:/私有云/Personal%20project/frontend/src/views/MyReservationsView.vue)：我的预约
- [NotificationsView.vue](/E:/私有云/Personal%20project/frontend/src/views/NotificationsView.vue)：通知页
- [AdminApprovalsView.vue](/E:/私有云/Personal%20project/frontend/src/views/AdminApprovalsView.vue)：预约审批
- [UserManagementView.vue](/E:/私有云/Personal%20project/frontend/src/views/UserManagementView.vue)：用户管理

### `src/components/` 复用组件

- [AuthMeshLogo.vue](/E:/私有云/Personal%20project/frontend/src/components/AuthMeshLogo.vue)：动态 logo 组件
- [PageStatusPanel.vue](/E:/私有云/Personal%20project/frontend/src/components/PageStatusPanel.vue)：统一的页面状态反馈组件

说明：
[AuthMeshLogo.vue](/E:/私有云/Personal%20project/frontend/src/components/AuthMeshLogo.vue) 虽然位于 Vue 项目中，但内部通过 React 和 `@paper-design/shaders-react` 实现动态图形 shader，这也是前端依赖中仍保留 `react` 与 `react-dom` 的原因。

### `src/api/` 接口层

- [http.ts](/E:/私有云/Personal%20project/frontend/src/api/http.ts)：Axios 实例、请求拦截、401 处理、基础设施错误提示翻译
- [mrs.ts](/E:/私有云/Personal%20project/frontend/src/api/mrs.ts)：业务接口封装
- [types.ts](/E:/私有云/Personal%20project/frontend/src/api/types.ts)：接口数据类型定义

### `src/store/` 状态层

- [auth.ts](/E:/私有云/Personal%20project/frontend/src/store/auth.ts)：登录态、用户信息、角色和 token 持久化
- [serviceStatus.ts](/E:/私有云/Personal%20project/frontend/src/store/serviceStatus.ts)：后端服务状态，用于登录页和业务页展示统一状态反馈

### `src/router/` 路由层

- [index.ts](/E:/私有云/Personal%20project/frontend/src/router/index.ts)：路由表、权限校验、默认首页跳转、懒加载恢复逻辑

### `src/utils/` 工具层

- [authRoute.ts](/E:/私有云/Personal%20project/frontend/src/utils/authRoute.ts)：根据角色解析默认首页
- [chunkRecovery.ts](/E:/私有云/Personal%20project/frontend/src/utils/chunkRecovery.ts)：处理动态路由模块加载失败的恢复逻辑

### `src/assets/` 静态资源

- [linen-noise.svg](/E:/私有云/Personal%20project/frontend/src/assets/linen-noise.svg)：全局亚麻颗粒背景素材

## 运行逻辑说明

### 登录与权限

- 登录成功后，前端会把 `token`、`userId`、`username`、`role` 存到本地
- 路由守卫会根据角色跳转默认首页
- `USER` 默认进入 `/dashboard`
- `ADMIN` 默认进入 `/admin/approvals`
- `SUPER_ADMIN` 默认进入 `/admin/users`

相关文件：

- [src/store/auth.ts](/E:/私有云/Personal%20project/frontend/src/store/auth.ts)
- [src/router/index.ts](/E:/私有云/Personal%20project/frontend/src/router/index.ts)
- [src/utils/authRoute.ts](/E:/私有云/Personal%20project/frontend/src/utils/authRoute.ts)

### 接口请求

- 所有接口统一通过 [http.ts](/E:/私有云/Personal%20project/frontend/src/api/http.ts) 发出
- 开发环境下，`/api` 会代理到 `http://127.0.0.1:8080`
- 登录态请求会自动带上 `Authorization: Bearer <token>`
- 当后端未启动或代理失败时，会统一翻译成中文提示，而不是直接显示原始英文错误

### 页面状态反馈

目前项目已把高频页面的状态反馈统一为一套设计语言：

- 加载中
- 空态
- 错误态
- 后端服务未就绪提示

主要由 [PageStatusPanel.vue](/E:/私有云/Personal%20project/frontend/src/components/PageStatusPanel.vue) 和 [serviceStatus.ts](/E:/私有云/Personal%20project/frontend/src/store/serviceStatus.ts) 共同支撑。

### 路由懒加载恢复

为了应对开发态依赖缓存异常，项目增加了动态模块加载失败恢复逻辑：

- 首次失败时尝试自动恢复
- 避免进入无限刷新循环
- 在必要时回退到登录页

相关文件：

- [src/router/index.ts](/E:/私有云/Personal%20project/frontend/src/router/index.ts)
- [src/utils/chunkRecovery.ts](/E:/私有云/Personal%20project/frontend/src/utils/chunkRecovery.ts)

## 开发注意事项

### 1. Windows 中文路径问题

当前仓库位于：`E:/私有云/Personal project`

在这个路径下：

- `npm run dev` 可以正常作为日常开发入口
- `npm run dev:recover` 可以作为异常恢复入口
- `npm run dev:force` 不适合作为默认开发命令

### 2. 动态 Logo 不要随意修改

[AuthMeshLogo.vue](/E:/私有云/Personal%20project/frontend/src/components/AuthMeshLogo.vue) 是当前视觉识别的重要部分。
如果要改动它，建议优先保留：

- 当前动效节奏
- 当前色彩层级
- 当前圆形容器结构

### 3. `components.d.ts` 不要手改

[components.d.ts](/E:/私有云/Personal%20project/frontend/src/components.d.ts) 为自动生成文件。
如果组件自动导入发生变化，应通过 Vite 插件配置或重新构建生成，而不是手工维护。

## 常见维护入口

1. 想改全局视觉：看 [src/style.css](/E:/私有云/Personal%20project/frontend/src/style.css)
2. 想改登录流程：看 [src/views/LoginView.vue](/E:/私有云/Personal%20project/frontend/src/views/LoginView.vue) 和 [src/store/auth.ts](/E:/私有云/Personal%20project/frontend/src/store/auth.ts)
3. 想改导航和登录后壳层：看 [src/views/LayoutView.vue](/E:/私有云/Personal%20project/frontend/src/views/LayoutView.vue)
4. 想改接口与错误处理：看 [src/api/http.ts](/E:/私有云/Personal%20project/frontend/src/api/http.ts) 和 [src/api/mrs.ts](/E:/私有云/Personal%20project/frontend/src/api/mrs.ts)
5. 想改路由或默认首页：看 [src/router/index.ts](/E:/私有云/Personal%20project/frontend/src/router/index.ts) 和 [src/utils/authRoute.ts](/E:/私有云/Personal%20project/frontend/src/utils/authRoute.ts)
