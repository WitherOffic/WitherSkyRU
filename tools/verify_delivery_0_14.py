"""Verify ZIP/data byte integrity and copy only this source delivery to Desktop."""
import hashlib,json,shutil,zipfile
from pathlib import Path
P=Path(__file__).resolve().parents[1];ROOT=P.parents[2];OUT=ROOT/'outputs'
NAME='WitherSkyRU_0.14.0-dev_MC26.1.2_project.zip'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def main():
    packed=ROOT/'work/packaging-0.14.0-source-checked'/NAME
    archive=OUT/NAME
    if archive.exists():assert sha(archive)==sha(packed),'Existing output archive differs; do not overwrite'
    else:shutil.copy2(packed,archive)
    with zipfile.ZipFile(archive) as z,zipfile.ZipFile(OUT/'WitherSkyRU_0.13.0-dev_MC26.1.2_project.zip') as old:
        assert z.testzip() is None
        names=z.namelist();files=[n for n in names if not n.endswith('/')]
        assert 'tools/verify_delivery_0_14.py' in files
        assert not any(n.lower().endswith(('.jar','.class')) for n in files)
        for n in files:assert z.read(n)==(P/n).read_bytes(),n
        java=[n for n in files if n.endswith('.java')]
        assert len(java)==9
        for n in java:assert z.read(n)==old.read(n),n
        npc='src/main/resources/data/witherskyru/npc-dialogues.json'
        assert z.read(npc)==old.read(npc)
        for n in old.namelist():
            if n.startswith('translation-source/BATCH_0.13') and not n.endswith('/'):
                assert z.read(n)==old.read(n),n
        meta=json.loads(z.read('src/main/resources/fabric.mod.json'));assert meta['version']=='0.14.0-dev'
        qa=json.loads(z.read('QA_REPORT.json'));assert qa['build']=='0.14.0-dev' and not qa['jar_built_this_batch']
        coverage=json.loads(z.read('translation-source/coverage.json'))
        assert coverage['snapshot_strings']==33126 and coverage['remaining_snapshot_strings']==603
        history=json.loads(z.read('translation-source/COVERAGE_0.13.0_HISTORY.json'))
        assert history['translated_or_partially_translated_snapshot_strings']==27328
        assert len(json.loads(z.read('translation-source/item-context.json')))==8940
        derivation=json.loads(z.read('translation-source/BATCH_0.14_DERIVATION.json'))
        assert derivation['imported_rows']==5195
    previous=OUT/'WitherSkyRU-26.1.2-fabric-0.12.0-dev.jar'
    assert sha(previous)=='5E5210E64E1E13220438877EC2D8F3E045AEFB406E571C40266FDEE81645E498'
    report={'revision':'0.14.0-dev','source_only':True,'source_zip':str(archive),'source_zip_sha256':sha(archive),
            'source_zip_bytes':archive.stat().st_size,'zip_crc_passed':True,'zip_files_compared':len(files),
            'java_sources_compared_unchanged':len(java),'npc_resource_unchanged':True,'previous_batch_sources_unchanged':True,
            'snapshot_size':33126,'known_union_size':33147,'new_sources':5195,'remaining_originals':603,
            'new_jar_present':False,'last_compiled_0_12_jar_unchanged':True,
            'expected_installed_0_3_jar_present':qa['active_installed_jar_present'],
            'game_directory_written':False,'runtime_tested':False,'full_server_coverage':False,'quality_verified':False}
    check=OUT/'WitherSkyRU_0.14.0_DELIVERY_CHECKS.json';check.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    desktop=Path('C:/Users/Wither/Desktop/WitherSkyRU/translation-batch-0.14.0');desktop.mkdir(parents=True,exist_ok=True)
    delivery=[p for p in OUT.iterdir() if p.is_file() and (p.name.startswith('WitherSkyRU_0.14.0_') or p.name==NAME or p.name in {'ПРОГРЕСС_WITHERSKYRU_0.14.0.md','ПЛАН_ПЕРЕВОДА_WITHERSKYRU.md'})]
    assert not any(p.suffix=='.jar' for p in delivery)
    for p in delivery:
        dest=desktop/p.name;shutil.copy2(p,dest);assert sha(p)==sha(dest)
    plan=desktop.parent/'ПЛАН_ПЕРЕВОДА_WITHERSKYRU.md';shutil.copy2(OUT/'ПЛАН_ПЕРЕВОДА_WITHERSKYRU.md',plan)
    assert sha(plan)==sha(OUT/'ПЛАН_ПЕРЕВОДА_WITHERSKYRU.md')
    print(json.dumps({**report,'desktop_directory':str(desktop),'desktop_files_copied_and_hash_verified':len(delivery)},ensure_ascii=False))
if __name__=='__main__':main()
