"""Finite authored cosmetic labels expanded offline, never runtime grammar or API."""
import argparse,json,re
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from prepare_system107 import validate
CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
BASE='00C1829666B04B918D37682D18C9BFAD65A64650A7839EF8D4DCD42CA1DD8D00'
EXCLUDE={'Harvester_Helmet_Skin','Catgirl_Black_Cat_Skin','Baby_Spinosaurus_Skin','Cute_Jellyfish_Skin','Campfire_Power_Orb_Skin','Corrupt_Wither_Goggles_Helmet_Skin','Hungry_Flying_Fish_Skin','Ice_Cream_Cone_Power_Orb_Skin'}
def table(name):
    out={}
    for line in (SOURCE/f'SKINS_{name}_0.15.109_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        source,*target=line.split('|');assert source not in out;out[source]=target
    return out
def label(source,page,labels,modifiers,nouns):
    if source in labels:
        target=labels[source][0]
        if source=='Rose' and page in ('Flower_Crow_Skin','Petal_Rose_Dragon_Skin'):return 'Роза'
        if page in ('Canned_Hermit_Crab_Skin','Gummy_Worm_Scatha_Skin'):
            if source=='Orange':return 'Апельсин'
            if source=='Lime':return 'Лайм'
        if page=='Biome_Frog_Skin':return {'Cherry':'Вишнёвая роща','Mushroom':'Грибные поля','Crimson':'Багровый лес','Warped':'Искажённый лес'}.get(source,target)
        return target
    if source in nouns:return nouns[source][0]
    suffix=next((n for n in nouns if source.endswith(' '+n)),None);assert suffix,source
    variant=source[:-len(suffix)-1];assert variant in modifiers,source
    noun,gender=nouns[suffix];mode,male,female=modifiers[variant];modifier=male if gender=='m' else female
    if mode=='pre':return modifier+' '+noun[0].lower()+noun[1:]
    if mode=='post':return noun+' '+modifier
    assert mode=='join';return noun+modifier
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--baseline');a=ap.parse_args()
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    queue=rows(CORPUS/'translation_queue.jsonl');previous=rows(SOURCE/'NPC_WORKLIST_0.15.108.jsonl');known={r['source'] for r in previous if r['exact_registration']};occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
    selected=[r for r in queue if r['source'] not in known and r['tag'] is None and not any(r[k] for k in ('variable_definition','unresolved_markup','historical_heading','alpha_page')) and re.fullmatch(r'.+! You swapped to the .+ skin!',r['source']) and r['source_url'].split('/w/')[-1].split('?')[0] not in EXCLUDE]
    assert len(selected)==804;labels=table('LABELS');modifiers=table('MODIFIERS');nouns=table('NOUNS');headers=table('HEADERS');candidates=[];seen=set();pairs=[]
    for r in selected:
        source=r['source'];assert source not in seen;seen.add(source);page=r['source_url'].split('/w/')[-1].split('?')[0];m=re.fullmatch(r'(.+!) You swapped to the (.+) skin!',source);head,variant=m.groups();assert head in headers
        ru_head=headers[head][0];ru_label=label(variant,page,labels,modifiers,nouns);target=f'{ru_head} Вы сменили облик на «{ru_label}»!';assert not re.search('[A-Za-z]',target)
        anchors=[{'source':head,'target':ru_head},{'source':variant,'target':ru_label}];validate(source,target,anchors);assert re.findall('[0-9]+',source)==re.findall('[0-9]+',target)
        layouts=list(dict.fromkeys(occurrences[k]['source_legacy_line'].strip() for k in r['occurrence_ids']));assert all(re.sub('(?i)§[0-9a-fk-or]','',v)==source for v in layouts)
        candidates.append({'speaker':'','tag':'SYSTEM','source':source,'translation':target,'style_anchors':anchors,'source_url':r['source_url'],'published_row':r,'all_occurrences':[occurrences[k] for k in r['occurrence_ids']],'layouts':layouts,'label_source':variant,'label_target':ru_label})
        pairs.append({'page':page,'source_label':variant,'target_label':ru_label,'source':source,'target':target})
    write(SOURCE/'SKINS_CANDIDATES_0.15.109.json',{'records':candidates,'scope':'804 authored finite variants expanded offline before runtime screening;not NPC-dialogue coverage or all cosmetics.'})
    write(SOURCE/'SKINS_LABEL_REVIEW_0.15.109.json',{'records':pairs,'scope':'Every selected concrete label/target pair;author tables and grammatical modes retained.'})
    if not a.baseline:print('804 candidates;50 pages;'+str(sum(len(r['layouts']) for r in candidates))+' publication layouts');return
    baseline=json.loads((ROOT/a.baseline).read_text(encoding='utf-8-sig'));assert baseline['runtime_jar_sha256']==BASE
    old={r['source']:r for r in baseline['records']};assert old.keys()==seen
    kept=[];retained=[]
    for r in candidates:
        prior=old[r['source']]
        if prior['flat']==r['source'] and all(v['output']==r['source'] for v in prior['layouts']):kept.append(r)
        else:retained.append({'candidate':r,'baseline':prior})
    assert len(kept)==804 and not retained
    records=[{k:r[k] for k in ('speaker','tag','source','translation','style_anchors','source_url')} for r in kept];write(DATA/'skins-expansion109.json',{'format_version':1,'language':'ru','scope':'804 exact known cosmetic-change notifications;finite authored label expansion performed offline only. Existing SYSTEM provider;no runtime grammar/API/render pass.','records':records})
    cases=[]
    for r in kept:
        s=r['source'];h,v=r['style_anchors'];primary='§6§l'+h['source']+'§r§f You swapped to the §b'+v['source']+'§f skin!'
        for kind,legacy in [('primary',primary)]+[(f'published{n}',value) for n,value in enumerate(r['layouts'])]:cases.append({'source':s,'translation':r['translation'],'legacy':legacy,'kind':kind,'style_anchors':r['style_anchors']})
    write(SOURCE/'SKINS_CASES_0.15.109.json',{'cases':cases})
    ids={o['block_id'] for r in kept for o in r['all_occurrences']};blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/name) if b['block_id'] in ids];pages=[p for p in rows(CORPUS/'source_pages.jsonl') if p['title'] in {b['page'] for b in blocks}]
    primary_pages={r['published_row']['page'] for r in kept};assert len(primary_pages)==50 and len(pages)==52
    assert {p['title'] for p in pages}-primary_pages=={'Seal Hoodie Skin','Penguin Hoodie Skin'}
    write(SOURCE/'SKINS_SOURCES_0.15.109.json',{'records':kept,'retained_not_new':retained,'source_pages':pages,'blocks':blocks,'baseline_jar_sha256':BASE,'scope':'Pinned public notifications,not NPC count,private/live scripts or every cosmetic variant.'})
    path=ROOT/'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';text=path.read_text(encoding='utf-8-sig');old='"/data/witherskyru/system-expansion107.json"}';new='"/data/witherskyru/system-expansion107.json","/data/witherskyru/skins-expansion109.json"}';assert text.count(old)==1 or text.count(new)==1
    if old in text:path.write_text(text.replace(old,new),encoding='utf-8')
    license='SKINS109 public-source attribution (CC BY-NC-SA3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\nAuthored Russian cosmetic labels/notifications;finite expansion offline only. English sources pinned below.\n\n'+'\n'.join(f"{p['title']} | revision{p['revision']} | {p['source_url']}" for p in pages)+'\n';(ROOT/'src/main/resources/META-INF/licenses/SKINS109-SOURCES.txt').write_text(license,encoding='utf-8')
    write(SOURCE/'SKINS_PREPARATION_0.15.109.json',{'new_records':804,'cases':len(cases),'published_layouts':len(cases)-804,'primary_pages':50,'attribution_pages':52,'shared_hoodie_keys':20,'exact_before':11416,'exact_after':12220,'total':16244,'without_exact_after':4024,'provider_before':148,'provider_after':952,'new_npc_keys':0})
    print(json.dumps({'records':804,'cases':len(cases),'primary_pages':50,'attribution_pages':52}))
if __name__=='__main__':main()
