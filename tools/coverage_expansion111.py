"""Closed,source-backed upgrades of historical negative coverage expectations only."""
import json
from prepare_npc78 import SOURCE,write
GROUP_IDS={
    'TestNpc85.java':['N208','N209']+['N'+str(i) for i in range(51,58)],
    'TestNpc87.java':['N'+str(i) for i in range(177,182)],
    'TestNpc88.java':['N194'],
    'TestNpc91.java':['N187','N188','N551','N553'],
    'TestNpc99.java':['N'+str(i) for i in range(688,699)],
    'TestNpc102.java':['N478']
}
def cases():
    selected={r['id']:r for r in json.loads((SOURCE/'EXPANSION_SELECTED_0.15.111.json').read_text(encoding='utf-8'))['records']}
    out=[]
    for name,ids in GROUP_IDS.items():
        prior=json.loads((SOURCE/('NPC_SOURCES_0.15.'+name.removeprefix('TestNpc').removesuffix('.java')+'.json')).read_text(encoding='utf-8'))
        for uid in ids:
            r=selected[uid];assert r['tag']=='NPC' and not r['published_row']['variable_definition'] and not r['published_row']['unresolved_markup'] and not r['published_row']['alpha_page']
            bucket=next(k for k in ('pending_variables','unsafe_excluded') if any(v['source']==r['source'] for v in prior[k]));assert bucket!='unsafe_excluded' or r['archival']
            out.append({'id':uid,'source':r['source'],'translation':r['translation'],'source_url':r['source_url'],'archival':r['archival'],'test':name,'previous_bucket':bucket,'layouts':r['layouts']})
    assert len(out)==31
    return out
def adjust(name,text):
    if name not in GROUP_IDS:return text
    chosen=[r for r in cases() if r['test']==name]
    body='\n    private static String newlyReviewed111(String source){return switch(source){\n'+''.join('        case '+json.dumps(r['source'],ensure_ascii=False)+' -> '+json.dumps(r['translation'],ensure_ascii=False)+';\n' for r in chosen)+'        default -> null;\n    };}\n'
    marker='    private static int checks;';assert text.count(marker)==1;text=text.replace(marker,marker+body)
    for label in ('placeholder never faked as literal coverage','excluded old-layout source not silently registered'):
        old='check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"'+label+'");'
        new='check(!packaged.containsKey(source)&&(newlyReviewed111(source)==null?story.translate(literal,db)==literal:plain(story.translate(literal,db)).equals(newlyReviewed111(source))),"111 explicitly reviewed literal/archive coverage;all other negatives retained");'
        assert text.count(old)==1;text=text.replace(old,new)
    return text
def journal():
    write(SOURCE/'EXPANSION_COVERAGE_ADDITIONS_0.15.111.json',{'scope':'31 explicit upgrades of old negative coverage expectations in6 isolated test copies. Original Java files,positive translation/style goldens and old reference buckets are unchanged. All other negative cases retain unchanged-object checks.','reason':'111 intentionally adds130 clearly labelled archival literals. Two fixed Fann slot numbers and the fixed [Eye] speaker are complete published strings,not variable tokens.','cases':cases(),'earlier_failed_run_before_full_scope_audit':'build/expansion111-checks-8ebc04cbe3b34286974660f33059c13c','retained_earlier_test_source':'build/expansion111-test-sources-v4/TestNpc99.java'})
