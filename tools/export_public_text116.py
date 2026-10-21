"""All published content in explicit gameplay-related namespaces,not all private server text.

Read-only,rate-limited,resumable source capture. Never imports strings into the mod.
Raw text is authoritative; candidate extraction deliberately does not certify game provenance.
"""
from __future__ import annotations
import argparse,csv,hashlib,json,re,time,urllib.request,urllib.parse,urllib.error,uuid
from pathlib import Path
from datetime import datetime,timezone
from collections import Counter
from export_npc_corpus77 import templates,split_parameters,provisional_text

ROOT=Path(__file__).resolve().parents[1]
API='https://hypixelskyblock.minecraft.wiki/api.php'
NAMESPACES=(0,8,10,14,112,114,828,9592)
PRIORITY=['Ryan','Quests','Kelly','Into the Woods','A Helping Hand','The Campfire Cult','Trial of Fire','The Park','Campfire Badge']
RESOURCE_ENDPOINTS=['resources/skyblock/items','resources/skyblock/collections','resources/skyblock/skills','resources/skyblock/election','resources/skyblock/bingo','resources/achievements','resources/quests','resources/packs']

def stamp():return datetime.now(timezone.utc).isoformat()
def digest(value):return hashlib.sha256(value.encode('utf-8') if isinstance(value,str) else value).hexdigest().upper()
def write(path,value):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def jsonl(path,values):
    with path.open('w',encoding='utf-8',newline='\n') as f:
        for value in values:f.write(json.dumps(value,ensure_ascii=False)+'\n')
def rows(path):
    with path.open(encoding='utf-8-sig') as f:
        for line in f:
            if line.strip():yield json.loads(line)

class Capture:
    def __init__(self,root,offline=False):
        self.root=root;self.offline=offline;self.last=0.;self.live=0;self.hits=0;self.pages={};self.page_times={};(root/'requests').mkdir(parents=True,exist_ok=True)
        # Reuse independently complete page records,not only identical batch boundaries.
        # Adding a namespace must not force downloading the same old pages again.
        for path in (root/'requests').glob('*.json'):
            saved=json.loads(path.read_text(encoding='utf-8'));assert digest(saved['body'])==saved['body_sha256'];url=urllib.parse.urlsplit(saved['url']);params=dict(urllib.parse.parse_qsl(url.query))
            if url.hostname!='hypixelskyblock.minecraft.wiki' or not params.get('pageids'):continue
            response=json.loads(saved['body'])
            if re.search(r'truncat|too (?:many|large)',str(response.get('warnings',{})),re.I):continue
            for page in response.get('query',{}).get('pages',[]):
                if not page.get('revisions') or page['revisions'][0].get('slots',{}).get('main',{}).get('content') is None:continue
                when=saved['retrieved_utc']
                if when>=self.page_times.get(page['pageid'],''):self.pages[page['pageid']]=page;self.page_times[page['pageid']]=when
    def get(self,url):
        path=self.root/'requests'/(digest(url)+'.json')
        if path.exists():
            record=json.loads(path.read_text(encoding='utf-8'));assert record['url']==url and digest(record['body'])==record['body_sha256'];self.hits+=1;return json.loads(record['body'])
        if self.offline:raise RuntimeError('Missing captured request: '+url)
        for attempt in range(6):
            time.sleep(max(0.,1.05-(time.monotonic()-self.last)));self.last=time.monotonic()
            try:
                req=urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-public-text-export/2.0 (read-only offline localization research)','Accept':'application/json','Accept-Encoding':'gzip'})
                with urllib.request.urlopen(req,timeout=40) as response:
                    body=response.read()
                    if response.headers.get('Content-Encoding')=='gzip':
                        import gzip
                        body=gzip.decompress(body)
                    body=body.decode('utf-8')
                parsed=json.loads(body)
                if 'error' in parsed:
                    if parsed['error'].get('code') in ('maxlag','ratelimited','readonly'):raise TimeoutError(str(parsed['error']))
                    raise RuntimeError(str(parsed['error']))
                write(path,{'url':url,'retrieved_utc':stamp(),'body_sha256':digest(body),'body':body});self.live+=1;return parsed
            except (urllib.error.HTTPError,urllib.error.URLError,TimeoutError,json.JSONDecodeError) as e:
                if attempt==5:raise RuntimeError('Request failed;all completed responses retained: '+url) from e
                time.sleep(min(30,2**attempt))
        raise AssertionError('Unreachable')
    def query(self,**params):return self.get(API+'?'+urllib.parse.urlencode(sorted({'action':'query','format':'json','formatversion':'2','maxlag':'5',**params}.items())))

