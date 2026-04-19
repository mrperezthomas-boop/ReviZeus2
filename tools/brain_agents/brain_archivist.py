from __future__ import annotations

from brain_runtime import AgentContext


def build_journal_entry(ctx: AgentContext, activity: dict, mapping: dict, analysis: dict, rules: dict) -> str:
    last_commit = activity.get("last_commit")
    commit_subject = last_commit["subject"] if last_commit else "Aucun commit récent"
    blocs = ", ".join(mapping.get("detected_blocs", [])) if mapping.get("detected_blocs") else "Aucun bloc détecté"
    warnings = len(rules.get("warnings", []))
    return (
        f"- [{ctx.timestamp}] Source={ctx.source} | Branche={activity.get('branch', 'unknown')} | "
        f"Commit={commit_subject} | Blocs={blocs} | Warnings={warnings} | Résumé={analysis.get('summary', '')}\n"
    )
