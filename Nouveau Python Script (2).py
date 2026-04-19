#!/usr/bin/env python3
# ReviZeus Packager — génère un JSON du repo à coller dans l'Oracle
# Utilisation : python revizeus_pack.py
import os, json, sys

REPO_PATH = r"E:\ReviZeus"

BRAIN_FOLDERS = ['BRAIN_REVIZEUS', 'Brain_revizeus', 'brain_revizeus', 'Ia_docs', 'IA_DOCS']
CTX_EXTS  = {'.md', '.txt', '.json', '.yaml', '.yml'}
CODE_EXTS = {'.kt', '.xml', '.gradle', '.kts', '.toml', '.properties'}
SKIP_DIRS = {'build', '.gradle', '.idea', '__pycache__', 'node_modules', '.git'}

MAX_CTX_SIZE  = 20_000   # chars par fichier contexte
MAX_CODE_SIZE = 12_000   # chars par fichier code

def is_brain(path):
    parts = path.replace('\\', '/').split('/')
    return any(p in BRAIN_FOLDERS for p in parts)

result = {"ctx": {}, "code": {}, "meta": {"repo": REPO_PATH}}
total_ctx, total_code = 0, 0

for root, dirs, files in os.walk(REPO_PATH):
    dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
    for fname in files:
        fpath = os.path.join(root, fname)
        rel   = os.path.relpath(fpath, REPO_PATH).replace('\\', '/')
        _, ext = os.path.splitext(fname)
        ext = ext.lower()
        try:
            if is_brain(rel) and ext in CTX_EXTS:
                with open(fpath, encoding='utf-8', errors='ignore') as f:
                    content = f.read()[:MAX_CTX_SIZE]
                result["ctx"][rel] = content
                total_ctx += 1
            elif not is_brain(rel) and ext in CODE_EXTS:
                with open(fpath, encoding='utf-8', errors='ignore') as f:
                    content = f.read()[:MAX_CODE_SIZE]
                result["code"][rel] = content
                total_code += 1
        except Exception as e:
            print(f"Skip {rel}: {e}")

result["meta"]["stats"] = {"ctx_files": total_ctx, "code_files": total_code}
out_path = os.path.join(os.path.dirname(__file__), "revizeus_context.json")
with open(out_path, 'w', encoding='utf-8') as f:
    json.dump(result, f, ensure_ascii=False, indent=2)

print(f"\n✓ Packagé : {total_ctx} fichiers contexte + {total_code} fichiers code")
print(f"✓ Fichier généré : {out_path}")
print("\n→ Ouvre revizeus_context.json, copie tout le contenu, colle dans l'Oracle.")
