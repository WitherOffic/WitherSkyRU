"""Bounded public-corpus accounting. No automatic translation or production writes.

Run fetch once; prepare/probe/summarize are offline and deterministic for snapshots.
Exact keys and changed displays are diagnostics, never whole-game completeness.
"""
import argparse, csv, hashlib, json, re, urllib.request, urllib.error, zipfile
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'translation-source/coverage117-001'
ARCHIVE = ROOT / '../../rebuild/WitherSkyRU/build'
OLD = ARCHIVE / 'npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
CAP = ARCHIVE / 'five-pass117-20261021'
WIKI = ARCHIVE / 'public-text116-6ee5c58738c54ede81623ec912a75105'
COLOR = re.compile(r'(?i)§[0-9a-fk-or]')
API_COLORS = dict(zip(['black','dark_blue','dark_green','dark_aqua','dark_red','dark_purple','gold','gray','dark_gray','blue','green','aqua','red','light_purple','yellow','white','obfuscated','bold','strikethrough','underline','italic','reset'], '0123456789abcdefklmnor'))
FIELDS = {'name', 'description', 'lore', 'fulllore', 'displayname', 'title', 'text', 'unlocks', 'category_display'}

def digest(b): return hashlib.sha256(b).hexdigest()
def read(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def rows(p):
    if not p.exists() and p.parent==OUT and (OUT/'exports.zip').exists():
        import io
        with zipfile.ZipFile(OUT/'exports.zip') as z, z.open(p.name) as stream:
            for line in io.TextIOWrapper(stream,encoding='utf-8-sig'):
                if line.strip(): yield json.loads(line)
        return
    with p.open(encoding='utf-8-sig') as f:
        for line in f:
            if line.strip(): yield json.loads(line)
def write(p, v):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(v, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
def jsonl(p, values):
    p.parent.mkdir(parents=True, exist_ok=True)
    with p.open('w', encoding='utf-8', newline='\n') as f:
        for v in values: f.write(json.dumps(v, ensure_ascii=False)+'\n')
def legacy(s): return re.sub(r'%%([a-z_]+)%%', lambda m:'§'+API_COLORS[m[1]] if m[1] in API_COLORS else m[0], s)
def plain(s): return COLOR.sub('', legacy(s)).strip()

def fetch():
    endpoints = ['resources/skyblock/items', 'resources/skyblock/skills',
                 'resources/skyblock/collections', 'resources/skyblock/bingo',
                 'resources/skyblock/election', 'resources/achievements', 'resources/packs',
                 'resources/quests', 'resources/challenges']
    targets = [('api-'+e.replace('/', '-')+'.json', 'https://api.hypixel.net/v2/'+e) for e in endpoints]
    targets += [('api-documentation.html', 'https://api.hypixel.net/'),
                ('api-policy.html', 'https://developer.hypixel.net/policies/'),
                ('community-siteinfo.json', 'https://hypixelskyblock.minecraft.wiki/api.php?action=query&meta=siteinfo&format=json')]
    for m in read(CAP/'PASS1_REPOSITORIES.json'):
        targets.append(('head-'+m['repository'].replace('/', '--')+'.json',
                        'https://api.github.com/repos/'+m['repository']+'/commits/'+m['branch']))
    receipts = []
    for filename, url in targets:
        p = OUT/'snapshots'/filename; receipt = p.with_suffix(p.suffix+'.receipt.json')
        if receipt.exists():
            r = read(receipt)
            if r.get('sha256'): assert digest(p.read_bytes()) == r['sha256']
            receipts.append(r); continue
        r = {'url': url, 'retrieved_utc': datetime.now(timezone.utc).isoformat(), 'file': filename}
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'WitherSkyRU-coverage117/1.0 public-resource-research', 'Accept': 'application/json,text/html'})
            with urllib.request.urlopen(req, timeout=30) as response:
                b = response.read(16000000)
                assert len(b) < 16000000
                r.update(status=response.status, resolved_url=response.url, bytes=len(b), sha256=digest(b), content_type=response.headers.get('Content-Type'))
            p.parent.mkdir(parents=True, exist_ok=True); p.write_bytes(b)
        except (urllib.error.URLError, TimeoutError) as e:
            r.update(status=getattr(e, 'code', 'unavailable'), error=str(e))
        write(receipt, r); receipts.append(r)
        print(filename, r['status'], r.get('bytes', 0), flush=True)
    write(OUT/'sources.json', receipts)

