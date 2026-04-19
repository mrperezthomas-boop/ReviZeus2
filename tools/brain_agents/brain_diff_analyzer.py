from __future__ import annotations
from collections import Counter

def _pick(values, fallback="UNKNOWN"):
    vals = [v for v in values if v and v != "UNKNOWN" and v != "unknown"]
    if vals:
        return Counter(vals).most_common(1)[0][0]
    return fallback

def _session_type(groups: list[str]) -> str:
    g = set(groups)
    if not g or g == {"unknown"}:
        return "unknown"
    if g <= {"runtime"}:
        return "runtime_only"
    if g <= {"archive"}:
        return "archive_heavy"
    if g <= {"canonical"}:
        return "canonical_documentation"
    if g <= {"active_planning"}:
        return "active_planning"
    return "mixed"

def analyze(mapping: dict, diff_stat: str = "", changed_count: int = 0) -> dict:
    mapped = mapping.get("mapped_files", [])
    blocs = [m.get("bloc", "UNKNOWN") for m in mapped]
    owners = [m.get("owner", "unknown") for m in mapped]
    groups = [m.get("group", "unknown") for m in mapped]
    unknown_files = [m["file"] for m in mapped if m.get("bloc") == "UNKNOWN"]

    dominant_bloc = _pick(blocs)
    dominant_owner = _pick(owners, "unknown")
    session_type = _session_type(groups)

    confidence = "LOW"
    if dominant_bloc != "UNKNOWN" and not unknown_files:
        confidence = "HIGH"
    elif dominant_bloc != "UNKNOWN":
        confidence = "MEDIUM"

    labels = {
        "canonical_documentation": "documentation canonique",
        "active_planning": "pilotage actif",
        "runtime_only": "runtime agent/documentaire",
        "archive_heavy": "archive",
        "mixed": "session mixte",
        "unknown": "session non classée",
    }
    label = labels.get(session_type, session_type)
    summary = f"{changed_count} fichier(s) classé(s) en {label}, bloc dominant = {dominant_bloc}, owner dominant = {dominant_owner}, confiance = {confidence}."

    key_behaviors = []
    if unknown_files:
        key_behaviors.append(f"{len(unknown_files)} fichier(s) restent non cartographié(s) et demandent une règle explicite si critiques.")
    if session_type == "canonical_documentation":
        key_behaviors.append("Session dominée par des référentiels canoniques du brain.")
    elif session_type == "active_planning":
        key_behaviors.append("Session dominée par des documents de pilotage actif.")
    elif session_type == "runtime_only":
        key_behaviors.append("Session dominée par des sorties runtime ou journaux agents.")

    return {
        "diff_stat": diff_stat,
        "dominant_bloc": dominant_bloc,
        "dominant_owner": dominant_owner,
        "session_type": session_type,
        "unknown_files": unknown_files,
        "key_behaviors": key_behaviors,
        "summary": summary,
        "confidence": confidence,
    }
