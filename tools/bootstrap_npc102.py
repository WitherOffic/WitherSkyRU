"""Freeze the next literal NPC selection, preserving malformed/variable evidence separately."""
import sys,json,re
from collections import Counter
from pathlib import Path
import prepare_npc101 as prior
ROOT=prior.ROOT
known={r['source'] for f in prior.DATA.glob('*.json') if f.name!='npc-expansion102.json' for r in json.loads(f.read_text(encoding='utf-8-sig')).get('records',[]) if r.get('translation') and r.get('source')}
q=prior.rows(prior.CORPUS/'translation_queue.jsonl')
initial=[r for r in q if r['source'] not in known and r['tag']=='NPC' and ': ' in r['source'] and not prior.structured(r) and not prior.neutral(r) and not prior.unsafe(r) and not prior.extraction(r) and not prior.reused(r)]
old={r['source']:r for r in prior.rows(ROOT/'build/npc101-dispatch-86979e7334cd487da5d0ec67b5547c34/0.15.101-dev/DISPATCH_ROWS.jsonl')}
extra_structured={r['source'] for r in initial if r['speaker']=='[Eye]' or r['speaker'] in {'Carnival Leader','Pesthunter Phillip','Phillip'} and re.search(r'\b[Pp]layer\b',r['source'])}
malformed={r['source'] for r in initial if r['speaker'] in {'Brynmor','Generow','Frosty'} or r['source']=='[NPC] Mayor Nancy: Hi|'}
neutrals={'[NPC] Dante Goon: o7'}
reused={r['source']:old[r['source']] for r in initial if r['source'] not in extra_structured|malformed|neutrals and r['source'] in old and (old[r['source']]['flat_changed_not_quality_proof'] or old[r['source']]['published_style_changed_not_quality_proof'])}
eligible=[r for r in initial if r['source'] not in extra_structured|malformed|neutrals|reused.keys()]
s=(ROOT/'tools/prepare_npc101.py').read_text(encoding='utf-8')
start=s.index('SPEAKERS=');end=s.index('def selection():')
header="SPEAKERS="+repr(dict(Counter(r['speaker'] for r in eligible)))+"\nALL_SPEAKERS="+repr(sorted({r['speaker'] for r in initial}))+"\n"
header+="PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-19_4')\nAUDIT=ROOT/'build/npc101-dispatch-86979e7334cd487da5d0ec67b5547c34/0.15.101-dev/DISPATCH_ROWS.jsonl'\n"
header+='EXTRA_STRUCTURED='+repr(extra_structured)+'\nMALFORMED='+repr(malformed)+'\nNEUTRALS='+repr(neutrals)+'\nREUSED='+repr({k:{f:v[f] for f in ('flat_result','published_style_result')} for k,v in reused.items()})+'\n'
header+="def structured(r):return base_structured(r) or r['source'] in EXTRA_STRUCTURED\ndef extraction(r):return base_extraction(r) or r['source'] in MALFORMED\ndef neutral(r):return base_neutral(r) or r['source'] in NEUTRALS\ndef reused(r):return r['source'] in REUSED\ndef choice_unsafe(r):return unsafe(r) or r['source']=='Select an option: [Yeah].'\n\n"
s=s[:start]+header+s[end:]
s=s.replace('from prepare_npc100 import ROOT,SOURCE,DATA,CORPUS,structured as base_structured,neutral,unsafe,extraction as base_extraction','from prepare_npc101 import ROOT,SOURCE,DATA,CORPUS,structured as base_structured,neutral as base_neutral,unsafe,extraction as base_extraction')
s=s.replace('0.15.101','0.15.102').replace('npc-expansion101','npc-expansion102').replace('NPC101','NPC102').replace('batch101','batch102')
s=s.replace('build/npc101-dispatch-86979e7334cd487da5d0ec67b5547c34/0.15.102-dev','build/npc101-dispatch-86979e7334cd487da5d0ec67b5547c34/0.15.101-dev')
s=s.replace('==10391','==10776').replace('after=10391+','after=10776+').replace("'exact_resource_before':10391","'exact_resource_before':10776").replace('len(known)==276','len(known)==282').replace("r['speaker'] in SPEAKERS and","r['speaker'] in ALL_SPEAKERS and")
s=re.sub(r' assert all\(r\[\x27source\x27\]==.*? for r in eligible\)'," assert all(r['source'] in old and not old[r['source']]['flat_changed_not_quality_proof'] and not old[r['source']]['published_style_changed_not_quality_proof'] for r in eligible)",s)
s=s.replace("for r in reused_rows:assert previous_outputs[r['source']]['flat_result']==previous_outputs[r['source']]['published_style_result']=='[NPC] Maddox: ✆ Кто-то ответил!'","for r in reused_rows:assert all(previous_outputs[r['source']][k]==v for k,v in REUSED[r['source']].items())")
s=s.replace("  for token in re.findall", "  if source.startswith('[NPC] Maths Enjoyer:'):allowed|={'ln','AP'}\n  if source.startswith('[NPC] Salesman:'):allowed|={'store','hypixel','net'}\n  for token in re.findall")
(ROOT/'tools/prepare_npc102.py').write_text(s,encoding='utf-8')
print('Frozen',len(initial),'remaining candidates;',len(eligible),'new;',len(reused),'old dynamic;',len(malformed),'extraction;',len(extra_structured),'variables;',len(neutrals),'neutral.')
