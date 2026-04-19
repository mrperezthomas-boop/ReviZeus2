from __future__ import annotations

from pathlib import Path

from brain_runtime import AgentContext


def check_rules(ctx: AgentContext, changed_files: list[str]) -> dict:
    forbidden_patterns = ctx.config.get("forbidden_patterns", [])
    warnings = []
    checked_files = []

    for rel_path in changed_files:
        abs_path = ctx.project_root / rel_path
        if not abs_path.exists() or abs_path.suffix.lower() not in {".kt", ".xml", ".gradle", ".kts"}:
            continue
        checked_files.append(rel_path)
        try:
            content = abs_path.read_text(encoding="utf-8", errors="ignore")
        except Exception:
            continue

        for pattern in forbidden_patterns:
            if pattern in content:
                warnings.append(f"{rel_path} contient le pattern interdit : {pattern}")

        if abs_path.suffix.lower() == ".kt":
            if "[20" not in content:
                warnings.append(f"{rel_path} : aucun commentaire horodaté détecté dans le fichier Kotlin modifié.")

    status = "OK" if not warnings else "A_VERIFIER"
    return {
        "status": status,
        "checked_files": checked_files,
        "warnings": warnings,
    }
