"""Read local SkyBlock catalogues and build a provenance-backed translation corpus."""
from __future__ import annotations
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import re
import zipfile
import sys

COLOR = re.compile(r"§[0-9a-fk-or]", re.I)
TEXT_KEYS = {"displayname", "displayName", "name", "title", "text", "description", "desc", "lore", "loreList", "tooltip", "lines", "dialogue", "dialog", "message", "messages", "crafttext", "entity", "label", "help", "instructions", "reforgeName", "reforgeAbility"}
SKIP_KEYS = {"nbttag", "texture", "textures", "profile", "minecraft:profile", "variables", "recipe", "recipes", "clickcommand", "internalname", "id", "entitytype", "itemid", "url", "info", "petInfo", "minecraft:custom_data"}

def main():
    args = argparse.ArgumentParser()
    args.add_argument("--game-dir", type=Path, required=True)
    args.add_argument("--output", type=Path, required=True)
    opts = args.parse_args()
    opts.output.mkdir(parents=True, exist_ok=True)
    records = {}
    items = {}
    counts = Counter()
    errors = []

    def add(raw, origin, role):
        if not isinstance(raw, str): return
        text = COLOR.sub("", raw).strip()
        if not 2 <= len(text) <= 1000 or not re.search(r"[A-Za-z]", text): return
        if text.startswith(("http://", "https://", "^", "/")) or re.fullmatch(r"[a-z0-9_-]+[:/][a-z0-9_:./-]+", text): return
        if "\t" in text: return
        for line in text.splitlines():
            if not line.strip(): continue
            entry = records.setdefault(line, {"source": line, "occurrences": 0, "origins": [], "roles": []})
            entry["occurrences"] += 1
            if origin not in entry["origins"] and len(entry["origins"]) < 8: entry["origins"].append(origin)
            if role not in entry["roles"]: entry["roles"].append(role)

    def visit(data, origin, parent="", role=""):
        if isinstance(data, dict):
            for key, value in data.items():
                if key in SKIP_KEYS: continue
                new_role = key if key in TEXT_KEYS else role
                if key in TEXT_KEYS and isinstance(value, str): add(value, origin, new_role)
                elif isinstance(value, (dict, list)): visit(value, origin, key, new_role)
        elif isinstance(data, list):
            for value in data:
                if isinstance(value, str) and (parent in TEXT_KEYS or role in TEXT_KEYS): add(value, origin, role or parent)
                elif isinstance(value, (dict, list)): visit(value, origin, parent, role)

    roots = ["config/notenoughupdates/repo", "config/skyblocker/item-repo", "config/skyhanni/repo", "catharsis-repo-cache", "skyblock-repo-cache"]
    for relative in roots:
        root = opts.game_dir / relative
        for file in sorted(root.rglob("*.json")):
            if ".git" in file.parts: continue
            origin = file.relative_to(opts.game_dir).as_posix()
            try: data = json.loads(file.read_text(encoding="utf-8-sig"))
            except (ValueError, UnicodeError, OSError) as e:
                errors.append({"file": origin, "error": str(e)})
                continue
            counts[relative] += 1
            visit(data, origin)
            if isinstance(data, dict) and isinstance(data.get("displayname"), str):
                item_id = data.get("internalname", file.stem)
                items.setdefault(item_id, {"id": item_id, "name": COLOR.sub("", data["displayname"]).strip(), "lore": [COLOR.sub("", x) for x in data.get("lore", []) if isinstance(x, str)], "origin": origin})

    output = sorted(records.values(), key=lambda x: (-x["occurrences"], x["source"].casefold()))
    with (opts.output / "corpus.jsonl").open("w", encoding="utf-8", newline="\n") as stream:
        for record in output: stream.write(json.dumps(record, ensure_ascii=False) + "\n")
    (opts.output / "items.json").write_text(json.dumps(sorted(items.values(), key=lambda x: x["id"]), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    manifest = {"roots": dict(counts), "files": sum(counts.values()), "unique_strings": len(records), "item_records": len(items), "parse_errors": errors, "live_server_capture": False}
    (opts.output / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, ensure_ascii=False))
    print("Most frequent real strings:")
    for record in output[:100]: print(f"{record['occurrences']:5} {record['source']}")

if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
