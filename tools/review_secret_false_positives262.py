"""Review only concrete false positives; never suppress an entire secret rule/file.

The historical data uses `source_key` for public NPC record IDs, not credentials.
Ignores are exact commit:file:rule:line fingerprints, so future edits are scanned.
"""
import argparse
import collections
import hashlib
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("report", type=Path)
    parser.add_argument("review", type=Path)
    args = parser.parse_args()
    findings = json.loads(args.report.read_text(encoding="utf-8-sig"))
    cache = {}
    reviewed = []
    for finding in findings:
        assert finding["RuleID"] == "generic-api-key", "Different rule needs independent review"
        path, commit = finding["File"], finding["Commit"]
        key = (commit, path)
        if key not in cache:
            cache[key] = subprocess.check_output(["git", "show", f"{commit}:{path}"], cwd=ROOT).decode("utf-8-sig").splitlines()
        line = cache[key][finding["StartLine"]-1].strip()
        if finding["Match"].startswith('source_key": '):
            assert path.endswith(".json") and path.startswith(("src/main/resources/data/witherskyru/", "translation-source/", "translation-source-regression115/"))
            match = re.search(r'"source_key"\s*:\s*("(?:[^"\\]|\\.)*")', line)
            assert match, (path, finding["StartLine"])
            value = json.loads(match[1])
            assert re.fullmatch(r"(?:npc|continuous-pass43|dungeon98|garden-observed|night-pass(?:9|10|11)|safety)\.[a-z0-9._-]{1,180}", value), (path, finding["StartLine"], "Unknown public ID shape")
            reason = "Public human-readable NPC source record ID; not an API credential"
        else:
            assert Path(path).name == "TestPlainBoosterContexts.java"
            assert line.startswith('System.out.println("PASS: plain booster contexts "+checks+')
            assert "three complete paragraphs/five repeats" in line and "two canonical names" in line
            reason = "Human-readable diagnostic sentence in an existing test; not a credential"
        reviewed.append({"fingerprint":finding["Fingerprint"],"file":path,"line":finding["StartLine"],"reason":reason,"line_sha256":hashlib.sha256(line.encode()).hexdigest()})
    destination = ROOT/".gitleaksignore"
    existing = set(destination.read_text().splitlines()) if destination.exists() else set()
    existing.update(row["fingerprint"] for row in reviewed)
    fingerprints = sorted(line for line in existing if line and not line.startswith("#"))
    destination.write_text("# Exact independently reviewed public NPC IDs/test diagnostics only.\n# No whole-rule, whole-file, credential-shaped or future-commit exclusions.\n"+"\n".join(fingerprints)+"\n",encoding="utf-8")
    args.review.parent.mkdir(parents=True,exist_ok=True)
    args.review.write_text(json.dumps({"reviewed":len(reviewed),"reasons":dict(collections.Counter(r["reason"] for r in reviewed)),"records":reviewed},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print("Verified exact non-credential findings:",len(reviewed))


if __name__ == "__main__":
    main()
