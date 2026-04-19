#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from __future__ import annotations
import subprocess, sys, re
from datetime import datetime, timedelta
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.resolve()
BRAIN_DIR = PROJECT_ROOT / "BRAIN_REVIZEUS"
FILE_ETAT = BRAIN_DIR / "00_QUICK_START" / "ETAT_TEMPS_REEL.md"
FILE_INDEX = BRAIN_DIR / "02_BLOCS" / "INDEX_BLOCS.md"

BLOC_MAPPING = {
    "BRAIN_CORE": ["RULES_SACREES", "CARTOGRAPHIE_MASTER", "DOMAINES_OWNERS", "IA_DOCS", "TAXONOMIE"],
    "BRAIN_BLOCKS": ["INDEX_BLOCS", "BLOC_"],
    "LORE": ["LORE", "MYTHOLOGIQUE", "DIEUX_PERSONNALITES"],
    "H_AVENTURE": ["AVENTURE", "TEMPLE", "WORLD_MAP", "BESTIAIRE"],
    "AI_TOOLING": ["CURSOR", "SKILLS", "PROMPT"],
}

def git(args):
    r = subprocess.run(["git"] + args, cwd=str(PROJECT_ROOT), capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout.strip()

def get_commits(hours):
    since = (datetime.now() - timedelta(hours=hours)).strftime("%Y-%m-%d %H:%M:%S")
    raw = git(["log", f"--since={since}", "--pretty=format:%h|%ai|%s", "--no-merges"])
    out = []
    if raw:
        for line in raw.splitlines():
            p = line.split("|", 2)
            if len(p) == 3:
                out.append({"hash": p[0], "date": p[1][:16], "msg": p[2]})
    return out

def get_changed_files(hours):
    since = (datetime.now() - timedelta(hours=hours)).strftime("%Y-%m-%d %H:%M:%S")
    raw = git(["log", f"--since={since}", "--name-only", "--pretty=format:", "--diff-filter=AM"])
    files = []
    if raw:
        for line in raw.splitlines():
            line = line.strip()
            if line:
                files.append(line)
    return sorted(set(files))

def detect_blocs(files):
    touched = {}
    for fp in files:
        name = Path(fp).name.upper()
        if fp.replace("\\", "/").startswith("BRAIN_REVIZEUS/09_RUNTIME_AGENT/"):
            touched.setdefault("AGENT_OPS", []).append(Path(fp).name)
            continue
        if fp.replace("\\", "/").startswith("BRAIN_REVIZEUS/10_RAPPORTS_QA/"):
            touched.setdefault("QA", []).append(Path(fp).name)
            continue
        for bloc, patterns in BLOC_MAPPING.items():
            for pattern in patterns:
                if pattern in name:
                    touched.setdefault(bloc, []).append(Path(fp).name)
                    break
    return touched

def build_etat(commits, files, hours):
    touched = detect_blocs(files)
    lines = [
        "# ⚡ ÉTAT EN TEMPS RÉEL — RÉVIZEUS",
        f"> Mis à jour automatiquement le **{datetime.now().strftime('%d/%m/%Y %H:%M')}** par `brain_agent.py`",
        "",
        "## Dépôt Git",
        f"- Branche : `{git(['branch', '--show-current']) or 'main'}`",
        f"- Dernier commit : `{git(['log', '-1', '--pretty=format:%h — %s'])}`",
        f"- Fenêtre analysée : {hours}h",
        "",
        f"## Commits récents ({len(commits)})",
    ]
    for c in commits[:10]:
        lines.append(f"- `{c['hash']}` {c['date']} — {c['msg']}")
    lines += ["", f"## Fichiers modifiés ({len(files)})"]
    for f in files[:50]:
        lines.append(f"- `{f}`")
    lines += ["", "## Blocs impactés"]
    if touched:
        for bloc, fl in sorted(touched.items()):
            lines.append(f"- **{bloc}** : {', '.join(f'`{x}`' for x in fl[:5])}")
    else:
        lines.append("- Aucun bloc clairement identifié")
    lines.append("")
    return "\n".join(lines) + "\n"

def inject_index(commits, files):
    if not FILE_INDEX.exists():
        return
    content = FILE_INDEX.read_text(encoding="utf-8")
    touched = detect_blocs(files)
    section = ["", "---", "", f"## 🔄 Activité récente — {datetime.now().strftime('%d/%m/%Y %H:%M')}", ""]
    for c in commits[:5]:
        section.append(f"- `{c['hash']}` {c['date']} — {c['msg']}")
    if touched:
        section.append("")
        section.append("**Blocs touchés :**")
        for bloc, fl in sorted(touched.items()):
            section.append(f"- {bloc} : {', '.join(f'`{x}`' for x in fl[:3])}")
    section += ["", "---", ""]
    clean = re.sub(r"\n---\n\n## 🔄 Activité récente.*?---\n", "\n", content, flags=re.DOTALL)
    FILE_INDEX.write_text(clean.rstrip() + "\n" + "\n".join(section), encoding="utf-8")

def main():
    status = "--status" in sys.argv[1:]
    hours = 24
    BRAIN_DIR.mkdir(parents=True, exist_ok=True)
    FILE_ETAT.parent.mkdir(parents=True, exist_ok=True)

    commits = get_commits(hours)
    files = get_changed_files(hours)

    print("╔══════════════════════════════════════════╗")
    print("║       REVIZEUS BRAIN AGENT — TRUE        ║")
    print("╚══════════════════════════════════════════╝")
    print(f"  Projet     : {PROJECT_ROOT}")
    print(f"  Brain      : {BRAIN_DIR}")
    print(f"  Mode       : {'STATUS ONLY' if status else 'STANDARD (24h)'}")
    print(f"  Horodatage : {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
    print()
    print("→ Lecture des commits récents...")
    print(f"  {len(commits)} commit(s) trouvé(s)")
    print("→ Scan des fichiers modifiés...")
    print(f"  {len(files)} fichier(s) modifié(s)")
    print("→ Analyse des blocs impactés...")
    touched = detect_blocs(files)
    if touched:
        for bloc, fl in sorted(touched.items()):
            print(f"  ✓ {bloc} : {', '.join(fl[:3])}")
    else:
        print("  Aucun bloc clairement identifié")

    if status:
        print()
        print("Mode --status : aucune écriture effectuée.")
        return 0

    FILE_ETAT.write_text(build_etat(commits, files, hours), encoding="utf-8")
    inject_index(commits, files)
    print()
    print("✅ BRAIN mis à jour.")
    print(f"- {FILE_ETAT}")
    print(f"- {FILE_INDEX}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
