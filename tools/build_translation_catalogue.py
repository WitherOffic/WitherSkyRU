"""Materialize conservative, authored translation rules against a local snapshot.

This is catalogue generation, not a claim of runtime compatibility or full server
coverage. Untranslated strings remain an explicit backlog; never guess words.
"""
from __future__ import annotations
import argparse
from collections import Counter
import json
from pathlib import Path
import re
from translation_quality import polish, editorial_rules, TIERS

def rows(path):
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        if line.strip() and not line.startswith("#"):
            yield line.split("\t")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--corpus", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--vanilla-en", type=Path)
    parser.add_argument("--vanilla-ru", type=Path)
    parser.add_argument("--items", type=Path)
    args = parser.parse_args()
    data = Path(__file__).resolve().parents[1] / "src/main/resources/data/witherskyru"
    dictionary, provenance = {}, {}
    corpus_rows = [json.loads(line) for line in args.corpus.read_text(encoding="utf-8").splitlines()]
    corpus_names = {record["source"] for record in corpus_rows}
    reviewed = editorial_rules(data)
    (data / "editorial-patterns.tsv").write_text(
        "# Generated from editorial-numeric.tsv; reviewed closed fragments, no guessed English prose.\n" +
        "".join(a + "\t" + b + "\n" for a, b in reviewed), encoding="utf-8")
    # Human-authored pet descriptions: only numeric placeholders vary. Never
    # infer new prose, discard conditions, or replace arbitrary English words.
    parameter = re.compile(r"\{[A-Z0-9_]+\}")
    pet_patterns = []
    for source, target in rows(data / "pet-lore.tsv"):
        if set(parameter.findall(source)) != set(parameter.findall(target)):
            raise ValueError("Numeric placeholders must be preserved: " + source)
        captures, pieces, offset = {}, [], 0
        for index, match in enumerate(parameter.finditer(source), 1):
            pieces.append(re.escape(source[offset:match.start()]))
            pieces.append(r"(\{[^}]+\}|[-+]?\d[\d,.]*[kKmMbB]?)")
            captures.setdefault(match[0], index)
            offset = match.end()
        if not captures: continue
        pieces.append(re.escape(source[offset:]))
        translated = parameter.sub(lambda match: "$" + str(captures[match[0]]), target)
        pet_patterns.append("^" + "".join(pieces) + "$\t" + translated)
    (data / "pet-lore-patterns.tsv").write_text(
        "# Generated numeric variants of human-authored pet-lore.tsv.\n" + "\n".join(pet_patterns) + "\n", encoding="utf-8")
    if args.vanilla_en and args.vanilla_ru:
        en = json.loads(args.vanilla_en.read_text(encoding="utf-8"))
        ru = json.loads(args.vanilla_ru.read_text(encoding="utf-8"))
        candidates = {}
        for key, english in en.items():
            if not key.startswith(("item.minecraft.", "block.minecraft.")) or key not in ru or english not in corpus_names: continue
            if english in candidates and candidates[english] != ru[key]: continue
            candidates[english] = ru[key]
        (data / "vanilla-names.tsv").write_text("# Exact vanilla item/block names present in the local catalogue; installed Minecraft language assets.\n" + "".join(key + "\t" + value + "\n" for key, value in sorted(candidates.items())), encoding="utf-8")
    for name in ("phrases.tsv", "vanilla-names.tsv", "lexicon.tsv", "curated.tsv", "lore.tsv", "continuation.tsv", "enchantments.tsv", "names.tsv", "pet-lore.tsv", "corrections.tsv", "proofread.tsv"):
        for row in rows(data / name):
            dictionary[row[0]] = row[1]
            provenance[row[0]] = name
    corrections = dict(rows(data / "corrections.tsv"))
    corrections.update(rows(data / "proofread.tsv"))
    lexicon = {row[0]: row[1:] for row in rows(data / "lexicon.tsv")}
    grammar = {row[0].casefold(): row[1:] for row in rows(data / "grammar.tsv")}
    for key, values in lexicon.items():
        # Authored full item names take priority over a general noun.
        dictionary.setdefault(key, values[0])
        provenance.setdefault(key, "lexicon.tsv")
    families = {row[0]: row[1:] for row in rows(data / "name-families.tsv")}
    reforges = {row[0]: row[1:] for row in rows(data / "reforges.tsv")}
    suffix_reforges = dict(rows(data / "reforge-placement.tsv"))
    enchantment_names = dict(rows(data / "enchantments.tsv"))
    # Match runtime ambiguity handling instead of silently choosing one casing.
    folded, ambiguous = {}, set()
    for key, value in sorted(dictionary.items()):
        normalized = ' '.join(key.split()).casefold()
        if normalized in folded and folded[normalized] != value: ambiguous.add(normalized)
        folded.setdefault(normalized, value)
    for key in ambiguous: folded.pop(key, None)
    rarity = {"COMMON": "ОБЫЧНЫЙ", "UNCOMMON": "НЕОБЫЧНЫЙ", "RARE": "РЕДКИЙ", "EPIC": "ЭПИЧЕСКИЙ", "LEGENDARY": "ЛЕГЕНДАРНЫЙ", "MYTHIC": "МИФИЧЕСКИЙ", "DIVINE": "БОЖЕСТВЕННЫЙ", "SPECIAL": "ОСОБЫЙ", "VERY SPECIAL": "ОЧЕНЬ ОСОБЫЙ", "SUPREME": "ВЫСШИЙ", "ULTIMATE": "УЛЬТИМАТИВНЫЙ"}
    gender_index = {"m": 0, "f": 1, "n": 2, "p": 3}
    known_types = {key.upper(): (value[0], value[2]) for key, value in lexicon.items()}
    known_types.update({"ACCESSORY": ("Аксессуар", "m"), "COSMETIC": ("Косметический предмет", "m"), "RUNE": ("Руна", "f"), "DEPLOYABLE": ("Размещаемый предмет", "m"), "DRILL": ("Бур", "m"), "WAND": ("Жезл", "m"), "HOE": ("Мотыга", "f"), "AXE": ("Топор", "m"), "ITEM": ("Предмет", "m"), "TRAVEL SCROLL": ("Свиток перемещения", "m"), "REFORGE STONE": ("Камень перековки", "m"), "BAIT": ("Наживка", "f"), "FURNITURE": ("Мебель", "f"), "LONGSWORD": ("Длинный меч", "m"), "GEMSTONE": ("Драгоценный камень", "m"), "GARDEN CHIP": ("Чип Сада", "m"), "FOREST SHARD": ("Лесной осколок", "m"), "COMBAT SHARD": ("Боевой осколок", "m"), "WATER SHARD": ("Водный осколок", "m"), "POWER STONE": ("Камень силы", "m"), "MUTATION": ("Мутация", "f")})
    # Tool categories omitted by the original equipment audit. Individual noun
    # exceptions (Shredder, gauntlets, etc.) remain in authored gender overrides.
    known_types.update({"CARNIVAL MASK": ("Карнавальная маска", "f"), "CHISEL": ("Резец", "m"),
                        "VACUUM": ("Пылесос", "m"), "WATERING CAN": ("Лейка", "f"),
                        "FISHING NET": ("Рыболовная сеть", "f"), "LASSO": ("Лассо", "n")})
    if args.items:
        genders, conflicts = {}, set()
        for item in json.loads(args.items.read_text(encoding="utf-8")):
            for line in item["lore"]:
                for tier in sorted(rarity, key=len, reverse=True):
                    if not line.startswith(tier + " "): continue
                    kind = line[len(tier) + 1:].removeprefix("DUNGEON ")
                    if kind not in known_types: continue
                    key, gender = item["name"], known_types[kind][1]
                    if key in genders and genders[key] != gender: conflicts.add(key)
                    genders[key] = gender
        for key in conflicts: genders.pop(key, None)
        (data / "item-genders.tsv").write_text("# Item genders from explicit rarity/type lore; ambiguous names excluded.\n" + "".join(key + "\t" + value + "\n" for key, value in sorted(genders.items())), encoding="utf-8")
    genders = dict(rows(data / "item-genders.tsv"))
    genders.update(dict(rows(data / "item-gender-overrides.tsv")))
    canonical = {"Mithril": "Мифрил", "Titanium": "Титан", "Glacite": "Глацит", "Heart of the Mountain": "Сердца горы", "митрил": "мифрил", "ледниковой пыли": "глацитовой пыли", "собирательства": "лесорубства"}
    with (data / "canonical-patterns.tsv").open("w", encoding="utf-8", newline="\n") as stream:
        stream.write("# Canonical terminology applied to authored legacy templates.\n")
        for row in rows(data / "patterns.tsv"):
            for old, new in canonical.items(): row[1] = row[1].replace(old, new)
            stream.write("\t".join(row) + "\n")
    rules = []
    for filename in ("proofread-patterns.tsv", "editorial-patterns.tsv", "pet-lore-patterns.tsv", "continuation-patterns.tsv", "expanded-patterns.tsv", "canonical-patterns.tsv"):
        for row in rows(data / filename):
            # Java \Q..\E quoting and group references are converted explicitly.
            pattern = re.sub(r"\\Q(.*?)\\E", lambda m: re.escape(m[1]), row[0])
            replacement = re.sub(r"\$(\d+)", r"\\g<\1>", row[1])
            rules.append((re.compile(pattern, re.I), replacement, filename))
    def lower_first(text):
        return text[0].lower() + text[1:] if text else text
    def adjective(text, gender):
        if gender == "m": return text
        stem = text[:-2]
        return stem + {"f": "ая", "n": "ое", "p": "ые"}[gender]
    def name(source, depth=0):
        if depth > 4: return None
        if source in dictionary: return dictionary[source]
        for prefix, forms in sorted(reforges.items(), key=lambda entry: (-len(entry[0]), entry[0])):
            if source.startswith(prefix + " "):
                base = source[len(prefix) + 1:]
                target = name(base, depth + 1)
                if target and base in genders:
                    modifier = forms[gender_index[genders[base]]]
                    return target + " " + modifier if prefix in suffix_reforges else modifier + " " + target
        m = re.fullmatch(r"(\[[^\]]+\] )?([✪★☆✦]+ ?)?(.+?)( [✪★☆✦]+)?", source)
        if m and any((m[1], m[2], m[4])):
            target = name(m[3], depth + 1)
            if target: return (m[1] or "").replace("[Lvl ", "[Ур. ") + (m[2] or "") + target + (m[4] or "")
        # The roman level is not a word and must remain exactly as sent.
        m = re.fullmatch(r"(.+) ([IVXLCDM]+|\d+)", source)
        if m:
            base = name(m[1], depth + 1)
            if base: return base + " " + m[2]
        m = re.fullmatch(r"(.+) Minion(?: ([IVXLCDM]+))?", source)
        if m and m[1] in lexicon:
            return "Миньон " + lexicon[m[1]][1] + (" " + m[2] if m[2] else "")
        m = re.fullmatch(r"Enchanted (.+)", source)
        if m and m[1] in lexicon:
            noun, genitive, gender = lexicon[m[1]]
            return ["Зачарованный", "Зачарованная", "Зачарованное", "Зачарованные"][gender_index[gender]] + " " + lower_first(noun)
        for noun, values in sorted(lexicon.items(), key=lambda x: -len(x[0])):
            if not source.endswith(" " + noun): continue
            prefix = source[:-(len(noun) + 1)]
            if prefix not in families: continue
            forms = families[prefix]
            if forms[0] == "suffix": return values[0] + " " + forms[1]
            return forms[gender_index[values[2]] + 1] + " " + lower_first(values[0])
        # Kuudra equipment stages: adjective wrappers, only recognized noun genders.
        for prefix, forms in {"Hot": ("Горячий", "Горячая", "Горячее", "Горячие"), "Burning": ("Пылающий", "Пылающая", "Пылающее", "Пылающие"), "Fiery": ("Огненный", "Огненная", "Огненное", "Огненные"), "Infernal": ("Адский", "Адская", "Адское", "Адские")}.items():
            if source.startswith(prefix + " "):
                rest = source[len(prefix) + 1:]
                target = name(rest, depth + 1)
                noun = next((v for k, v in lexicon.items() if rest.endswith(" " + k)), None)
                if target and noun: return forms[gender_index[noun[2]]] + " " + lower_first(target)
        return None
    def translated_capture(value):
        # Runtime lookup normalizes surrounding/repeated whitespace too.
        # Unknown values retain their original spacing; never rewrite identifiers.
        return name(value.strip()) or folded.get(' '.join(value.split()).casefold(), value)
    # Authored categories seen in the local "Mob Types" descriptions. This is
    # a closed list, not a general English-word substitution. Unknown members
    # keep the entire source list unchanged; icon variants are preserved.
    mob_types = {
        "Aquatic": "водное", "Animal": "животное", "Elusive": "неуловимое",
        "Spooky": "жуткое", "Shielded": "защищённое щитом", "Humanoid": "гуманоид",
        "Woodland": "лесное", "Magmatic": "магматическое", "Glacial": "ледниковое",
        "Arcane": "магическое", "Cubic": "кубическое", "Subterranean": "подземное",
        "Undead": "нежить", "Airborne": "летающее", "Critter": "мелкое существо",
        "Construct": "конструкт", "Arthropod": "членистоногое", "Infernal": "адское",
        "Frozen": "замёрзшее", "Mythological": "мифологическое", "Skeletal": "скелетное",
        "Timid": "робкое", "Pest": "вредитель",
    }
    def translate(source):
        if source in corrections: return corrections[source], "corrections.tsv"
        for pattern, replacement, origin in rules:
            if origin not in ("proofread-patterns.tsv", "editorial-patterns.tsv"): continue
            match = pattern.fullmatch(source)
            if match: return match.expand(replacement), origin
        if source in dictionary: return dictionary[source], provenance[source]
        # Whole lists only, with explicit known enchantment names and levels.
        # Never translate arbitrary comma-separated prose, profile names or IDs.
        if "," in source:
            parts = re.split(r"(,\s*)", source)
            translated_parts, members = [], 0
            for index, part in enumerate(parts):
                if index % 2:
                    translated_parts.append(part)
                    continue
                if not part and index == len(parts) - 1:
                    translated_parts.append(part)
                    continue
                member = re.fullmatch(r"(.+?) ([IVXLCDM]+|\d+)", part)
                if not member or member[1] not in enchantment_names:
                    break
                translated_parts.append(enchantment_names[member[1]] + " " + member[2])
                members += 1
            else:
                if members >= 2:
                    return "".join(translated_parts), "authored-enchantment-list"
        converted = name(source)
        if converted: return converted, "authored-name-family"
        if source.startswith("Mob Types: "):
            labels = []
            for entry in source[len("Mob Types: "):].split(","):
                member = re.fullmatch(r"(\s*[^A-Za-zА-Яа-яЁё]+\s+)([A-Za-z]+)(\s*)", entry)
                if not member or member[2] not in mob_types:
                    break
                labels.append(member[1] + mob_types[member[2]] + member[3])
            else:
                return "Типы существа: " + ",".join(labels), "authored-mob-type-list"
        # Tier labels have grammatical agreement rather than blind word swaps.
        for tier in sorted(rarity, key=len, reverse=True):
            if source.startswith(tier + " "):
                noun_type = source[len(tier) + 1:]
                dungeon = noun_type.startswith("DUNGEON ")
                noun_type = noun_type.removeprefix("DUNGEON ")
                if noun_type in known_types:
                    noun, gender = known_types[noun_type]
                    return tier + " " + noun.upper() + (" ПОДЗЕМЕЛИЙ" if dungeon else ""), "rarity-type"
        m = re.fullmatch(r"\[Lvl ([\d{}A-Z_]+)\] (.+)", source)
        if m:
            target = name(m[2])
            if target: return "[Ур. " + m[1] + "] " + target, "pet-level"
        # Preserve stat icons, numbers, brackets and placeholders byte-for-byte.
        m = re.fullmatch(r"([+\-•◆✦❁❤❈☠☣☘☯✎✧♣α❣⚔⫽⸕☂⚒☄⚓❂⛏♨♧♠✯✿♆❃☢❍⏣⚚★☆✪\s]*)([^:\n]+): ?(.*)", source)
        if m:
            label = translated_capture(m[2])
            right = translated_capture(m[3])
            if label != m[2] and (not re.search(r"[A-Za-z]{2}", m[3]) or re.fullmatch(r"[\d+,.%() /\-{}A-Z_⚔⸕☘✎❁❤❈☠✧✦♣α❣⚒☄⚓❂⛏★☆✪\[\]]+", m[3]) or right != m[3]):
                return m[1] + label + ": " + right, "label-value"
        m = re.fullmatch(r"([-•] )(.+)", source)
        if m:
            target = name(m[2])
            if target: return m[1] + target, "item-type-list"
        m = re.fullmatch(r"([^A-Za-z]*)([A-Za-z][A-Za-z '’-]*)([^A-Za-z]*)", source)
        if m:
            target = translated_capture(m[2].strip())
            if target != m[2].strip():
                suffix = m[3]
                # A dictionary fragment may already include the same punctuation.
                if suffix in (".", ",", "!", "?") and target.endswith(suffix): suffix = ""
                return m[1] + target + m[2][len(m[2].rstrip()):] + suffix, "decorated-label"
        m = re.fullmatch(r"(.+) Collection", source)
        if m and m[1] in lexicon: return "Коллекция " + lexicon[m[1]][1], "collection-name"
        m = re.fullmatch(r"([IVXLCDM]+(?:-[IVXLCDM]+)?): (.+?)(,|\.)?", source)
        if m:
            parts = m[2].split(", ")
            targets = [translated_capture(part) for part in parts]
            if all(a != b for a, b in zip(parts, targets)):
                return m[1] + ": " + ", ".join(targets) + (m[3] or ""), "enchantment-source"
        for pattern, replacement, origin in rules:
            match = pattern.fullmatch(source)
            if match:
                def capture(ref):
                    value = match[int(ref[2] or ref[3])] or ""
                    case = ref[1]
                    if case and value.casefold() in grammar:
                        return grammar[value.casefold()][0 if case == "gen" else 1]
                    return translated_capture(value)
                target = re.sub(r"\$\{(gen|acc):(\d+)\}|\\g<(\d+)>", capture, replacement)
                return target, origin
        return None, None
    records = corpus_rows
    generated, backlog, counts, retained = {}, [], Counter(), []
    partial = 0
    occurrence_total = occurrence_changed = 0
    args.report.mkdir(parents=True, exist_ok=True)
    with (args.report / "translation-provenance.jsonl").open("w", encoding="utf-8") as stream:
        for record in records:
            source = record["source"]
            target, origin = translate(source)
            if target: target = polish(source, target)
            occurrence_total += record["occurrences"]
            if target and target != source:
                generated[source] = target
                counts[origin] += 1
                counted_text = re.sub(r"\{[^}]+\}", "", target)
                counted_text = re.sub(r"\b[IVXLCDM]+\b", "", counted_text)
                if re.search(r"[A-Za-z]{3}", counted_text):
                    partial += 1
                occurrence_changed += record["occurrences"]
                stream.write(json.dumps({**record, "translation": target, "translation_origin": origin}, ensure_ascii=False) + "\n")
            elif source[:1].isupper() and source.upper() in TIERS and target == source:
                retained.append(record)
            else: backlog.append(record)
    # Explicit outputs from a generator are deterministic data, not manual edits.
    with (data / "catalogue.tsv").open("w", encoding="utf-8", newline="\n") as stream:
        stream.write("# Generated from local catalogue + authored rules; tools/build_translation_catalogue.py\n")
        for key, value in sorted(generated.items()): stream.write(key + "\t" + value + "\n")
    with (args.report / "untranslated-backlog.tsv").open("w", encoding="utf-8", newline="\n") as stream:
        stream.write("occurrences\tEnglish source\torigin\n")
        for record in backlog:
            stream.write(str(record["occurrences"]) + "\t" + record["source"] + "\t" + record["origins"][0] + "\n")
    # Two-column runtime resource, retain lexicon morphology separately in source.
    (data / "lexicon-names.tsv").write_text("# Nominative names generated from lexicon.tsv\n" + "".join(key + "\t" + value[0] + "\n" for key, value in sorted(lexicon.items())), encoding="utf-8")
    runtime_pairs = {}
    for file in ("phrases.tsv", "vanilla-names.tsv", "lexicon-names.tsv", "curated.tsv", "lore.tsv", "continuation.tsv", "enchantments.tsv", "names.tsv", "pet-lore.tsv", "catalogue.tsv", "corrections.tsv"):
        for row in rows(data / file): runtime_pairs[row[0].strip()] = row[1].strip()
    quality_fixes = {k: polish(k, v) for k, v in runtime_pairs.items() if polish(k, v) != v}
    runtime_pairs.update(quality_fixes)
    runtime_pairs.update(rows(data / "proofread.tsv"))
    # Final generated layer also covers supporting keys outside the frozen corpus.
    (data / "quality-fixes.tsv").write_text(
        "# Generated final editorial/rarity layer; tools/build_translation_catalogue.py\n" +
        "".join(k + "\t" + v + "\n" for k, v in sorted(quality_fixes.items())),
        encoding="utf-8")
    manifest = {"snapshot_strings": len(records), "translated_or_partially_translated_snapshot_strings": len(generated), "intentionally_preserved_rarity_strings": len(retained), "intentionally_preserved_rarity_sources": [r["source"] for r in retained], "processed_snapshot_strings": len(generated) + len(retained), "translated_strings_with_remaining_latin_terms": partial, "remaining_snapshot_strings": len(backlog), "runtime_dictionary_entries": len(runtime_pairs), "runtime_identity_entries": sum(k == v for k, v in runtime_pairs.items()), "runtime_templates": len(rules), "snapshot_occurrences": occurrence_total, "translated_or_partially_translated_occurrences": occurrence_changed, "translation_origins": dict(counts), "full_server_coverage": False, "runtime_tested": False, "note": "Counts are authored catalogue coverage, not quality assertions or live game tests. English rarity labels intentionally retained by user request and counted separately, not untranslated. Latin terms include preserved rarity/brands/ranks and unlocalized arguments."}
    (args.report / "coverage.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (args.report / "translations.json").write_text(json.dumps({"format_version": 1, "language": "ru", "coverage": manifest, "translations": dict(sorted(runtime_pairs.items())), "reforge_forms": reforges, "item_genders": genders, "stat_grammar": grammar}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, ensure_ascii=False))

if __name__ == "__main__": main()
