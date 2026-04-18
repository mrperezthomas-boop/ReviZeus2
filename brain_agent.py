#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
╔══════════════════════════════════════════════════════════════════╗
║           REVIZEUS BRAIN AGENT — v2.0                           ║
║  Mise à jour automatique des fichiers BRAIN_REVIZEUS/           ║
║  À placer à la RACINE du projet (même niveau que BRAIN_REVIZEUS) ║
╚══════════════════════════════════════════════════════════════════╝

Fichiers mis à jour automatiquement :
  → BRAIN_REVIZEUS/00_QUICK_START/ETAT_TEMPS_REEL.md   (créé/écrasé)
  → BRAIN_REVIZEUS/02_BLOCS/INDEX_BLOCS.md              (section activité injectée)

Déclenchement :
  - Git hook post-commit  (automatique après chaque commit)
  - Task Scheduler Windows (automatique chaque jour à 22h)
  - Manuel : python brain_agent.py

Usage :
  python brain_agent.py           # mise à jour standard (24h)
  python brain_agent.py --full    # scan complet (7 jours)
  python brain_agent.py --status  # affiche l'état sans écrire
"""

import subprocess
import os
import sys
import re
from datetime import datetime, timedelta
from pathlib import Path

# ─── CONFIGURATION ───────────────────────────────────────────────────────────
# Chemin du projet (détecté automatiquement depuis l'emplacement du script)
PROJECT_ROOT = Path(__file__).parent.resolve()
BRAIN_DIR    = PROJECT_ROOT / "BRAIN_REVIZEUS"

# Fichiers à mettre à jour
FILE_ETAT_TEMPS_REEL = BRAIN_DIR / "00_QUICK_START" / "ETAT_TEMPS_REEL.md"
FILE_INDEX_BLOCS     = BRAIN_DIR / "02_BLOCS" / "INDEX_BLOCS.md"

# Fenêtre de scan par défaut
HOURS_DEFAULT = 24
HOURS_FULL    = 168   # 7 jours si --full

# ─── MAPPING FICHIERS → BLOCS ─────────────────────────────────────────────────
# Chaque liste contient des sous-chaînes à chercher dans le nom du fichier
BLOC_MAPPING = {
    "B — Dialogues RPG": [
        "DialogRPG", "GodSpeechAnimator", "DialogCategory", "DialogContext",
        "DialogRPGConfig", "DivineDialogue", "DivineMicroCopy", "TechnicalError",
        "LoadingDivineDialog", "GodMatiereActivity", "SettingsActivity",
        "LoginActivity", "AvatarActivity", "AccountSelectActivity",
        "InventoryActivity", "HeroSelectActivity", "TitleScreenActivity",
        "SavoirActivity", "OraclePromptActivity", "BadgeBookActivity",
        "AuthActivity", "MainMenuActivity",
    ],
    "C — Audio Global / TTS": [
        "SoundManager", "SpeakerTtsHelper", "LyriaManager",
        "OlympianMusicCatalog", "JukeboxAdapter", "MusicTrackItem",
    ],
    "D — Oracle Premium": [
        "OracleActivity", "OraclePromptActivity", "GeminiManager",
        "AssetTextReader", "IAristote",
    ],
    "E — Quiz Nouvelle Génération": [
        "QuizActivity", "TrainingQuizActivity", "TrainingSelectActivity",
        "QuizQuestion", "QuizTimerManager", "QuizQuestionPlanner",
        "GeminiQuestionTypeHelper", "NormalTrainingBuilder", "UltimateQuizBuilder",
        "QuizResultActivity",
    ],
    "F — Récompenses / Économie": [
        "QuizRewardManager", "CurrencyManager", "HudRewardAnimator",
        "ResultActivity",
    ],
    "G — Forge & Inventaire": [
        "ForgeActivity", "CraftingSystem", "InventoryActivity",
        "InventoryItem", "RecipeAdapter",
    ],
    "H — Temple Progress / Monde": [
        "TempleAdventure", "TempleMap", "TempleNode", "WorldMap",
        "AdventureManager", "AdventureState", "MapTempleActivity",
        "TempleNodeEncounterActivity", "WorldMapActivity", "BaseAdventureActivity",
    ],
    "I — Savoirs Vivants": [
        "SavoirActivity", "KnowledgeFragmentManager", "CourseEntry",
    ],
    "J — Extensions Divines": [
        "GodLoreManager", "GodPersonalityEngine", "DivineDialogueOrchestrator",
        "DivineMicroCopyLibrary", "GodManager", "GodTriggerEngine",
        "PantheonConfig",
    ],
    "K — IA Adaptative": [
        "AdaptiveContextFormatter", "AdaptiveDialogueEngine",
        "AdaptiveLearningContext", "PlayerAdaptiveSnapshot",
        "PlayerContextResolver", "AdaptiveLearningContextResolver",
    ],
    "L — Analytics Pédagogiques": [
        "UserAnalyticsEngine", "UserWeaknessAnalyzer", "UserInsight",
        "InsightCache", "MemoryScore", "UserAnalytics",
    ],
    "M — Recommandations IA": [
        "LearningRecommendation", "IAristoteEngine",
    ],
    "N — Dashboard Vivant": [
        "DashboardActivity", "DashboardInsightsWidget",
    ],
    "O — Rangs & Badges": [
        "BadgeManager", "BadgeDefinition", "BadgeBookActivity",
        "BadgeUnlockOverlayManager", "AchievementPopupManager",
    ],
}

# ─── GIT UTILS ───────────────────────────────────────────────────────────────
def git(args):
    """Exécute une commande git dans le répertoire projet."""
    try:
        r = subprocess.run(
            ["git"] + args,
            cwd=str(PROJECT_ROOT),
            capture_output=True, text=True,
            encoding="utf-8", errors="replace"
        )
        return r.stdout.strip()
    except Exception as e:
        return f"[GIT_ERROR: {e}]"

def get_commits(hours):
    since = (datetime.now() - timedelta(hours=hours)).strftime("%Y-%m-%d %H:%M:%S")
    raw = git(["log", f"--since={since}", "--pretty=format:%h|%ai|%s", "--no-merges"])
    if not raw or "GIT_ERROR" in raw:
        return []
    result = []
    for line in raw.splitlines():
        parts = line.split("|", 2)
        if len(parts) == 3:
            result.append({"hash": parts[0].strip(), "date": parts[1][:16], "msg": parts[2].strip()})
    return result

def get_changed_files(hours):
    since = (datetime.now() - timedelta(hours=hours)).strftime("%Y-%m-%d %H:%M:%S")
    raw = git(["log", f"--since={since}", "--name-only", "--pretty=format:", "--diff-filter=AM"])
    if not raw or "GIT_ERROR" in raw:
        return []
    files = set()
    for line in raw.splitlines():
        line = line.strip()
        if line and (line.endswith(".kt") or line.endswith(".xml")):
            files.add(line)
    return sorted(files)

def get_branch():
    return git(["branch", "--show-current"]) or "main"

def get_last_commit():
    return git(["log", "-1", "--pretty=format:%h — %s (%cd)", "--date=format:%d/%m/%Y %H:%M"])

def get_total_commits():
    return git(["rev-list", "--count", "HEAD"])

# ─── ANALYSE DES FICHIERS → BLOCS ────────────────────────────────────────────
def detect_blocs(files):
    """Retourne un dict {nom_bloc: [fichiers concernés]}."""
    touched = {}
    for filepath in files:
        basename = Path(filepath).name
        for bloc_name, patterns in BLOC_MAPPING.items():
            for pattern in patterns:
                if pattern.lower() in basename.lower():
                    touched.setdefault(bloc_name, [])
                    if basename not in touched[bloc_name]:
                        touched[bloc_name].append(basename)
                    break
    return touched

def categorize_files(files):
    """Classe les fichiers par type pour l'affichage."""
    cats = {
        "Activities": [], "Managers": [], "Core / IA": [],
        "Models / DAO": [], "UI / Adapters": [], "Layouts XML": [], "Autres": []
    }
    for f in files:
        b = Path(f).name
        if "Activity" in b:               cats["Activities"].append(b)
        elif "Manager" in b:              cats["Managers"].append(b)
        elif "core/" in f or "Engine" in b or "Analyzer" in b or "Builder" in b:
                                          cats["Core / IA"].append(b)
        elif "models/" in f or "Dao" in b or "Entity" in b or "Profile" in b:
                                          cats["Models / DAO"].append(b)
        elif "Adapter" in b or "Item" in b or "Widget" in b:
                                          cats["UI / Adapters"].append(b)
        elif b.endswith(".xml"):          cats["Layouts XML"].append(b)
        else:                             cats["Autres"].append(b)
    return {k: v for k, v in cats.items() if v}

