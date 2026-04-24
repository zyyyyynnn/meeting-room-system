@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"
set "BACKEND_READY_URL=http://127.0.0.1:8082/api/health"
set "BACKEND_READY_TIMEOUT=60"
set "BACKEND_COMMAND=mvn spring-boot:run"
set "FRONTEND_COMMAND=npm run dev -- --host 127.0.0.1 --port 5175"
set "FRONTEND_URL=http://127.0.0.1:5175"
set "MYSQL_HOST=127.0.0.1"
set "MYSQL_PORT=3306"
set "MYSQL_SERVICE=MySQL80"

echo [INFO] Meeting Room System startup check
echo [INFO] Root: %ROOT%
echo.

if not exist "%BACKEND_DIR%" (
  echo [ERROR] Missing backend directory: %BACKEND_DIR%
  echo [FIX] Confirm this script is in the project root, then run it again.
  pause
  exit /b 1
)

if not exist "%FRONTEND_DIR%" (
  echo [ERROR] Missing frontend directory: %FRONTEND_DIR%
  echo [FIX] Confirm this script is in the project root, then run it again.
  pause
  exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven command not found: mvn
  echo [FIX] Install Maven and add it to PATH, then reopen this terminal.
  pause
  exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] npm command not found.
  echo [FIX] Install Node.js/npm and add it to PATH, then reopen this terminal.
  pause
  exit /b 1
)

echo [INFO] Checking MySQL at %MYSQL_HOST%:%MYSQL_PORT% ...
call :check_mysql
if errorlevel 1 (
  echo [ERROR] MySQL is not reachable at %MYSQL_HOST%:%MYSQL_PORT%.
  echo.
  echo [INFO] Current %MYSQL_SERVICE% service status:
  sc query "%MYSQL_SERVICE%"
  echo.
  echo [FIX] Start MySQL manually, then run this script again.
  echo       Administrator command:
  echo         net start %MYSQL_SERVICE%
  echo       Foreground command example:
  echo         mysqld.exe --defaults-file=^<your-my.ini-path^> --console
  echo       Common my.ini path:
  echo         C:\ProgramData\MySQL\MySQL Server 8.0\my.ini
  pause
  exit /b 1
)
echo [INFO] MySQL is reachable.
echo.

if not exist "%FRONTEND_DIR%\node_modules" (
  echo [INFO] Frontend dependencies missing, running npm install ...
  call npm --prefix "%FRONTEND_DIR%" install
  if errorlevel 1 (
    echo [ERROR] Frontend dependency installation failed.
    echo [FIX] Run manually:
    echo       cd /d "%FRONTEND_DIR%"
    echo       npm install
    pause
    exit /b 1
  )
) else (
  echo [INFO] Frontend dependencies already installed.
)
echo.

echo [INFO] Starting backend: %BACKEND_COMMAND%
start "Backend - Spring Boot" cmd /k "cd /d ""%BACKEND_DIR%"" && %BACKEND_COMMAND%"

echo [INFO] Waiting for backend health check:
echo       %BACKEND_READY_URL%
echo       timeout: %BACKEND_READY_TIMEOUT%s
call :wait_for_backend
if errorlevel 1 (
  echo [ERROR] Backend did not become healthy within %BACKEND_READY_TIMEOUT%s. Frontend will not start.
  echo.
  echo [CHECK] MySQL is still reachable at %MYSQL_HOST%:%MYSQL_PORT%.
  echo [CHECK] Backend config: backend\src\main\resources\application.yml
  echo [CHECK] If you use a local override, inspect application-local.yml too.
  echo [CHECK] Read the "Backend - Spring Boot" window logs for the first error.
  pause
  exit /b 1
)
echo [INFO] Backend is healthy.
echo.

echo [INFO] Starting frontend: %FRONTEND_COMMAND%
start "Frontend - Vite" cmd /k "cd /d ""%FRONTEND_DIR%"" && %FRONTEND_COMMAND%"

echo.
echo [OK] Services launched.
echo - Backend:  http://127.0.0.1:8082
echo - Frontend: %FRONTEND_URL%
echo.
echo To stop services, close each window or press Ctrl+C.
pause
goto :eof

:check_mysql
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$client = New-Object Net.Sockets.TcpClient;" ^
  "try {" ^
  "  $iar = $client.BeginConnect('%MYSQL_HOST%', [int]%MYSQL_PORT%, $null, $null);" ^
  "  if (-not $iar.AsyncWaitHandle.WaitOne(2000, $false)) { exit 1 }" ^
  "  $client.EndConnect($iar);" ^
  "  exit 0;" ^
  "} catch { exit 1 } finally { $client.Close() }"
exit /b %errorlevel%

:wait_for_backend
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$url = '%BACKEND_READY_URL%';" ^
  "$deadline = (Get-Date).AddSeconds([int]%BACKEND_READY_TIMEOUT%);" ^
  "while ((Get-Date) -lt $deadline) {" ^
  "  try {" ^
  "    $resp = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 5;" ^
  "    if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 300) { exit 0 }" ^
  "  } catch {}" ^
  "  Start-Sleep -Seconds 2;" ^
  "}" ^
  "exit 1"
exit /b %errorlevel%
