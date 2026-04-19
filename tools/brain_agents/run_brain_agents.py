from __future__ import annotations
import argparse, json, subprocess
from datetime import datetime, timedelta
from pathlib import Path

from brain_runtime import load_context
from brain_mapper import map_files
from brain_diff_analyzer import analyze

def git(project_root: Path, args: list[str]) -> str:
    r = subprocess.run(["git"] + args, cwd=str(project_root), capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout.strip()

def recent_commits(project_root: Path, hours: int = 24) -> list[dict]:
    since = (datetime.now() - timedelta(hours=hours)).strftime("%Y-%m-%d %H:%M:%S")
    raw = git(project_root, ["log", f"--since={since}", "--pretty=format:%H|%ai|%s", "--no-merges"])
    out = []
    if raw:
        for line in raw.splitlines():
            parts = line.split("|", 2)
            if len(parts) == 3:
                out.append({"hash": parts[0], "date": parts[1], "subject": parts[2]})
    return out

def changed_files(project_root: Path, hours: int = 24) -> list[str]:
    since = (datetime.now() - timedelta(hours=hours)).strftime("%Y-%m-%d %H:%M:%S")
    raw = git(project_root, ["log", f"--since={since}", "--name-only", "--pretty=format:", "--diff-filter=AM"])
    files = []
    if raw:
        for line in raw.splitlines():
            line = line.strip()
            if line:
                files.append(line)
    return sorted(set(files))

def write_summary(path: Path, payload: dict):
    text = []
    text.append("# LAST RUN SUMMARY")
    text.append("")
    text.append(f"- Timestamp : {payload['timestamp']}")
    text.append(f"- Dominant bloc : {payload['analysis']['dominant_bloc']}")
    text.append(f"- Dominant owner : {payload['analysis']['dominant_owner']}")
    text.append(f"- Session type : {payload['analysis'].get('session_type', 'unknown')}")
    text.append(f"- Confidence : {payload['analysis']['confidence']}")
    text.append("")
    text.append(payload['analysis']['summary'])
    text.append("")
    if payload['analysis']['key_behaviors']:
        text.append("## Key behaviors")
        for item in payload['analysis']['key_behaviors']:
            text.append(f"- {item}")
    path.write_text("\n".join(text) + "\n", encoding="utf-8")

def write_qa(path: Path, mapping: dict):
    unknown = [m['file'] for m in mapping.get('mapped_files', []) if m.get('bloc') == 'UNKNOWN']
    lines = ["# LAST RULES REPORT", ""]
    lines.append("Status: OK" if not unknown else "Status: WARN")
    lines.append("")
    if unknown:
        lines.append("## Unknown files")
        for f in unknown:
            lines.append(f"- {f}")
    else:
        lines.append("Aucun fichier inconnu sur cette fenêtre d'analyse.")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--status", action="store_true")
    parser.add_argument("--source", default="manual")
    parser.add_argument("--hours", type=int, default=24)
    args = parser.parse_args()

    project_root = Path(__file__).resolve().parents[2]
    ctx = load_context(project_root, source=args.source)
    paths = ctx["paths"]

    commits = recent_commits(project_root, args.hours)
    files = changed_files(project_root, args.hours)
    mapping = map_files(project_root, files)
    diff_stat = git(project_root, ["diff", "--stat", "HEAD~1", "HEAD"]) if commits else ""
    analysis = analyze(mapping, diff_stat=diff_stat, changed_count=len(files))

    payload = {
        "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M"),
        "source": args.source,
        "activity": {
            "branch": git(project_root, ["branch", "--show-current"]) or "main",
            "commits": commits[:10],
            "changed_files": files,
            "last_commit": commits[0] if commits else None,
        },
        "mapping": mapping,
        "analysis": analysis,
        "rules": {
            "status": "OK" if not analysis["unknown_files"] else "WARN",
            "checked_files": [f for f in files if f.endswith(".kts") or f.endswith(".gradle")],
            "warnings": [],
        }
    }

    print("╔══════════════════════════════════════════╗")
    print("║       REVIZEUS BRAIN AGENTS — TRUE       ║")
    print("╚══════════════════════════════════════════╝")
    print(f"  Projet     : {project_root}")
    print(f"  Brain      : {paths['brain_dir']}")
    print(f"  Mode       : {'STATUS ONLY' if args.status else 'WRITE'}")
    print(f"  Horodatage : {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
    print()
    print("→ Lecture des commits récents...")
    print(f"  {len(commits)} commit(s) trouvé(s)")
    print("→ Scan des fichiers modifiés...")
    print(f"  {len(files)} fichier(s) surveillé(s) modifié(s)")
    print("→ Analyse des blocs impactés...")
    print(f"  {analysis['dominant_bloc']}")

    if args.status:
        print()
        print("Mode --status : aucune écriture effectuée.")
        return 0

    paths["last_analysis"].write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    write_summary(paths["last_summary"], payload)
    write_qa(paths["qa_report"], mapping)

    print()
    print("Écriture terminée.")
    print(f"- {paths['last_analysis'].relative_to(project_root)}")
    print(f"- {paths['last_summary'].relative_to(project_root)}")
    print(f"- {paths['qa_report'].relative_to(project_root)}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
