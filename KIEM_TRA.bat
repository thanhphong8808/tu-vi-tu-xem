@echo off
setlocal EnableExtensions
chcp 65001 >nul 2>nul
title Tu Vi Tu Xem - Tu kiem tra
cd /d "%~dp0"

echo ==============================================================
echo   TU VI TU XEM - TU KIEM TRA TREN WINDOWS
echo   Khong can Android Studio. Chi can Java 17+ va Internet.
echo ==============================================================
echo.

call :find_java
if defined JAVA_OK goto :have_java

echo [!] Chua tim thay Java 17 tro len tren may.
where winget >nul 2>nul
if errorlevel 1 goto :no_winget

echo [..] Dang cai Java 21 - Eclipse Temurin, mien phi - bang winget.
echo      Neu Windows hoi quyen quan tri thi bam Yes.
echo.
winget install --id EclipseAdoptium.Temurin.21.JDK -e --source winget --accept-source-agreements --accept-package-agreements
call :find_java
if defined JAVA_OK goto :have_java
echo.
echo [X] Trinh cai da chay nhung chua thay Java.
echo     Dong cua so nay roi bam dup lai KIEM_TRA.bat.
goto :end_fail

:no_winget
echo [X] May khong co winget de cai tu dong.
echo     Trang tai Java 21 dang mo: chon ban Windows x64, tai file .msi,
echo     cai xong thi bam dup lai KIEM_TRA.bat.
start "" "https://adoptium.net/temurin/releases/?os=windows&arch=x64&package=jdk&version=21"
goto :end_fail

:have_java
echo [OK] Java %JAVA_VER%
echo      %JAVA_HOME%
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo.
echo [..] Buoc 1/2: bien dich va chay toan bo bai kiem tra.
echo      Lan dau may tu tai Gradle va thu vien, khoang 200 MB, mat 3-10 phut.
echo      Cac lan sau chi mat khoang 1 phut.
echo.
call "%~dp0gradlew.bat" test --continue --no-daemon --console=plain
if errorlevel 1 goto :tests_failed

echo.
echo ==============================================================
echo   BUOC 1 DAT: BUILD SUCCESSFUL - tat ca bai kiem tra deu qua
echo ==============================================================
if exist "core\engine\build\reports\tests\test\index.html" start "" "core\engine\build\reports\tests\test\index.html"
echo.
echo Buoc 2/2 - tuy chon: vet can 2,2 trieu la so, moi ngay gio sinh 1900-2100.
choice /c CK /t 20 /d K /m "Chay khong? C = Co, K = Khong. Tu chon K sau 20 giay"
if errorlevel 2 goto :end_ok
echo.
call "%~dp0gradlew.bat" :core:engine:stressCheck --no-daemon --console=plain
if errorlevel 1 goto :stress_failed
echo.
echo [OK] Vet can dat: 0 loi.
goto :end_ok

:tests_failed
echo.
echo [X] Co bai kiem tra KHONG dat hoac build bi loi.
echo [..] Dang ghi nhat ky day du vao kiem_tra_log.txt, cho them 1-2 phut...
call "%~dp0gradlew.bat" test --continue --no-daemon --console=plain > "%~dp0kiem_tra_log.txt" 2>&1
echo.
echo     Gui file kiem_tra_log.txt trong thu muc nay cho Claude de sua.
if exist "core\engine\build\reports\tests\test\index.html" start "" "core\engine\build\reports\tests\test\index.html"
goto :end_fail

:stress_failed
echo.
echo [X] Vet can bao loi. Chup man hinh doan cuoi cua so nay gui cho Claude.
goto :end_fail

:end_ok
echo.
echo Xong. Bam phim bat ky de dong cua so.
pause >nul
exit /b 0

:end_fail
echo.
echo Bam phim bat ky de dong cua so.
pause >nul
exit /b 1

rem ------------------------------------------------------------------
rem Tim JDK 17+: JAVA_HOME, cac thu muc cai dat thuong gap, roi PATH.
rem ------------------------------------------------------------------
:find_java
set "JAVA_OK="
if defined JAVA_HOME call :try_java "%JAVA_HOME%"
if defined JAVA_OK exit /b 0
for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-*" "%ProgramFiles%\Microsoft\jdk-*" "%ProgramFiles%\Java\jdk-*" "%ProgramFiles%\Zulu\zulu-*" "%ProgramFiles%\Amazon Corretto\jdk*" "%ProgramFiles%\Android\Android Studio\jbr" "%LocalAppData%\Programs\Android Studio\jbr") do call :try_java "%%~d"
if defined JAVA_OK exit /b 0
for /f "delims=" %%j in ('where java 2^>nul') do call :try_java_exe "%%~j"
exit /b 0

:try_java_exe
if defined JAVA_OK exit /b 0
for %%p in ("%~dp1..") do call :try_java "%%~fp"
exit /b 0

:try_java
if defined JAVA_OK exit /b 0
set "_JH=%~1"
if "%_JH:~-1%"=="\" set "_JH=%_JH:~0,-1%"
if not exist "%_JH%\bin\java.exe" exit /b 0
set "_JV="
for /f "tokens=3" %%v in ('call "%_JH%\bin\java.exe" -version 2^>^&1 ^| findstr /i /c:"version"') do if not defined _JV set "_JV=%%~v"
if not defined _JV exit /b 0
set "_JM=0"
for /f "delims=.-+_" %%m in ("%_JV%") do set "_JM=%%m"
set "_JMN=0"
set /a _JMN=_JM+0 2>nul
if %_JMN% LSS 17 exit /b 0
set "JAVA_HOME=%_JH%"
set "JAVA_VER=%_JV%"
set "JAVA_OK=1"
exit /b 0
