"""Audit local display misses; do not copy ordinary chat/player identifiers."""
from collections import Counter
from pathlib import Path
import json
import re

P=Path(__file__).resolve().parents[1]
S=P/'translation-source'
GAME=Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft')

def excluded(text):
    if re.match(r'^Bazaar ➜ "', text): return 'search_input_not_translated'
    if text.startswith(('Chat log codec error','Player name contained disallowed characters:')): return 'debug_or_network_diagnostics'
    if text.startswith('/') or re.fullmatch(r'\(ID [A-Z]\d+\)',text): return 'command_or_identifier'
    if re.search(r'(?:MiB|\bMB\b|\bfps\b|\bGMT[+-]|^XYZ:|^Facing:|^Section-relative:|^Chunk:|^TS \(|^\[Iris\]|^Allocation rate:|^Buffer Pool:|^Off-Heap:|^Async Culling:|^Sodium Renderer|^Java:|^CPU:|^Display:|^GPU:|BungeeCord|^HUD:)',text): return 'debug_or_network_diagnostics'
    if re.match(r'^(?:Created at: )?(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)\b',text): return 'real_world_timestamp'
    if re.search(r"(?:'s Profile$|Click to (?:open|view).*(?:profile|Profile)|^Seller: \S+$|^Owner: \S+$)",text) or re.fullmatch(r'\w{3,16}:',text): return 'player_identity_hover_not_exported'
    if re.search(r'\((?:НПС|НПС Разлома)\)$',text): return 'translated_role_with_preserved_name'
    if text in {'COMMON','UNCOMMON','RARE','EPIC','LEGENDARY','MYTHIC','DIVINE','SPECIAL','VERY SPECIAL','SUPREME','SkyBlock','SkyHanni','SkyMart','Skyblocker','Bazaar Utils','Skyblock.bz','Prism','Smash Heroes','The Hypixel Pit','The TNT Games','Arcade Games'}: return 'rarity_brand_or_other_game'
    return None

def main():
    snapshot=P/'build/source-audit-0.15/unknown-snapshot.tsv'
    raw=(snapshot if snapshot.exists() else GAME/'config/witherskyru/unknown.tsv').read_text(encoding='utf-8',errors='replace').splitlines()
    flags_path=P/'build/source-audit-0.15/display-flags.txt'
    flags=flags_path.read_text(encoding='utf-8') if flags_path.exists() else ''
    assert not flags or len(flags)==len(raw), 'Local unknown log changed; rerun current Java audit'
    translated_now={re.sub(r'§[0-9a-fk-or]','',s,flags=re.I).strip() for i,s in enumerate(raw) if flags and flags[i]=='1'}
    lines=set(re.sub(r'§[0-9a-fk-or]','',s,flags=re.I).strip() for s in raw if s.strip())
    pairs=json.loads((S/'translations.json').read_text(encoding='utf-8'))['translations']
    folded={ ' '.join(k.split()).casefold():v for k,v in pairs.items() }
    patterns=[]
    for name in ('editorial-patterns','pet-lore-patterns','continuation-patterns','expanded-patterns','canonical-patterns'):
        for line in (P/'src/main/resources/data/witherskyru'/(name+'.tsv')).read_text(encoding='utf-8').splitlines():
            if line.strip() and not line.startswith('#'):
                patterns.append(re.compile(re.sub(r'\\Q(.*?)\\E',lambda m:re.escape(m[1]),line.split('\t')[0]),re.I))
    categories=Counter()
    families={}
    for text in sorted(lines):
        if not re.search('[A-Za-z]{2}',text): categories['no_english_words']+=1;continue
        if re.search(r'\[(?:MVP|VIP|ADMIN|YOUTUBE|MOD|HELPER)|https?://|www\.|(?:^|\s)[\w.-]+\.(?:com|net|gg|org|io|ru)\b',text,re.I): categories['player_rank_or_url']+=1;continue
        if re.match(r'^\[\d\d:\d\d:\d\d\]',text) and not re.search(r'\[NPC\]',text): categories['timestamped_chat_not_imported']+=1;continue
        if re.fullmatch(r'[\d\s+.,:%()/\-]+(?:[kMdhms]+[\d\s.,:%()/\-]*)*',text): categories['timer_or_numeric']+=1;continue
        skip=excluded(text)
        if skip: categories[skip]+=1;continue
        if text in translated_now: categories['now_current_java_dictionary_changed']+=1;continue
        target=pairs.get(text,folded.get(' '.join(text.split()).casefold(),text))
        if target != text: categories['now_exact_or_normalized_lookup']+=1;continue
        if any(p.fullmatch(text) for p in patterns): categories['existing_template_match_not_quality_proof']+=1;continue
        if re.fullmatch(r'[A-Za-z0-9_]{1,32}',text) and text not in {'Normal','Spring','Summer','Autumn','Winter','Error','Enabled','Disabled','Next','Previous','Confirm','Cancel','Coins','Arrow','Armor','Sowdust','Cultivating','Materials'}: categories['single_token_name_or_identifier']+=1;continue
        if len(text)>400: categories['long_text_needs_review']+=1;continue
        # Numeric normalization groups observed variants; families are not source-string counts.
        family=re.sub(r'[+-]?\d+(?:[,.]\d+)*(?:st|nd|rd|th)?','{N}',text)
        row=families.setdefault(family,{'family':family,'observed_variants':0,'samples':[]})
        row['observed_variants']+=1
        if len(row['samples'])<3:row['samples'].append(text)
        categories['candidate_display_line']+=1
    output=sorted(families.values(),key=lambda r:(-r['observed_variants'],r['family']))
    report={'unique_nonempty_lines':len(lines),'physical_lines':len(raw),'categories':dict(categories),'candidate_families':len(output),'full_server_coverage':False,'no_ordinary_player_chat_dump':True,'identity_hover_lines_not_exported':True,'candidate_is_not_confirmed_translation_failure':True,'exact_matching_is_not_runtime_display_verification':True}
    (S/'DISPLAY_0.15_AUDIT.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (S/'DISPLAY_0.15_CANDIDATES.json').write_text(json.dumps(output,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False))

if __name__=='__main__':main()
