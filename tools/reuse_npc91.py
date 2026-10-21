"""Exact body-pair copy only, after reading all recipient conditions; not translation."""
import json,re
from prepare_npc91 import SOURCE,CORPUS,SPEAKERS,rows,sha,write
DONORS={'NPC89_BRUUH_REVIEWED.tsv':['Nuvian','Plenk','An','Yoink','Barbarian Emissary','Chihai','Jine'],'NPC91_ARBADAK_REVIEWED.tsv':['Colore','Mazakala'],'NPC90_DUST_REVIEWED.tsv':['Creed']}
q=rows(SOURCE/'NPC_WORKLIST_0.15.90.jsonl');proof=[]
existing=SOURCE/'NPC_SHARED_REUSE_0.15.91.json'
if existing.exists():
 saved=json.loads(existing.read_text(encoding='utf-8-sig'));assert saved['count']==len(saved['records'])==147
 for r in saved['records']:
  assert sha(SOURCE/r['reviewed_target_source_file'])==r['reviewed_file_sha256']
  name=r['source'].split(': ',1)[0][6:];target=SOURCE/('NPC91_'+re.sub('[^A-Z0-9]+','_',name.upper())+'_REVIEWED.tsv')
  assert r['authored_tsv_row'] in target.read_text(encoding='utf-8-sig').splitlines()
 print('Verified147 existing exact copies;no files changed');raise SystemExit(0)
for donor,names in DONORS.items():
 path=SOURCE/donor;original=[s for s in path.read_text(encoding='utf-8-sig').splitlines() if s and not s.startswith('#')];pairs={s.split('|')[1]:s.split('|') for s in original}
 for name in names:
  target=SOURCE/('NPC91_'+re.sub('[^A-Z0-9]+','_',name.upper())+'_REVIEWED.tsv');current=target.read_text(encoding='utf-8-sig');present={s.split('|')[1] for s in current.splitlines() if s and not s.startswith('#')};added=[]
  for r in q:
   if r['speaker']!=name or r['exact_registration']:continue
   body=r['source'].split(': ',1)[1]
   if body not in pairs or body in present:continue
   f=[name]+pairs[body][1:];line='|'.join(f);added.append(line);proof.append({'source':r['source'],'authored_tsv_row':line,'reviewed_target_source_file':donor,'reviewed_file_sha256':sha(path),'occurrence_ids':r['occurrence_ids'],'automatic_translation':False});present.add(body)
  target.write_text(current+'\n'.join(added)+'\n',encoding='utf-8');print(name,len(added))
assert len(proof)==147,len(proof)
write(SOURCE/'NPC_SHARED_REUSE_0.15.91.json',{'scope':'147 exact reviewed source/body pairs;recipient conditions read individually;no approximate or automatic translation','count':len(proof),'records':proof})
