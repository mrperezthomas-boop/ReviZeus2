from __future__ import annotations

import argparse
import json
from pathlib import Path

from brain_archivist import build_journal_entry
from brain_diff_analyzer import analyze_changes
from brain_mapper import map_files_to_domains
from brain_rules_guard import check_rules
from brain_runtime import load_context, safe_slug, write_json, write_text
from brain_scribe import append_journal, update_block_index, update_etat_temps_reel, update_last_run_summary, update_rules_report
from brain_watcher import collect_recent_activity


def main() -> int:
    parser = argparse.ArgumentParser(description="RéviZeus Brain Agents v1")
    parser.add_argument("--status", action="store_true", help="Lecture seule, aucune écriture.")
    parser.add_argument("--full", action="store_true", help="Analyse les 7 derniers jours au lieu de 24h.")
    parser.add_argument("--source", default="manual", help="Origine du run : manual, post-commit, scheduler...")
    args = parser.parse_args()

    project_root = Path(__file__).resolve().parents[2]
    ctx = load_context(project_root, source=args.source)

    print("╔══════════════════════════════════════════╗")
    print("║       REVIZEUS BRAIN AGENTS — v1         ║")
    print("╚══════════════════════════════════════════╝")
    print(f"  Projet     : {ctx.project_root}")
    print(f"  Brain      : {ctx.brain_root}")
    print(f"  Mode       : {'STATUS ONLY' if args.status else 'WRITE'}")
    print(f"  Horodatage : {ctx.now.strftime('%d/%m/%Y %H:%M:%S')}")
    print()

    activity = collect_recent_activity(ctx, full=args.full)
    mapping = map_files_to_domains(ctx, activity.get("changed_files", []))
    analysis = analyze_changes(ctx, activity.get("changed_files", []), mapping)
    rules = check_rules(ctx, activity.get("changed_files", []))

    report = {
        "timestamp": ctx.timestamp,
        "source": ctx.source,
        "activity": activity,
        "mapping": mapping,
        "analysis": analysis,
        "rules": rules,
    }

    print("→ Lecture des commits récents...")
    print(f"  {len(activity.get('commits', []))} commit(s) trouvé(s)")
    print("→ Scan des fichiers modifiés...")
    print(f"  {len(activity.get('changed_files', []))} fichier(s) surveillé(s) modifié(s)")
    print("→ Analyse des blocs impactés...")
    if mapping.get("detected_blocs"):
        print(f"  {', '.join(mapping['detected_blocs'])}")
    else:
        print("  Aucun bloc clairement identifié")

    if args.status:
        print("\nMode --status : aucune écriture effectuée.")
        return 0

    last_analysis_path = ctx.project_root / ctx.config["last_analysis_file"]
    write_json(last_analysis_path, report)

    snapshot_name = f"snapshot-{ctx.now.strftime('%Y%m%d-%H%M%S')}-{safe_slug(ctx.source)}.json"
    snapshot_path = ctx.project_root / ctx.config["snapshots_dir"] / snapshot_name
    write_json(snapshot_path, report)

    update_etat_temps_reel(ctx, activity, mapping, analysis, rules)
    update_block_index(ctx, activity, mapping, analysis)
    update_rules_report(ctx, rules)
    update_last_run_summary(ctx, activity, mapping, analysis, rules)
    append_journal(ctx, build_journal_entry(ctx, activity, mapping, analysis, rules))

    print("\nÉcriture terminée.")
    print(f"- {ctx.config['etat_temps_reel_file']}")
    print(f"- {ctx.config['block_index_file']}")
    print(f"- {ctx.config['rules_report_file']}")
    print(f"- {ctx.config['journal_file']}")
    print(f"- {ctx.config['last_analysis_file']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
