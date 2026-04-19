@echo off
setlocal ENABLEDELAYEDEXPANSION

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"
set "PYTHON_EXE=python"
set "RUNNER=%PROJECT_ROOT%\tools\brain_agents\run_brain_agents.py"
set "HOOK_DIR=%PROJECT_ROOT%\.git\hooks"
set "HOOK_FILE=%HOOK_DIR%\post-commit"

if not exist "%RUNNER%" (
    echo [ERREUR] Fichier introuvable : %RUNNER%
    pause
    exit /b 1
)

if not exist "%PROJECT_ROOT%\.git" (
    echo [ERREUR] Aucun depot Git trouve dans : %PROJECT_ROOT%
    pause
    exit /b 1
)

if not exist "%HOOK_DIR%" mkdir "%HOOK_DIR%"

(
    echo @echo off
    echo cd /d "%PROJECT_ROOT%"
    echo %PYTHON_EXE% "%RUNNER%" --source post-commit
) > "%HOOK_FILE%"

echo [OK] Hook Git post-commit cree : %HOOK_FILE%

schtasks /create /tn "ReviZeus_BrainAgent" /sc daily /st 22:00 /tr "cmd /c cd /d \"%PROJECT_ROOT%\" && %PYTHON_EXE% \"%RUNNER%\" --source scheduler" /f >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] Tache planifiee non creee. Lance ce fichier en administrateur si tu veux l'automatiser a 22h.
) else (
    echo [OK] Tache planifiee Windows creee : ReviZeus_BrainAgent
)

echo.
echo [TEST] Execution d'un status run...
%PYTHON_EXE% "%RUNNER%" --status

echo.
echo Installation terminee.
pause
