"""Check renamed public artifact, retained translation resources and bundled attribution."""
import hashlib
import json
from pathlib import Path
import zipfile
from audit_public117 import archive

ROOT=Path(__file__).resolve().parents[1]

def main():
    jar=ROOT/'build/libs/SkyblockRPG_RU-26.2-fabric-0.16.21-dev.jar'
    old=json.loads((ROOT/'translation-source/workflow/public117-001-audit.json').read_text(encoding='utf-8'))
    report={'state':'PASS','archives':[]}
    archive(jar.read_bytes(),jar.name,report)
    with zipfile.ZipFile(jar) as z:
        meta=json.loads(z.read('fabric.mod.json'))
        assert (meta['name'],meta['id'],meta['version'])==('SkyblockRPG_RU','witherskyru','0.16.21-dev')
        assert meta['entrypoints']=={'client':['ru.wither.skyru.SkyRuClient']}
        assert set(meta['depends'])=={'fabricloader','fabric-resource-loader-v1','minecraft','java'}
        kept=[]
        for source,h in old['source_inventory'].items():
            prefix='src/main/resources/'
            if not source.startswith(prefix):continue
            name=source[len(prefix):]
            if not name.startswith(('data/','assets/','META-INF/licenses/','licenses/')):continue
            assert hashlib.sha256(z.read(name)).hexdigest()==h,name
            kept.append(name)
        assert len(kept)==137
        assert z.read('META-INF/licenses/PROJECT-MIT.txt')==(ROOT/'LICENSE').read_bytes()
        assert z.read('META-INF/licenses/THIRD-PARTY-NOTICES.txt')==(ROOT/'THIRD_PARTY_NOTICES.md').read_bytes()
        notice=z.read('META-INF/licenses/BULK117-SOURCES.txt').decode('utf-8')
        assert 'https://creativecommons.org/licenses/by-nc-sa/3.0/' in notice
        sources=set()
        for i in range(1,8):
            fixture=json.loads((ROOT/f'translation-source/BULK117_{i:03d}_ACCEPTANCE.json').read_text(encoding='utf-8'))
            for row in fixture['records']:sources.add(row['origin']['source_url'])
        assert len(sources)==359 and all(url in notice for url in sources)
        for name in ('ru/wither/skyru/SkyRuSettingsScreen.class','ru/wither/skyru/mixin/OptionsMenuMixin.class'):
            body=z.read(name);assert b'SkyblockRPG_RU' in body and b'WitherSkyRU' not in body,name
    report.update(jar={'name':jar.name,'bytes':jar.stat().st_size,'sha256':hashlib.sha256(jar.read_bytes()).hexdigest()},
        metadata=meta,preserved_prior_resources=len(kept),bundled_source_urls=len(sources),new_legal_resources=3)
    out=ROOT/'translation-source/workflow/brand117-001-checks.json'
    out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('PASS: exact name/ID/version;137 old resources unchanged;359 source URLs/3 legal notices; private classes/network refs absent')

if __name__=='__main__':main()
