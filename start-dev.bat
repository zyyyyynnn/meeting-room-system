@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"
set "BACKEND_READY_URL=http://127.0.0.1:8080/"
set "BACKEND_READY_TIMEOUT=60"

if not exist "%BACKEND_DIR%" (
  echo [ERROR] Backend directory not found: %BACKEND_DIR%
  pause
  exit /b 1
)

if not exist "%FRONTEND_DIR%" (
  echo [ERROR] Frontend directory not found: %FRONTEND_DIR%
  pause
  exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven command not found ^(mvn^). Install Maven and add it to PATH.
  pause
  exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] npm command not found. Install Node.js and add it to PATH.
  pause
  exit /b 1
)

where redis-server >nul 2>nul
if errorlevel 1 (
  echo [WARN] redis-server command not found. Ensure Redis is installed and in PATH.
  echo [WARN] You can also start Redis manually via: redis-server.exe
) else (
  echo Starting Redis ^(redis-server^)...
  start "Redis - Server" cmd /k "redis-server"
  timeout /t 1 /nobreak >nul

  where redis-cli >nul 2>nul
  if errorlevel 1 (
    echo [INFO] redis-cli not found, skip PING check.
  ) else (
    redis-cli ping | findstr /i "PONG" >nul
    if errorlevel 1 (
      echo [WARN] Redis PING did not return PONG, check Redis logs.
    ) else (
      echo [INFO] Redis is ready ^(PONG^).
    )
  )
)

echo.
echo [INFO] Please make sure MySQL is already running.
echo.

if not exist "%FRONTEND_DIR%\node_modules" (
  echo [INFO] Frontend dependencies missing, running npm install ...
  call npm --prefix "%FRONTEND_DIR%" install
  if errorlevel 1 (
    echo [ERROR] Frontend dependency installation failed. Run manually:
    echo         cd /d "%FRONTEND_DIR%" ^&^& npm install
    pause
    exit /b 1
  )
)

echo Starting backend ^(backend: mvn spring-boot:run^)...
start "Backend - Spring Boot" cmd /k "cd /d "%BACKEND_DIR%" && mvn spring-boot:run"

echo [INFO] Waiting for backend readiness at %BACKEND_READY_URL% ^(timeout: %BACKEND_READY_TIMEOUT%s^)...
call :wait_for_backend
if errorlevel 1 (
  echo [WARN] Backend did not become reachable within %BACKEND_READY_TIMEOUT%s.
  echo [WARN] Frontend will still start, but login may fail until backend is ready.
  echo [WARN] Please check MySQL, Redis, and the "Backend - Spring Boot" window.
) else (
  echo [INFO] Backend is reachable. Starting frontend next.
)

echo Starting frontend ^(frontend: npm run dev^)...
start "Frontend - Vite" cmd /k "cd /d "%FRONTEND_DIR%" && npm run dev"

echo.
echo Services launched:
echo - Redis window: Redis - Server ^(if redis-server exists^)
echo - Backend window: Backend - Spring Boot
echo - Frontend window: Frontend - Vite
echo.
echo To stop services, close each window or press Ctrl+C.
pause
goto :eof

:wait_for_backend
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$url = '%BACKEND_READY_URL%';" ^
  "$deadline = (Get-Date).AddSeconds([int]%BACKEND_READY_TIMEOUT%);" ^
  "$ready = $false;" ^
  "while ((Get-Date) -lt $deadline) {" ^
  "  try {" ^
  "    Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 5 | Out-Null;" ^
  "    $ready = $true;" ^
  "    break;" ^
  "  } catch {" ^
  "    if ($_.Exception.Response) {" ^
  "      $ready = $true;" ^
  "      break;" ^
  "    }" ^
  "  }" ^
  "  Start-Sleep -Seconds 2;" ^
  "}" ^
  "if ($ready) { exit 0 } else { exit 1 }"
exit /b %errorlevel%
