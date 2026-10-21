#!/usr/bin/env python3
"""Optional static validation. Run only when testing is requested.

Does not launch Minecraft or connect to a server. Does not overwrite the
versioned QA_REPORT; writes only static-validation.json.
"""
import json
from pathlib import Path
import re

def main():
    project = Path(__file__).resolve().parents[1]
    root = project / "src/main/resources/data/witherskyru"
    metadata = json.loads((project / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    phrases, conflicts, overrides = {}, [], []
    files = ("phrases.tsv", "vanilla-names.tsv", "lexicon-names.tsv", "curated.tsv", "lore.tsv", "continuation.tsv", "enchantments.tsv", "names.tsv", "pet-lore.tsv", "catalogue.tsv", "corrections.tsv", "quality-fixes.tsv", "proofread.tsv")
    for filename in files:
        local = {}
        for number, line in enumerate((root / filename).read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip() or line.startswith("#"): continue
            fields = line.split("\t")
            assert len(fields) == 2 and all(fields), f"Invalid {filename}:{number}"
            source, target = (f.strip() for f in fields)
            if source in local and local[source] != target:
                conflicts.append({"source": source, "file": filename, "line": number})
            local[source] = target
            if source in phrases and phrases[source] != target: overrides.append(source)
            phrases[source] = target
    assert not conflicts, f"Conflicting entries within one resource: {conflicts}"
    patterns = 0
    for filename in ("proofread-patterns.tsv", "editorial-patterns.tsv", "pet-lore-patterns.tsv", "continuation-patterns.tsv", "expanded-patterns.tsv", "canonical-patterns.tsv"):
        for number, line in enumerate((root / filename).read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip() or line.startswith("#"): continue
            fields = line.split("\t")
            assert len(fields) == 2, f"Invalid {filename}:{number}"
            assert fields[0].startswith("^") and fields[0].endswith("$")
            pattern = re.compile(re.sub(r"\\Q(.*?)\\E", lambda m: re.escape(m[1]), fields[0]), re.I)
            for match in re.finditer(r"\$(?:(\d+)|\{(?:gen|acc):(\d+)\})", fields[1]):
                assert 1 <= int(match[1] or match[2]) <= pattern.groups
            patterns += 1
    auxiliary = {}
    for filename, columns in (("reforges.tsv", 5), ("reforge-placement.tsv", 2), ("item-genders.tsv", 2), ("item-gender-overrides.tsv", 2)):
        local = {}
        for number, line in enumerate((root / filename).read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip() or line.startswith("#"): continue
            fields = line.split("\t")
            assert len(fields) == columns and all(fields), f"Invalid {filename}:{number}"
            key = fields[0].casefold()
            assert key not in local, f"Duplicate metadata {filename}:{number}"
            if "gender" in filename: assert fields[1] in ("m", "f", "n", "p"), f"Bad gender {filename}:{number}"
            if filename == "reforge-placement.tsv": assert fields[1] == "suffix"
            local[key] = fields[1:]
        auxiliary[filename] = local
    assert auxiliary["reforge-placement.tsv"].keys() <= auxiliary["reforges.tsv"].keys()
    result = {"build": metadata["version"], "static_validation": "PASSED", "dictionary_entries": len(phrases), "templates": patterns, "reforge_prefixes": len(auxiliary["reforges.tsv"]), "authored_item_genders": len(auxiliary["item-gender-overrides.tsv"]), "intentional_cross_resource_overrides": len(overrides), "runtime_tested": False}
    output = project / "translation-source/static-validation.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))

if __name__ == "__main__": main()
