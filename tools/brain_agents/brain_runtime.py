from __future__ import annotations

import json
import subprocess
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any


@dataclass
class AgentContext:
    project_root: Path
    brain_root: Path
    config: dict[str, Any]
    now: datetime
    source: str

    @property
    def timestamp(self) -> str:
        return self.now.strftime("%Y-%m-%d %H:%M")


def load_context(project_root: Path, source: str) -> AgentContext:
    brain_root = project_root / "BRAIN_REVIZEUS"
    config_path = brain_root / "07_RUNTIME_AGENT" / "agent_config.json"
    with config_path.open("r", encoding="utf-8") as f:
        config = json.load(f)
    return AgentContext(project_root=project_root, brain_root=brain_root, config=config, now=datetime.now(), source=source)


def read_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: Path, data: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def append_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as f:
        f.write(text)


def git(args: list[str], cwd: Path) -> str:
    result = subprocess.run(["git", *args], cwd=str(cwd), capture_output=True, text=True, encoding="utf-8")
    if result.returncode != 0:
        return ""
    return result.stdout.strip()


def safe_slug(value: str) -> str:
    out = []
    for ch in value.lower():
        if ch.isalnum():
            out.append(ch)
        else:
            out.append("-")
    return "".join(out).strip("-") or "run"