# ─── GÉNÉRATION DES FICHIERS ──────────────────────────────────────────────────
def build_etat_temps_reel(commits, files, hours):
    """Génère le contenu complet de ETAT_TEMPS_REEL.md."""
    now = datetime.now().strftime("%d/%m/%Y %H:%M")
    branch = get_branch()
    last_commit = get_last_commit()
    total = get_total_commits()
    touched_blocs = detect_blocs(files)
    cats = categorize_files(files)

    lines = [
        "# ⚡ ÉTAT EN TEMPS RÉEL — RÉVIZEUS",
        f"> Mis à jour automatiquement le **{now}** par `brain_agent.py`",
        "",
        "---",
        "",
        "## 🔧 Dépôt Git",
        "",
        f"| Info | Valeur |",
        f"|------|--------|",
        f"| Branche | `{branch}` |",
        f"| Total commits | {total} |",
        f"| Dernier commit | `{last_commit}` |",
        f"| Fenêtre analysée | {hours}h |",
        "",
        "---",
        "",
    ]

    # Commits récents
    if commits:
        lines += [
            f"## 📝 Commits récents ({len(commits)} dans les dernières {hours}h)",
            "",
        ]
        for c in commits[:20]:
            lines.append(f"- `{c['hash']}` {c['date']} — {c['msg']}")
        lines.append("")
    else:
        lines += [f"## 📝 Aucun commit dans les dernières {hours}h", ""]

    lines.append("---")
    lines.append("")

    # Fichiers modifiés par catégorie
    if files:
        lines += [
            f"## 📂 Fichiers modifiés ({len(files)} fichiers .kt / .xml)",
            "",
        ]
        for cat, flist in cats.items():
            lines.append(f"**{cat}** ({len(flist)}) :")
            for f in flist:
                lines.append(f"  - `{f}`")
            lines.append("")
    else:
        lines += [f"## 📂 Aucun fichier modifié dans les dernières {hours}h", ""]

    lines.append("---")
    lines.append("")

    # Blocs impactés
    if touched_blocs:
        lines += [
            "## 🧱 Blocs impactés",
            "",
        ]
        for bloc, flist in sorted(touched_blocs.items()):
            lines.append(f"**Bloc {bloc}**")
            for f in flist:
                lines.append(f"  - `{f}`")
            lines.append("")
    else:
        lines += ["## 🧱 Aucun bloc clairement impacté détecté", ""]

    lines.append("---")
    lines.append("")
    lines += [
        "## 📋 Rappel état des blocs",
        "",
        "| Bloc | Statut |",
        "|------|--------|",
        "| A — Stabilisation technique | ✅ TERMINÉ |",
        "| B — Dialogues RPG | 🟡 EN COURS (~30%) |",
        "| B2 — Personas divines | 📋 Planifié |",
        "| C → Q | 📋 Planifiés |",
        "",
        "---",
        "",
        f"*Généré par `brain_agent.py` — {now}*",
        "",
    ]

    return "\n".join(lines)


