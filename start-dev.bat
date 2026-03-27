@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"

if not exist "%BACKEND_DIR%" (
  echo [ERROR] 未找到后端目录: %BACKEND_DIR%
  pause
  exit /b 1
)

if not exist "%FRONTEND_DIR%" (
  echo [ERROR] 未找到前端目录: %FRONTEND_DIR%
  pause
  exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] 未检测到 Maven 命令（mvn）。请先安装 Maven 并配置到 PATH。
  pause
  exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] 未检测到 npm 命令。请先安装 Node.js 并配置到 PATH。
  pause
  exit /b 1
)

where redis-server >nul 2>nul
if errorlevel 1 (
  echo [WARN] 未检测到 redis-server 命令。请确认 Redis 安装目录已加入 PATH。
  echo [WARN] 若你使用本地安装版，可先手动执行：redis-server.exe
) else (
  echo 正在启动 Redis（redis-server）...
  start "Redis - Server" cmd /k "redis-server"
  timeout /t 1 /nobreak >nul

  where redis-cli >nul 2>nul
  if errorlevel 1 (
    echo [INFO] 未检测到 redis-cli，跳过 PING 校验。
  ) else (
    redis-cli ping | findstr /i "PONG" >nul
    if errorlevel 1 (
      echo [WARN] Redis PING 未返回 PONG，请检查 Redis 窗口日志。
    ) else (
      echo [INFO] Redis 已就绪（PONG）。
    )
  )
)

echo.
echo [INFO] 请确保 MySQL 已提前启动。
echo.

if not exist "%FRONTEND_DIR%\node_modules" (
  echo [INFO] 检测到前端依赖未安装，正在执行 npm install ...
  call npm --prefix "%FRONTEND_DIR%" install
  if errorlevel 1 (
    echo [ERROR] 前端依赖安装失败，请手动执行：
    echo         cd /d "%FRONTEND_DIR%" ^&^& npm install
    pause
    exit /b 1
  )
)

echo 正在启动后端（backend: mvn spring-boot:run）...
start "Backend - Spring Boot" cmd /k "cd /d "%BACKEND_DIR%" && mvn spring-boot:run"

echo 正在启动前端（frontend: npm run dev）...
start "Frontend - Vite" cmd /k "cd /d "%FRONTEND_DIR%" && npm run dev"

echo.
echo 已发起启动：
echo - Redis 窗口：Redis - Server（若检测到 redis-server）
echo - 后端窗口：Backend - Spring Boot
echo - 前端窗口：Frontend - Vite
echo.
echo 关闭服务时，直接关闭对应窗口或按 Ctrl+C。
pause
