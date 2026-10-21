"""Mechanical reuse of the pinned-corpus compiler; no automated Russian targets."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
groups={6:['Argofay Sonfather','Argofay Treemerger','Brigette','Dalbrek','Duncan','Feast Baker Scott','Grumblefoot','Gulliver','Hound','Hunter Harry','Hunter Jasper','Hunter Luigi','Hunter Reeves','Master Tactician Funk','Old Man Garry','Researcher Ribery','Sawyer','Shen Citrinitas','Skylark','Spike','Treehugger Timothy'],5:['Banker Broadjaw','Campfire Adept','Combat Merchant','Dr. Hibble','Fafnir','Gavin','Gold Forger','Krevius','Grelius','Hunter Billy','Hunter Lennard','Huntress Melissa','Ian','Raffle Box','Jade Dragon','Joey McPizza','Maddox','Mason','Mushroom Guy','Obo','Rescue Recruiter','Roy','Royal Guard','Scout Plinius','Tammy','Torrhus Canyon','Vaeri'],4:['Agnes','Albert','Alixer','Ant','Aviar','Bromm','Chunk','Curator','Ghost of Goon','Defunct Wizardman','Elmar','Eludore','Emissary Braum','Farmer Jon','Fragilis','Gnyl','Guard','Guildford','Guy','Untitled NPC','Jonesy','Karis','Lumina','Sunny','Puzzler','Researcher Beryl','Resident Snooty','Soul Campfire Initiate','Tilus','Tornora','Zarina']}
s=(ROOT/'tools/prepare_npc100.py').read_text(encoding='utf-8')
start=s.index('SPEAKERS=');end=s.index('PREVIOUS=',start)
s=s[:start]+'SPEAKERS='+repr({name:(count-1 if name=='Maddox' else count+1 if name=='Fragilis' else count) for count,names in groups.items() for name in names})+'\n'+s[end:]
s=s.replace('from prepare_npc99 import','from prepare_npc100 import').replace('neutral,unsafe','neutral,unsafe,extraction as base_extraction')
s=re.sub(r'def structured\(r\):.*?\n',"def structured(r):return base_structured(r) and r['source']!=\"[NPC] Fragilis: Y'know now that we are getting deeper and deeper a lot of these blocks are getting harder to mine!\"\n",s)
s=re.sub(r'def extraction\(r\):.*?\n','def extraction(r):return base_extraction(r)\n',s)
s=s.replace('def selection():',"def reused(r):return r['source']=='[NPC] Maddox: ✆ Someone answers!'\n\ndef selection():")
s=s.replace('def selection():',"def choice_unsafe(r):return unsafe(r) or r['source']=='Select an option: [Yeah].'\n\ndef selection():")
s=s.replace('any(not unsafe(o)','any(not choice_unsafe(o)').replace('all(unsafe(r)','all(choice_unsafe(r)').replace("'historical_excluded':", "'excluded_groups_not_runtime':")
s=s.replace('and not extraction(r)]','and not extraction(r) and not reused(r)]')
s=s.replace('malformed,neutrals,excluded)))','malformed,neutrals,excluded,reused_rows)))')
s=s.replace(" retained=[r for r in selected", " reused_rows=[r for r in selected if reused(r)];previous_outputs={r['source']:r for r in rows(AUDIT)}\n for r in reused_rows:assert previous_outputs[r['source']]['flat_result']==previous_outputs[r['source']]['published_style_result']=='[NPC] Maddox: ✆ Кто-то ответил!'\n retained=[r for r in selected")
s=s.replace("'runtime_retained':[]", "'runtime_retained':[{'source':r['source'],'published_row':r,'before_flat':previous_outputs[r['source']]['flat_result'],'before_colored':previous_outputs[r['source']]['published_style_result']} for r in reused_rows]")
s=s.replace('0.15.100','0.15.101').replace('npc-expansion100','npc-expansion101').replace('NPC100','NPC101').replace('batch100','batch101')
s=s.replace('NPC_TRANSLATION_BATCH_2026-10-19_2','NPC_TRANSLATION_BATCH_2026-10-19_3').replace('build/npc99-dispatch-6701729d7cb340cdb6caf4af097a9319/0.15.99-dev','build/npc100-dispatch-691e3ad65f7f4ed882a0eaca2b12efde/0.15.100-dev').replace('==10009','==10391').replace('after=10009+','after=10391+').replace("'exact_resource_before':10009","'exact_resource_before':10391").replace('len(known)==268','len(known)==276')
s=s.replace("  for token in re.findall", "  if source==\"[NPC] Gavin: Don't tell mum this, but I just use /scg to open it.\":allowed.add('scg')\n  for token in re.findall")
s=s.replace("assert all(r['source'] in old and not old[r['source']]['flat_changed_not_quality_proof'] and not old[r['source']]['published_style_changed_not_quality_proof'] for r in eligible)","assert all(r['source']==\"[NPC] Fragilis: Y'know now that we are getting deeper and deeper a lot of these blocks are getting harder to mine!\" or r['source'] in old and not old[r['source']]['flat_changed_not_quality_proof'] and not old[r['source']]['published_style_changed_not_quality_proof'] for r in eligible)")
(ROOT/'tools/prepare_npc101.py').write_text(s,encoding='utf-8')
print('Prepared compiler; intended',sum(c*len(ns) for c,ns in groups.items()),'literals from',sum(map(len,groups.values())),'speaker labels.')
