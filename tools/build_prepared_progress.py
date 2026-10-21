"""Count the unique union of the fixed item snapshot and authored NPC sample.

This reports prepared data, never live-server or runtime compatibility coverage.
"""
import argparse
import json
from pathlib import Path
import re


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--previous-count", type=int, required=True)
    parser.add_argument("--previous-snapshot-count", type=int, required=True)
    parser.add_argument("--previous-version", required=True)
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[1]
    data = project / "translation-source"
    coverage = json.loads((data / "coverage.json").read_text(encoding="utf-8"))
    corpus = {json.loads(line)["source"] for line in (data / "corpus.jsonl").read_text(encoding="utf-8").splitlines()}
    prepared = {}
    for line in (data / "translation-provenance.jsonl").read_text(encoding="utf-8").splitlines():
        row = json.loads(line)
        prepared[row["source"]] = row["translation"]
    if len(corpus) != coverage["snapshot_strings"] or len(prepared) != coverage["translated_or_partially_translated_snapshot_strings"]:
        raise ValueError("Snapshot report is stale or inconsistent")
    retained = set(coverage.get("intentionally_preserved_rarity_sources", []))
    npc = json.loads((data / "NPC_DIALOGUES_RU.json").read_text(encoding="utf-8"))
    npc_sources = set()
    for row in npc["records"]:
        if row["source"] in npc_sources:
            raise ValueError("Duplicate NPC source")
        npc_sources.add(row["source"])
        if row["translation"] != row["source"]:
            prepared[row["source"]] = row["translation"]
    all_sources = corpus | npc_sources
    if not set(prepared).issubset(all_sources):
        raise ValueError("Translation has no source")
    def has_latin(text):
        text = re.sub(r"\{[^}]+\}", "", text)
        text = re.sub(r"\b[IVXLCDM]+\b", "", text)
        return bool(re.search(r"[A-Za-z]{3}", text))
    latin = sum(has_latin(text) for text in prepared.values())
    translated_count = len(prepared)
    count, total = translated_count + len(retained), len(all_sources)
    latin += len(retained)
    no_latin, remaining = count - latin, total - count
    percent = lambda value: round(100 * value / total, 2)
    version = json.loads((project / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))["version"]
    report = {
        "format_version": 1, "build": version,
        "scope": "Prepared translations of the fixed snapshot union the confirmed NPC sample; not runtime display coverage",
        "original_snapshot_strings": len(corpus),
        "original_snapshot_changed_strings": coverage["translated_or_partially_translated_snapshot_strings"],
        "original_snapshot_changed_strings_delta": coverage["translated_or_partially_translated_snapshot_strings"] - args.previous_snapshot_count,
        "delta_previous_version": args.previous_version,
        "npc_sample_unique_strings": len(npc_sources),
        "npc_sample_prepared_translations": sum(row["translation"] != row["source"] for row in npc["records"]),
        "npc_exact_source_overlap_with_original_snapshot": len(corpus & npc_sources),
        "known_union_unique_strings": total,
        "known_union_changed_or_prepared_strings": count,
        "known_union_actually_changed_translations": translated_count,
        "known_union_intentionally_english_rarity_strings": len(retained),
        "known_union_changed_or_prepared_strings_without_latin_heuristic_only": no_latin,
        "known_union_changed_or_prepared_strings_with_latin_terms": latin,
        "known_union_remaining_strings": remaining,
        "known_union_changed_or_prepared_percent_rounded": percent(count),
        "known_union_no_latin_percent_rounded": percent(no_latin),
        "known_union_with_latin_percent_rounded": percent(latin),
        "known_union_remaining_percent_rounded": percent(remaining),
        "known_union_changed_or_prepared_delta": count - args.previous_count,
        "npc_latin_note": "Prefix [NPC] and original speaker names are retained; Latin does not necessarily mean an untranslated dialogue body",
        "npc_chat_handler_implemented": npc.get("runtime_chat_handler_implemented", False),
        "npc_chat_runtime_verified": False,
        "total_live_npc_dialogue_count": None,
        "full_server_coverage": False, "quality_verified": False,
        "sources": ["coverage.json", "translation-provenance.jsonl", "corpus.jsonl", "NPC_DIALOGUES_RU.json"],
        "count_method": "Exact full-source union; changed translations and intentionally retained English rarity labels counted separately. No fabricated numeric variants.",
    }
    (data / "progress-coverage.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    main()
