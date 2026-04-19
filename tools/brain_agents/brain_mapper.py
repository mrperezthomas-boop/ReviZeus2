from __future__ import annotations

from pathlib import Path

from brain_runtime import AgentContext, read_json


def _apply_rule(file_path: str, stem: str, rule: dict) -> dict:
    return {
        "file": file_path,
        "bloc": rule.get("bloc", "UNKNOWN"),
        "zone": rule.get("zone", "UNKNOWN"),
        "systeme": rule.get("systeme", "Non cartographié"),
        "priority": rule.get("priority", "P9"),
        "owner": rule.get("owner", "unknown"),
    }


def _load_rules(ctx: AgentContext) -> dict:
    rules_path = ctx.brain_root / "07_RUNTIME_AGENT" / "revizeus_mapper_rules.json"
    return read_json(rules_path, {})


def map_files_to_domains(ctx: AgentContext, changed_files: list[str]) -> dict:
    rules = _load_rules(ctx)
    explicit_map = rules.get("explicit_stem_map", {})
    path_rules = rules.get("path_contains_rules", [])
    prefix_rules = rules.get("prefix_stem_rules", [])
    suffix_rules = rules.get("suffix_stem_rules", [])
    keyword_rules = rules.get("keyword_stem_rules", [])
    fallback = rules.get("unknown_fallback", {"bloc": "UNKNOWN", "zone": "UNKNOWN", "systeme": "Non cartographié", "priority": "P9", "owner": "unknown"})

    mapped = []
    detected_blocs = set()
    detected_systemes = set()
    detected_owners = set()

    for file_path in changed_files:
        stem = Path(file_path).stem
        stem_lower = stem.lower()
        file_path_lower = file_path.lower()
        row = None

        if stem in explicit_map:
            row = _apply_rule(file_path, stem, explicit_map[stem])
            row["match_type"] = "explicit_stem"

        if row is None:
            for rule in path_rules:
                if rule.get("contains", "").lower() in file_path_lower:
                    row = _apply_rule(file_path, stem, rule)
                    row["match_type"] = "path_contains"
                    break

        if row is None:
            for rule in prefix_rules:
                if stem_lower.startswith(rule.get("prefix", "").lower()):
                    row = _apply_rule(file_path, stem, rule)
                    row["match_type"] = "prefix_stem"
                    break

        if row is None:
            for rule in suffix_rules:
                if stem.endswith(rule.get("suffix", "")):
                    row = _apply_rule(file_path, stem, rule)
                    row["match_type"] = "suffix_stem"
                    break

        if row is None:
            for rule in keyword_rules:
                if rule.get("keyword", "").lower() in stem_lower:
                    row = _apply_rule(file_path, stem, rule)
                    row["match_type"] = "keyword_stem"
                    break

        if row is None:
            row = _apply_rule(file_path, stem, fallback)
            row["match_type"] = "fallback"

        mapped.append(row)
        detected_blocs.add(row["bloc"])
        detected_systemes.add(row["systeme"])
        detected_owners.add(row["owner"])

    return {
        "mapped_files": mapped,
        "detected_blocs": sorted(detected_blocs),
        "detected_systemes": sorted(detected_systemes),
        "detected_owners": sorted(detected_owners),
    }
