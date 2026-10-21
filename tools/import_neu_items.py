#!/usr/bin/env python3
"""Local-only NEU catalogue extractor: does not invent translations.

Usage (Windows):
 py tools/import_neu_items.py "C:/.../minecraft/config/notenoughupdates/repo"

Output: tools/generated_neu_to_review.tsv (source + blank target, for manual curation).
Never load the unreviewed output into the runtime dictionary.
"""
import json
from pathlib import Path
import re
import sys

IGNORED = re.compile(r"(?:§[0-9a-fk-or]|\u00a7[0-9a-fk-or])", re.I)

def main(root: Path):
    if not root.is_dir():
        raise SystemExit(f"NEU repository folder not found: {root}")
    count = 0
    lines = set()
    for file in root.rglob("*.json"):
        if any(part == '.git' for part in file.parts):
            continue
        try:
            data = json.loads(file.read_text(encoding='utf-8-sig'))
        except (ValueError, UnicodeError, OSError):
            continue
        records = data if isinstance(data, list) else [data]
        for record in records:
            if not isinstance(record, dict):
                continue
            count += 1
            for key in ("displayname", "displayName", "name"):
                value = record.get(key)
                if isinstance(value, str) and 2 <= len(value) <= 160:
                    lines.add(IGNORED.sub('', value).strip())
            for key in ("lore", "loreList"):
                lore = record.get(key)
                if isinstance(lore, list):
                    for value in lore:
                        if isinstance(value, str):
                            value = IGNORED.sub('', value).strip()
                            if 4 <= len(value) <= 250 and re.search('[A-Za-z]', value):
                                lines.add(value)
    out = Path(__file__).resolve().parent / "generated_neu_to_review.tsv"
    with out.open('w',encoding='utf-8',newline='\n') as f:
        f.write('# Untranslated source strings extracted from NEU. REVIEW BEFORE USE.\n')
        for s in sorted(lines, key=str.casefold):
            if '\t' not in s and '\n' not in s:
                f.write(s + '\t' + '[НУЖЕН ПЕРЕВОД]' + '\n')
    print(f'Catalog records examined: {count}. Unique text strings: {len(lines)}.')
    print(f'For review: {out}')

if __name__ == '__main__':
    if len(sys.argv)!=2:raise SystemExit('Usage: py tools/import_neu_items.py <NEU-repo-path>')
    main(Path(sys.argv[1]))
