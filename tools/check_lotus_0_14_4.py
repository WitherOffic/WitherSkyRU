"""Check authored Lotus lore and unchanged numeric/style tokens; no JAR or game."""
from collections import Counter
from pathlib import Path
import json
import re

P = Path(__file__).resolve().parents[1]
S = P / 'translation-source'

def main():
    pairs = json.loads((S / 'translations.json').read_text(encoding='utf-8'))['translations']
    old = json.loads((S / 'TRANSLATIONS_0.14.3_HISTORY.json').read_text(encoding='utf-8'))['translations']
    changed = {k: v for k, v in pairs.items() if k in old and v != old[k]}
    added = {k: v for k, v in pairs.items() if k not in old}
    checked = 0
    def check(condition, detail):
        nonlocal checked
        assert condition, detail
        checked += 1
    for key, value in (changed | added).items():
        check(Counter(re.findall(r'[+-]?\d+(?:[.,]\d+)*', key)) == Counter(re.findall(r'[+-]?\d+(?:[.,]\d+)*', value)), ('numbers', key, value))
        check(Counter(re.findall(r'\{[^}]+\}', key)) == Counter(re.findall(r'\{[^}]+\}', value)), ('placeholders', key))
        check(Counter(re.findall(r'[\ue000-\uf8ff]', key)) == Counter(re.findall(r'[\ue000-\uf8ff]', value)), ('icons', key))
    checks = [
        (['Named by the first man to discover', 'the island, this species is endemic to', 'the Atoll.'], 'Название дал первооткрыватель острова. Этот вид встречается только на атолле.'),
        (['Throw this into the waters of the', "Lotus Eater's Cave to gain", 'temporary stat bonuses while on the', 'Lotus Atoll!'], 'Бросьте монету в воду пещеры Пожирателя лотосов, чтобы получить временные бонусы характеристик на атолле лотосов!'),
        (['A small coin depicting a wrinkly,', 'bearded frog. Coins of this nature', 'are said to bring good luck when', 'thrown into small fountains or ponds.'], 'Маленькая монета с изображением морщинистой, бородатой лягушки. Говорят, такие монеты приносят удачу, если их бросить в небольшой фонтан или пруд.'),
        (['Caught while having an active', 'Blessing from throwing a Frogcoin', "into the waters of the Lotus Eater's", 'Cave.'], 'Ловится при действующем благословении за бросок лягушачьей монеты в воду пещеры Пожирателя лотосов.'),
        (['Trophies are +10% more likely to be', 'DIAMOND.'], 'Трофеи на +10% чаще имеют качество АЛМАЗНОЕ.'),
        (['A splendid, yet tiny crown; just', 'about the right size for your pet!'], 'Великолепная, но крошечная корона — как раз по размеру вашему питомцу!'),
        (['Lies dormant on the ocean floor of', 'the Lotus Atoll.'], 'Дремлет на дне океана на атолле лотосов.'),
        (['Consume this item to add its', 'destination to your Fast Travel', 'options.'], 'Используйте этот предмет, чтобы добавить его направление в список быстрого перемещения.'),
    ]
    for sources, target in checks:
        check(' '.join(pairs[x] for x in sources) == target, ('context', sources))
    items = json.loads((S / 'item-context.json').read_text(encoding='utf-8'))
    affected = [i for i in items if any(x.strip() in changed for x in [i['name'], *i['lore']])]
    for item in affected:
        for a, b in zip(item['lore'], item['lore'][1:]):
            if a.strip() not in changed and b.strip() not in changed:
                continue
            left, right = pairs.get(a.strip(), a), pairs.get(b.strip(), b)
            x, y = re.search(r'\b([А-Яа-яЁё]+)\s*$', left), re.match(r'\s*([А-Яа-яЁё]+)\b', right)
            check(not (x and y and x[1].casefold() == y[1].casefold()), ('repeated join', item['id'], left, right))
    for tier in ('COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY', 'MYTHIC', 'DIVINE', 'SPECIAL'):
        check(pairs[tier] == tier, ('rarity', tier))
    report = {'build': '0.14.4-dev', 'checks_passed': checked, 'previous_targets_changed': len(changed), 'runtime_support_keys_added': len(added), 'affected_item_records': len(affected), 'full_context_fixtures': len(checks), 'jar_built_this_batch': False, 'game_launched': False, 'npc_dialogues_added': 0, 'full_atoll_translation_confirmed': False}
    (S / 'LOTUS_0.14.4_CHECKS.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False))

if __name__ == '__main__':
    main()