def prepare():
    grouped = {}; probes = []; provenance = []
    def add(source, corpus, origin, flags=None, keep_nonlatin=False):
        source = plain(source)
        if not source or not keep_nonlatin and not re.search('[A-Za-z]', source): return None
        key = digest(source.encode())
        r = grouped.setdefault(key, {'id': key, 'source': source, 'corpora': [], 'origins': [], 'flags': []})
        assert r['source'] == source
        if corpus not in r['corpora']: r['corpora'].append(corpus)
        r['origins'].append(origin)
        for flag in flags or []:
            if flag not in r['flags']: r['flags'].append(flag)
        return key
    npc = list(rows(OLD/'translation_queue.jsonl'))
    assert len(npc) == 16244 and len({r['source'] for r in npc}) == 16244
    assert digest((OLD/'translation_queue.jsonl').read_bytes()) == 'fedac2aaafca9674634dd99eaac9a2435aaf6177a74cfb7b1ff485e62e938925'
    for r in npc:
        flags = [k for k in ('variable_definition', 'unresolved_markup', 'historical_heading', 'alpha_page') if r.get(k)]
        if not re.search('[A-Za-z]', r['source']): flags.append('nonlexical_separator_or_markup')
        i = add(r['source'], 'npc_frozen16244', {'url': r['source_url'], 'page': r['page'], 'revision': r['revision'], 'occurrence_ids': r['occurrence_ids']}, flags=flags, keep_nonlatin=True)
        probes.append({'surface': 'chat', 'ids': [i], 'lines': [r.get('source_legacy_line', r['source'])]})
    # Reuse the existing source parser, not the historical filtered missing-key queue.
    from export_npc_corpus77 import templates, split_parameters, provisional_text
    tagged = set()
    for page in rows(WIKI/'source_pages.jsonl'):
        raw = page['raw_wikitext']; assert digest(raw.encode()).lower() == page['raw_sha256'].lower()
        for start,end,name,block in templates(raw):
            if name.strip().casefold() != 'dialogue': continue
            value = '|'.join(split_parameters(block[2:-2])[1:])
            for n,line in enumerate(provisional_text(value).splitlines()):
                line = COLOR.sub('',line).strip()
                origin = {'url':page['source_url'],'page':page['title'],'revision':page['revision'],'raw_sha256':page['raw_sha256'],'block_start':start,'block_end':end,'line':n}
                if line.startswith(('Select an option:','NEW OBJECTIVE','QUEST COMPLETE','OBJECTIVE COMPLETE')):
                    i=add(line,'wiki_objective_choice',origin,flags=['wiki_variable_or_markup'] if re.search(r'\{[^}]+\}|<[^>]+>',line) else [])
                    if i: probes.append({'surface':'chat','ids':[i],'lines':[line]})
                for text in re.split(r'/(?=\[(?:NPC|BOSS|SECURITY)\] [^:\n]{1,100}:)',line):
                    if not re.match(r'^\[(?:NPC|BOSS|SECURITY)\] [^:\n]{1,100}:',text): continue
                    i=add(text,'wiki_tagged11409',origin,flags=['wiki_variable_or_markup'] if re.search(r'\{[^}]+\}|<[^>]+>|\[[^]]+\]',text.split(':',1)[1]) else [])
                    if i and text not in tagged: probes.append({'surface':'chat','ids':[i],'lines':[text]})
                    tagged.add(text)
    assert len(tagged)==11409, len(tagged)
    # New tagged messages with primary wiki revision/block provenance, kept separate from prose.
    for r in rows(WIKI/'NPC_DIALOGUE_REVIEW_QUEUE.jsonl'):
        i = add(r['source'], 'npc_tagged_additional', {'wiki_dialogue_origins': r['origins']}, flags=['wiki_markup_review'] if r['variable_or_markup_review'] else [])
        probes.append({'surface': 'chat', 'ids': [i], 'lines': [r['source']]})
    meta = next(m for m in read(CAP/'PASS1_REPOSITORIES.json') if m['repository'].startswith('NotEnoughUpdates/'))
    folder = CAP/'repositories/NotEnoughUpdates--NotEnoughUpdates-REPO'
    archive = folder/(meta['commit']+'.zip'); assert digest(archive.read_bytes()).lower() == meta['archive_sha256'].lower()
    with zipfile.ZipFile(archive) as z:
        for name in sorted(z.namelist()):
            if '/items/' not in name or not name.endswith('.json'): continue
            obj = json.loads(z.read(name)); rel = name.split('/', 1)[1]
            url = 'https://github.com/'+meta['repository']+'/blob/'+meta['commit']+'/'+rel
            text = obj.get('displayname', '')
            if text:
                i = add(text, 'neu_item_names', {'url': url, 'path': rel, 'json_path': ['displayname'], 'item_id': obj.get('internalname'), 'file_sha256': digest(z.read(name))})
                if i: probes.append({'surface': 'component', 'ids': [i], 'lines': [text]})
            ids = []
            for n, text in enumerate(obj.get('lore', [])):
                ids.append(add(text, 'neu_item_lore', {'url': url, 'path': rel, 'json_path': ['lore', n], 'item_id': obj.get('internalname'), 'file_sha256': digest(z.read(name))}, flags=['source_placeholder'] if re.search(r'\{[^}]+\}', text) else []))
            if ids: probes.append({'surface': 'tooltip', 'ids': [None]+ids, 'lines': [obj.get('displayname','')]+obj['lore']})
        member = next(n for n in z.namelist() if n.endswith('/constants/abiphone.json'))
        contacts = json.loads(z.read(member)); url='https://github.com/'+meta['repository']+'/blob/'+meta['commit']+'/constants/abiphone.json'
        for contact,obj in sorted(contacts.items()):
            ids=[add(text,'neu_abiphone_guide',{'url':url,'contact':contact,'json_path':[contact,'requirement',n]}) for n,text in enumerate(obj.get('requirement',[]))]
            if ids: probes.append({'surface':'tooltip','ids':[None]+ids,'lines':[contact]+obj['requirement']})
    def visit(value, path, corpus, url, filename, parent=''):
        if isinstance(value, dict):
            for k, v in value.items(): visit(v, path+[k], corpus, url, filename, k.casefold())
        elif isinstance(value, list):
            for n, v in enumerate(value): visit(v, path+[n], corpus, url, filename, parent)
        elif isinstance(value, str) and parent in FIELDS:
            for n,text in enumerate(value.splitlines()):
                i = add(text, corpus, {'url': url, 'snapshot': filename, 'json_path': path, 'line':n}, flags=['api_dynamic_placeholder'] if re.search(r'%%(?:value|critter)%%',text) else [])
                if i: probes.append({'surface': 'component', 'ids': [i], 'lines': [legacy(text)]})
    for receipt in read(OUT/'sources.json'):
        filename = receipt['file']
        if receipt['status'] != 200 or not (filename.startswith('api-resources-skyblock-') or filename=='api-resources-achievements.json'): continue
        d = read(OUT/'snapshots'/filename); assert d['success']
        if 'achievements' in filename:
            visit(d['achievements']['skyblock'], ['achievements', 'skyblock'], 'api_skyblock_achievements', receipt['url'], filename)
        else:
            corpus = 'api_'+filename.removeprefix('api-resources-skyblock-').removesuffix('.json')
            visit(d, [], corpus, receipt['url'], filename)
    for r in read(ROOT/'translation-source/MELODY_PROGRESS_117.json')['records']:
        i = add(r['source'], 'melody_fixed30', {'registry': 'translation-source/MELODY_PROGRESS_117.json', 'status': r['status']})
        probes.append({'surface': 'component', 'ids': [i], 'lines': [r['source']]})
    for r in grouped.values():
        if r['source'] == 'I love the warmth of the Holidays despite the snow': r['flags'].append('required_english_input_keep')
    jsonl(OUT/'corpus.jsonl', sorted(grouped.values(), key=lambda r:r['id']))
    jsonl(OUT/'probes.jsonl', probes)
    info = {'normalization': 'API multiline fields split at explicit newlines, not visual wrapping. Convert only recognized API %%color%%/style markers to legacy; remove legacy colors; trim outer whitespace; preserve variables, inner whitespace and case; SHA256 of UTF8 source ID. Nonlexical rows retained in historical NPC denominator for comparability; other corpora require ASCII letters.',
            'counts': dict(Counter(c for r in grouped.values() for c in r['corpora'])), 'unique_union': len(grouped),
            'probe_groups': len(probes), 'probe_slots': sum(sum(i is not None for i in r['ids']) for r in probes),
            'npc_queue_sha256': digest((OLD/'translation_queue.jsonl').read_bytes()), 'neu_commit': meta['commit'], 'neu_archive_sha256': digest(archive.read_bytes()),
            'source_limits': 'NEU public repository templates include placeholders/test/unobtainable items; wiki includes historical/alpha/variables. None is all live game packets. Rows retained and flagged, not silently removed.',
            'wiki_prose_candidates_excluded': 138828}
    write(OUT/'prepared.json', info); print(json.dumps(info, ensure_ascii=False))

