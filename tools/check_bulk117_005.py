"""Validate the fixed1000-line authored batch without repeating repository-wide analysis."""
import collections
import hashlib
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]


def main():
    fixture = json.loads((ROOT / "translation-source/BULK117_005_ACCEPTANCE.json").read_text(encoding="utf-8"))
    resource = json.loads((ROOT / "src/main/resources/data/witherskyru/bulk117-005.json").read_text(encoding="utf-8"))
    rows = fixture["records"]
    assert len(rows) == 1000 and len(resource["records"]) == 1000
    assert len({r["source"].strip().casefold() for r in rows}) == 1000
    assert len({re.sub(r"\b[IVXLCDM]{1,9}\b", "#ROMAN", re.sub(r"\d+(?:[.,]\d+)*", "#", r["source"])).casefold() for r in rows}) == 1000
    numeric = re.compile(r"\d+(?:[.,]\d+)*")
    for expected, actual in zip(rows, resource["records"]):
        assert {k: expected[k] for k in ("source", "translation", "parts")} == actual
        assert expected["source"] != expected["translation"]
        assert numeric.findall(expected["source"]) == numeric.findall(expected["translation"]), expected["batch_index"]
        assert "".join(a for a, b in expected["parts"]) == expected["source"]
        assert "".join(b for a, b in expected["parts"]) == expected["translation"]
        assert expected["origin"]["source_url"].startswith("https://")
    keys = {r["source"].strip().casefold(): r["translation"] for r in rows}
    occurrences = collections.Counter()
    for name in ("phrases", "vanilla-names", "lexicon-names", "curated", "lore", "continuation", "enchantments", "names", "pet-lore", "catalogue", "corrections", "quality-fixes", "proofread", "quest-interface"):
        path = ROOT / ("src/main/resources/data/witherskyru/" + name + ".tsv")
        for line in path.read_text(encoding="utf-8").splitlines():
            if "\t" not in line or line.startswith("#"):
                continue
            source, target, *_ = line.split("\t")
            key = source.strip().casefold()
            if key in keys:
                assert target == keys[key], source
                occurrences[key] += 1
    assert len(occurrences) == 1000 and all(n == 1 for n in occurrences.values())
    assert "I love the warmth of the Holidays despite the snow" not in {r["source"] for r in rows}
    print(json.dumps({"state": "PASS", "unique_new_rows": 1000, "duplicate_keys": 0, "numeric_and_span_integrity": "PASS", "kinds": dict(collections.Counter(r["kind"] for r in rows)), "fixture_sha256": hashlib.sha256((ROOT / "translation-source/BULK117_005_ACCEPTANCE.json").read_bytes()).hexdigest()}, ensure_ascii=False))


if __name__ == "__main__":
    main()
