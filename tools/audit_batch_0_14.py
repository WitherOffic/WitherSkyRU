"""Static integrity checks. Never starts Java, Minecraft or Fabric."""
from collections import Counter
import json
from pathlib import Path
import re

PROJECT=Path(__file__).resolve().parents[1]
SOURCE=PROJECT/'translation-source'
DATA=PROJECT/'src/main/resources/data/witherskyru'

def read(name):return json.loads((SOURCE/name).read_text(encoding='utf-8'))

def main():
    previous=read('TRANSLATIONS_0.13.0_HISTORY.json')['translations']
    old=read('COVERAGE_0.13.0_HISTORY.json')
    coverage=read('coverage.json')
    records=[json.loads(s) for s in (SOURCE/'translation-provenance.jsonl').read_text(encoding='utf-8').splitlines()]
    current={r['source']:r['translation'] for r in records}
    corpus={json.loads(s)['source'] for s in (SOURCE/'corpus.jsonl').read_text(encoding='utf-8').splitlines()}
    new=[r for r in records if previous.get(r['source'],r['source'])==r['source']]
    revised=[r for r in records if r['source'] in previous and previous[r['source']]!=r['translation']]
    regressions=[s for s in corpus if previous.get(s,s)!=s and s not in current]
    hazards=[]
    # Exact reviewed English pronouns/French elision/level abbreviation, not Roman ranks.
    non_levels={'Am I a parasite?':['I'],'I achieved nothing.':['I'],
                'I come from a land down under.':['I'],'I guess you have this now.':['I'],
                "I'm a special snowflake.":['I'],"I'm empty inside.":['I'],
                'Mmm, oh I lom chomlate so mumch!':['I'],"Mwah! C'est magnifique, no?":['C'],
                'Spider Slayer LVL Rewards':['LVL'],'Vampire Slayer LVL Rewards':['LVL'],
                'Wolf Slayer LVL Rewards':['LVL'],'Zombie Slayer LVL Rewards':['LVL'],
                '"Give me some Sulphur and I\'ll make a':['I'],
                'and innovative! I would even add':['I'],'better than I!':['I'],
                'how much I care for them." - End':['I'],
                'I once tried a clear pond. Too shiny.':['I'],
                "I...cam't get enoumgh of it! It's the":['I'],
                'If the salad is on top, I send it back.':['I'],
                'love, I hope this simple rose shows':['I'],
                "wand. Give me all the Sulphur and I'll":['I'],
                'whereby I conquer, but what none':['I'],'❤ Health (MM): 6 hits':['MM']}
    non_level_expansions=[]
    for row in new+revised:
        for label,regex in [('number',r'\d+(?:[,.]\d+)*'),('roman',r'\b[IVXLCDM]+\b'),
                            ('icon',r'[\uE000-\uE1FF◆❤☯Ⓑ✓✗▶]'),('placeholder',r'\{[^}]+\}'),
                            ('shard ID',r'\b[CURLE]\d+\b'),('hex color',r'#[0-9A-Fa-f]{6}\b'),
                            ('command',r'(?<!\w)/[A-Za-z][A-Za-z0-9_]*'),
                            ('internal identifier',r'\b[A-Z][A-Z0-9]*_[A-Z0-9_:]+\b')]:
            source=row['source']
            if label=='roman' and source in non_levels:
                for token in non_levels[source]:source=re.sub(r'\b'+token+r'\b','reviewed_word',source)
                non_level_expansions.append({'source':row['source'],'non_level_words':non_levels[row['source']]})
            if re.findall(regex,source)!=re.findall(regex,row['translation']):
                hazards.append({'source':row['source'],'translation':row['translation'],'reason':label})
    malformed,duplicates=[],{}
    files={DATA/'continuation.tsv':2,DATA/'continuation-patterns.tsv':2,DATA/'lexicon.tsv':4}
    files.update({p:2 for p in SOURCE.glob('BATCH_0.14_*.tsv')})
    for path,width in files.items():
        keys=[]
        for index,line in enumerate(path.read_text(encoding='utf-8').splitlines(),1):
            if not line.strip() or line.startswith('#'):continue
            row=line.split('\t')
            if len(row)!=width or not all(row):malformed.append({'file':path.name,'line':index})
            keys.append(row[0])
        duplicates[path.name]={k:v for k,v in Counter(keys).items() if v>1}
    guards={'Alligator Skin':'Шкура аллигатора','Daedalus Stick':'Палка Дедала','Power.':'аксессуаров.','Scarf':'Скарф',
            'Portal to the Murkwater Loch':'Портал к Тёмному озеру',
            'Travel Scroll to the End':'Свиток перемещения в Край',
            'Black Holes by 10%.':'чёрных дыр на 10%.',
            "minion's appearance to an":'вашего миньона на',
            'Halves your damage for 2s afterwards.':'После этого вдвое снижает ваш урон на 2с.'}
    report={'revision':'0.14.0-dev','scope':'Static integrity, not runtime or universal quality verification',
            'new_snapshot_sources':len(new),'previous_targets_updated':len(revised),
            'delta_reported':coverage['translated_or_partially_translated_snapshot_strings']-old['translated_or_partially_translated_snapshot_strings'],
            'numeric_roman_icon_placeholder_id_mismatches':hazards,'malformed_rows':malformed,'duplicate_keys':duplicates,
            'regressed_previously_processed_sources':regressions,'context_guards':{s:current.get(s) for s in guards},
            'updated_previous_sources':revised,'non_level_words_reviewed':non_level_expansions,
            'quality_verified':False,'runtime_verified':False}
    (SOURCE/'BATCH_0.14_DATA_CHECKS.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    assert len(new)==report['delta_reported'] and len(new)>=2000,(len(new),report['delta_reported'])
    assert not hazards and not malformed and not regressions,json.dumps({'hazards':hazards,'malformed':malformed,'regressions':regressions},ensure_ascii=False)
    assert duplicates['continuation.tsv']=={'Deep Sea Diver':2,'Intelligence.':2}
    assert duplicates['lexicon.tsv']=={'Rabbit':2}
    assert all(not value for key,value in duplicates.items() if key not in {'continuation.tsv','lexicon.tsv'})
    assert all(current[s]==target for s,target in guards.items()),report['context_guards']
    print(json.dumps({'new_sources':len(new),'previous_targets_updated':len(revised),'hazards':0,'new_duplicates':0,'regressions':0},ensure_ascii=False))

if __name__=='__main__':main()
