"""Static translation integrity only. No game, Java, Fabric or server launch."""
from collections import Counter
import json
from pathlib import Path
import re

PROJECT=Path(__file__).resolve().parents[1]
SOURCE=PROJECT/'translation-source'
DATA=PROJECT/'src/main/resources/data/witherskyru'

def main():
    previous=json.loads((SOURCE/'TRANSLATIONS_0.11.0_HISTORY.json').read_text(encoding='utf-8'))['translations']
    old=json.loads((SOURCE/'COVERAGE_0.11.0_HISTORY.json').read_text(encoding='utf-8'))
    coverage=json.loads((SOURCE/'coverage.json').read_text(encoding='utf-8'))
    records=[json.loads(s) for s in (SOURCE/'translation-provenance.jsonl').read_text(encoding='utf-8').splitlines()]
    current={r['source']:r['translation'] for r in records}
    corpus={json.loads(s)['source'] for s in (SOURCE/'corpus.jsonl').read_text(encoding='utf-8').splitlines()}
    new=[r for r in records if r['source'] not in previous or previous[r['source']]==r['source']]
    revised=[r for r in records if r['source'] in previous and previous[r['source']]!=r['translation']]
    regressions=[s for s in corpus if s in previous and previous[s]!=s and s not in current]
    hazards=[]
    abbreviation_expansions=[]
    for row in new+revised:
        for label,regex in [('number',r'\d+(?:[,.]\d+)*'),('roman',r'\b[IVXLCDM]+\b'),
                            ('icon',r'[\uE000-\uE1FF◆❤☯Ⓑ✓✗▶]'),('placeholder',r'\{[^}]+\}'),
                            ('shard ID',r'\b[CURLE]\d+\b')]:
            source,target=row['source'],row['translation']
            if label=='roman' and source.startswith('❤ Health (MM) : '):
                assert target.startswith('❤ Здоровье (мастер-режим) : ')
                source=source.replace('(MM)','(master mode)',1)
                abbreviation_expansions.append(row['source'])
            if re.findall(regex,source)!=re.findall(regex,target):
                hazards.append({'source':row['source'],'translation':row['translation'],'reason':label})
    malformed,duplicates=[],{}
    files={DATA/'continuation.tsv':2,DATA/'continuation-patterns.tsv':2,DATA/'lexicon.tsv':4}
    files.update({p:3 if p.name.endswith('LOCATIONS.tsv') else 2 for p in SOURCE.glob('BATCH_0.12_*.tsv')})
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
    report={'revision':'0.12.0-dev','scope':'Static data integrity, not runtime or universal quality verification',
            'new_snapshot_sources':len(new),'previous_targets_updated':len(revised),
            'delta_reported':coverage['translated_or_partially_translated_snapshot_strings']-old['translated_or_partially_translated_snapshot_strings'],
            'numeric_roman_icon_placeholder_id_mismatches':hazards,'malformed_rows':malformed,'duplicate_keys':duplicates,
            'regressed_previously_processed_sources':regressions,'context_guards':{s:current.get(s) for s in guards},
            'updated_previous_sources':revised,'non_level_abbreviation_MM_expanded':abbreviation_expansions,
            'quality_verified':False,'runtime_verified':False}
    (SOURCE/'BATCH_0.12_DATA_CHECKS.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    assert len(new)==report['delta_reported'] and len(new)>=2000
    assert not hazards and not malformed and not regressions, json.dumps(report,ensure_ascii=False)
    assert duplicates['continuation.tsv']=={'Deep Sea Diver':2,'Intelligence.':2}
    assert duplicates['lexicon.tsv']=={'Rabbit':2}
    assert all(not value for key,value in duplicates.items() if key not in {'continuation.tsv','lexicon.tsv'})
    assert all(current[s]==target for s,target in guards.items()),report['context_guards']
    print(json.dumps({'new_sources':len(new),'previous_targets_updated':len(revised),'hazards':0,'new_duplicates':0,'regressions':0},ensure_ascii=False))

if __name__=='__main__':main()
