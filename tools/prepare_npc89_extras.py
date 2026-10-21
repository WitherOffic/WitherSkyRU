"""Separately audited raw repairs/new revision and explicit archival-source provenance."""
import json,re
from prepare_npc78 import rows,sha,write,anchors,validate
from prepare_npc88 import ROOT,SOURCE,DATA,CORPUS
from export_npc_corpus77 import provisional_text,COLOR

def main():
 queue=rows(CORPUS/'translation_queue.jsonl');before=sha(CORPUS/'translation_queue.jsonl')
 blocks=[b for file in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/file)]
 pages={p['title']:p for p in rows(CORPUS/'source_pages.jsonl')}
 delta=json.loads((SOURCE/'NPC_0.15.89_REVISION_DELTA.json').read_text(encoding='utf-8'))
 assert delta['accepted_for_batch89'] and delta['only_expected_replacement_of_info_needed']
 selected={'Molbert':('I.. I',"[NPC] Molbert: I.. I must have slipped by accident and then fell right into the trap! ... Yes, <hat's what happened!"),'Udel':('First, I would',"[NPC] Udel: First, I would like to apologize for this one being a little later ðan usual.")}
 records=[];evidence=[]
 for line in (SOURCE/'NPC89_EXTRAS_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
  if not line or line.startswith('#'):continue
  speaker,en,ru,plan=line.split('|');source=f'[NPC] {speaker}: {en}';target=f'[NPC] {speaker}: {ru}'
  definitions=anchors(plan);validate(source,target,definitions)
  assert source not in {r['source'] for r in queue};assert re.findall('[0-9]+',en)==re.findall('[0-9]+',ru)
  if speaker in selected:
   needle,frozen=selected[speaker]
   found=[(b,l) for b in blocks if b['page']==speaker for l in b.get('payload_raw','').splitlines() if needle in l]
   assert len(found)==1;b,raw=found[0];legacy=provisional_text(raw).strip();kind='raw_source_repair'
   assert source==COLOR.sub('',legacy) and frozen in {r['source'] for r in queue}
   original=[r for path in DATA.glob('*.json') if path.name!='npc-extras89.json' for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]) if r.get('source')==frozen]
   assert len(original)==1 and original[0]['translation']==target,'Repair must keep independently reviewed Russian target'
  else:
   assert speaker=='Pesthunter Phillip';raw="&e[NPC] &6Pesthunter Phillip&f: &fUnfortunate. The &2 Pests &faren't gonna hunt themselves!"
   assert raw in delta['new_raw_dialogues'][0];legacy=provisional_text(raw).strip();assert COLOR.sub('',legacy)==source
   text=next(r['slots']['main']['content'] for p in delta['full_revisions']['query']['pages'] for r in p['revisions'] if r['revid']==859508)
   assert '| Denying\n|{{Dialogue|\n'+raw in text
   b={'page':speaker,'revision':859508,'source_url':'https://hypixelskyblock.minecraft.wiki/w/Pesthunter_Phillip?oldid=859508','condition_context_raw':'Denying Garden visitor offer','template_raw':delta['new_raw_dialogues'][0]};frozen=None;kind='new_published_revision'
  records.append({'source_key':f'npc89.extra.{len(records)}','speaker':speaker,'tag':'NPC','source':source,'translation':target,'style_anchors':definitions,'source_url':b['source_url'],'evidence':kind+'_not_frozen_queue_key'})
  evidence.append({'source':source,'translation':target,'source_legacy_line':legacy,'raw_line':raw,'frozen_queue_source':frozen,'kind':kind,'block':b,'counted_as_frozen_corpus_exact_key':False})
 assert len(records)==3
 write(DATA/'npc-extras89.json',{'format_version':1,'language':'ru','scope':'Two raw-source repairs and one newly published denial;separate from immutable16244 denominator.','records':records})
 write(SOURCE/'NPC_EXTRAS_0.15.89.json',{'queue_sha256':before,'revision_delta_sha256':sha(SOURCE/'NPC_0.15.89_REVISION_DELTA.json'),'records':evidence})
 # Frozen heading flags miss historical templates and table-row headers. Classify by original source.
 occurrences={o['occurrence_id']:o for o in rows(CORPUS/'dialogue_occurrences.jsonl')};blockmap={b['block_id']:b for b in blocks}
 authored=json.loads((DATA/'npc-expansion89.json').read_text(encoding='utf-8'))['records'];archived=[]
 assert pages['Candidate Aura']['raw_wikitext'].startswith('{{Historical|update=January 19, 2026}}')
 auraraw=pages['Aura']['raw_wikitext'];start=auraraw.index('Removed');end=auraraw.index('== Trivia',start) if '== Trivia' in auraraw[start:] else len(auraraw)
 for r in authored:
  q=next(q for q in queue if q['source']==r['source']);proof=[]
  for oid in q['occurrence_ids']:
   o=occurrences[oid];b=blockmap[o['block_id']]
   if b['page']=='Candidate Aura':proof.append({'occurrence_id':oid,'block_id':b['block_id'],'page':'Candidate Aura','revision':b['revision'],'reason':'explicit page-level Historical template','source_marker':'{{Historical|update=January 19, 2026}}'})
   elif b['page']=='Aura' and start<=auraraw.find(b['template_raw'])<end:
    proof.append({'occurrence_id':oid,'block_id':b['block_id'],'page':'Aura','revision':b['revision'],'reason':'Removed table section;not detected by frozen heading flag','source_marker':auraraw[max(0,start-100):start+120]})
  if proof:archived.append({'source':r['source'],'translation':r['translation'],'all_archival_proofs':proof,'frozen_historical_heading':q['historical_heading']})
 assert len(archived)==53,(len(archived),[r['source'] for r in archived])
 write(SOURCE/'NPC_ARCHIVAL_PROVENANCE_0.15.89.json',{'scope':'53 authored archival/removed literals separately identified from original source. Other681 are not thereby certified live.','queue_sha256':before,'explicit_archived_or_removed_authored_literals':53,'other_authored_literals_not_live_certified':681,'source_pages':[pages['Aura'],pages['Candidate Aura']],'records':archived})
 assert sha(CORPUS/'translation_queue.jsonl')==before
 print('Extras3:two raw repairs/one current denial;53 archival-source rows explicitly separated;frozen queue unchanged')
if __name__=='__main__':main()
