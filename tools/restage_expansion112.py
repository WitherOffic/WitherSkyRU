"""Final editorial review before delivery;never reuse preliminary-JAR test results."""
import shutil
from prepare_expansion112 import *

def main():
    prior=ROOT/'build/expansion112-superseded-draft';before=load(prior/'EXPANSION_SELECTED_0.15.112.json')['records'];old={r['source']:r for r in before};prepare();after=load(SOURCE/'EXPANSION_SELECTED_0.15.112.json')['records'];changes=[]
    for r in after:
        prev=old[r['source']]
        if r['translation']!=prev['translation']:changes.append({'id':r['id'],'source':r['source'],'before':prev['translation'],'after':r['translation'],'before_anchors':prev['style_anchors'],'after_anchors':r['style_anchors']})
    assert {r['id'] for r in changes}=={'2414','2423','3366'}
    write(SOURCE/'EXPANSION_EDITORIAL_SUPERSESSION_0.15.112.json',{'scope':'Three new112 draft targets edited before delivery;no existing111 translation or historical positive oracle changed. Preliminary-JAR results are retained but not used as final verification.','reason':'Fine Flour and Warty aligned with existing item terminology;Warden Teal described as a cosmetic style,not a new creature.','preliminary_jar_sha256':sha(prior/'WitherSkyRU-26.1.2-fabric-0.15.112-dev.jar'),'preliminary_checks':'build/expansion112-checks-d95a836456b24ec5a79059906d49fec7','preliminary_snapshot':'build/expansion112-snapshots-a357165616854f5aa13c6b88b72850af','retained_preliminary_sources':prior.relative_to(ROOT).as_posix(),'changes':changes})
    dataset=ROOT/'translation-source-regression112-v2';dataset.mkdir(exist_ok=False)
    for p in SOURCE.iterdir():
        if p.is_file():shutil.copy2(p,dataset/p.name)
    previous=ROOT/'translation-source-regression112';sup=load(previous/'EXPECTATION_SUPERSESSION105.json')
    for r in sup['changed_files']:shutil.copy2(previous/r['file'],dataset/r['file'])
    shutil.copy2(previous/'EXPECTATION_SUPERSESSION105.json',dataset/'EXPECTATION_SUPERSESSION105.json')
    print('Staged final reviewed112 dataset;all185 groups must run fresh against the final digest.')

if __name__=='__main__':main()
