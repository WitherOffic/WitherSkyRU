"""Package/verify only the current source delivery; never write to the game."""
from pathlib import Path
import hashlib,json,shutil,zipfile
P=Path(__file__).resolve().parents[1];OUT=P.parents[2]/'outputs'
INCLUDED=('src','tools','tests','licenses','translation-source','build-local.ps1','build.gradle','settings.gradle','gradle.properties','LICENSE','THIRD_PARTY_NOTICES.md','README_RU.md','CHANGELOG_RU.md','QA_REPORT.json')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def main():
    version=json.loads((P/'src/main/resources/fabric.mod.json').read_text())['version']
    assert version=='0.15.0-dev'
    qa=json.loads((P/'QA_REPORT.json').read_text(encoding='utf-8'))
    assert qa['build']==version and not qa['jar_built_this_batch']
    archive=OUT/f'WitherSkyRU_{version}_MC26.1.2_project.zip'
    assert not archive.exists(),'Never overwrite an existing delivered archive'
    files=[]
    for entry in INCLUDED:
        path=P/entry;assert path.exists(),entry
        files.extend(f for f in (path.rglob('*') if path.is_dir() else [path]) if f.is_file() and '__pycache__' not in f.parts and f.suffix not in {'.pyc','.class','.jar'})
    expected={f.relative_to(P).as_posix():f for f in files}
    with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for n,f in sorted(expected.items()):z.write(f,n)
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        assert set(z.namelist())==set(expected)
        for n,f in expected.items():assert z.read(n)==f.read_bytes(),n
        assert not any(n.startswith('build/') or n.endswith('unknown-snapshot.tsv') for n in z.namelist())
    desktop=Path('C:/Users/Wither/Desktop/WitherSkyRU/translation-batch-0.15.0')
    desktop.mkdir(parents=True,exist_ok=True)
    delivery=[archive,OUT/'WitherSkyRU_0.15_FULL_REVIEW_RU.md']
    result={'version':version,'source_only':True,'source_zip':str(archive),'source_zip_bytes':archive.stat().st_size,'source_zip_sha256':sha(archive),'zip_crc_passed':True,'zip_byte_matches':len(expected),'zip_completeness_passed':True,'private_raw_log_not_packaged':True,'desktop_directory':str(desktop),'game_written':False,'jar_built':False}
    check=OUT/'WitherSkyRU_0.15_DELIVERY_CHECKS.json'
    assert not check.exists()
    check.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    delivery.append(check)
    for f in delivery:
        target=desktop/f.name
        assert not target.exists(),'Never overwrite a prior Desktop delivery'
        shutil.copy2(f,target);assert sha(f)==sha(target)
    print(json.dumps({**result,'desktop_files_byte_verified':len(delivery)},ensure_ascii=False))
if __name__=='__main__':main()
