"""Informational triage only; never alters the dictionary or coverage denominator."""
import json,re
from collections import Counter
from pathlib import Path
P=Path(__file__).resolve().parents[1];S=P/'translation-source'
MIRRORED={'!sepicer weiv ot kcilc-thgiR','.wohemoS','daeL','doR gnihsiF','ekaC yrutneC tfiR','elabitur.',
          'eroC enotS egrofeR','etalptsehC norI','gnirtS','gorF','hsiF ehT gorF','kcitS','knalP dooW kaO',
          'llabemilS','mrahcemiT esrevrorriM','remmaH yniT','siht si htrae no tahW','ttemme .rD','woB',
          '⸮morf emoc ti did erehw dna ,gniht','⸮tibbiR'}
BRANDS={'AquaMaster™ Hydromax','AquaMaster™ X','B.I.N.G.O.','Bob-omb','FTX 3070','Feather Client',
        'Lunar Client','Modrinth','MultiMC','Vanilla Launcher','HYPIXEL DISCORD','SkyBlock.','HydroMax.',
        'Pickson.','PlantBoy Advance','Bingonimbus 2000'}
def main():
    translations=json.loads((S/'translations.json').read_text(encoding='utf-8'))['translations']
    records=[json.loads(l) for l in (S/'corpus.jsonl').read_text(encoding='utf-8').splitlines()]
    rows=[]
    for r in records:
        t=r['source']
        if translations.get(t,t)!=t:continue
        category='ambiguous_label_needs_context_review'
        if t in MIRRORED:category='mirrored_puzzle_original_retained'
        elif t.startswith('- '):category='player_credit_names_original_retained'
        elif t in BRANDS:category='brand_original_retained'
        elif (any(c in t for c in ('\\','(?:','(?<','.*','.+','[IVX','[XIV'))
              or re.fullmatch(r'[A-Z][A-Z0-9_]+',t) or t.startswith('at.hannibal2.')
              or t in {'OreMinedEvent','a SPECIAL a','MMage OutlawM','SkeletonHorse','SiNelverfish','Dev Env'}):
            category='technical_pattern_identifier_original_retained'
        elif re.fullmatch(r'[\d,.]+(?:x\.|/[\d,.]+[MK]?)',t):category='numeric_value_no_translation_needed'
        elif r['roles']==['entity']:category='entity_identifier_requires_context_review'
        elif (r['roles']==['name'] and any(o.endswith('mobs.min.json') for o in r['origins'])):
            category='proper_name_candidate_retained_requires_review'
        rows.append({**r,'triage_category':category,'original_retained':True})
    rows.sort(key=lambda r:(r['triage_category'],r['source']))
    report={'revision':'0.14.0-dev','unchanged_sources':len(rows),'categories':dict(Counter(r['triage_category'] for r in rows)),
            'classification_is_not_quality_verification':True,'denominator_unchanged':33147,
            'all_remaining_text_translated':False,'all_npc_names_verified':False,
            'note':'Candidate names and ambiguous labels require contextual review. They are not automatically marked as translated or exempted from the denominator.',
            'records':rows}
    assert len(rows)==json.loads((S/'coverage.json').read_text(encoding='utf-8'))['remaining_snapshot_strings']
    (S/'BATCH_0.14_REMAINING_REVIEW.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (S/'REMAINING_0.14_REVIEW.tsv').write_text('category\tEnglish source\torigin\n'+''.join(r['triage_category']+'\t'+r['source']+'\t'+r['origins'][0]+'\n' for r in rows),encoding='utf-8')
    print(json.dumps({'remaining':len(rows),'categories':report['categories']},ensure_ascii=False))
if __name__=='__main__':main()
