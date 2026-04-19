from __future__ import annotations
import json
from pathlib import Path

DEFAULT_UNKNOWN = {
    "bloc": "UNKNOWN",
    "zone": "UNKNOWN",
    "systeme": "Non cartographié",
    "owner": "unknown",
    "priority": "P9",
    "truth": "UNKNOWN",
    "stability": "UNKNOWN",
    "group": "unknown",
    "match_type": "fallback",
}

FOLDER_RULES = {
    "00_QUICK_START": ("BRAIN_RUNTIME", "QUICK_START", "Entrée rapide et état consolidé", "production_brain_system", "P2", "ACTUEL", "MEDIUM", "runtime"),
    "01_REGLES_SACREES": ("BRAIN_CORE", "REGLES_SACREES", "Doctrine et contraintes absolues", "governance_core", "P0", "SACRE", "VERY_HIGH", "canonical"),
    "02_BLOCS": ("BRAIN_BLOCKS", "PLANIFICATION_BLOCS", "Pilotage d’implémentation", "delivery_system", "P2", "ACTUEL", "MEDIUM", "active_planning"),
    "03_SYSTEMES": ("BRAIN_CORE", "SYSTEMES", "Cartographie et gouvernance système", "system_architecture_core", "P0", "ACTUEL", "HIGH", "canonical"),
    "04_JOURNAUX": ("BRAIN_RUNTIME", "JOURNAUX", "Journal vivant", "production_brain_system", "P3", "RUNTIME", "LOW", "runtime"),
    "05_DIRECTION_ARTISTIQUE": ("DA_CORE", "DIRECTION_ARTISTIQUE", "Direction artistique et ressenti", "art_direction_system", "P2", "SACRE", "HIGH", "canonical"),
    "06_CURSOR_SKILLS": ("AI_TOOLING", "CURSOR_SKILLS", "Outillage Cursor", "ai_ops", "P2", "ACTUEL", "MEDIUM", "active_planning"),
    "07_ARCHIVES": ("BRAIN_ARCHIVE", "ARCHIVES", "Historique non canonique", "archive_system", "P4", "ARCHIVE", "VARIABLE", "archive"),
    "08_SNAPSHOTS": ("BRAIN_RUNTIME", "SNAPSHOTS", "États figés automatiques", "runtime_snapshot_system", "P3", "RUNTIME", "LOW", "runtime"),
    "09_RUNTIME_AGENT": ("AGENT_OPS", "RUNTIME_AGENT", "Sorties machine et analyses agent", "agent_runtime_system", "P3", "RUNTIME", "LOW", "runtime"),
    "10_RAPPORTS_QA": ("QA", "RAPPORTS_QA", "Contrôle qualité et vérifications", "qa_system", "P3", "RUNTIME", "LOW", "runtime"),
    "11_LORE": ("LORE", "LORE", "Univers narratif officiel", "narrative_system", "P2", "ACTUEL", "HIGH", "canonical"),
    "12_AVENTURE": ("H_AVENTURE", "AVENTURE", "Conception aventure", "adventure_design_system", "P2", "ACTUEL", "MEDIUM", "active_planning"),
    "13_IA_DOCS": ("BRAIN_CORE", "IA_DOCS", "Référentiel technique et métier", "knowledge_core_system", "P0", "ACTUEL", "HIGH", "canonical"),
    "14_TAXONOMIE": ("TAXONOMY", "TAXONOMIE", "Classification officielle", "taxonomy_core_system", "P0", "SACRE", "VERY_HIGH", "canonical"),
}

SPECIAL_FILES = {
    "build.gradle.kts": ("CORE_TECH", "BUILD_SYSTEM", "Build et dépendances", "tech_core", "P1", "ACTUEL", "HIGH", "technical"),
}

def _pack(file_path: str, tpl, match_type: str):
    return {
        "file": file_path,
        "bloc": tpl[0],
        "zone": tpl[1],
        "systeme": tpl[2],
        "owner": tpl[3],
        "priority": tpl[4],
        "truth": tpl[5],
        "stability": tpl[6],
        "group": tpl[7],
        "match_type": match_type,
    }

def map_file(project_root: Path, file_path: str) -> dict:
    normalized = file_path.replace("\\", "/")
    if normalized in SPECIAL_FILES:
        return _pack(file_path, SPECIAL_FILES[normalized], "special_file")

    parts = normalized.split("/")
    if len(parts) >= 2 and parts[0] == "BRAIN_REVIZEUS":
        folder = parts[1]
        if folder in FOLDER_RULES:
            return _pack(file_path, FOLDER_RULES[folder], "brain_folder")

    return {"file": file_path, **DEFAULT_UNKNOWN}

def map_files(project_root: Path, files: list[str]) -> dict:
    mapped = [map_file(project_root, f) for f in files]
    return {
        "mapped_files": mapped,
        "detected_blocs": sorted({m["bloc"] for m in mapped}) or ["UNKNOWN"],
        "detected_systemes": sorted({m["systeme"] for m in mapped}) or ["Non cartographié"],
        "detected_owners": sorted({m["owner"] for m in mapped}) or ["unknown"],
        "detected_groups": sorted({m["group"] for m in mapped}) or ["unknown"],
    }
