from __future__ import annotations

from pathlib import Path

from brain_runtime import AgentContext, append_text, write_text


def update_etat_temps_reel(ctx: AgentContext, activity: dict, mapping: dict, analysis: dict, rules: dict) -> None:
    target = ctx.project_root / ctx.config["etat_temps_reel_file"]
    commit = activity.get("last_commit")
    mapped_lines = []
    for row in mapping.get("mapped_files", []):
        mapped_lines.append(f"- {row['file']} -> {row['bloc']} / {row['zone']} / {row['systeme']}")
    if not mapped_lines:
        mapped_lines.append("- Aucun fichier surveillé détecté sur le dernier commit.")

    warning_lines = [f"- {w}" for w in rules.get("warnings", [])] or ["- Aucun avertissement simple détecté."]
    text = f"""# État temps réel RéviZeus

## Dernier run
- Horodatage : {ctx.timestamp}
- Source : {ctx.source}
- Branche : {activity.get('branch', 'unknown')}

## Dernier commit observé
- Sujet : {commit['subject'] if commit else 'Aucun commit récent'}
- Hash : {commit['hash'] if commit else 'N/A'}
- Date : {commit['date'] if commit else 'N/A'}

## Fichiers et cartographie
{chr(10).join(mapped_lines)}

## Résumé métier court
- {analysis.get('summary', 'Aucun résumé.')}
{chr(10).join('- ' + line for line in analysis.get('key_behaviors', [])) if analysis.get('key_behaviors') else '- Aucun impact métier déterminé automatiquement.'}

## Contrôle de règles
- Statut : {rules.get('status', 'UNKNOWN')}
{chr(10).join(warning_lines)}
"""
    write_text(target, text)


def update_block_index(ctx: AgentContext, activity: dict, mapping: dict, analysis: dict) -> None:
    target = ctx.project_root / ctx.config["block_index_file"]
    if not target.exists():
        return
    original = target.read_text(encoding="utf-8")
    marker = "## Activité récente auto"
    if marker not in original:
        return
    head, _, tail = original.partition(marker)
    lines = [
        marker,
        "",
        "> Cette section est maintenue automatiquement par les agents.",
        "> Les faits observés doivent rester courts et datés.",
        "",
    ]
    commit = activity.get("last_commit")
    if mapping.get("mapped_files"):
        for row in mapping["mapped_files"][:8]:
            lines.append(f"- [{ctx.timestamp}] {row['file']} -> {row['bloc']} / {row['zone']} / {row['systeme']}")
    else:
        lines.append(f"- [{ctx.timestamp}] Aucun fichier surveillé détecté sur le dernier commit observé.")
    new_content = head.rstrip() + "\n\n" + "\n".join(lines) + "\n"
    write_text(target, new_content)


def update_rules_report(ctx: AgentContext, rules: dict) -> None:
    target = ctx.project_root / ctx.config["rules_report_file"]
    lines = [
        "# Dernier rapport de conformité",
        "",
        f"- Horodatage : {ctx.timestamp}",
        f"- Statut : {rules.get('status', 'UNKNOWN')}",
        "",
        "## Avertissements",
        "",
    ]
    warnings = rules.get("warnings", [])
    if warnings:
        lines.extend(f"- {w}" for w in warnings)
    else:
        lines.append("- Aucun avertissement simple détecté.")
    write_text(target, "\n".join(lines) + "\n")


def update_last_run_summary(ctx: AgentContext, activity: dict, mapping: dict, analysis: dict, rules: dict) -> None:
    target = ctx.project_root / ctx.config["last_run_summary_file"]
    lines = [
        "# Last run summary",
        "",
        f"- Horodatage : {ctx.timestamp}",
        f"- Source : {ctx.source}",
        f"- Branche : {activity.get('branch', 'unknown')}",
        f"- Fichiers surveillés : {len(activity.get('changed_files', []))}",
        f"- Blocs détectés : {', '.join(mapping.get('detected_blocs', [])) if mapping.get('detected_blocs') else 'Aucun'}",
        f"- Statut règles : {rules.get('status', 'UNKNOWN')}",
        "",
        "## Résumé",
        "",
        f"- {analysis.get('summary', 'Aucun résumé.')}",
    ]
    write_text(target, "\n".join(lines) + "\n")


def append_journal(ctx: AgentContext, entry: str) -> None:
    target = ctx.project_root / ctx.config["journal_file"]
    append_text(target, entry)
