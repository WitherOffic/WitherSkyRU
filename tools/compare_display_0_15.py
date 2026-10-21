"""Compare real old/current core behavior on one frozen, private display snapshot."""
from pathlib import Path
import json,re,hashlib
P=Path(__file__).resolve().parents[1];B=P/'build/source-audit-0.15';S=P/'translation-source'
def main():
    raw=(B/'unknown-snapshot.tsv').read_text(encoding='utf-8').splitlines()
    old=(B/'display-flags-before.txt').read_text();new=(B/'display-flags.txt').read_text()
    assert len(raw)==len(old)==len(new)
    def changed(flags):return {re.sub(r'§[0-9a-fk-or]','',s,flags=re.I).strip() for i,s in enumerate(raw) if flags[i]=='1'}
    a,b=changed(old),changed(new)
    result={'snapshot_sha256':hashlib.sha256((B/'unknown-snapshot.tsv').read_bytes()).hexdigest().upper(),'physical_lines':len(raw),'unique_nonempty_lines':len({re.sub(r'§[0-9a-fk-or]','',s,flags=re.I).strip() for s in raw if s.strip()}),'before_version':'0.14.4-dev','after_version':'0.15.0-dev','before_changed_physical':old.count('1'),'after_changed_physical':new.count('1'),'before_changed_unique':len(a),'after_changed_unique':len(b),'newly_changed_unique':len(b-a),'ceased_to_change_unique':len(a-b),'both_actual_core_versions_used':True,'counts_include_partial_style_run_translation':True,'numeric_variants_are_not_distinct_authored_phrases':True,'snapshot_packaged':False,'live_game_test':False}
    (S/'DISPLAY_0.15_COMPARISON.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result))
if __name__=='__main__':main()
