"""Closed alternate color plans, unchanged targets and primary layouts; no new receipt scan."""
import json,re
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write,anchors,validate

CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
BASELINE=ROOT/'build/npc103-dispatch-19427e454c9b49c4ad4f5b5dffd73fe4/0.15.103-dev/DISPATCH_ROWS.jsonl'
PENDING={0:'The existing target omits the highlighted quantifier lot; revise prose rather than move an unrelated style.',3:'The existing target omits special; revised Russian prose required.',13:'The highlighted you has no explicit pronoun in the existing target; retain until prose revision.',25:'Idiomatic target needs a separate wording review before assigning the highlighted Crazy.',52:'The emphatic do is absent from the target; add explicit Russian emphasis in a wording revision.'}

def main():
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    gaps=[r for r in rows(BASELINE) if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']];assert len(gaps)==72
    queue={r['source']:r for r in rows(CORPUS/'translation_queue.jsonl')};occ={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
    known=defaultdict(list)
    for path in DATA.glob('*.json'):
        if path.name=='npc-styles104.json':continue
        for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
            if r.get('source') and r.get('translation'):known[r['source']].append({'resource':path.name,'record':r})
    records=[];evidence=[];seen=set()
    for line in (SOURCE/'NPC_STYLES_0.15.104_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):continue
        index,authored=line.split('|',1);index=int(index);assert index not in seen and index not in PENDING;seen.add(index)
        r=gaps[index];source=r['source'];target=r['flat_result'];assert all(e['record']['translation']==target for e in known[source])
        definitions=anchors(authored);validate(source,target,definitions)
        records.append({'speaker':r['speaker'],'source':source,'translation':target,'style_anchors':definitions,'source_url':queue[source]['source_url'],'evidence':'explicit_alternate_semantic_layout_target_and_primary_unchanged'})
        evidence.append({'index':index,'source':source,'translation':target,'style_anchors':definitions,'queue_row':queue[source],'original_entries':known[source],'layouts':list(dict.fromkeys(occ[i]['source_legacy_line'] for i in queue[source]['occurrence_ids']))})
    assert len(seen)==67 and seen|PENDING.keys()==set(range(72))
    ids={occ[i]['block_id'] for r in evidence for i in r['queue_row']['occurrence_ids']}
    blocks=[b for f in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/f) if b['block_id'] in ids]
    pages=[p for p in rows(CORPUS/'source_pages.jsonl') if p['title'] in {b['page'] for b in blocks}]
    write(DATA/'npc-styles104.json',{'format_version':1,'scope':'67 alternate published layouts. Existing targets/primary plans retained; not arbitrary style guessing.','records':records})
    write(SOURCE/'NPC_STYLE_SOURCES_0.15.104.json',{'scope':'Pinned public layouts, not live server capture. No new exact keys.','baseline_jar_sha256':'01E04AD01F2CF57B09CD5AF4A28093BC6130D48D4D79B2D5748C45D59ECFF028','gap_snapshot_sha256':sha(BASELINE),'gaps':gaps,'records':evidence,'pending':[{'index':i,'snapshot':gaps[i],'reason':reason,'queue_row':queue[gaps[i]['source']]} for i,reason in PENDING.items()],'blocks':blocks,'source_pages':pages})
    license_text='NPC104 public-source attribution (CC BY-NC-SA 3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\nAdapted semantic color plans; Russian wording and primary style plans unchanged.\n\n'
    license_text+='\n'.join(f"{p['title']} | revision {p['revision']} | https://hypixelskyblock.minecraft.wiki/w/{p['title'].replace(' ','_')}?oldid={p['revision']}" for p in pages)+'\n'
    (ROOT/'src/main/resources/META-INF/licenses/NPC104-SOURCES.txt').write_text(license_text,encoding='utf-8')
    print(json.dumps({'repairs':len(records),'remaining':len(PENDING),'speakers':dict(Counter(r['speaker'] for r in records)),'layouts':sum(len(r['layouts']) for r in evidence),'pages':len(pages)},ensure_ascii=False))

if __name__=='__main__':main()
