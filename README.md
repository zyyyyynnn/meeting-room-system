# 会议室预约与资源协调系统

基于 Spring Boot 3 + Vue 3 的前后端分离会议室预约系统，面向企业/组织内部会议室资源管理场景，覆盖会议室查询、预约申请、冲突检测、审批流转、用户管理、运营看板和通知提醒等核心流程。

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![Vue](https://img.shields.io/badge/Vue-3-brightgreen)
![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue)
![MySQL](https://img.shields.io/badge/MySQL-8-orange)
![Redis](https://img.shields.io/badge/Redis-required-red)

## 项目亮点

- 前后端分离架构，后端提供 REST API，前端提供完整管理界面
- 支持 USER、ADMIN、SUPER_ADMIN 三类角色与差异化权限
- 支持会议日历、会议室筛选、预约冲突检测与替代建议
- 支持管理员审批、超级管理员撤销审批和会议室高级治理
- 集成 Redis 用于运行态缓存、通知等能力
- 提供课程实验文档、图示和项目页面截图，便于课程交付与答辩展示

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3、Spring Security、MyBatis-Plus、MySQL 8、Redis、JWT |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、FullCalendar |
| 构建 | Maven、npm |
| 文档 | OpenAPI / Swagger 配置、课程实验文档 |
| 附加依赖 | React 19、React DOM 19、`@paper-design/shaders-react`，仅用于动态 logo |

## 功能模块

### 认证与权限

- 用户注册、登录
- JWT 登录态维护
- USER、ADMIN、SUPER_ADMIN 角色权限控制
- 登录后统一进入运营看板

### 会议室与预约

- 会议室列表查看
- 会议室状态、容量、设备与维护信息管理
- 会议预约日历
- 房间筛选、时段查看、冲突检测与替代建议
- 我的预约查看、取消与删除

### 审批与通知

- 管理员审批待处理预约
- 超级管理员撤销历史审批结果
- 通知抽屉集中展示会议相关通知

### 运营与用户管理

- 运营看板
- 管理员/普通用户差异化数据展示
- 用户账号、角色和启停状态管理

## 角色权限

| 角色 | 说明 | 默认首页 |
| --- | --- | --- |
| `USER` | 普通用户，可预约会议室并管理自己的预约记录 | `/dashboard` |
| `ADMIN` | 管理员，可审批预约、查看运营数据，并进入用户管理 | `/dashboard` |
| `SUPER_ADMIN` | 超级管理员，具备管理员能力，并可执行会议室高级治理与审批撤销 | `/dashboard` |

## 环境要求

默认开发环境优先按 Windows 11 + PowerShell 7+ 使用。

- Java 21
- Maven 3.8+
- Node.js 18+ 与 npm
- MySQL 8，默认端口 `3306`
- Redis，默认端口 `6379`

后端默认配置见 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)。

## 快速开始

### 方式一：一键启动

在项目根目录执行：

```powershell
.\start-dev.bat
```

脚本会检查目录、`mvn`、`npm`、MySQL 端口和前端依赖，随后启动后端并等待 `/api/health` 健康检查通过，再启动前端。

### 方式二：手动启动

启动后端：

```powershell
cd .\backend
mvn spring-boot:run
```

启动前端：

```powershell
cd .\frontend
npm install
npm run dev
```

## 默认访问地址

- 前端：[http://localhost:5175](http://localhost:5175)
- 后端：[http://localhost:8082](http://localhost:8082)
- 接口文档：[http://localhost:8082/doc.html](http://localhost:8082/doc.html)

## 默认账号

默认账号由后端启动配置自动注入：

- 管理员：`admin / admin123`
- 超级管理员：`root / root123`
- 普通用户：通过注册页自行创建

> 默认账号仅用于本地开发和课程演示，实际部署前应修改默认密码并关闭演示初始化逻辑。

## 项目结构

```text
meeting-room-system/
├── backend/              # Spring Boot 后端工程
├── frontend/             # Vue 3 前端工程
├── docs/                 # 实验文档、图示与项目截图
├── .github/              # GitHub 配置
├── start-dev.bat         # Windows 一键启动脚本
├── .gitignore
└── README.md
```

### 后端主要模块

- `auth/`：登录注册
- `security/`：认证与权限控制
- `room/`：会议室管理
- `reservation/`：预约、审批与通知
- `stats/`：运营看板统计
- `user/`：用户管理
- `bootstrap/`：默认账号与演示数据初始化
- `health/`：启动脚本使用的健康检查接口

### 前端主要目录

- `views/`：页面
- `components/`：复用组件
- `api/`：接口封装
- `store/`：状态管理
- `router/`：路由与权限守卫
- `utils/`：工具函数
- `assets/`：静态资源

## 实验文档

课程实验交付物位于 `docs/` 目录。

| 目录 | 内容 |
| --- | --- |
| `docs/documents` | 需求规格、产品原型、用户故事地图、系统架构、数据库设计、API 接口文档 |
| `docs/diagrams` | 架构图、流程图、ER 图、时序图、用户故事地图 |
| `docs/page-screenshots` | 系统真实运行页面截图 |

正式文档清单：

- `docs/documents/01-需求规格说明书.docx`
- `docs/documents/02-产品原型设计.docx`
- `docs/documents/03-用户故事地图.docx`
- `docs/documents/04-系统架构设计文档.docx`
- `docs/documents/05-数据库设计文档.docx`
- `docs/documents/06-API接口文档.docx`

## 常用开发命令

### 后端

```powershell
cd .\backend
mvn clean package
mvn spring-boot:run
```

### 前端

```powershell
cd .\frontend
npm install
npm run dev
npm run build
npm run preview
```

## 前端异常恢复

如果项目位于 Windows 中文路径下，Vite 依赖优化可能偶发异常。

常规启动：

```powershell
npm run dev
```

清理缓存后启动：

```powershell
npm run dev:recover
```

强制重建依赖缓存：

```powershell
npm run dev:force
```

常见表现：

- 登录后空白页
- 页面切换后内容消失
- 控制台出现 `504 Outdated Optimize Dep`
- 控制台出现 `Failed to fetch dynamically imported module`

## 常用修改入口

- 登录、权限与默认跳转：`frontend/src/store/auth.ts`、`frontend/src/router/index.ts`、`frontend/src/utils/authRoute.ts`、`backend/src/main/java/com/example/mrs/auth/`、`backend/src/main/java/com/example/mrs/security/`
- 前端页面与样式：`frontend/src/views/`、`frontend/src/components/`、`frontend/src/style.css`
- 预约、审批与会议室业务：`frontend/src/api/mrs.ts`、`backend/src/main/java/com/example/mrs/reservation/`、`backend/src/main/java/com/example/mrs/room/`
- 启动流程与环境配置：`start-dev.bat`、`frontend/vite.config.ts`、`backend/src/main/resources/application.yml`

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

## 说明

本项目主要用于课程综合项目、系统设计练习和前后端分离开发实践。仓库中的默认账号、演示数据和本地启动脚本主要面向开发与演示环境，生产部署前需要重新配置数据库、Redis、密钥、默认账号和跨域策略。
