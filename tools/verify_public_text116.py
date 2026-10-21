"""Independent completeness/hash checks on the explicit public namespace inventory."""
import argparse,json,hashlib,re,urllib.parse
from pathlib import Path
from collections import Counter
from export_public_text116 import ROOT,NAMESPACES,digest,rows,write,API

def main():
    ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);args=ap.parse_args();root=args.root;checks=Counter();requests={};captured={};listed={n:[] for n in NAMESPACES};continued=[]
    def check(ok,why,group):
        if not ok:raise AssertionError(why)
        checks[group]+=1
    for p in (root/'requests').glob('*.json'):
        r=json.loads(p.read_text(encoding='utf-8'));check(digest(r['body'])==r['body_sha256'],str(p),'response_hash');check(p.stem==digest(r['url']),str(p),'request_identity');body=json.loads(r['body']);requests[r['url']]=body
        if urllib.parse.urlsplit(r['url']).hostname!='hypixelskyblock.minecraft.wiki':continue
        params=dict(urllib.parse.parse_qsl(urllib.parse.urlsplit(r['url']).query));query=body.get('query',{})
        if params.get('list')=='allpages':listed[int(params['apnamespace'])].extend(query['allpages'])
        truncated=bool(re.search(r'truncat|too (?:many|large)',str(body.get('warnings',{})),re.I))
        # A rejected truncated parent is replaced by complete smaller requests over
        # the same IDs. Its own continuation is not also required:that would be a
        # different recovery strategy. Every replacement child must be captured.
        if 'continue' in body and not (params.get('pageids') and truncated):
            follow=params|{k:str(v) for k,v in body['continue'].items()};continued.append(API+'?'+urllib.parse.urlencode(sorted(follow.items())))
        if params.get('pageids'):
            check({int(v) for v in params['pageids'].split('|')}=={p['pageid'] for p in query['pages']},'batch omission','batch_identity')
            if truncated:
                ids=params['pageids'].split('|');check(len(ids)>1,'unresolved single-page truncation','truncation');mid=len(ids)//2
                for part in (ids[:mid],ids[mid:]):continued.append(API+'?'+urllib.parse.urlencode(sorted((params|{'pageids':'|'.join(part)}).items())))
                continue
            for page in query['pages']:
                if 'revisions' in page:
                    revision=page['revisions'][0];captured.setdefault(page['pageid'],set()).add((page['title'],revision['revid'],digest(revision['slots']['main']['content'])))
    for url in continued:check(url in requests,'pagination not closed '+url,'pagination')
    inventory=list(rows(root/'page_inventory.jsonl'));check(len(inventory)==len({p['pageid'] for p in inventory}),'duplicate inventory','inventory');inv={p['pageid']:p for p in inventory}
    for ns in NAMESPACES:
        expected={p['pageid']:p for p in listed[ns]};actual={p['pageid']:p for p in inventory if p['ns']==ns};check(expected==actual,'namespace listing missing '+str(ns),'inventory')
    saved={}
    for p in rows(root/'source_pages.jsonl'):
        check(p['pageid'] not in saved,'duplicate revision','revision');saved[p['pageid']]=p
        check(p['pageid'] in inv and inv[p['pageid']]['ns']==p['namespace'],'outside inventory','revision')
        check((p['title'],p['revision'],digest(p['raw_wikitext'])) in captured[p['pageid']],'saved content differs from all complete API responses','revision')
        check(digest(p['raw_wikitext'])==p['raw_sha256'],'raw text corruption','revision')
        check(p['source_url'].endswith('?oldid='+str(p['revision'])),'unpinned revision','revision')
    missing=json.loads((root/'MISSING_PAGES.json').read_text(encoding='utf-8'));check(set(saved)|{p['pageid'] for p in missing}==inv.keys(),'unaccounted pages','inventory')
    candidate_ids=set();by_source={};kinds=Counter()
    for r in rows(root/'text_candidates.jsonl'):
        check(r['id'] not in candidate_ids,'duplicate candidate','candidate');candidate_ids.add(r['id']);pageid=int(r['id'].split('-')[0][1:]);p=saved[pageid]
        check(p['title']==r['page'] and p['revision']==r['revision'],'bad candidate reference','candidate')
        raw=p['raw_wikitext'][r['char_start']:r['char_end']];check(digest(raw)==r['template_raw_sha256'],'truncated block','candidate')
        check(not r['certified_game_text'] and not r['automatically_added_to_runtime'],'unsupported quality claim','candidate');by_source.setdefault(r['provisional_plain_text'],[]).append(r['id']);kinds[r['category']]+=1
    queue=list(rows(root/'candidate_index.jsonl'));check(len(queue)==len(by_source),'candidate dedup mismatch','queue')
    for r in queue:check(r['occurrence_ids']==by_source[r['source']] and r['translation_ru']=='','lost duplicate provenance or invented translation','queue')
    manifest=json.loads((root/'MANIFEST.json').read_text(encoding='utf-8'));check(manifest['captured_pages']==len(saved) and manifest['inventory_pages']==len(inv),'manifest totals','manifest');check(not manifest['all_live_server_text_known'] and not manifest['runtime_automatically_modified'],'false complete-server claim','manifest')
    check('Ryan' in {p['title'] for p in saved.values()},'Ryan absent','observed_regressions')
    kelly=next(p for p in saved.values() if p['title']=='Kelly');check('Collect Spruce Logs' in kelly['raw_wikitext'],'screenshot objective absent from source','observed_regressions')
    check(any(r['source']=='Collect Spruce Logs' for r in queue),'condition/presentation objective lost again','observed_regressions')
    result={'all_passed':True,'checks':dict(checks),'total_assertions':sum(checks.values()),'inventory_pages':len(inv),'saved_pages':len(saved),'missing_pages':len(missing),'public_namespaces_exhausted':list(NAMESPACES),'candidate_occurrences':len(candidate_ids),'candidate_kinds':dict(kinds),'unique_review_candidates':len(queue),'captured_api_responses':len(requests),'all_live_server_text_known':False,'profile_settings_verified':False,'runtime_or_game_modified':False};write(root/'VERIFICATION.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
