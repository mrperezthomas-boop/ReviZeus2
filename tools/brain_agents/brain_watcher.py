from __future__ import annotations

from pathlib import Path

from brain_runtime import AgentContext, git


def collect_recent_activity(ctx: AgentContext, full: bool = False) -> dict:
    since = "7 days ago" if full else "24 hours ago"
    log_output = git(["log", f"--since={since}", "--pretty=format:%H|%ad|%s", "--date=iso"], ctx.project_root)
    commits = []
    if log_output:
        for line in log_output.splitlines():
            parts = line.split("|", 2)
            if len(parts) == 3:
                commits.append({"hash": parts[0], "date": parts[1], "subject": parts[2]})

    files_output = git(["log", "-1", "--name-only", "--pretty=format:"], ctx.project_root)
    watched_exts = tuple(ctx.config.get("watched_extensions", []))
    changed_files = [line.strip() for line in files_output.splitlines() if line.strip().endswith(watched_exts)] if files_output else []

    branch = git(["branch", "--show-current"], ctx.project_root) or "unknown"
    last_commit = commits[0] if commits else None

    return {
        "branch": branch,
        "commits": commits,
        "changed_files": changed_files,
        "last_commit": last_commit,
    }
