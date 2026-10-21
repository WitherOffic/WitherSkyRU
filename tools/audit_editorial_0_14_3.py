"""Whole database/static description scan. Flags are candidates, not quality proofs."""
import json, re, hashlib, zipfile
from pathlib import Path
from collections import Counter

P = Path(__file__).resolve().parents[1]
S = P / 'translation-source'
D = P / 'src/main/resources/data/witherskyru'
FILES = ['phrases','vanilla-names','lexicon-names','curated','lore','continuation',
         'enchantments','names','pet-lore','catalogue','corrections','quality-fixes']
def read_json(path): return json.loads(path.read_text(encoding='utf-8'))
def tokens(text): return Counter(re.findall(r'\d+(?:[.,]\d+)*', re.sub(r'\{[^}]+\}', '', text)))
def icons(text): return Counter(re.findall(r'[\ue000-\uf8ff]', text))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest().upper()

def main():
    pairs, rows = {}, 0
    for name in FILES:
        local = {}
        for n, line in enumerate((D / (name + '.tsv')).read_text(encoding='utf-8').splitlines(), 1):
            if not line.strip() or line.startswith('#'): continue
            fields = line.split('\t'); assert len(fields) == 2 and all(fields), (name, n)
            k, v = (s.strip() for s in fields)
            assert not any(c in line for c in ('\ufffd', '\x00')), (name, n)
            assert k not in local or local[k] == v, (name, n, k)
            assert Counter(re.findall(r'\{[^}]+\}', k)) == Counter(re.findall(r'\{[^}]+\}', v)), (name, n, k)
            local[k] = v; pairs[k] = v; rows += 1
    assert pairs == read_json(S / 'translations.json')['translations']
    previous = {k.strip(): v.strip() for k, v in read_json(S / 'TRANSLATIONS_0.14.2_HISTORY.json')['translations'].items()}
    changed = {k: (previous[k], v) for k, v in pairs.items() if k in previous and previous[k] != v}
    additions = {k: v for k, v in pairs.items() if k not in previous}
    icon_changes = [{'source': k, 'old': a, 'new': b} for k, (a,b) in changed.items() if icons(a) != icons(b)]
    assert not icon_changes, icon_changes
    assert not [(k,v) for k,v in pairs.items() if re.search(r'(?<!\.)\.\.(?!\.)',v) and '..' not in k]
    items = read_json(S / 'item-context.json')
    repeated, mismatches = {}, []
    for item in items:
        en = '\n'.join(item['lore']); ru = '\n'.join(pairs.get(x.strip(),x) for x in item['lore'])
        if tokens(en) != tokens(ru): mismatches.append({'item':item['id'], 'missing':dict(tokens(en)-tokens(ru)), 'added':dict(tokens(ru)-tokens(en))})
        for a,b in zip(item['lore'],item['lore'][1:]):
            x,y = pairs.get(a.strip(),a), pairs.get(b.strip(),b)
            m,n = re.search(r'\b([А-Яа-яЁё]+)\s*$',x), re.match(r'\s*([А-Яа-яЁё]+)\b',y)
            if m and n and m[1].lower() == n[1].lower(): repeated[(a,b)] = {'source':[a,b], 'target':[x,y]}
    # Three observed exceptions: title/description or clucking, not duplicated prose.
    assert set(repeated) == {('Freedom','Freedom from fear.'),('Slimy Minions','Slime and Magma Cube minions work'),('dolphin, sometimes a chicken. Cluck','cluck!')}, list(repeated.values())
    sapphire = next(i for i in items if i['id'] == 'SAPPHIRE_POWER_SCROLL')
    assert '5 с' in '\n'.join(pairs.get(x,x) for x in sapphire['lore'])
    # Existing number -> words (2x -> doubled) and roman/name differences are reported,
    # not automatically rewritten or counted as defects. Inspect changed-only tokens.
    changed_numbers = [{'source':k, 'old':a, 'new':b} for k,(a,b) in changed.items() if tokens(a) != tokens(b)]
    changed_placeholders = [{'source':k,'old':a,'new':b} for k,(a,b) in changed.items() if Counter(re.findall(r'\{[^}]+\}',a)) != Counter(re.findall(r'\{[^}]+\}',b))]
    assert not changed_placeholders
    jar = P / 'build/libs/WitherSkyRU-26.1.2-fabric-0.14.3-dev.jar'
    with zipfile.ZipFile(jar) as z:
        assert z.testzip() is None
        resources = [f for f in (P / 'src/main/resources').rglob('*') if f.is_file()]
        for f in resources: assert z.read(f.relative_to(P / 'src/main/resources').as_posix()) == f.read_bytes(), f
        mixins = read_json(P / 'src/main/resources/witherskyru.mixins.json')
        for m in mixins['client']: assert (mixins['package'].replace('.','/') + '/' + m + '.class') in z.namelist()
        classes = sum(n.endswith('.class') for n in z.namelist())
    installed = Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/mods/WitherSkyRU-26.1.2-fabric-0.14.2-dev.jar')
    assert sha(installed) == '0D2FBDEEEA977AF77041057D85DF1FF066688883B5DD168C1DC632F1EC63A35A'
    report = {'build':'0.14.3-dev', 'physical_pair_rows_scanned':rows, 'runtime_unique_trimmed_keys':len(pairs),
        'previous_targets_changed':len(changed), 'rarity_prefix_or_token_changes':sum(bool(re.match(r'^(?:VERY )?(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|SUPREME|ULTIMATE)(?: |$)',k)) or k.upper() in ('COMMON','UNCOMMON','RARE','EPIC','LEGENDARY','MYTHIC','DIVINE','SPECIAL','SUPREME','VERY SPECIAL') for k in changed),
        'additional_runtime_support_keys':additions, 'full_item_descriptions_machine_scanned':len(items),
        'adjacent_repeat_exceptions_reviewed':list(repeated.values()), 'full_item_numeric_candidates_not_automatically_errors':mismatches,
        'changed_numeric_candidates_reviewed':changed_numbers,
        'changed_numeric_review_resolutions': ['Restored the missing sapphire scroll 5-second cooldown', '2x expressed as Удваивает; multiplier meaning preserved'],
        'changed_icon_multisets':icon_changes,
        'new_double_periods':0, 'placeholder_mismatches':0, 'jar_sha256':sha(jar), 'jar_bytes':jar.stat().st_size,
        'jar_resource_byte_matches':len(resources), 'jar_classes':classes, 'mixin_classes':len(mixins['client']),
        'installed_0_14_2_jar_hash_unchanged':True, 'full_manual_proofread_of_all_items':False,
        'in_game_visual_verification':False, 'full_server_coverage':False,
        'limits':'Machine scan is not full manual proofread. 3D name tags/holograms and unknown NPC dialogues are not covered by the GUI hooks.'}
    (S / 'EDITORIAL_0.14.3_AUDIT.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in report.items() if k not in ('full_item_numeric_candidates_not_automatically_errors','additional_runtime_support_keys','adjacent_repeat_exceptions_reviewed')},ensure_ascii=False))
if __name__ == '__main__': main()
