"""Mechanical exact-body reuse; no approximate matching or automatic translation."""
import json,re
from prepare_npc78 import sha,write
from prepare_npc93 import SOURCE,selection
def main():
 donor=SOURCE/'NPC92_OFFEA_REVIEWED.tsv';lines=[l.split('|') for l in donor.read_text(encoding='utf-8-sig').splitlines() if l and not l.startswith('#')];donors={f[1]:f for f in lines[6:]};assert len(donors)==12
 evidence=[];queue,known,selected,eligible,occ,blocks=selection()
 for name in ('Alwin','Ezekiel','Kheharad','Seffea','Ulyn'):
  path=SOURCE/('NPC93_'+re.sub('[^A-Z0-9]+','_',name.upper())+'_REVIEWED.tsv');current={f[1]:f for l in path.read_text(encoding='utf-8-sig').splitlines() if l and not l.startswith('#') and (f:=l.split('|'))};ordered=[];count=0
  for r in eligible:
   if r['speaker']!=name:continue
   body=r['source'].split(': ',1)[1]
   if body in donors:
    f=[name]+donors[body][1:];current[body]=f;count+=1;evidence.append({'source':r['source'],'donor_file':donor.name,'donor_sha256':sha(donor),'donor_authored_row':'|'.join(donors[body]),'new_authored_row':'|'.join(f),'occurrence_ids':r['occurrence_ids'],'automatic_translation':False})
   ordered.append('|'.join(current[body]))
  assert count==12
  path.write_text('\n'.join(ordered)+'\n',encoding='utf-8')
 assert len(evidence)==60;write(SOURCE/'NPC_SHARED_REUSE_0.15.93.json',{'scope':'60 exact body/target/style pairs from reviewed92 Offea after recipient context review','records':evidence})
 print('Verified/copied60 exact authored pairs;no approximate matching')
if __name__=='__main__':main()
