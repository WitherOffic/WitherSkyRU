"""Source-only data audit, before comparison, and privacy-safe audit exports."""
from pathlib import Path
from collections import Counter
import json,re,zipfile
from audit_unknown_display_0_15 import excluded

P=Path(__file__).resolve().parents[1];S=P/'translation-source'
def load(p): return json.loads(p.read_text(encoding='utf-8'))
def number(s): return Counter(re.findall(r'\d+(?:[.,]\d+)*',re.sub(r'\{[^}]+\}','',s)))
def icons(s): return Counter(re.findall(r'[\ue000-\uf8ff]',s))
def params(s): return Counter(re.findall(r'\{[^}]+\}',s))
def main():
    now=load(S/'translations.json')['translations']
    before=load(S/'TRANSLATIONS_0.14.4_HISTORY.json')['translations']
    changed={k:v for k,v in now.items() if k not in before or v!=before[k]}
    numbered=[]; icon_errors=[]; placeholder_errors=[]
    for k,v in changed.items():
        if number(k)!=number(v): numbered.append({'source':k,'translation':v,'missing':dict(number(k)-number(v)),'added':dict(number(v)-number(k))})
        if icons(k)!=icons(v): icon_errors.append(k)
        if params(k)!=params(v): placeholder_errors.append(k)
    items=load(S/'item-context.json');repeat={}
    for item in items:
        for a,b in zip(item['lore'],item['lore'][1:]):
            x,y=now.get(a.strip(),a),now.get(b.strip(),b)
            m,n=re.search(r'\b([А-Яа-яЁё]+)\s*$',x),re.match(r'\s*([А-Яа-яЁё]+)\b',y)
            if m and n and m[1].casefold()==n[1].casefold():repeat[(a,b)]={'source':[a,b],'translation':[x,y]}
    npc=load(S/'NPC_DIALOGUES_RU.json')['records']
    assert len({r['source'] for r in npc})==len(npc)
    assert (S/'NPC_DIALOGUES_RU.json').read_bytes()==(P/'src/main/resources/data/witherskyru/npc-dialogues.json').read_bytes()
    # Remove player-hover identities from intermediary reports before packaging.
    removed=0
    for name in ('DISPLAY_0.15_CANDIDATES_BEFORE.json','DISPLAY_0.15_CANDIDATES.json'):
        data=load(S/name);clean=[]
        for row in data:
            if excluded(row['family'])=='player_identity_hover_not_exported':removed+=1;continue
            row['samples']=[s for s in row['samples'] if excluded(s)!='player_identity_hover_not_exported']
            if row['samples']:clean.append(row)
        (S/name).write_text(json.dumps(clean,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    # Genuine 0.14.4 resources for a same-snapshot comparison, outside deliverables.
    baseline=P/'build/source-audit-0.15/before-project'
    archive=P.parents[2]/'outputs/WitherSkyRU_0.14.4-dev_MC26.1.2_project.zip'
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        for n in z.namelist():
            if (n.startswith('src/main/resources/') or n in {'src/main/java/ru/wither/skyru/OfflineDictionary.java','src/main/java/ru/wither/skyru/TranslationGrammar.java'}) and not n.endswith('/'):
                target=baseline/n;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(n))
        java_unchanged=[];java_changed=[]
        for f in (P/'src/main/java').rglob('*.java'):
            key=f.relative_to(P).as_posix()
            if z.read(key)==f.read_bytes():java_unchanged.append(key)
            else:java_changed.append(key)
    result={'revision':'0.15.0-dev','previous_targets_updated':sum(k in before for k in changed),'runtime_keys_added':sum(k not in before for k in changed),'runtime_entries':len(now),'new_or_changed_pairs':len(changed),'new_numeric_candidates':numbered,'new_icon_mismatches':icon_errors,'new_placeholder_mismatches':placeholder_errors,'item_records_machine_scanned':len(items),'adjacent_repeat_candidates':list(repeat.values()),'npc_total':len(npc),'production_java_files_unchanged':len(java_unchanged),'production_java_changed':java_changed,'private_hover_families_removed_from_intermediary_exports':removed,'in_game_tested':False,'full_manual_proofread':False,'full_server_coverage':False}
    (S/'DATA_0.15_AUDIT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result,ensure_ascii=False))
    assert not icon_errors and not placeholder_errors
if __name__=='__main__':main()
