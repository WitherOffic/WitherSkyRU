"""Publish verified public source capture and all review queues,not a mod build."""
import argparse,hashlib,json,shutil,zipfile
from pathlib import Path
from collections import Counter
from export_public_text116 import ROOT,rows,write,digest,stamp

DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/PUBLIC_TEXT_EXPORT_2026-10-21')
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_11')

def sha(p):return digest(p.read_bytes())

def main():
    ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);args=ap.parse_args();root=args.root.resolve();verified=json.loads((root/'VERIFICATION.json').read_text(encoding='utf-8'));summary=json.loads((root/'REVIEW_SUMMARY.json').read_text(encoding='utf-8'));assert verified['all_passed'] and verified['saved_pages']==summary['captured_pages'] and verified['missing_pages']==0
    boundary=json.loads((root/'RUNTIME_BOUNDARY.json').read_text(encoding='utf-8'))
    for rel,h in boundary.items():assert sha(ROOT/rel)==h,rel
    old=json.loads((PREVIOUS/'HASHES.json').read_text(encoding='utf-8'))
    for rel,h in old.items():assert sha(PREVIOUS/rel)==h,rel
    observed=json.loads((root/'OBSERVED_MISSING.json').read_text(encoding='utf-8'));expected={r.get('source',r.get('source_pattern')) for r in observed['observations']};found=set();total=0;kinds=Counter()
    for r in rows(root/'MASTER_REVIEW_QUEUE.jsonl'):
        assert r['translation_ru']=='' and not r['automatically_added_to_mod'];total+=1
        if r['source'] in expected:found.add(r['source'])
        kinds.update(r['kinds'])
    assert found==expected and total==summary['master_review_unique_sources_or_patterns']
    priorities=list(rows(root/'REPORTED_NPC_PRIORITY_QUEUE.jsonl'));assert len(priorities)==62 and Counter(r['npc'] for r in priorities)=={'Ryan':41,'Melody':21}
    assert kinds['user_reported_npc_priority_review']==len(priorities)
    for p in rows(root/'REPORTED_NPC_SOURCE_PAGES.jsonl'):assert digest(p['raw_wikitext'])==p['raw_sha256']
    for p in rows(root/'NPC_DIALOGUE_REVIEW_QUEUE.jsonl'):assert p['source'] and p['origins']
    packs=json.loads((root/'RESOURCE_PACK_CAPTURE.json').read_text(encoding='utf-8'));api=json.loads((root/'hypixel-resources/resources-packs.json').read_text(encoding='utf-8'));assert len(packs['packs'])==sum(len(p['versions']) for p in api['packs'])==3
    for p in packs['packs']:
        data=(root/'official-resource-packs'/f"{p['pack_id']}-{p['format']}.zip").read_bytes();assert hashlib.sha1(data).hexdigest()==p['api_expected_sha1'] and digest(data)==p['sha256']
        with zipfile.ZipFile(root/'official-resource-packs'/f"{p['pack_id']}-{p['format']}.zip") as z:
            assert z.testzip() is None
            assert len([m for m in z.namelist() if '/lang/' in m and not m.endswith('/')])==p['language_files']
            for f in p['extracted_text_files']:
                content=z.read(f['member']);assert digest(content)==f['sha256'] and content==(root/'pack-text-files'/str(p['format'])/f['member']).read_bytes()
    testlog=(ROOT/'build/public116-tests-output.txt').read_text(encoding='utf-8-sig');assert 'Ran 14 tests' in testlog and 'OK' in testlog
    scope={'verified_utc':stamp(),'runtime_source_files_unchanged':len(boundary),'previous114_delivery_files_unchanged':len(old),'all_user_observations_present_in_master':len(found),'master_unique_sources_or_patterns':total,'master_kinds':dict(kinds),'official_pack_archives_verified':3,'export_regression_tests':14,'new_data_not_imported_into_mod':True,'installed_pack_modified':False,'profile_api_settings_verified':False,'all_live_server_text_known':False};write(root/'DELIVERY_SCOPE.json',scope);write(root/'STATUS.json',{'stage':'complete_and_independently_verified','pages':summary['captured_pages'],'master_review_rows':total,'updated_utc':stamp(),'all_live_server_text_known':False})
    assert not DEST.exists();archive=DEST.parent/'PUBLIC_TEXT_EXPORT_2026-10-21.zip';assert not archive.exists();shutil.copytree(root,DEST)
    for p in (ROOT/'tools').glob('*116.py'):shutil.copy2(p,DEST/p.name)
    shutil.copy2(ROOT/'tools/export_npc_corpus77.py',DEST/'export_npc_corpus77.py');shutil.copy2(ROOT/'tests/test_public_text116.py',DEST/'test_public_text116.py')
    shutil.copy2(ROOT/'build/public116-tests-output.txt',DEST/'TESTS_OUTPUT.txt')
    for name in ('public116-resume-output.txt','public116-final-capture-output.txt','public116-packs-output.txt','public116-verification-output.txt','public116-review-output.txt'):
        p=ROOT/'build'/name
        if p.exists():shutil.copy2(p,DEST/name)
    images={r['source_image'] for r in observed['observations']};image_status=[]
    for path in sorted(images):
        p=Path(path);entry={'original_path':path,'available':p.exists()}
        if p.exists():(DEST/'observed-screenshots').mkdir(exist_ok=True);shutil.copy2(p,DEST/'observed-screenshots'/p.name);entry['sha256']=sha(p)
        image_status.append(entry)
    write(DEST/'OBSERVED_IMAGE_STATUS.json',image_status)
    (DEST/'README_RU.md').write_text('# Полная публичная выгрузка и очередь проверки\n\nОткройте SUMMARY_RU.md. Основная объединённая очередь: MASTER_REVIEW_QUEUE.jsonl. Полные исходники: source_pages.jsonl,зафиксированные ответы API: requests/.\n\n100% относится к найденному публичному списку8 пространств,не к частной базе сервера и не к проценту перевода игры. В широкой очереди есть технические и справочные кандидаты;их нельзя без проверки подмешивать в мод.\n\nВсе3 объявленные API версии официального ресурс-пака сохранены в official-resource-packs/. Их SHA-1 проверены;языковые файлы и метаданные выделены в pack-text-files/. Ничего не установлено.\n\nHASHES.json содержит контрольные суммы всех файлов,кроме самого манифеста. Выгрузка и проверки не являются новой версией JAR. Ключ API,пароли и приватные данные профиля не требовались.\n\nИнструменты экспорта приложены для аудита;подготовка очередей и полный повторный контроль используют рабочий проект и прежнюю замороженную базу. Старые16 244 строки и предыдущий пакет114 сохранены.\n',encoding='utf-8')
    hashes={p.relative_to(DEST).as_posix():sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',hashes)
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(DEST.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(hashes)+1
        for rel,h in hashes.items():assert sha(DEST/rel)==h and digest(z.read(rel))==h
        assert z.read('HASHES.json')==(DEST/'HASHES.json').read_bytes()
    result={'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'archive_bytes':archive.stat().st_size,'manifest_files':len(hashes),'zip_entries':len(hashes)+1,'summary':summary,'scope':scope};write(ROOT/'build/PUBLIC116_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
