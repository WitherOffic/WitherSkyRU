"""Capture API-announced public packs,verify their hashes,extract text without installing."""
import argparse,hashlib,json,re,time,urllib.request,urllib.parse,zipfile
from pathlib import Path
from export_public_text116 import digest,write,jsonl,stamp

def main():
    ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);args=ap.parse_args();root=args.root.resolve();api=json.loads((root/'hypixel-resources/resources-packs.json').read_text(encoding='utf-8'));saved=[];texts=[]
    for pack in api['packs']:
        for version in pack['versions']:
            url=version['url'];parsed=urllib.parse.urlsplit(url);assert parsed.scheme=='https' and parsed.hostname=='resourcepacks.hypixel.net'
            path=root/'official-resource-packs'/f"{pack['id']}-{version['packFormat']}.zip";path.parent.mkdir(exist_ok=True)
            if not path.exists():
                req=urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-public-text-export/2.0 (read-only localization research)'})
                with urllib.request.urlopen(req,timeout=45) as response:
                    length=int(response.headers.get('Content-Length','0'));assert length<=128*1024*1024,'Oversized pack;manual review required'
                    with path.open('wb') as output:
                        total=0
                        while block:=response.read(1024*1024):
                            total+=len(block);assert total<=128*1024*1024;output.write(block)
            raw=path.read_bytes();expected=version['hash'].lower();actual=hashlib.sha1(raw).hexdigest();assert actual==expected,('API pack checksum mismatch',path,actual,expected)
            members=[];language_files=0;selected=[]
            with zipfile.ZipFile(path) as z:
                assert z.testzip() is None
                for m in z.infolist():
                    if m.is_dir():continue
                    members.append({'name':m.filename,'size':m.file_size,'compressed_size':m.compress_size,'crc32':f'{m.CRC:08X}'})
                    language=bool(re.fullmatch(r'assets/[^/]+/lang/[^/]+\.(?:json|lang)',m.filename));metadata=m.filename=='pack.mcmeta'
                    if not language and not metadata:continue
                    assert m.file_size<=64*1024*1024;target=(root/'pack-text-files'/str(version['packFormat'])/m.filename).resolve();assert target.is_relative_to(root/'pack-text-files');target.parent.mkdir(parents=True,exist_ok=True);content=z.read(m);target.write_bytes(content);language_files+=int(language)
                    selected.append({'member':m.filename,'sha256':digest(content),'size':len(content)})
                    if language and '/en_' not in m.filename:continue
                    def visit(value,at):
                        if isinstance(value,str) and re.search('[A-Za-z]',value):texts.append({'source':value,'pack_id':pack['id'],'format':version['packFormat'],'url':url,'archive_member':m.filename,'json_path':at,'automatically_added_to_mod':False})
                        elif isinstance(value,dict):
                            for k,v in value.items():visit(v,at+[k])
                        elif isinstance(value,list):
                            for i,v in enumerate(value):visit(v,at+[i])
                    if m.filename.endswith('.json') or metadata:visit(json.loads(content.decode('utf-8-sig')),[])
                    else:
                        for n,line in enumerate(content.decode('utf-8-sig').splitlines(),1):
                            if line and not line.startswith('#') and '=' in line:visit(line.split('=',1)[1],[n])
            saved.append({'pack_id':pack['id'],'format':version['packFormat'],'url':url,'api_expected_sha1':expected,'verified_sha1':actual,'sha256':digest(raw),'archive_bytes':len(raw),'archive_members':members,'language_files':language_files,'extracted_text_files':selected,'zip_integrity_passed':True,'installed':False});print(f"PACK{version['packFormat']}:verified{len(raw)} bytes;language files{language_files}",flush=True);time.sleep(1.05)
    jsonl(root/'RESOURCE_PACK_TEXTS.jsonl',texts);write(root/'RESOURCE_PACK_CAPTURE.json',{'packs':saved,'text_occurrences':len(texts),'completed_utc':stamp(),'all_api_announced_versions_captured':True,'private_server_text_included':False,'installed':False});print('Pack text capture complete',len(saved),len(texts),flush=True)

if __name__=='__main__':main()
