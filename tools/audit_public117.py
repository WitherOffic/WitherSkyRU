"""Inspect public classes, resources, nested dependencies and source inventory. No network.

Constant-pool checks cover actual class/method references rather than filenames alone.
NPC 'Collector', java.util.stream.Collectors and native RenderingTextCollector are allowed.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import struct
import zipfile

ROOT=Path(__file__).resolve().parents[1]
PRIVATE=re.compile(rb'ru[/\.]+wither[/\.]+skyru[/\.]+Collector(?:Capture|Consent|Enrollment|Environment|OfflineSpool|SourcePolicy|Telemetry)|CollectorLifecycleMixin|skyblockrpgru_collector|/api/v1/(?:events|telemetry|clients|sessions|profiles)/(?:batch|register)|skypixel\.wither101\.xyz',re.I)
NETWORK=(b'java/net/http/',b'java/net/Socket',b'java/net/DatagramSocket',b'okhttp3/',b'org/apache/http/',b'java/nio/channels/SocketChannel')
SECRET=re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{30,}|AKIA[A-Z0-9]{16}')

def sha(data):return hashlib.sha256(data).hexdigest()

def pool(data):
    assert data[:4]==b'\xca\xfe\xba\xbe'
    count=struct.unpack_from('>H',data,8)[0];at=10;i=1;values=[]
    sizes={3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}
    while i<count:
        tag=data[at];at+=1
        if tag==1:
            n=struct.unpack_from('>H',data,at)[0];at+=2;values.append(data[at:at+n]);at+=n
        else:
            assert tag in sizes,tag
            at+=sizes[tag]
            if tag in (5,6):i+=1
        i+=1
    return values

def archive(data,label,report):
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        names=z.namelist();assert len(names)==len(set(names)),label+' duplicate ZIP names'
        item={'archive':label,'sha256':sha(data),'entries':len(names),'classes':0,'nested_jars':[],'class_major_versions':[]};report['archives'].append(item)
        for name in names:
            if name.endswith('/'):continue
            body=z.read(name);where=label+'!'+name
            assert not PRIVATE.search(name.encode()) and not PRIVATE.search(body),where+' private reference'
            assert not SECRET.search(body),where+' credential-like material'
            assert not re.search(r'(?i)(collector.*\.(?:key|properties)|\.env(?:\.|$)|\.(?:pem|p12|pfx)$)',name),where+' private configuration'
            if name.endswith('.class'):
                assert not name.startswith(('smoke/','publicsmoke/','skycubedsmoke/','collectorsmoke/')),where+' test driver in production'
                strings=pool(body);item['classes']+=1;joined=b'\n'.join(strings)
                major=struct.unpack_from('>H',body,6)[0]
                if major not in item['class_major_versions']:item['class_major_versions'].append(major)
                assert not any(token in joined for token in NETWORK),where+' network client reference'
                assert not (b'java/net/URL' in strings and any(t in strings for t in (b'openConnection',b'openStream'))),where+' URL connection'
                assert not (b'java/lang/Runtime' in strings and b'exec' in strings),where+' process execution'
                assert b'java/lang/ProcessBuilder' not in strings,where+' process execution'
            if name.endswith('.jar'):
                item['nested_jars'].append(name);archive(body,where,report)

def main():
    ap=argparse.ArgumentParser();ap.add_argument('jar',type=Path);ap.add_argument('--before',type=Path,required=True);ap.add_argument('--out',type=Path,required=True);args=ap.parse_args()
    report={'state':'PASS','method':'all archive bytes plus decoded class constant pools; recurse embedded JARs; no name-only inference','archives':[]}
    archive(args.jar.read_bytes(),args.jar.name,report)
    with zipfile.ZipFile(args.jar) as z,zipfile.ZipFile(args.before) as old:
        meta=json.loads(z.read('fabric.mod.json'));mixins=json.loads(z.read('witherskyru.mixins.json'))
        assert meta['id']=='witherskyru' and meta['version']=='0.16.20-dev'
        assert meta['entrypoints']=={'client':['ru.wither.skyru.SkyRuClient']}
        assert set(meta['depends'])=={'fabricloader','fabric-resource-loader-v1','minecraft','java'}
        assert 'HudCollectorMixin' in mixins['client'] and 'SkyCubedDialogueMixin' in mixins['client']
        assert not any('Lifecycle' in n for n in mixins['client'])
        preserved=[n for n in old.namelist() if n.startswith(('data/','assets/','META-INF/licenses/','licenses/')) and not n.endswith('/')]
        for n in preserved:assert z.read(n)==old.read(n),'translation/license resource changed: '+n
        for n in ('ru/wither/skyru/mixin/HudCollectorMixin.class','ru/wither/skyru/mixin/ChatDrawMixin.class','ru/wither/skyru/mixin/ChatClickableDrawMixin.class'):
            assert z.read(n)==old.read(n),'native text/privacy hook changed: '+n
        removed=sorted(set(old.namelist())-set(z.namelist()));added=sorted(set(z.namelist())-set(old.namelist()))
        report.update(metadata=meta,mixins=mixins,identical_translation_and_license_resources=len(preserved),removed_entries=removed,added_entries=added)
    inventory={};risk=[]
    for folder in ('src','tests','tools','gradle','.github'):
        for p in (ROOT/folder).rglob('*'):
            if not p.is_file() or '__pycache__' in p.parts:continue
            body=p.read_bytes();name=p.relative_to(ROOT).as_posix();inventory[name]=sha(body)
            if name.startswith('src/main/java/'):
                assert not PRIVATE.search(body),name+' private source reference'
                assert not re.search(rb'java\.net\.(?:http|Socket|DatagramSocket)|openConnection\(|ProcessBuilder',body),name+' network/process source reference'
            if PRIVATE.search(body):risk.append(name) # Negative verification tools/tests are expected here.
    for p in ROOT.iterdir():
        if p.is_file() and (p.suffix in ('.gradle','.properties','.ps1') or p.name in ('gradlew','gradlew.bat')):inventory[p.name]=sha(p.read_bytes())
    report.update(source_inventory=inventory,reviewed_negative_test_or_audit_matches=risk,scope_files=len(inventory),standalone_dependency_required=False)
    args.out.parent.mkdir(parents=True,exist_ok=True);args.out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:report[k] for k in ('state','scope_files','identical_translation_and_license_resources','standalone_dependency_required')},ensure_ascii=False));print('ARCHIVES',[(x['archive'],x['classes']) for x in report['archives']])

if __name__=='__main__':main()
