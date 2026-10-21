"""Static batch-data consistency checks, not Minecraft/Fabric tests."""
import json
from collections import Counter
from pathlib import Path
import re

PROJECT = Path(__file__).resolve().parents[1]
SOURCE = PROJECT / "translation-source"
DATA = PROJECT / "src/main/resources/data/witherskyru"


def main():
    old = json.loads((SOURCE / "COVERAGE_0.8.0_HISTORY.json").read_text(encoding="utf-8"))
    current = json.loads((SOURCE / "coverage.json").read_text(encoding="utf-8"))
    previous = json.loads((SOURCE / "TRANSLATIONS_0.8.0_HISTORY.json").read_text(encoding="utf-8"))["translations"]
    records = [json.loads(line) for line in (SOURCE / "translation-provenance.jsonl").read_text(encoding="utf-8").splitlines()]
    new = [r for r in records if r["source"] not in previous or previous[r["source"]] == r["source"]]
    changed_previous = [r for r in records if r["source"] in previous and previous[r["source"]] != r["translation"]]
    hazards = []
    for r in new:
        a, b = r["source"], r["translation"]
        for label, regex in [("number", r"\d+(?:[,.]\d+)*"), ("roman level", r"\b[IVXLCDM]+\b"),
                             ("icon", r"[\uE000-\uE1FF◆❤☯Ⓑ✓✗]")]:
            # MM and LVL in these labels are abbreviations, not Roman levels.
            compared_source = a
            if label == "roman level":
                compared_source = re.sub(r"^MM (?=The Catacombs)|\(MM\)|LVL (?=Rewards$)", "", a)
            if re.findall(regex, compared_source) != re.findall(regex, b):
                hazards.append({"source": a, "translation": b, "reason": label})
        if "SHARD (ID " in a:
            if re.search(r"\(ID ([A-Z]\d+)\)", a)[1] != re.search(r"\(ID ([A-Z]\d+)\)", b)[1]:
                hazards.append({"source": a, "reason": "ID changed"})
    duplicates, malformed = {}, []
    for filename, width in [("continuation.tsv", 2), ("continuation-patterns.tsv", 2), ("lexicon.tsv", 4)]:
        keys = []
        for i, line in enumerate((DATA / filename).read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip() or line.startswith("#"):
                continue
            row = line.split("\t")
            if len(row) != width or not all(row):
                malformed.append({"file": filename, "line": i})
            keys.append(row[0])
        duplicates[filename] = {k: n for k, n in Counter(keys).items() if n > 1}
    derivation = json.loads((SOURCE / "BATCH_0.9_DERIVATION.json").read_text(encoding="utf-8"))
    report = {"revision": "0.9.0-dev", "check_scope": "Static TSV data and catalogue integrity only; no Java/Fabric/game execution",
              "new_snapshot_sources": len(new), "delta_reported": current["translated_or_partially_translated_snapshot_strings"] - old["translated_or_partially_translated_snapshot_strings"],
              "numeric_roman_icon_id_mismatches": hazards, "malformed_resource_rows": malformed,
              "resource_duplicate_keys": duplicates,
              "previous_targets_changed": changed_previous,
              "closed_derivation_categories": dict(Counter(r["reason"] for r in derivation["generated_entries"])),
              "npc_name_collision_guard": next(r for r in derivation["generated_entries"] if r["source"] == "Ant (NPC)"),
              "npc_dialogues_new_this_batch": 0, "runtime_verified": False, "quality_verified": False}
    (SOURCE / "BATCH_0.9_DATA_CHECKS.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if hazards or malformed or len(new) != report["delta_reported"]:
        raise ValueError(json.dumps({"hazards": hazards, "malformed": malformed, "new": len(new), "delta": report["delta_reported"]}, ensure_ascii=False))
    # These identical/historical duplicates are also present in the 0.8 ZIP.
    if duplicates["continuation.tsv"] != {"Deep Sea Diver": 2, "Intelligence.": 2} or duplicates["lexicon.tsv"] != {"Rabbit": 2} or duplicates["continuation-patterns.tsv"]:
        raise ValueError("Unexpected new duplicate resource key")
    print(json.dumps({"new_sources": len(new), "previous_targets_changed": len(changed_previous), "hazards": len(hazards),
                      "malformed": len(malformed), "duplicates": duplicates}, ensure_ascii=False))


if __name__ == "__main__":
    main()