def page_record(p):
    revision=p['revisions'][0];text=revision['slots']['main'].get('content')
    if text is None:raise RuntimeError('Revision lacks public main-slot content: '+p['title'])
    return {'pageid':p['pageid'],'namespace':p['ns'],'title':p['title'],'revision':revision['revid'],'revision_timestamp':revision['timestamp'],'source_url':'https://hypixelskyblock.minecraft.wiki/w/'+urllib.parse.quote(p['title'].replace(' ','_'),safe='/:')+'?oldid='+str(revision['revid']),'content_model':revision['slots']['main'].get('contentmodel'),'raw_sha256':digest(text),'raw_wikitext':text,'redirect':bool(re.match(r'\s*#redirect\b',text,re.I))}

def fetch(capture,**selection):
    if 'pageids' in selection and hasattr(capture,'pages'):
        ids=[int(v) for v in selection['pageids'].split('|')];missing=[v for v in ids if v not in capture.pages]
        if not missing:capture.hits+=1;return [capture.pages[v] for v in ids]
        if len(missing)<len(ids):
            fetch(capture,pageids='|'.join(str(v) for v in missing));return [capture.pages[v] for v in ids]
    d=capture.query(prop='revisions|info',rvprop='ids|timestamp|content',rvslots='main',inprop='url',**selection)
    for warning in d.get('warnings',{}).values():
        if re.search(r'truncat|too (?:many|large)',str(warning),re.I):
            field='pageids' if 'pageids' in selection else 'titles';items=selection[field].split('|')
            if len(items)<=1:raise RuntimeError('Single-page API truncation;cannot claim full capture: '+items[0])
            # Do not accept a truncated parent response as evidence of page content.
            # Recursive smaller batches keep the same IDs and close their own continuations.
            event={'selection':selection,'warning':warning,'split_into':[len(items)//2,len(items)-len(items)//2],'time_utc':stamp()}
            with (capture.root/'BATCH_SPLITS.jsonl').open('a',encoding='utf-8') as f:f.write(json.dumps(event,ensure_ascii=False)+'\n')
            print(f'API size boundary: splitting{len(items)} pages into{event["split_into"]}',flush=True)
            mid=len(items)//2;out=[]
            for batch in (items[:mid],items[mid:]):out+=fetch(capture,**(selection|{field:'|'.join(batch)}))
            return out
    if 'pageids' in selection and hasattr(capture,'pages'):
        for page in d['query']['pages']:
            if page.get('revisions'):capture.pages[page['pageid']]=page
    return d['query']['pages']

def extract(root):
    """No prose discarded from raw capture. This index is only a review aid."""
    counts=Counter();by_source={};section_counts=Counter();priority=[]
    with (root/'text_candidates.jsonl').open('w',encoding='utf-8',newline='\n') as output:
        for page in rows(root/'source_pages.jsonl'):
            text=page['raw_wikitext'];counts['source_pages']+=1;seen=set()
            # Full nonempty raw lines retain *all* prose,including non-game narrative,code,and markup.
            # They are not represented as an all-game translation queue.
            for number,line in enumerate(text.splitlines(),1):
                if line.strip():section_counts['all_nonempty_raw_lines']+=1
            for start,end,name,raw in templates(text):
                kind=name.strip().casefold();parts=split_parameters(raw[2:-2]);entries=[]
                if kind=='dialogue':entries.append(('dialogue_block','content','|'.join(parts[1:])))
                for n,p in enumerate(parts[1:],1):
                    if '=' in p:
                        key,value=p.split('=',1);key=key.strip()
                        category='text_parameter' if re.search(r'(?:dialog|lore|desc|objective|goal|task|tooltip|flavou?r|message|instruction|title|text|name|label|ability|effect|perk|reward|option)',key,re.I) else 'unclassified_named_parameter'
                    else:
                        key=str(n);value=p;category='presentation_parameter' if kind in ('rmt','minecraft','color','red','green','yellow','gold','gray','grey','aqua','blue','white','purple','light purple','dark purple') else 'unclassified_positional_parameter'
                    # Retain unclassified fields too. They may be wiki prose or identifiers,
                    # never silently counted as certified missing game translations.
                    entries.append((category,key,value))
                for category,key,value in entries:
                    if not value.strip():continue
                    identity=(start,end,key,value)
                    if identity in seen:continue
                    seen.add(identity);visible=provisional_text(value);plain=re.sub(r'(?i)§[0-9a-fk-or]','',visible).strip()
                    if not re.search('[A-Za-z]',plain):continue
                    record={'id':f"p{page['pageid']}-r{page['revision']}-o{start}-{digest(key+value)[:12]}",'category':category,'page':page['title'],'namespace':page['namespace'],'source_url':page['source_url'],'revision':page['revision'],'template_name':name,'parameter':key,'char_start':start,'char_end':end,'template_raw_sha256':digest(raw),'raw_value':value,'provisional_legacy_text':visible,'provisional_plain_text':plain,'requires_markup_review':bool(re.search(r'\{\{|\}\}|<[^>]+>',plain)),'certified_game_text':False,'automatically_added_to_runtime':False}
                    output.write(json.dumps(record,ensure_ascii=False)+'\n');counts[category]+=1;by_source.setdefault(plain,[]).append(record['id'])
                    if page['title']=='Ryan':priority.append(record)
    jsonl(root/'candidate_index.jsonl',({'source':s,'occurrence_ids':ids,'translation_ru':'','review_status':'unreviewed_not_game_text_certification'} for s,ids in sorted(by_source.items())))
    jsonl(root/'RYAN_REVIEW.jsonl',priority);result=dict(counts)|dict(section_counts)|{'unique_provisional_candidates':len(by_source),'ryan_candidate_blocks':len(priority),'raw_pages_are_authoritative':True};write(root/'EXTRACTION.json',result);print('EXTRACTED '+json.dumps(result),flush=True)

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--output',type=Path);ap.add_argument('--offline',action='store_true');ap.add_argument('--extract-only',action='store_true');args=ap.parse_args();root=args.output or ROOT/'build'/('public-text116-'+uuid.uuid4().hex);root.mkdir(parents=True,exist_ok=True)
    if args.extract_only:
        manifest=json.loads((root/'MANIFEST.json').read_text(encoding='utf-8'));extract(root);manifest['extraction']=json.loads((root/'EXTRACTION.json').read_text(encoding='utf-8'));manifest['extraction_refreshed_utc']=stamp();write(root/'MANIFEST.json',manifest);return
    capture=Capture(root,args.offline)
    print('PUBLIC116 ROOT:'+str(root),flush=True);write(root/'STATUS.json',{'stage':'started','started_utc':stamp(),'all_live_server_text_known':False})
    site=capture.query(meta='siteinfo',siprop='general|statistics|namespaces|rightsinfo');write(root/'SITEINFO.json',site)
    priority=fetch(capture,titles='|'.join(PRIORITY));jsonl(root/'priority_pages.jsonl',(page_record(p) for p in priority if 'revisions' in p));write(root/'PRIORITY_STATUS.json',[{k:p.get(k) for k in ('title','pageid','missing','invalid')} for p in priority]);print('Saved priority Ryan/quest sources',flush=True)
    resources=[]
    for endpoint in RESOURCE_ENDPOINTS:
        url='https://api.hypixel.net/v2/'+endpoint
        try:
            data=capture.get(url);write(root/'hypixel-resources'/(endpoint.replace('/','-')+'.json'),data);resources.append({'endpoint':url,'saved':True,'success':data.get('success'),'profile_or_api_key_used':False})
        except Exception as e:resources.append({'endpoint':url,'saved':False,'error':type(e).__name__+': '+str(e),'profile_or_api_key_used':False})
    write(root/'HYPixel_RESOURCE_STATUS.json',resources)
    inventory=[];listing=[]
    for namespace in NAMESPACES:
        continuation={};seen=set();total=0;calls=0
        while True:
            d=capture.query(list='allpages',apnamespace=str(namespace),aplimit='500',**continuation);part=d['query']['allpages'];assert len({r['pageid'] for r in part})==len(part);inventory+=part;total+=len(part);calls+=1
            print(f'Inventory namespace{namespace}: {total} pages',flush=True)
            if 'continue' not in d:break
            continuation=d['continue'];token=json.dumps(continuation,sort_keys=True);assert token not in seen;seen.add(token)
        listing.append({'namespace':namespace,'pages':total,'requests':calls,'continuation_exhausted':True})
    assert len({p['pageid'] for p in inventory})==len(inventory);jsonl(root/'page_inventory.jsonl',inventory);write(root/'LISTING_COMPLETION.json',listing)
    inventory.sort(key=lambda p:p['pageid']);missing=[];captured=0
    pending=[p['pageid'] for p in inventory if p['pageid'] not in capture.pages]
    for at in range(0,len(pending),50):fetch(capture,pageids='|'.join(str(v) for v in pending[at:at+50]))
    with (root/'source_pages.jsonl').open('w',encoding='utf-8',newline='\n') as output:
        for start in range(0,len(inventory),50):
            batch=inventory[start:start+50];pages=fetch(capture,pageids='|'.join(str(p['pageid']) for p in batch));assert {p['pageid'] for p in pages}=={p['pageid'] for p in batch}
            for p in pages:
                if 'revisions' not in p:missing.append(p);continue
                output.write(json.dumps(page_record(p),ensure_ascii=False)+'\n');captured+=1
            output.flush();write(root/'STATUS.json',{'stage':'fetching_revisions','inventory_pages':len(inventory),'processed_inventory_pages':start+len(batch),'captured_pages':captured,'missing_pages':len(missing),'live_requests_this_run':capture.live,'cache_hits_this_run':capture.hits,'updated_utc':stamp()})
            print(f'Revisions {start+len(batch)}/{len(inventory)};saved{captured};missing{len(missing)}',flush=True)
    write(root/'MISSING_PAGES.json',missing);extract(root)
    manifest={'scope':'Every public current revision in enumerated gameplay-related wiki namespaces,including redirects,templates,modules,archive/alpha pages and all raw prose. Not private server scripts or all live packets.','source_api':API,'completed_utc':stamp(),'namespaces':list(NAMESPACES),'inventory_pages':len(inventory),'captured_pages':captured,'missing_pages':len(missing),'closed_namespace_listings':listing,'resources':resources,'all_live_server_text_known':False,'runtime_automatically_modified':False,'game_or_pack_modified':False,'profile_api_settings_verified':False,'live_requests_this_run':capture.live,'verified_cache_hits_this_run':capture.hits,'extraction':json.loads((root/'EXTRACTION.json').read_text(encoding='utf-8'))}
    write(root/'MANIFEST.json',manifest);write(root/'STATUS.json',{'stage':'captured_not_independently_verified','captured_pages':captured,'updated_utc':stamp()})
    (root/'LICENSE_SOURCES.md').write_text('# Attribution\n\nWiki texts: contributors to https://hypixelskyblock.minecraft.wiki/ . License and rightsinfo are preserved in SITEINFO.json. Each page has exact revision URL,timestamp,and raw text hash.\n\nHypixel API responses: https://api.hypixel.net/ ;Hypixel terms apply. Game texts remain the property of their respective authors.\n\nNot an official complete server text database. No player chat,private files,API key,or credentials were captured.\n',encoding='utf-8')
    print('CAPTURE COMPLETE:'+str(root),flush=True)

if __name__=='__main__':main()