def summarize():
    inventory = list(rows(OUT/'runtime-inventory.jsonl')); exact = defaultdict(list)
    for r in inventory: exact[plain(r['source'])].append(r)
    observations = defaultdict(list)
    for r in rows(OUT/'runtime-probes.jsonl'): observations[r['id']].append(r)
    result = []
    for r in rows(OUT/'corpus.jsonl'):
        refs = exact.get(r['source'], []); obs = observations[r['id']]; assert obs
        changed = [o for o in obs if plain(o['output']) != r['source']]
        matched = bool(refs)
        state = 'registered_exact' if matched else 'changed_without_exact_review' if changed else 'unchanged_review'
        r.update(exact_records=refs, status=state, offline_changed=bool(changed),
                 identity_exact_only=bool(refs) and all(plain(e['translation']) == r['source'] for e in refs),
                 russian_exact_target=any(re.search('[А-Яа-яЁё]',e['translation']) for e in refs),
                 offline_outputs=sorted({o['output'] for o in obs}),
                 surfaces=sorted({o['surface'] for o in obs}), probe_occurrences=len(obs),
                 unchanged_occurrences=sum(plain(o['output']) == r['source'] for o in obs))
        result.append(r)
    summary = {}
    groups = sorted({c for r in result for c in r['corpora']})+['unique_union']
    for corpus in groups:
        selected = [r for r in result if corpus == 'unique_union' or corpus in r['corpora']]
        counts = Counter(r['status'] for r in selected); total = len(selected)
        summary[corpus] = dict(total=total, **counts, exact_percent=round(100*counts['registered_exact']/total, 4),
                               offline_changed=sum(r['offline_changed'] for r in selected),
                               exact_but_unchanged=sum(r['status']=='registered_exact' and not r['offline_changed'] for r in selected),
                               identity_exact_only=sum(r['identity_exact_only'] for r in selected),
                               russian_exact_target=sum(r['russian_exact_target'] for r in selected),
                               flagged_source_review=sum(bool(r['flags']) for r in selected))
    jsonl(OUT/'coverage.jsonl', result)
    for status in ['registered_exact', 'changed_without_exact_review', 'unchanged_review']:
        jsonl(OUT/(status+'.jsonl'), (r for r in result if r['status']==status))
    with (OUT/'coverage.csv').open('w', encoding='utf-8-sig', newline='') as f:
        fields=['id','source','corpora','status','exact_target','offline_changed','offline_outputs','flags','source_url']
        writer=csv.DictWriter(f, fieldnames=fields); writer.writeheader()
        for r in result:
            writer.writerow(dict(id=r['id'],source=r['source'],corpora=';'.join(r['corpora']),status=r['status'],
                exact_target=' | '.join(sorted({e['translation'] for e in r['exact_records']})),offline_changed=r['offline_changed'],
                offline_outputs=' | '.join(r['offline_outputs']),flags=';'.join(r['flags']),source_url=' | '.join(o.get('url','') for o in r['origins'][:3])))
    write(OUT/'summary.json', summary)
    print(json.dumps(summary,ensure_ascii=False,indent=2))