def inject_activity_in_index(commits, files, touched_blocs):
    """Injecte une section ACTIVITÉ RÉCENTE dans INDEX_BLOCS.md."""
    if not FILE_INDEX_BLOCS.exists():
        print(f"  ⚠️  INDEX_BLOCS.md introuvable : {FILE_INDEX_BLOCS}")
        return False

    with open(FILE_INDEX_BLOCS, "r", encoding="utf-8") as f:
        content = f.read()

    now = datetime.now().strftime("%d/%m/%Y %H:%M")

    # Construit la section
    section_lines = [
        "",
        "---",
        "",
        f"## 🔄 Activité récente — {now}",
        "",
    ]

    if commits:
        section_lines.append(f"**{len(commits)} commit(s) récent(s) :**")
        for c in commits[:5]:
            section_lines.append(f"- `{c['hash']}` {c['date']} — {c['msg']}")
        section_lines.append("")

    if touched_blocs:
        section_lines.append("**Blocs touchés :**")
        for bloc in sorted(touched_blocs.keys()):
            section_lines.append(f"- Bloc {bloc} : {', '.join(f'`{f}`' for f in touched_blocs[bloc][:3])}")
        section_lines.append("")

    if not commits and not touched_blocs:
        section_lines.append("*Aucune activité détectée dans la fenêtre d'analyse.*")
        section_lines.append("")

    section_lines.append("---")
    section_lines.append("")

    # Supprime l'ancienne section activité si elle existe
    clean = re.sub(
        r"\n---\n\n## 🔄 Activité récente.*?---\n",
        "\n",
        content,
        flags=re.DOTALL
    )

    # Ajoute la nouvelle section à la fin
    new_content = clean.rstrip() + "\n" + "\n".join(section_lines)

    with open(FILE_INDEX_BLOCS, "w", encoding="utf-8") as f:
        f.write(new_content)

    return True


