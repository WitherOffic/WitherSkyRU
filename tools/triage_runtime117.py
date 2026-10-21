"""Do not turn changed text or a style mismatch into a false quality/FPS score."""
import argparse,json,re
from pathlib import Path
from collections import Counter
from audit_five_pass117 import rows,jsonl,write

def normal(s):return re.sub(r'\s+',' ',s).strip()
def main():
 ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);ap.add_argument('probe',type=Path);a=ap.parse_args();root=a.root;records=list(rows(a.probe/'RUNTIME_ROWS.jsonl'));proof=json.loads((a.probe/'RUNTIME_SUMMARY.json').read_text());assert proof['inputs']==len(records)
 issues=[];counts=Counter();observed=[]
 for r in records:
  for layout in r['layouts']:
   if layout['same_flat_and_structured_target']:continue
   kind='whitespace_only' if normal(layout['structured_component_result'])==normal(r['flat_result']) else 'translation_or_safety_decline_needs_context'
   counts[kind]+=1;issues.append({'source':r['source'],'flat_result':r['flat_result'],'published_legacy':layout['legacy'],'legacy_result':layout['result'],'structured_component_result':layout['structured_component_result'],'classification':kind,'not_proven_live_bug_or_wrong_translation':True})
  if r['source'].startswith(('Collect Spruce Logs','Collect Dark Oak Logs','Find the Campfire','Travel to the Savanna Woodland','Objective: Collect','Objective: Find','Objective: Travel')):observed.append(r)
 jsonl(root/'PASS5_STYLE_LAYOUT_REVIEW.jsonl',issues);jsonl(root/'PASS5_OBSERVED_OBJECTIVE_RUNTIME.jsonl',observed)
 write(root/'PASS5_RUNTIME_TRIAGE.json',{'runtime_probe':str(a.probe.relative_to(root)),'runtime_jar_sha256':proof['runtime_jar_sha256'],'probe_inputs':proof['inputs'],'saved_coloured_layouts':proof['layouts'],'layout_disagreements':len(issues),'by_classification':dict(counts),'observed_goal_probe_inputs':len(observed),'changed_text_not_translation_quality_score':True,'not_an_FPS_measurement':True,'not_all_live_layouts':True})
 print(json.dumps({'issues':len(issues),'classification':dict(counts),'observed_goal_inputs':len(observed)},ensure_ascii=False))

if __name__=='__main__':main()
