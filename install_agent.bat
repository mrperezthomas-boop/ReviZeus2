@echo off
chcp 65001 > nul
echo.
echo ╔══════════════════════════════════════════════════════╗
echo ║       REVIZEUS BRAIN AGENT — Installation            ║
echo ╚══════════════════════════════════════════════════════╝
echo.

:: ─── DETECTION PYTHON ────────────────────────────────────────────────────────
echo [1/3] Verification de Python...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERREUR : Python introuvable dans le PATH.
    echo Installe Python depuis https://python.org et relance.
    pause
    exit /b 1
)
python --version
echo      OK
echo.

:: ─── DETECTION GIT ───────────────────────────────────────────────────────────
echo [2/3] Installation du git hook post-commit...

:: Détecte la racine du repo git (dossier parent du script)
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

:: Cherche .git dans le dossier courant ou son parent
if exist "%SCRIPT_DIR%\.git" (
    set "GIT_DIR=%SCRIPT_DIR%\.git"
) else if exist "%SCRIPT_DIR%\..\git" (
    set "GIT_DIR=%SCRIPT_DIR%\..\git"
) else (
    echo ERREUR : Dossier .git introuvable.
    echo Lance ce script depuis la racine du projet.
    pause
    exit /b 1
)

set "HOOKS_DIR=%GIT_DIR%\hooks"
set "HOOK_FILE=%HOOKS_DIR%\post-commit"
set "AGENT_PATH=%SCRIPT_DIR%\brain_agent.py"

echo    Racine projet : %SCRIPT_DIR%
echo    Git hooks     : %HOOKS_DIR%
echo    Agent Python  : %AGENT_PATH%
echo.

:: Vérifie que brain_agent.py existe
if not exist "%AGENT_PATH%" (
    echo ERREUR : brain_agent.py introuvable dans %SCRIPT_DIR%
    echo Place brain_agent.py a la racine du projet avant de relancer.
    pause
    exit /b 1
)

:: Crée le hook post-commit
(
echo #!/bin/sh
echo # REVIZEUS BRAIN AGENT — Git Hook post-commit
echo # Genere automatiquement par install_agent.bat
echo python "%AGENT_PATH:\=/%"
) > "%HOOK_FILE%"

if %errorlevel% equ 0 (
    echo    Hook cree : %HOOK_FILE%
    echo    OK
) else (
    echo    ERREUR lors de la creation du hook.
)
echo.

:: ─── TASK SCHEDULER ──────────────────────────────────────────────────────────
echo [3/3] Creation de la tache planifiee Windows ^(22h00 chaque jour^)...
echo.

:: Supprime l'ancienne tache si elle existe
schtasks /delete /tn "ReviZeus_BrainAgent" /f >nul 2>&1

:: Crée la nouvelle tache
schtasks /create ^
    /tn "ReviZeus_BrainAgent" ^
    /tr "python \"%AGENT_PATH%\"" ^
    /sc daily ^
    /st 22:00 ^
    /ru "%USERNAME%" ^
    /f >nul 2>&1

if %errorlevel% equ 0 (
    echo    Tache planifiee creee : "ReviZeus_BrainAgent" — tous les jours a 22h00
    echo    OK
) else (
    echo    AVERTISSEMENT : Tache planifiee non creee (droits insuffisants ?).
    echo    Lance ce script en tant qu'Administrateur pour activer le Task Scheduler.
    echo    Le git hook seul suffira dans l'immediat.
)
echo.

:: ─── TEST IMMEDIAT ────────────────────────────────────────────────────────────
echo ─────────────────────────────────────────────────────
echo Test immediat de l'agent...
echo ─────────────────────────────────────────────────────
echo.
python "%AGENT_PATH%" --status
echo.

:: ─── RESUME ──────────────────────────────────────────────────────────────────
echo ╔══════════════════════════════════════════════════════╗
echo ║  Installation terminee !                            ║
echo ╚══════════════════════════════════════════════════════╝
echo.
echo Ce qui va se passer automatiquement :
echo.
echo   - Apres chaque  git commit   → brain_agent.py se lance
echo   - Chaque jour a 22h00        → brain_agent.py se lance
echo.
echo Fichiers mis a jour automatiquement :
echo   BRAIN_REVIZEUS/00_QUICK_START/ETAT_TEMPS_REEL.md
echo   BRAIN_REVIZEUS/02_BLOCS/INDEX_BLOCS.md
echo.
echo Pour lancer manuellement :
echo   python brain_agent.py          (24 dernieres heures)
echo   python brain_agent.py --full   (7 derniers jours)
echo   python brain_agent.py --status (lecture seule)
echo.
pause
