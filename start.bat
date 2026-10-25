@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul
cd /d "%~dp0"
title Cloudinary Media Lab

echo ============================================
echo   Cloudinary Media Lab - cai dat va chay
echo ============================================
echo.

where docker >nul 2>&1
if errorlevel 1 (
  echo [LOI] Chua cai Docker Desktop.
  echo Tai tai: https://www.docker.com/products/docker-desktop
  pause
  exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
  echo [LOI] Docker chua chay.
  echo Hay mo Docker Desktop, cho den khi bao Engine running, roi chay lai file nay.
  pause
  exit /b 1
)

if not exist ".env" (
  echo Lan dau chay: nhap khoa Cloudinary.
  echo Lay tai: console.cloudinary.com  ^>  Settings  ^>  API Keys
  echo.
  set /p CN=Cloud name: 
  set /p AK=API key: 
  set /p AS=API secret: 
  set "CN=!CN: =!"
  set "AK=!AK: =!"
  set "AS=!AS: =!"
  if "!CN!"=="" goto :thieu
  if "!AK!"=="" goto :thieu
  if "!AS!"=="" goto :thieu
  (
    echo CLOUDINARY_CLOUD_NAME=!CN!
    echo CLOUDINARY_API_KEY=!AK!
    echo CLOUDINARY_API_SECRET=!AS!
  ) > .env
  echo Da tao file .env
  echo.
)

echo Dang build va khoi dong. Lan dau co the mat vai phut, xin cho...
docker compose up --build -d
if errorlevel 1 (
  echo [LOI] Khong khoi dong duoc. Xem thong bao loi o tren.
  pause
  exit /b 1
)

echo Dang cho ung dung san sang...
powershell -NoProfile -Command "for($i=0;$i -lt 90;$i++){ try { Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 http://localhost:8090/api/images | Out-Null; exit 0 } catch { Start-Sleep 2 } }; exit 1"
if errorlevel 1 (
  echo [LOI] Ung dung chua san sang. Xem log bang lenh: docker compose logs
  pause
  exit /b 1
)

echo.
echo Xong! Dang mo http://localhost:8090
start "" http://localhost:8090
echo De dung ung dung: docker compose down
pause
exit /b 0

:thieu
echo [LOI] Ban chua nhap du 3 gia tri. Chay lai file nay va nhap lai.
pause
exit /b 1
