"""Static catalogue/data checks only; does not execute Minecraft or Java."""
from collections import Counter
import json
from pathlib import Path
import re

PROJECT = Path(__file__).resolve().parents[1]
SOURCE = PROJECT / "translation-source"
DATA = PROJECT / "src/main/resources/data/witherskyru"


def main():
    previous = json.loads((SOURCE / "TRANSLATIONS_0.9.0_HISTORY.json").read_text(encoding="utf-8"))["translations"]
    old_coverage = json.loads((SOURCE / "COVERAGE_0.9.0_HISTORY.json").read_text(encoding="utf-8"))
    coverage = json.loads((SOURCE / "coverage.json").read_text(encoding="utf-8"))
    records = [json.loads(line) for line in (SOURCE / "translation-provenance.jsonl").read_text(encoding="utf-8").splitlines()]
    current = {r["source"]: r["translation"] for r in records}
    new = [r for r in records if r["source"] not in previous or previous[r["source"]] == r["source"]]
    revised = [r for r in records if r["source"] in previous and previous[r["source"]] != r["translation"]]
    hazards, malformed, duplicate_keys = [], [], {}
    for r in new + revised:
        a, b = r["source"], r["translation"]
        for label, regex in [("number", r"\d+(?:[,.]\d+)*"), ("roman level", r"\b[IVXLCDM]+\b"),
                             ("icon", r"[\uE000-\uE1FF◆❤☯Ⓑ✓✗▶]")]:
            if re.findall(regex, a) != re.findall(regex, b):
                hazards.append({"source": a, "translation": b, "reason": label})
    resources = {"continuation.tsv": 2, "continuation-patterns.tsv": 2, "lexicon.tsv": 4}
    for file in SOURCE.glob("BATCH_0.10_*.tsv"):
        resources[str(file)] = 3 if file.name.endswith("OBJECTS.tsv") else 2
    for filename, width in resources.items():
        path = Path(filename) if Path(filename).is_absolute() else DATA / filename
        keys = []
        for index, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip() or line.startswith("#"):
                continue
            row = line.split("\t")
            if len(row) != width or not all(row):
                malformed.append({"file": path.name, "line": index})
            keys.append(row[0])
        duplicate_keys[path.name] = {k:v for k,v in Counter(keys).items() if v > 1}
    report = {"revision": "0.10.0-dev", "scope": "Static source-data integrity; no Java/Fabric/game execution",
              "new_snapshot_sources": len(new), "previous_targets_updated": len(revised),
              "delta_reported": coverage["translated_or_partially_translated_snapshot_strings"] - old_coverage["translated_or_partially_translated_snapshot_strings"],
              "numeric_roman_icon_mismatches": hazards, "malformed_rows": malformed, "duplicate_keys": duplicate_keys,
              "context_guard_examples": {"Alligator Skin": current.get("Alligator Skin"), "Daedalus Stick": current.get("Daedalus Stick"),
                                         "Large Enchanted Agronomy Sack": current.get("Large Enchanted Agronomy Sack")},
              "updated_previous_sources": revised, "quality_verified": False, "runtime_verified": False}
    (SOURCE / "BATCH_0.10_DATA_CHECKS.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    assert len(new) == report["delta_reported"] and len(new) >= 2000
    assert not hazards and not malformed, json.dumps({"hazards": hazards, "malformed": malformed}, ensure_ascii=False)
    assert duplicate_keys["continuation.tsv"] == {"Deep Sea Diver": 2, "Intelligence.": 2}
    assert duplicate_keys["lexicon.tsv"] == {"Rabbit": 2}
    assert all(not value for key,value in duplicate_keys.items() if key not in {"continuation.tsv", "lexicon.tsv"})
    assert current["Alligator Skin"] == "Шкура аллигатора"
    assert current["Daedalus Stick"] == "Палка Дедала"
    assert current["Large Enchanted Agronomy Sack"] == "Большой мешок для зачарованного урожая"
    print(json.dumps({"new_sources": len(new), "previous_targets_updated": len(revised), "hazards": len(hazards),
                      "malformed": len(malformed), "new_duplicates": 0}, ensure_ascii=False))


if __name__ == "__main__":
    main()
