"""Import authored terms and closed, snapshot-backed caption variants.

Mechanical generation only. Unknown names are preserved, not guessed. No
network, compilation or runtime tests. Existing authored translations win.
All generated exact entries are stored in continuation.tsv for the runtime.
"""
import json
from pathlib import Path
import re

PROJECT = Path(__file__).resolve().parents[1]
SOURCE = PROJECT / "translation-source"
DATA = PROJECT / "src/main/resources/data/witherskyru"
MARKER = "# Closed exact captions materialized for batch 0.9.0; tools/prepare_batch_0_9.py"


def rows(path):
    return [line.split("\t") for line in path.read_text(encoding="utf-8-sig").splitlines()
            if line.strip() and not line.startswith("#")]


def main():
    continuation = DATA / "continuation.tsv"
    original = continuation.read_text(encoding="utf-8")
    if MARKER in original:
        original, previous_section = original.split(MARKER, 1)
        previous_report = json.loads((SOURCE / "BATCH_0.9_DERIVATION.json").read_text(encoding="utf-8"))
        previous_pairs = {r["source"]: r["translation"] for r in previous_report["generated_entries"]}
        actual_pairs = dict(line.split("\t") for line in previous_section.splitlines() if line.strip())
        if actual_pairs != previous_pairs:
            raise ValueError("Generated section was edited independently; preserve it and reconcile manually")
    corpus = {json.loads(line)["source"] for line in (SOURCE / "corpus.jsonl").read_text(encoding="utf-8").splitlines()}
    items = json.loads((SOURCE / "item-context.json").read_text(encoding="utf-8"))
    dictionary = {}
    for filename in ("phrases.tsv", "vanilla-names.tsv", "lexicon.tsv", "curated.tsv", "lore.tsv", "continuation.tsv", "enchantments.tsv", "names.tsv", "pet-lore.tsv"):
        file_rows = [line.split("\t") for line in original.splitlines() if line.strip() and not line.startswith("#")] if filename == "continuation.tsv" else rows(DATA / filename)
        dictionary.update({r[0]: r[1] for r in file_rows})
    old_keys = set(dictionary)
    previous_authored_dictionary = dict(dictionary)
    added, provenance = {}, {}
    skipped = []
    for row in rows(SOURCE / "BATCH_0.9_AUTHORED.tsv"):
        if len(row) != 2 or not all(row):
            raise ValueError("Invalid authored row")
        key, value = row
        if key in dictionary:
            skipped.append({"source": key, "retained": dictionary[key], "candidate": value})
            continue
        dictionary[key] = value
        # Ant is also a proper NPC name. Use the noun only in explicit creature
        # and shard captions; leave the ambiguous stand-alone heading original.
        if key == "Ant":
            continue
        added[key] = value
        provenance[key] = "hand-authored term"

    def lookup(value):
        if value in dictionary:
            return dictionary[value]
        m = re.fullmatch(r"(.+) ([IVXLCDM]+|\d+)", value)
        if m and m[1] in dictionary:
            return dictionary[m[1]] + " " + m[2]
        return value

    def put(source, target, reason):
        if source in corpus and source not in old_keys and source not in added and target != source:
            added[source] = target
            provenance[source] = reason

    roles = {"NPC": "НПС", "Rift NPC": "НПС Разлома", "Monster": "существо",
             "Sea Creature": "морское существо", "Boss": "босс", "Miniboss": "мини-босс",
             "Animal": "животное", "Pest": "вредитель", "Mayor": "мэр", "Retired Mayor": "бывший мэр"}
    role_regex = re.compile(r"(.+) \((" + "|".join(map(re.escape, roles)) + r")\)")
    trophy_ranks = {"BRONZE": "бронзовый трофей", "SILVER": "серебряный трофей",
                    "GOLD": "золотой трофей", "DIAMOND": "алмазный трофей"}
    for item in items:
        name = item["name"]
        m = role_regex.fullmatch(name)
        if m:
            # New animal nouns must not rename identically named NPCs (Ant).
            # Keep established earlier aliases, otherwise preserve the NPC name.
            character_role = m[2] in {"NPC", "Rift NPC", "Mayor", "Retired Mayor"}
            label = previous_authored_dictionary.get(m[1], m[1]) if character_role else lookup(m[1])
            put(name, label + " (" + roles[m[2]] + ")", "observed entity role; unknown names preserved")
        m = re.fullmatch(r"◆ (.+ Rune) ([IVXLCDM]+)", name)
        if m and m[1] in dictionary:
            put(name, "◆ " + dictionary[m[1]] + " " + m[2], "observed rune symbol and level")
        m = re.fullmatch(r"(.+) (BRONZE|SILVER|GOLD|DIAMOND)", name)
        if m and m[1] in dictionary:
            put(name, dictionary[m[1]] + " — " + trophy_ranks[m[2]], "observed trophy rank")
        m = re.fullmatch(r"(.+) Shard", name)
        if m and lookup(m[1]) != m[1]:
            put(name, "Осколок «" + lookup(m[1]) + "»", "observed named shard; nominative quoted to avoid invented declension")

    # Structural lore/type captions only; no arbitrary English prose replacement.
    rarity = {"COMMON": "ОБЫЧНЫЙ", "UNCOMMON": "НЕОБЫЧНЫЙ", "RARE": "РЕДКИЙ", "EPIC": "ЭПИЧЕСКИЙ", "LEGENDARY": "ЛЕГЕНДАРНЫЙ"}
    shard_types = {"FOREST": "ЛЕСНОЙ", "COMBAT": "БОЕВОЙ", "WATER": "ВОДНЫЙ"}
    for source in corpus:
        m = re.fullmatch(r"(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY) (FOREST|COMBAT|WATER) SHARD \(ID ([CUREL]\d+)\)", source)
        if m:
            put(source, rarity[m[1]] + " " + shard_types[m[2]] + " ОСКОЛОК (ID " + m[3] + ")", "observed rarity/type/ID; ID preserved")
        m = re.fullmatch(r"([\uE000-\uE1FF☯]+) (Additive Buffs|Flat Bonuses|Multiplicative Buffs|Cap)", source)
        if m:
            label = {"Additive Buffs": "Суммируемые усиления", "Flat Bonuses": "Прямые прибавки", "Multiplicative Buffs": "Перемножаемые усиления", "Cap": "Предел"}[m[2]]
            put(source, m[1] + " " + label, "observed stat breakdown; icon preserved")
        m = re.fullmatch(r"([\uE000-\uE1FF]+) (Rough|Flawed|Fine|Flawless|Perfect) (.+) Gemstone", source)
        if m:
            gem = lookup(m[3])
            if gem != m[3]:
                quality = {"Rough": "грубый", "Flawed": "с дефектами", "Fine": "качественный", "Flawless": "безупречный", "Perfect": "идеальный"}[m[2]]
                put(source, m[1] + " " + gem + " — " + quality, "observed gemstone grade; icon preserved")

    with continuation.open("w", encoding="utf-8", newline="\n") as stream:
        stream.write(original.rstrip("\n") + "\n" + MARKER + "\n")
        for key, value in sorted(added.items()):
            stream.write(key + "\t" + value + "\n")
    report = {"revision": "0.9.0-dev", "hand_authored_term_rows": len(rows(SOURCE / "BATCH_0.9_AUTHORED.tsv")),
              "terms_and_closed_exact_variants_imported": len(added), "existing_authored_terms_retained": skipped,
              "generated_entries": [{"source": k, "translation": v, "reason": provenance[k]} for k, v in sorted(added.items())],
              "context_only_terms_not_installed_as_global_keys": ["Ant"],
              "scope": "Fixed snapshot only; roles are not NPC dialogue translations; unknown names retained"}
    (SOURCE / "BATCH_0.9_DERIVATION.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"terms_and_variants": len(added), "hand_terms": report["hand_authored_term_rows"], "retained_existing_terms": len(skipped)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