def verify():
    result=list(rows(OUT/'coverage.jsonl')); by_source={r['source']:r for r in result}
    assert len(by_source)==len(result)
    for r in result:
        assert r['id']==digest(r['source'].encode()) and r['origins'] and r['probe_occurrences']>0
        assert 0<=r['unchanged_occurrences']<=r['probe_occurrences']
        for e in r['exact_records']: assert plain(e['source'])==r['source']
    summary=read(OUT/'summary.json')['unique_union']
    assert summary['total']==len(result)
    assert summary['total']==sum(summary.get(k,0) for k in ['registered_exact','changed_without_exact_review','unchanged_review'])
    fixture=read(ROOT/'translation-source/BULK117_001_ACCEPTANCE.json')['records']; assert len(fixture)==1000
    padded=[]
    for r in fixture:
        assert r['source'] in by_source, r['source']
        outputs=by_source[r['source']]['offline_outputs']
        assert any(plain(text)==plain(r['translation']) for text in outputs), r['source']
        if r['translation'] not in outputs: padded.append({'batch_index':r['batch_index'],'source':r['source'],'expected_unpadded':r['translation'],'observed':outputs})
    write(OUT/'recent-accepted-context-review.json',{'matched_after_documented_outer_padding_normalization':len(fixture),'preserved_source_padding_cases':padded,'scope':'Source identity already trims outer padding. Padding is retained by the renderer, not a translation failure. Independent fixture targets unchanged; no style or in-game validation implied.'})
    assert sum(1 for _ in csv.DictReader((OUT/'coverage.csv').open(encoding='utf-8-sig',newline='')))==len(result)
    assert sum(r['probe_occurrences'] for r in result)==sum(1 for _ in rows(OUT/'runtime-probes.jsonl'))
    for r in read(OUT/'sources.json'):
        if r.get('sha256'): assert digest((OUT/'snapshots'/r['file']).read_bytes())==r['sha256']
    patterns=list(rows(OUT/'runtime-patterns.jsonl')); assert len(patterns)==1231
    inventory=list(rows(OUT/'runtime-inventory.jsonl')); grouped=defaultdict(list)
    for r in inventory: grouped[r['source']].append(r)
    duplicate={k:v for k,v in grouped.items() if len(v)>1}
    write(OUT/'duplicate-provider-keys.json',{'same_source_multiple_providers':duplicate,'conflicting_target_count':sum(len({r['translation'] for r in v})>1 for v in duplicate.values()),'note':'Different routes may intentionally share a key. Two wording variants require editorial consistency review, not evidence of a functional crash or duplicate new translation.'})
    baseline=read(ROOT/'translation-source/workflow/bulk117-001.json')
    for name,h in baseline['file_sha256'].items():
        if name!='docs/DEV_STATUS.md': assert digest((ROOT/name).read_bytes()).lower()==h.lower(), name
    report={'result':'PASS','unique_source_ids':len(result),'runtime_probe_occurrences':sum(r['probe_occurrences'] for r in result),'accepted_bulk117_001_expected_targets_matched':len(fixture),'inventory_records':len(inventory),'inventory_unique_sources':len(grouped),'duplicate_keys_across_providers':len(duplicate),'regex_definitions':len(patterns),'original_production_and_previous_evidence_hashes_unchanged':True,'jar_sha256':digest((ROOT/'build/libs/WitherSkyRU-26.2-fabric-0.16.7-dev.jar').read_bytes()),'scope':'Source provenance/accounting and offline diagnostics, not an editorial review of all targets or an in-game test.'}
    write(OUT/'verification.json',report);print(json.dumps(report,ensure_ascii=False))

if __name__ == '__main__':
    ap=argparse.ArgumentParser();ap.add_argument('phase',choices=['fetch','prepare','summarize','verify']);args=ap.parse_args()
    {'fetch':fetch,'prepare':prepare,'summarize':summarize,'verify':verify}[args.phase]()