# ─── MAIN ─────────────────────────────────────────────────────────────────────
def main():
    args = sys.argv[1:]
    status_only = "--status" in args
    full_scan   = "--full" in args
    hours       = HOURS_FULL if full_scan else HOURS_DEFAULT

    print("╔══════════════════════════════════════════╗")
    print("║       REVIZEUS BRAIN AGENT — v2.0        ║")
    print("╚══════════════════════════════════════════╝")
    print(f"  Projet     : {PROJECT_ROOT}")
    print(f"  Brain      : {BRAIN_DIR}")
    print(f"  Mode       : {'STATUS ONLY' if status_only else ('FULL (7j)' if full_scan else 'STANDARD (24h)')}")
    print(f"  Horodatage : {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
    print()

    # Vérifie que le dossier BRAIN existe
    if not BRAIN_DIR.exists():
        print(f"❌ ERREUR : BRAIN_REVIZEUS/ introuvable dans {PROJECT_ROOT}")
        print("   Vérifie que brain_agent.py est bien à la racine du projet.")
        sys.exit(1)

    # Vérifie que c'est un repo git
    if not (PROJECT_ROOT / ".git").exists():
        print("❌ ERREUR : Pas un repo Git valide.")
        sys.exit(1)

    print("→ Lecture des commits récents...")
    commits = get_commits(hours)
    print(f"  {len(commits)} commit(s) trouvé(s)")

    print("→ Scan des fichiers modifiés...")
    files = get_changed_files(hours)
    print(f"  {len(files)} fichier(s) .kt/.xml modifié(s)")

    print("→ Analyse des blocs impactés...")
    touched_blocs = detect_blocs(files)
    if touched_blocs:
        for b, fl in touched_blocs.items():
            print(f"  ✓ Bloc {b} : {', '.join(fl[:3])}")
    else:
        print("  Aucun bloc clairement identifié")

    if status_only:
        print()
        print("Mode --status : aucune écriture effectuée.")
        return

    print()
    print("→ Génération de ETAT_TEMPS_REEL.md...")
    etat_content = build_etat_temps_reel(commits, files, hours)

    # Crée le dossier 00_QUICK_START si besoin
    FILE_ETAT_TEMPS_REEL.parent.mkdir(parents=True, exist_ok=True)

    with open(FILE_ETAT_TEMPS_REEL, "w", encoding="utf-8") as f:
        f.write(etat_content)
    print(f"  ✅ Écrit : {FILE_ETAT_TEMPS_REEL}")

    print("→ Mise à jour de INDEX_BLOCS.md...")
    ok = inject_activity_in_index(commits, files, touched_blocs)
    if ok:
        print(f"  ✅ Mis à jour : {FILE_INDEX_BLOCS}")

    print()
    print("→ Stage Git des fichiers BRAIN mis à jour...")
    subprocess.run(
        ["git", "add",
         str(FILE_ETAT_TEMPS_REEL),
         str(FILE_INDEX_BLOCS)],
        cwd=str(PROJECT_ROOT),
        capture_output=True
    )
    print("  ✅ Stagés (sans commit automatique — tu gardes le contrôle)")

    print()
    print("╔══════════════════════════════════════════╗")
    print("║  ✅  BRAIN MIS À JOUR AVEC SUCCÈS         ║")
    print("╚══════════════════════════════════════════╝")
    print()
    print("Fichiers mis à jour :")
    print(f"  • BRAIN_REVIZEUS/00_QUICK_START/ETAT_TEMPS_REEL.md")
    print(f"  • BRAIN_REVIZEUS/02_BLOCS/INDEX_BLOCS.md")
    print()
    print("Prochaine étape : commit si le contenu te convient :")
    print('  git commit -m "chore: mise à jour BRAIN auto"')


if __name__ == "__main__":
    main()
