"""Materialize closed, authored cosmetic/storage captions from the snapshot.

No word substitution in arbitrary prose; no network, JAR or game. Unknown
styles are left untouched. Unknown named shard species are preserved verbatim
inside a translated category caption, not guessed or called fully translated.
"""
from collections import Counter
import json
from pathlib import Path
import re

PROJECT = Path(__file__).resolve().parents[1]
SOURCE = PROJECT / "translation-source"
DATA = PROJECT / "src/main/resources/data/witherskyru"
MARKER = "# Batch 0.10 closed captions; tools/prepare_batch_0_10.py"


def rows(path):
    return [line.split("\t") for line in path.read_text(encoding="utf-8-sig").splitlines()
            if line.strip() and not line.startswith("#")]


def main():
    path = DATA / "continuation.tsv"
    original = path.read_text(encoding="utf-8")
    if MARKER in original:
        original, section = original.split(MARKER, 1)
        previous = json.loads((SOURCE / "BATCH_0.10_DERIVATION.json").read_text(encoding="utf-8"))
        if dict(line.split("\t") for line in section.splitlines() if line.strip()) != {r["source"]: r["translation"] for r in previous["generated_entries"]}:
            raise ValueError("Own generated block changed independently; reconcile without overwriting")
    corpus = {json.loads(line)["source"]: json.loads(line) for line in (SOURCE / "corpus.jsonl").read_text(encoding="utf-8").splitlines()}
    items = json.loads((SOURCE / "item-context.json").read_text(encoding="utf-8"))
    names = {item["name"] for item in items}
    dictionary = {}
    for filename in ("phrases.tsv", "vanilla-names.tsv", "lexicon.tsv", "curated.tsv", "lore.tsv", "continuation.tsv", "enchantments.tsv", "names.tsv", "pet-lore.tsv"):
        content = [line.split("\t") for line in original.splitlines() if line.strip() and not line.startswith("#")] if filename == "continuation.tsv" else rows(DATA / filename)
        dictionary.update({r[0]: r[1] for r in content})
    styles = dict(rows(SOURCE / "BATCH_0.10_STYLES.tsv"))
    objects = {r[0]: r[1:] for r in rows(SOURCE / "BATCH_0.10_OBJECTS.tsv")}
    added, reasons, retained = {}, {}, []

    def put(source, target, reason, allow_support=False):
        if source in dictionary:
            if target != dictionary[source]:
                retained.append({"source": source, "retained": dictionary[source], "candidate": target})
            return
        if source not in added and target != source and (source in corpus or allow_support):
            added[source], reasons[source] = target, reason

    authored = SOURCE / "BATCH_0.10_AUTHORED.tsv"
    if authored.exists():
        for source, target in rows(authored):
            put(source, target, "authored full phrase/name", True)

    def style(value):
        return styles.get(value)

    def skin(value):
        if value in dictionary:
            return dictionary[value]
        if not value.endswith(" Skin"):
            return None
        stem = value[:-5]
        for obj in sorted(objects, key=len, reverse=True):
            if stem == obj:
                return "Облик " + objects[obj][1]
            if stem.endswith(" " + obj):
                label = style(stem[:-len(obj)-1])
                if label:
                    if obj == "Space":
                        return "Космический облик «" + label + "»"
                    return "Облик " + objects[obj][1] + " «" + label + "»"
        label = style(stem)
        return "Облик «" + label + "»" if label else None

    sack_families = {"Agronomy": "урожая", "Husbandry": "продукции животноводства", "Combat": "боевых ресурсов",
                     "Mining": "ресурсов добычи", "Foraging": "ресурсов лесорубства", "Fishing": "улова", "Bait": "наживок",
                     "Dragon": "драконьих ресурсов", "Gemstone": "драгоценных камней", "Lava Fishing": "лавового улова",
                     "Mutations": "мутаций", "Nether": "ресурсов Багрового острова", "Slayer": "ресурсов боссов"}
    enchanted_sack_families = {"Agronomy": "зачарованного урожая", "Husbandry": "зачарованной продукции животноводства",
                             "Combat": "зачарованных боевых ресурсов", "Mining": "зачарованных ресурсов добычи",
                             "Foraging": "зачарованных ресурсов лесорубства", "Fishing": "зачарованного улова"}
    for item in items:
        name = item["name"]
        cosmetic = any("COSMETIC" in line for line in item["lore"]) or "Minion Skin" in item["lore"]
        target = skin(name) if cosmetic else None
        if target:
            put(name, target, "known cosmetic style and destination object")
        m = re.fullmatch(r"(Small|Medium|Large|Extra Large|Beginner) (Enchanted )?(.+) Sack", name)
        if m and m[3] in sack_families:
            size = {"Small": "Малый", "Medium": "Средний", "Large": "Большой", "Extra Large": "Очень большой", "Beginner": "Начальный"}[m[1]]
            content = enchanted_sack_families.get(m[3]) if m[2] else sack_families[m[3]]
            if content:
                put(name, size + " мешок для " + content, "observed storage family and size")
        m = re.fullmatch(r"(.+) Dye", name)
        if m and style(m[1]):
            put(name, "Краситель «" + style(m[1]) + "»", "observed dye name; colour codes not rewritten")
        m = re.fullmatch(r"(.+) Shard", name)
        if m and any(" SHARD (ID " in line for line in item["lore"]):
            label = dictionary.get(m[1], added.get(m[1], m[1]))
            put(name, "Осколок «" + label + "»", "confirmed Hunting shard; unknown species name retained")

    # Only display captions from the archived animated-skull metadata. Do not
    # translate instructions with uppercase item IDs or placeholder syntax.
    animated_families = {
        "Blazing Crimson": "Облик багрового шлема «Пылающий»", "Campfire Flux": "Облик сферы силы «Костёр»",
        "Corrupt Wither Goggles": "Облик очков иссушителя «Порча»", "Gemstone Divan": "Облик шлема Дивана «Драгоценные камни»",
        "Glacial Divan": "Облик шлема Дивана «Ледниковый»", "Great Shark Magma Lord": "Облик магмового владыки «Большая акула»",
        "Magma Lord Ragnarok": "Облик магмового владыки «Рагнарёк»", "Necron Diamond Knight": "Облик Некрона «Алмазный рыцарь»",
        "Reaper Spectre": "Облик жнеца «Призрак»", "Sentinel Warden": "Облик Стража «Часовой»",
        "Sorrow Samurai": "Облик скорби «Самурай»", "Werewolf Wolf Pack": "Облик оборотня «Волчья стая»",
        "Wither Awakened": "Облик иссушителя «Пробуждённый»",
        "Pet Skin Ender Dragon Swap Plushie": "Плюшевый облик дракона Энда",
        "Pet Skin Golden Dragon Ancient": "Древний облик золотого дракона",
        "Pet Skin Golden Dragon Swap Plushie": "Плюшевый облик золотого дракона",
        "Pet Skin Four Season Griffin": "Облик грифона «Четыре сезона»",
        "Pet Skin Frog Biome": "Облик лягушки «Биом»", "Pet Skin Jellyfish Cute": "Милый облик медузы",
        "Pet Skin Sheep Monster": "Облик овцы «Монстр»", "Pet Skin Turtle Shell Shulked": "Облик черепахи «Шалкерский панцирь»",
        "Pet Skin Whale Spirit Orca": "Облик кита «Призрачная косатка" + "»"}
    for source, record in corpus.items():
        if not any("animatedskulls" in origin for origin in record["origins"]):
            continue
        for prefix, target in sorted(animated_families.items(), key=lambda p: -len(p[0])):
            if source.startswith(prefix + " "):
                remainder = source[len(prefix)+1:]
                label = style(remainder.replace("_", " "))
                m = re.fullmatch(r"(.+) Baby", remainder)
                if m and style(m[1]):
                    label = style(m[1]) + ", детёныш"
                if label:
                    put(source, target + " — " + label, "known animated display variant; no ID/instruction conversion")
                break

    # An explicit authored suffix (genitive) plus an observed accessory type.
    family_file = SOURCE / "BATCH_0.10_ACCESSORY_FAMILIES.tsv"
    if family_file.exists():
        families = dict(rows(family_file))
        nouns = {"Talisman": "Талисман", "Ring": "Кольцо", "Artifact": "Артефакт", "Relic": "Реликвия", "Heirloom": "Наследие"}
        for name in names:
            for noun, target in nouns.items():
                if name.endswith(" " + noun) and name[:-len(noun)-1] in families:
                    put(name, target + " " + families[name[:-len(noun)-1]], "authored accessory family and observed item type")

    with path.open("w", encoding="utf-8", newline="\n") as stream:
        stream.write(original.rstrip("\n") + "\n" + MARKER + "\n")
        for key, value in sorted(added.items()):
            stream.write(key + "\t" + value + "\n")
    report = {"revision": "0.10.0-dev", "imported_rows": len(added), "categories": dict(Counter(reasons.values())),
              "retained_previous_terms": retained,
              "generated_entries": [{"source": k, "translation": v, "reason": reasons[k]} for k,v in sorted(added.items())],
              "npc_new_dialogues": 0, "runtime_verified": False, "full_server_coverage": False}
    (SOURCE / "BATCH_0.10_DERIVATION.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"imported": len(added), "categories": report["categories"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
