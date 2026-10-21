"""Review/compile one large fixed-source NPC batch. No machine translation or game network calls."""
import argparse,json,re,urllib.parse,urllib.request
from datetime import datetime
from collections import Counter,defaultdict
from pathlib import Path
from prepare_npc78 import anchors,validate,rows,sha,write
ROOT=Path(__file__).resolve().parents[1];SOURCE=ROOT/'translation-source';DATA=ROOT/'src/main/resources/data/witherskyru'
SPEAKERS={'Barry':61,'Tia the Fairy':39,'George':43,'Fann':41,'Alchemist':31,'Jerry':33,'Beth':46,'Jake':58,'Archaeologist':38,'Carrolyn':27,'Moby':42,'Seraphine':38,'Viking':37,'Shaggy':33,'Teddy':42,'Brarnas':38,'Damia':42}
CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
def body(r):return r['source'].split(': ',1)[1]
def structured(r):return r['variable_definition'] or bool(re.search(r'<[^>]+>|\{[^}]+\}|\[[^]]+\]|#',body(r)))
def neutral(r):return not re.search('[A-Za-z]',body(r))
def unsafe(r):return any(r[k] for k in ('historical_heading','alpha_page','unresolved_markup','nonstandard_tag_layout'))
def main():
    p=argparse.ArgumentParser();p.add_argument('--inspect');p.add_argument('--start',type=int,default=0);p.add_argument('--limit',type=int,default=100);p.add_argument('--validate-only',action='store_true');a=p.parse_args()
    queue=rows(CORPUS/'translation_queue.jsonl');known=defaultdict(list)
    for path in sorted(DATA.glob('*.json')):
        if path.name=='npc-expansion85.json':continue
        for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
            if r.get('source') and r.get('translation'):known[r['source']].append(r)
    selected=[r for r in queue if r['speaker'] in SPEAKERS];eligible=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and not unsafe(r)]
    assert Counter(r['speaker'] for r in eligible)==SPEAKERS
    if a.inspect:
        items=[r for r in eligible if r['speaker']==a.inspect]
        for index,r in enumerate(items[a.start:a.start+a.limit],a.start):print(str(index)+' | '+r['source_legacy_line']+' | '+' / '.join(r['headings']))
        return
    lookup={r['source']:r for r in eligible};records=[];authored=[];seen=set()
    for name in SPEAKERS:
        path=SOURCE/('NPC85_'+re.sub('[^A-Z0-9]+','_',name.upper())+'_REVIEWED.tsv')
        if not path.exists():assert a.validate_only,path;continue
        for line in path.read_text(encoding='utf-8-sig').splitlines():
            if not line or line.startswith('#'):continue
            f=line.split('|');assert len(f) in (3,4),line
            speaker,english,russian=f[:3];source=f'[NPC] {speaker}: {english}';target=f'[NPC] {speaker}: {russian}'
            assert speaker==name and source in lookup and source not in seen,(path,source);seen.add(source)
            assert re.findall('[0-9]+',english)==re.findall('[0-9]+',russian),source
            allowed={'SkyBlock','Hypixel','Abiphone','MVP','VIP'}
            if '9f™' in english:allowed.add('f') # Published network identifier, not English prose.
            if speaker=='Brarnas' and 'XsimulationX' in english:allowed.add('X') # Two obfuscated boundary characters.
            for token in re.findall('[A-Za-z]+',russian):assert token in allowed and (token in re.findall('[A-Za-z]+',english) or token=='X' and speaker=='Brarnas' and 'XsimulationX' in english),(source,token)
            definitions=anchors(f[3]) if len(f)==4 else [];validate(source,target,definitions)
            records.append({'source_key':f'npc85.{len(records)}','speaker':speaker,'tag':'NPC','source':source,'translation':target,'style_anchors':definitions,'source_url':lookup[source]['source_url'],'evidence':'public_wiki_revision_not_live_packet'});authored.append(line)
    print('Validated authored',len(seen),'of',len(eligible),dict(Counter(r['speaker'] for r in records)))
    if a.validate_only:return
    assert seen==lookup.keys() and len(records)==689
    (SOURCE/'NPC_0.15.85_REVIEWED.tsv').write_text('# Combined independently authored batch; edit individual NPC85_* sources.\n'+'\n'.join(authored)+'\n',encoding='utf-8')
    write(DATA/'npc-expansion85.json',{'format_version':1,'language':'ru','scope':'689 authored literals across17 speaker labels. Public published branches, not private/live or whole-game completeness. Previously translated, neutral, variable and unsafe rows separately retained.','records':records})
    occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')};ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
    blocks=[b for filename in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/filename) if b['block_id'] in ids];assert {b['block_id'] for b in blocks}==ids
    titles={r['page'] for r in selected};pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in titles];expected={r['title']:r['revision'] for r in pages}
    response_pages=[];urls=[]
    for start in range(0,len(titles),30):
        url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(sorted(titles)[start:start+30]),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'});urls.append(url)
        with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-local-review/0.15.85 (read-only)'}),timeout=45) as stream:response_pages.extend(json.load(stream)['query']['pages'])
    actual={r['title']:r['revisions'][0]['revid'] for r in response_pages};matches=actual==expected
    write(SOURCE/'NPC_0.15.85_PUBLIC_REVISION.json',{'checked_client_date':datetime.now().date().isoformat(),'checked_at':datetime.now().astimezone().isoformat(),'api_urls':urls,'matches_pinned_corpus':matches,'private_or_unpublished_dialogue_known':False,'pages':response_pages,'expected':expected,'differences':{k:{'pinned':expected.get(k),'current':actual.get(k)} for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)}})
    if not matches:
        # Individually reviewed exact change: removal of a stub header only. No generic delta bypass.
        differences={k:(expected.get(k),actual.get(k)) for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)}
        assert differences=={'Cult of the Fallen Star':(843725,859622)},differences
        delta=json.loads((SOURCE/'NPC_0.15.85_REVISION_DELTA.json').read_text(encoding='utf-8'))['records'];assert len(delta)==1
        reviewed=delta[0];assert reviewed['title']=='Cult of the Fallen Star' and reviewed['current_revision']==859622
        fresh=reviewed['current_page']['revisions'][0]['slots']['main']['content'];old=next(r['raw_wikitext'] for r in pages if r['title']==reviewed['title'])
        removed='{{Stub|Need to add the countdown on [[Events#Timers]] {{TODOstate|no}}}}\n\n'
        assert old.startswith(removed) and fresh==old[len(removed):],'Changed dialogue or any additional edit requires new review'
        verified=json.loads((SOURCE/'NPC_0.15.85_PUBLIC_REVISION.json').read_text(encoding='utf-8'));verified['dialogue_unchanged_after_exact_manual_delta_review']=True;verified['delta_review']='Cult of the Fallen Star843725→859622: exact removal of a leading stub notice; every remaining character unchanged';write(SOURCE/'NPC_0.15.85_PUBLIC_REVISION.json',verified)
    retained=[r for r in selected if r['source'] in known];pending=[r for r in selected if r['source'] not in known and structured(r)];neutrals=[r for r in selected if r['source'] not in known and not structured(r) and neutral(r)];excluded=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and unsafe(r)]
    assert len(selected)==len(seen)+len(retained)+len(pending)+len(neutrals)+len(excluded)
    write(SOURCE/'NPC_SOURCES_0.15.85.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'source_pages':pages,'blocks':blocks,'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained],'pending_variables':pending,'neutral_unchanged':neutrals,'unsafe_excluded':excluded})
    before=sum(r['source'] in known for r in queue);assert before==3298
    write(SOURCE/'NPC_PROGRESS_0.15.85.json',{'corpus_unique_rows':len(queue),'exact_resource_before':before,'exact_resource_after':before+len(seen),'after_percent':100*(before+len(seen))/len(queue),'without_confirmed_exact_key':len(queue)-before-len(seen),'new_reviewed_literal_rows':len(seen),'new_by_speaker':SPEAKERS,'published_branch_rows':len(selected),'structured_sources_not_registered':len(pending),'neutral_unchanged_rows':len(neutrals),'unsafe_excluded_rows':len(excluded),'retained_keys':len(retained),'runtime_validation':'pending','new_resource_sha256':sha(DATA/'npc-expansion85.json'),'installed_game_modified':False,'game_fps_measured':False})
    print('Prepared',len(records),'new;',len(selected),'total;',len(pending),'structured;',len(neutrals),'neutral;',len(excluded),'excluded;',len(retained),'retained')
if __name__=='__main__':main()
