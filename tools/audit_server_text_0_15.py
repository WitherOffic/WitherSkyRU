"""Local inventory, not a live-server crawler. Keep the original denominator."""
from collections import Counter
from pathlib import Path
import json
import re
from classify_remaining_0_14 import MIRRORED, BRANDS

P = Path(__file__).resolve().parents[1]
S = P / 'translation-source'
GAME = Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft')
COLOR = re.compile(r'§[0-9a-fk-or]', re.I)

def category(row):
    t = row['source']
    if t == 'Prism' and any(o.endswith('Launchers.json') for o in row['origins']): return 'brand'
    if any(o.endswith('LividSolver.json') for o in row['origins']): return 'technical_pattern_or_identifier'
    if t in {'Rusty','Crag','Cobble','Horizon','Mistral','Eleanor','Eleanor I','Eleanor IX','Eleanor V','Eleanor VII','Eleanor X','Fleespook','Tiki','Ikrus'}: return 'proper_name_candidate'
    if t in MIRRORED: return 'mirrored_puzzle'
    if t in BRANDS: return 'brand'
    if t.startswith('- '): return 'player_credits'
    if t.upper() in ('COMMON','UNCOMMON','RARE','EPIC','LEGENDARY','MYTHIC','DIVINE','SPECIAL','VERY SPECIAL','SUPREME'): return 'intentional_english_rarity'
    if any(c in t for c in ('\\','(?:','(?<','.*','.+','[IVX','[XIV')) or re.fullmatch(r'[A-Z][A-Z0-9_]+', t) or t.startswith('at.hannibal2.') or t in {'OreMinedEvent','a SPECIAL a','MMage OutlawM','SkeletonHorse','SiNelverfish','Dev Env'}: return 'technical_pattern_or_identifier'
    if re.fullmatch(r'[\d,.]+(?:x\.|/[\d,.]+[MK]?)',t): return 'numeric_only'
    if row.get('roles') == ['entity']: return 'entity_identifier'
    if row.get('roles') == ['name'] and any(o.endswith('mobs.min.json') for o in row['origins']): return 'proper_name_candidate'
    return 'readable_candidate'

def map_category(text):
    if text.startswith('todo') or text in {'Etc','aotv','hannibal ist doof','hoppity w','Hoppity w','hide during spooky','year of the witch hidden','year of the witch shown','Floor dropfd','Cracked Walls 44 a4','Not Coming Jungle Dungeon'}:
        return 'repository_annotation_not_server_text'
    if text in {'SwampCut Inc.','Abiphones & Co.'}: return 'brand'
    return 'proper_name_or_label_needs_context'

def main():
    pairs = json.loads((S/'translations.json').read_text(encoding='utf-8'))['translations']
    normalized = {}
    ambiguous = set()
    for key,value in pairs.items():
        key = ' '.join(key.split()).casefold()
        if key in normalized and normalized[key] != value: ambiguous.add(key)
        normalized.setdefault(key,value)
    for key in ambiguous: normalized.pop(key,None)
    def translated(text):
        return pairs.get(text,normalized.get(' '.join(text.split()).casefold(),text))
    records = [json.loads(x) for x in (S/'corpus.jsonl').read_text(encoding='utf-8').splitlines()]
    unchanged = [{**r,'category':category(r)} for r in records if pairs.get(r['source'],r['source']) == r['source']]
    partial = []
    tiers = r'\b(?:VERY SPECIAL|COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|SUPREME|ULTIMATE|NPC|ID|LVL|MM|[IVXLCDM]+)\b'
    for r in records:
        target = pairs.get(r['source'],r['source'])
        if target == r['source']: continue
        remaining = re.sub(tiers,'',re.sub(r'\{[^}]+\}','',target))
        words = re.findall(r'[A-Za-z]{3,}',remaining)
        if words: partial.append({**r,'translation':target,'latin_words':words})
    graph = {}
    files = sorted((GAME/'config/skyhanni/repo/constants/island_graphs').glob('*.json'))
    for file in files:
        data = json.loads(file.read_text(encoding='utf-8-sig'))
        for node in data.values():
            if not isinstance(node,dict): continue
            label = node.get('Name')
            if not isinstance(label,str) or label.startswith('/') or not re.search('[A-Za-z]',label) or label == 'no_area' or re.fullmatch('[A-Z][A-Z0-9_]+',label): continue
            label = COLOR.sub('',label).strip()
            row = graph.setdefault(label,{'source':label,'origins':[],'roles':['map_label'],'category':map_category(label)})
            origin = file.relative_to(GAME).as_posix()
            if origin not in row['origins']: row['origins'].append(origin)
    graph_missing = [r for r in graph.values() if translated(r['source']) == r['source']]
    npc = json.loads((S/'NPC_DIALOGUES_RU.json').read_text(encoding='utf-8'))
    npc_known = {r['source'] for r in npc['records']}
    found = {}
    # Only explicitly prefixed NPC bodies, no ordinary player chat.
    for file in sorted((GAME/'logs').glob('*.log')):
        for line in file.read_text(encoding='utf-8',errors='replace').splitlines():
            if '[CHAT] ' not in line: continue
            raw = line.split('[CHAT] ',1)[1]
            body = COLOR.sub('',raw)
            body = re.sub(r'^\[\d{2}:\d{2}:\d{2}\]\s*','',body)
            if not re.match(r'^\[NPC\] [^:]+: ',body): continue
            row = found.setdefault(body,{'source':body,'styled_samples':[],'origins':[]})
            if raw not in row['styled_samples']: row['styled_samples'].append(raw)
            if file.name not in row['origins']: row['origins'].append(file.name)
    report = {'revision':json.loads((P/'src/main/resources/fabric.mod.json').read_text())['version'],
        'frozen_snapshot_unique':len(records),'unchanged_snapshot_sources_including_rarities':len(unchanged),
        'unchanged_categories':dict(Counter(r['category'] for r in unchanged)),
        'partial_translations_with_non_rarity_latin':len(partial),'partial_is_not_automatically_incorrect':True,
        'map_graph_files':len(files),'map_unique_readable_labels':len(graph),'map_missing_exact_labels':len(graph_missing),'map_unchanged_categories':dict(Counter(r['category'] for r in graph_missing)),
        'npc_observed_unique_full_messages':len(found),'npc_missing_exact_messages':sum(t not in npc_known for t in found),
        'live_server_total_unknown':True,'game_launched':False,'frozen_denominator_unchanged':True}
    for name,data in [('SERVER_0.15_AUDIT',report),('SERVER_0.15_UNCHANGED',unchanged),('SERVER_0.15_PARTIAL',partial),('SERVER_0.15_MAP_MISSING',graph_missing),('SERVER_0.15_NPC_OBSERVED',list(found.values()))]:
        (S/(name+'.json')).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False))

if __name__ == '__main__': main()
