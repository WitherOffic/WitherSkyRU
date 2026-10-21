"""Close a finite recent-changes window without overwriting the frozen116 dump."""
import argparse,json,urllib.parse
from pathlib import Path
from datetime import datetime,timezone
from collections import Counter
from export_public_text116 import ROOT,Capture,NAMESPACES,rows,jsonl,write,page_record,fetch,digest,extract
from audit_five_pass117 import PUBLIC

def main():
 ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);a=ap.parse_args();root=a.root;out=root/'wiki-delta';out.mkdir(exist_ok=True)
 scopefile=out/'WINDOW.json'
 if scopefile.exists():window=json.loads(scopefile.read_text(encoding='utf-8'))
 else:
  first=min(json.loads(p.read_text(encoding='utf-8'))['retrieved_utc'] for p in (PUBLIC/'requests').glob('*.json'))
  window={'start':first[:19]+'Z','end':datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ'),'scope':'Public edit/new/log entries in same8 namespaces,not a fresh atomic allpages inventory.'};write(scopefile,window)
 capture=Capture(out);changes=[];continuation={};requests=0
 while True:
  d=capture.query(list='recentchanges',rcstart=window['start'],rcend=window['end'],rcdir='newer',rclimit='500',rcnamespace='|'.join(str(n) for n in NAMESPACES),rctype='edit|new|log',rcprop='title|ids|timestamp|loginfo',**continuation);requests+=1;changes+=d['query']['recentchanges']
  if 'continue' not in d:break
  continuation=d['continue'];assert requests<100,'Cannot claim complete after safety limit'
 jsonl(out/'RECENT_CHANGES.jsonl',changes)
 old={p['pageid']:p for p in rows(PUBLIC/'source_pages.jsonl')};ids=sorted({c['pageid'] for c in changes if c.get('pageid',0)>0});saved=[];missing=[]
 for i in range(0,len(ids),40):
  for p in fetch(capture,pageids='|'.join(map(str,ids[i:i+40]))):
   if p.get('missing') or not p.get('revisions'):missing.append(p);continue
   saved.append(page_record(p))
 new=[p for p in saved if p['pageid'] not in old];changed=[p for p in saved if p['pageid'] in old and p['raw_sha256']!=old[p['pageid']]['raw_sha256']]
 jsonl(out/'source_pages.jsonl',saved);write(out/'UNAVAILABLE_PAGES.json',missing);extract(out)
 master={r['source']:r for r in rows(root/'MASTER_REVIEW_QUEUE.jsonl')};before=len(master);known=json.loads((root/'EXACT_WORKTREE_REFERENCE.json').read_text(encoding='utf-8'));new_candidates=[]
 for r in rows(out/'candidate_index.jsonl'):
  s=r['source']
  if s in known:continue
  if s not in master:master[s]={'source':s,'translation_ru':'','origins':[],'kinds':[],'status':'recent_public_edit_context_and_runtime_review_required','automatically_added_to_mod':False};new_candidates.append(s)
  entry=master[s];entry['origins'].append({'delta_source_candidate_ids':r['occurrence_ids'],'source_window':window})
  if 'recent_wiki_delta_candidate' not in entry['kinds']:entry['kinds'].append('recent_wiki_delta_candidate')
 jsonl(root/'MASTER_REVIEW_QUEUE.jsonl',(master[s] for s in sorted(master)))
 result={'window':window,'continuation_exhausted':True,'requests':requests,'public_change_entries':len(changes),'affected_public_pageids':len(ids),'saved_affected_pages':len(saved),'unavailable_after_edit_log':len(missing),'new_pageids_not_in116':[p['title'] for p in new],'changed_raw_pages':[p['title'] for p in changed],'unregistered_candidate_sources_from_delta':sum(r['source'] not in known for r in rows(out/'candidate_index.jsonl')),'new_unique_review_candidates':len(new_candidates),'master_before':before,'master_after':len(master),'not_an_atomic_current_server_dump':True}
 write(root/'WIKI_DELTA117.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
