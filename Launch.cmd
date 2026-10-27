@echo off
cd /d "%~dp0"
if exist "%~dp0Pocket Music.exe" (
    start "" "%~dp0Pocket Music.exe"
    exit /b
)
powershell.exe -NoProfile -STA -ExecutionPolicy Bypass -File "%~dp0MusicDownloader.ps1" -Desktop
if errorlevel 1 pause
