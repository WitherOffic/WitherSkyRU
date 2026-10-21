"""Review/compile one large fixed-source NPC batch. No machine translation or game network calls."""
import argparse,json,re,urllib.parse,urllib.request
from datetime import datetime
from collections import Counter,defaultdict
from pathlib import Path
from prepare_npc78 import anchors,validate,rows,sha,write
ROOT=Path(__file__).resolve().parents[1];SOURCE=ROOT/'translation-source';DATA=ROOT/'src/main/resources/data/witherskyru'
SPEAKERS={'Dalir':95,'Thondin':52,'Dean':87,'Ugo':80,'Igrupan':44,'Alcina':50,'Rezzus':41,'Bednom':46,'Chief Scorn':45,'Dr. Emmett':44,'Swoop':41,'Pomtair':40,'Captain Ahone':34}
RUNTIME_RETAINED={f"[NPC] Dalir: You’ve now attended {n} meetings of the Cult of the Fallen Star! As a reward, here's some Starfall!" for n in (4,5,6)}
CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
def body(r):return r['source'].split(': ',1)[1]
def structured(r):return r['variable_definition'] or bool(re.search(r'<[^>]+>|\{[^}]+\}|\[[^]]+\]|#',body(r)))
def neutral(r):return not re.search('[A-Za-z]',body(r))
def unsafe(r):return any(r[k] for k in ('historical_heading','alpha_page','unresolved_markup','nonstandard_tag_layout'))
def main():
    p=argparse.ArgumentParser();p.add_argument('--inspect');p.add_argument('--start',type=int,default=0);p.add_argument('--limit',type=int,default=100);p.add_argument('--validate-only',action='store_true');a=p.parse_args()
    queue=rows(CORPUS/'translation_queue.jsonl');known=defaultdict(list)
    for path in sorted(DATA.glob('*.json')):
        if path.name=='npc-expansion86.json':continue
        for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
            if r.get('source') and r.get('translation'):known[r['source']].append(r)
    before=sum(r['source'] in known for r in queue);assert before==3987,'Baseline guard must pass before writing any generated runtime resource'
    selected=[r for r in queue if r['speaker'] in SPEAKERS];eligible=[r for r in selected if r['source'] not in known and r['source'] not in RUNTIME_RETAINED and not structured(r) and not neutral(r) and not unsafe(r)]
    assert Counter(r['speaker'] for r in eligible)==SPEAKERS
    if a.inspect:
        items=[r for r in eligible if r['speaker']==a.inspect]
        for index,r in enumerate(items[a.start:a.start+a.limit],a.start):print(str(index)+' | '+r['source_legacy_line']+' | '+' / '.join(r['headings']))
        return
    lookup={r['source']:r for r in eligible};records=[];authored=[];seen=set()
    for name in SPEAKERS:
        path=SOURCE/('NPC86_'+re.sub('[^A-Z0-9]+','_',name.upper())+'_REVIEWED.tsv')
        if not path.exists():assert a.validate_only,path;continue
        for line in path.read_text(encoding='utf-8-sig').splitlines():
            if not line or line.startswith('#'):continue
            f=line.split('|');assert len(f) in (3,4),line
            speaker,english,russian=f[:3];source=f'[NPC] {speaker}: {english}';target=f'[NPC] {speaker}: {russian}'
            assert speaker==name and source in lookup and source not in seen,(path,source);seen.add(source)
            assert re.findall('[0-9]+',english)==re.findall('[0-9]+',russian),source
            allowed={'SkyBlock','Hypixel','Abiphone','MVP','VIP'}
            if '9f™' in english:allowed.add('f') # Published network identifier, not English prose.
            if speaker in {'Dalir','Thondin'} and 'XsimulationX' in english:allowed.add('X') # Published obfuscated boundary characters.
            for token in re.findall('[A-Za-z]+',russian):assert token in allowed and (token in re.findall('[A-Za-z]+',english) or token=='X' and speaker in {'Dalir','Thondin'} and 'XsimulationX' in english),(source,token)
            definitions=anchors(f[3]) if len(f)==4 else [];validate(source,target,definitions)
            records.append({'source_key':f'npc86.{len(records)}','speaker':speaker,'tag':'NPC','source':source,'translation':target,'style_anchors':definitions,'source_url':lookup[source]['source_url'],'evidence':'public_wiki_revision_not_live_packet'});authored.append(line)
    print('Validated authored',len(seen),'of',len(eligible),dict(Counter(r['speaker'] for r in records)))
    if a.validate_only:return
    assert seen==lookup.keys() and len(records)==699
    recipe_pairs=[line.split('\t') for line in (SOURCE/'NPC86_RECIPE_NAMES.tsv').read_text(encoding='utf-8-sig').splitlines() if line and not line.startswith('#')];assert len(recipe_pairs)==len({pair[0] for pair in recipe_pairs})==21
    corrections=(DATA/'corrections.tsv').read_text(encoding='utf-8-sig').splitlines();canonical=json.loads((SOURCE/'translations.json').read_text(encoding='utf-8-sig'))['translations']
    assert corrections[-21:]==['\t'.join(pair) for pair in recipe_pairs]
    for english,russian in recipe_pairs:
        assert canonical[english]==russian
        linked=[r for r in records if r['speaker']=='Alcina' and english in r['source']];assert len(linked)==2 and all(russian in r['translation'] for r in linked)
    write(SOURCE/'NPC_RECIPE_SOURCES_0.15.86.json',{'scope':'21 separately counted exact item-name translations matched to both authored Alcina dialogue variants; not entire item lore coverage','records':[{'source':english,'translation':russian,'dialogue_sources':[r['source'] for r in records if r['speaker']=='Alcina' and english in r['source']]} for english,russian in recipe_pairs]})
    (SOURCE/'NPC_0.15.86_REVIEWED.tsv').write_text('# Combined independently authored batch; edit individual NPC86_* sources.\n'+'\n'.join(authored)+'\n',encoding='utf-8')
    write(DATA/'npc-expansion86.json',{'format_version':1,'language':'ru','scope':'699 authored literals across13 speaker labels. Three existing dynamic results retained without redundant unreachable keys. Public publication, not private/live or whole-game completeness. Neutral and structural rows separately counted.','records':records})
    occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')};ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
    blocks=[b for filename in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/filename) if b['block_id'] in ids];assert {b['block_id'] for b in blocks}==ids
    groups=defaultdict(list)
    for o in occurrences.values():
        if o['block_id'] in ids and o['source'].startswith('Select an option:'):groups[tuple(re.findall(r'\[([^][]+)\]',o['source']))].append(o)
    choices=[];seen_choices=set()
    for line in (SOURCE/'NPC_CHOICES_0.15.86_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):continue
        source,target=map(json.loads,line.split('|'));key=tuple(source);assert key in groups and key not in seen_choices and len(source)==len(target)
        seen_choices.add(key);choices.append({'source_labels':source,'target_labels':target,'all_occurrences':groups[key]})
    assert len(choices)==10 and sum(len(r['source_labels']) for r in choices)==12
    assert set(groups)-seen_choices=={('YES','NO'),('Yes','No'),()},'Any new public choice shape must be reviewed explicitly'
    write(SOURCE/'NPC_CHOICES_SOURCES_0.15.86.json',{'scope':'10 authored exact ordered groups/12 label slots/11 unique labels. Two old yes-no groups retained;one heading-only publication is not invented as buttons.','records':choices,'retained_groups':[{'source_labels':list(key),'all_occurrences':groups[key]} for key in [('YES','NO'),('Yes','No')]],'heading_only_excluded':groups[()],'blocks':blocks})
    print('Choice layouts:',sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in choices))
    titles={r['page'] for r in selected};pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in titles];expected={r['title']:r['revision'] for r in pages}
    response_pages=[];urls=[]
    for start in range(0,len(titles),30):
        url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(sorted(titles)[start:start+30]),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'});urls.append(url)
        with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-local-review/0.15.86 (read-only)'}),timeout=45) as stream:response_pages.extend(json.load(stream)['query']['pages'])
    actual={r['title']:r['revisions'][0]['revid'] for r in response_pages};matches=actual==expected
    write(SOURCE/'NPC_0.15.86_PUBLIC_REVISION.json',{'checked_client_date':datetime.now().date().isoformat(),'checked_at':datetime.now().astimezone().isoformat(),'api_urls':urls,'matches_pinned_corpus':matches,'private_or_unpublished_dialogue_known':False,'pages':response_pages,'expected':expected,'differences':{k:{'pinned':expected.get(k),'current':actual.get(k)} for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)}})
    if not matches:
        # Individually reviewed exact change: removal of a stub header only. No generic delta bypass.
        differences={k:(expected.get(k),actual.get(k)) for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)}
        assert differences=={'Cult of the Fallen Star':(843725,859622)},differences
        delta=json.loads((SOURCE/'NPC_0.15.86_REVISION_DELTA.json').read_text(encoding='utf-8'))['records'];assert len(delta)==1
        reviewed=delta[0];assert reviewed['title']=='Cult of the Fallen Star' and reviewed['current_revision']==859622
        fresh=reviewed['current_page']['revisions'][0]['slots']['main']['content'];old=next(r['raw_wikitext'] for r in pages if r['title']==reviewed['title'])
        removed='{{Stub|Need to add the countdown on [[Events#Timers]] {{TODOstate|no}}}}\n\n'
        assert old.startswith(removed) and fresh==old[len(removed):],'Changed dialogue or any additional edit requires new review'
        verified=json.loads((SOURCE/'NPC_0.15.86_PUBLIC_REVISION.json').read_text(encoding='utf-8'));verified['dialogue_unchanged_after_exact_manual_delta_review']=True;verified['delta_review']='Cult of the Fallen Star843725→859622: exact removal of a leading stub notice; every remaining character unchanged';write(SOURCE/'NPC_0.15.86_PUBLIC_REVISION.json',verified)
    retained=[r for r in selected if r['source'] in known];pending=[r for r in selected if r['source'] not in known and structured(r)];neutrals=[r for r in selected if r['source'] not in known and not structured(r) and neutral(r)];excluded=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and unsafe(r)]
    baseline={r['source']:r for r in rows(ROOT/'build/npc86-dispatch-13a90149072a4c378d93735ff9863dab/0.15.85-dev/DISPATCH_ROWS.jsonl')};runtime_retained=[r for r in selected if r['source'] in RUNTIME_RETAINED]
    assert len(runtime_retained)==3
    for r in runtime_retained:assert baseline[r['source']]['flat_changed_not_quality_proof'] and baseline[r['source']]['published_style_changed_not_quality_proof']
    assert len(selected)==len(seen)+len(retained)+len(pending)+len(neutrals)+len(excluded)+len(runtime_retained)
    write(SOURCE/'NPC_SOURCES_0.15.86.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'source_pages':pages,'blocks':blocks,'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained],'pending_variables':pending,'neutral_unchanged':neutrals,'unsafe_excluded':excluded,'runtime_retained':[{'source':r['source'],'published_row':r,'before_flat':baseline[r['source']]['flat_result'],'before_colored':baseline[r['source']]['published_style_result']} for r in runtime_retained]})
    before=sum(r['source'] in known for r in queue);assert before==3987
    write(SOURCE/'NPC_PROGRESS_0.15.86.json',{'corpus_unique_rows':len(queue),'exact_resource_before':before,'exact_resource_after':before+len(seen),'after_percent':100*(before+len(seen))/len(queue),'without_confirmed_exact_key':len(queue)-before-len(seen),'new_reviewed_literal_rows':len(seen),'new_by_speaker':SPEAKERS,'published_branch_rows':len(selected),'structured_sources_not_registered':len(pending),'neutral_unchanged_rows':len(neutrals),'unsafe_excluded_rows':len(excluded),'retained_keys':len(retained),'retained_dynamic_results':3,'runtime_validation':'pending','new_resource_sha256':sha(DATA/'npc-expansion86.json'),'additional_exact_recipe_names':21,'installed_game_modified':False,'game_fps_measured':False})
    print('Prepared',len(records),'new;',len(selected),'total;',len(pending),'structured;',len(neutrals),'neutral;',len(excluded),'excluded;',len(retained),'retained')
if __name__=='__main__':main()
