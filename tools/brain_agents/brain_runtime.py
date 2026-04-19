from __future__ import annotations
import json
from pathlib import Path

def resolve_brain_paths(project_root: Path) -> dict:
    brain = project_root / "BRAIN_REVIZEUS"
    runtime = brain / "09_RUNTIME_AGENT"
    snapshots = brain / "08_SNAPSHOTS"
    qa = brain / "10_RAPPORTS_QA"
    quick = brain / "00_QUICK_START"
    return {
        "project_root": project_root,
        "brain_dir": brain,
        "runtime_dir": runtime,
        "snapshots_dir": snapshots,
        "qa_dir": qa,
        "quick_dir": quick,
        "agent_config": runtime / "agent_config.json",
        "last_analysis": runtime / "LAST_ANALYSIS.json",
        "last_summary": runtime / "LAST_RUN_SUMMARY.md",
        "governance": runtime / "revizeus_mapper_rules.json",
        "qa_report": qa / "LAST_RULES_REPORT.md",
        "etat_temps_reel": quick / "ETAT_TEMPS_REEL.md",
    }

def ensure_runtime_files(project_root: Path) -> dict:
    ctx = resolve_brain_paths(project_root)
    ctx["runtime_dir"].mkdir(parents=True, exist_ok=True)
    ctx["snapshots_dir"].mkdir(parents=True, exist_ok=True)
    ctx["qa_dir"].mkdir(parents=True, exist_ok=True)
    ctx["quick_dir"].mkdir(parents=True, exist_ok=True)

    if not ctx["agent_config"].exists():
        config = {
            "project_name": "ReviZeus",
            "brain_root": "BRAIN_REVIZEUS",
            "runtime_dir": "09_RUNTIME_AGENT",
            "snapshots_dir": "08_SNAPSHOTS",
            "qa_dir": "10_RAPPORTS_QA",
            "quick_dir": "00_QUICK_START",
            "version": "true-brain-v1"
        }
        ctx["agent_config"].write_text(json.dumps(config, ensure_ascii=False, indent=2), encoding="utf-8")
    return ctx

def load_context(project_root: Path, source: str = "manual") -> dict:
    ctx = ensure_runtime_files(project_root)
    with ctx["agent_config"].open("r", encoding="utf-8") as f:
        config = json.load(f)
    return {"paths": ctx, "config": config, "source": source}
