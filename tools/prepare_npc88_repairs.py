"""Audit extraction collisions without rewriting the frozen corpus; compile one reviewed repair."""
import json,re
from pathlib import Path
from prepare_npc78 import rows,sha,write,anchors,validate
from prepare_npc88 import ROOT,SOURCE,DATA,CORPUS
from export_npc_corpus77 import provisional_text,COLOR
def main():
 queue_hash=sha(CORPUS/'translation_queue.jsonl')
 blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/name)]
 collisions=[]
 for b in blocks:
  if 'payload_raw' not in b:continue
  rendered=provisional_text(b['payload_raw'])
  if rendered!=b['provisional_legacy_text']:collisions.append({'block_id':b['block_id'],'page':b['page'],'source_url':b['source_url'],'revision':b['revision'],'headings':b['headings'],'raw':b['payload_raw'],'frozen_legacy':b['provisional_legacy_text'],'corrected_legacy':rendered,'automatically_translated':False})
 write(SOURCE/'NPC_EXTRACTION_COLLISIONS_0.15.88.json',{'scope':'Offline comparison of frozen blocks to repaired extractor. No frozen files rewritten,no automatic translation/certification of other collisions.','queue_sha256':queue_hash,'changed_blocks':len(collisions),'records':collisions})
 records=[];evidence=[]
 for line in (SOURCE/'NPC88_RAW_SOURCE_REPAIRS.tsv').read_text(encoding='utf-8').splitlines():
  if not line or line.startswith('#'):continue
  speaker,english,russian,plans=line.split('|');source=f'[NPC] {speaker}: {english}';target=f'[NPC] {speaker}: {russian}'
  definitions=anchors(plans);validate(source,target,definitions)
  matching=[(b,l) for b in blocks if b['page']=='Ramero' for l in b['payload_raw'].splitlines() if 'Let us employ' in l]
  assert len(matching)==1;b,raw=matching[0];legacy=provisional_text(raw).strip()
  assert raw=='&e[NPC] &bRamero&f: &fLet us employ these &cplanks &fto fortify the &centrance&f.'
  assert COLOR.sub('',legacy)==source and source not in {r['source'] for r in rows(CORPUS/'translation_queue.jsonl')}
  frozen='[NPC] Ramero: Let us employ these planks to fortify the ¢rance.'
  old_record=next(r for r in json.loads((DATA/'npc-expansion88.json').read_text(encoding='utf-8'))['records'] if r['source']==frozen)
  assert old_record['translation']==target
  records.append({'source_key':'npc88.raw_repair.0','speaker':speaker,'tag':'NPC','source':source,'translation':target,'style_anchors':definitions,'source_url':b['source_url'],'evidence':'exact_raw_legacy_source_repair_not_frozen_queue_key'})
  evidence.append({'source':source,'translation':target,'source_legacy_line':legacy,'raw_line':raw,'frozen_queue_source':frozen,'block':b,'counted_as_frozen_corpus_exact_key':False})
 assert len(records)==1
 write(DATA/'npc-raw-repairs88.json',{'format_version':1,'language':'ru','scope':'One independently reviewed corrected raw-wiki line. Not a new key in the immutable16244queue;separately counted.','records':records})
 write(SOURCE/'NPC_RAW_REPAIRS_0.15.88.json',{'queue_sha256':queue_hash,'records':evidence})
 pairs=[line.split('\t') for line in (SOURCE/'NPC88_ITEM_NAMES.tsv').read_text(encoding='utf-8').splitlines() if line and not line.startswith('#')]
 assert pairs==[['mrahcemiT esrevrorriM','Зеркальный талисман времени']]
 assert (DATA/'corrections.tsv').read_text(encoding='utf-8').splitlines()[-1]=='\t'.join(pairs[0])
 canonical=json.loads((SOURCE/'translations.json').read_text(encoding='utf-8'))['translations'];assert canonical[pairs[0][0]]==pairs[0][1]
 assert sha(CORPUS/'translation_queue.jsonl')==queue_hash
 print('Extraction changed blocks',len(collisions),';reviewed raw repair1;separate item name1;frozen queue unchanged')
if __name__=='__main__':main()
