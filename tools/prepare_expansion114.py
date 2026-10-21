"""Pinned114 screening;never treat extraction markers or placeholders as live literals."""
import json,re
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from prepare_npc113 import CORPUS
BASE='4350073540D2C94A291C9D8B9390D9BFA731C2F26B337487741AB5D10768A1BE'

def probe():
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    work=rows(SOURCE/'NPC_WORKLIST_0.15.113.jsonl');occ={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')};out=[]
    for index,r in enumerate(work):
        s=r['source'];tag=r['tag'] or 'SYSTEM'
        if r['exact_registration'] or any(r[k] for k in ('variable_definition','unresolved_markup','alpha_page')) or tag not in ('NPC','SYSTEM','BOSS'):continue
        p=s.find(': ')+2 if tag in ('NPC','BOSS') else 0
        if tag in ('NPC','BOSS') and (p<2 or not s.startswith('['+tag+'] '+r['speaker']+': ')):continue
        body=s[p:];guarded=body.replace('[OPEN MENU]','').replace('[Vargul]','').replace('[Lvl 1]','')
        if not re.search('[A-Za-z]{2}',body) or re.search(r'[#$<>{}\[\]|]',guarded) or body.endswith('/'):continue
        layouts=list(dict.fromkeys(occ[k]['source_legacy_line'].strip() for k in r['occurrence_ids']))
        if not layouts or not all(re.sub('(?i)§[0-9a-fk-or]','',v)==s for v in layouts):continue
        out.append({'id':str(index),'source':s,'speaker':r['speaker'] if tag in ('NPC','BOSS') else '', 'tag':tag,'source_url':r['source_url'],'published_row':r,'all_occurrences':[occ[k] for k in r['occurrence_ids']],'layouts':layouts})
    write(SOURCE/'EXPANSION_PROBE_0.15.114.json',{'scope':'Only literal candidates;real113 dispatch and manual full-block/style review still required. Not coverage.','records':out})
    (SOURCE/'EXPANSION_PROBE_0.15.114.txt').write_text('\n'.join(f"{r['id']}|{r['tag']}|{'ARCHIVE' if r['published_row']['historical_heading'] else ''}|{r['source_url'].split('/w/')[-1]}|{r['source']}" for r in out)+'\n',encoding='utf-8')
    print('Probed',len(out),'NPC',sum(r['tag']=='NPC' for r in out),'BOSS',sum(r['tag']=='BOSS' for r in out))

if __name__=='__main__':probe()
