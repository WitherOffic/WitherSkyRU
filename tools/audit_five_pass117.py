"""Five complementary offline localisation audits,never a claim of all server scripts.

Sources are untrusted data. No downloaded repository code is executed or installed.
The published116 capture is read-only;all derived findings go to a separate root.
"""
from __future__ import annotations
import argparse,hashlib,json,re,time,urllib.request,urllib.error,zipfile
from pathlib import Path,PurePosixPath
from collections import Counter,defaultdict
from export_public_text116 import ROOT,rows,write,jsonl,digest,stamp
from prepare_public_review116 import exact_reference

PUBLIC=ROOT/'build/public-text116-6ee5c58738c54ede81623ec912a75105'
REPOS=('NotEnoughUpdates/NotEnoughUpdates-REPO','SkyHanniStudios/SkyHanni-REPO','SkyblockerMod/Skyblocker-Assets')
COLOR=re.compile(r'(?i)§[0-9a-fk-or]')
VISIBLE={'name','displayname','display_name','lore','description','desc','text','title','message','messages','dialogue','dialogues','objective','objectives','tooltip','tooltips','fulllore','requirement','requirements','hint','hints','hovertext','hover_text','short_description','long_description','instruction','instructions'}
TAG=re.compile(r'\[(?:NPC|BOSS|SECURITY)\]\s+[^:\n]{1,100}:')

def blob_sha(data):return hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()

def json_strings(value,path=()):
    if isinstance(value,str):yield path,value
    elif isinstance(value,dict):
        for k,v in value.items():yield from json_strings(v,path+(k,))
    elif isinstance(value,list):
        for i,v in enumerate(value):yield from json_strings(v,path+(i,))

def visible_field(path):return any(isinstance(x,str) and x.casefold() in VISIBLE for x in path)

class Download:
    def __init__(self,root):self.root=root;self.last=0
    def get(self,url,destination,limit=256*1024*1024):
        receipt=destination.with_suffix(destination.suffix+'.receipt.json')
        if destination.exists() and receipt.exists():
            record=json.loads(receipt.read_text(encoding='utf-8'));data=destination.read_bytes()
            assert record['url']==url and record['sha256']==digest(data)
            return data
        destination.parent.mkdir(parents=True,exist_ok=True)
        for attempt in range(4):
            time.sleep(max(0,1.1-(time.monotonic()-self.last)));self.last=time.monotonic()
            try:
                request=urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-source-audit/1.0 (read-only localization research)','Accept':'application/vnd.github+json' if 'api.github.com' in url else '*/*'})
                with urllib.request.urlopen(request,timeout=50) as response:
                    data=response.read(limit+1);assert len(data)<=limit,'Download size guard'
                    meta={'url':url,'resolved_url':response.url,'retrieved_utc':stamp(),'sha256':digest(data),'bytes':len(data),'content_type':response.headers.get('Content-Type'),'rate_limit_remaining':response.headers.get('X-RateLimit-Remaining')}
                if 'api.github.com' in url:json.loads(data)
                destination.write_bytes(data);write(receipt,meta);return data
            except urllib.error.HTTPError as e:
                # Do not bypass access controls or hammer a rate-limited endpoint.
                if e.code in (401,403,404,429):raise
                if attempt==3:raise
                time.sleep(2**attempt)
            except (urllib.error.URLError,TimeoutError):
                if attempt==3:raise
                time.sleep(2**attempt)
        raise AssertionError('unreachable')
    def api(self,url,destination):return json.loads(self.get(url,destination))

def capture_repositories(root):
    client=Download(root);reports=[]
    for repo in REPOS:
        out=root/'repositories'/repo.replace('/','--');api='https://api.github.com/repos/'+repo
        meta=client.api(api,out/'metadata.json');canonical=meta['full_name'];branch=meta['default_branch']
        commit=client.api(api+'/commits/'+branch,out/'commit.json');sha=commit['sha'];tree=client.api(api+'/git/trees/'+sha+'?recursive=1',out/'tree.json')
        assert not tree.get('truncated'),'Incomplete GitHub tree cannot certify source capture'
        archive=out/(sha+'.zip');client.get('https://codeload.github.com/'+canonical+'/zip/'+sha,archive)
        expected={r['path']:r for r in tree['tree'] if r['type']=='blob'};actual={};json_files=0;parse_errors=[];licenses=[];strings=[]
        with zipfile.ZipFile(archive) as z:
            assert z.testzip() is None
            for info in z.infolist():
                if info.is_dir():continue
                parts=PurePosixPath(info.filename).parts;assert len(parts)>1 and '..' not in parts
                name='/'.join(parts[1:]);assert name not in actual and name in expected
                data=z.read(info);assert blob_sha(data)==expected[name]['sha'],name
                actual[name]=digest(data)
                if PurePosixPath(name).name.upper().startswith(('LICENSE','COPYING')):
                    licenses.append({'path':name,'text':data.decode('utf-8-sig')});continue
                if not name.endswith(('.json','.lang')):continue
                if name.endswith('.lang'):
                    for i,line in enumerate(data.decode('utf-8-sig').splitlines(),1):
                        if '=' in line and not line.lstrip().startswith('#'):strings.append({'source':line.split('=',1)[1],'path':name,'json_path':['line',i],'likely_visible_field':True})
                    continue
                try:value=json.loads(data.decode('utf-8-sig'))
                except (ValueError,UnicodeError) as e:parse_errors.append({'path':name,'error':str(e)});continue
                json_files+=1
                for path,s in json_strings(value):
                    if re.search('[A-Za-z]',s):strings.append({'source':s,'path':name,'json_path':list(path),'likely_visible_field':visible_field(path)})
            # GitHub archive may omit export-ignore files;never call such a case complete.
            missing=sorted(set(expected)-set(actual));assert not missing,missing[:10]
        assert licenses,'Retain repository licence and provenance'
        jsonl(out/'TEXT_OCCURRENCES.jsonl',strings);write(out/'FILE_HASHES.json',actual);write(out/'LICENSES.json',licenses)
        for r in strings:r['repository']=canonical;r['commit']=sha;r['source_url']='https://github.com/'+canonical+'/blob/'+sha+'/'+r['path']
        jsonl(out/'TEXT_WITH_PROVENANCE.jsonl',strings)
        report={'repository':canonical,'branch':branch,'commit':sha,'commit_time':commit['commit']['committer']['date'],'files_verified_against_git_blob':len(actual),'json_files':json_files,'parse_errors':parse_errors,'strings_in_json_and_lang':len(strings),'likely_visible_occurrences':sum(r['likely_visible_field'] for r in strings),'license':meta.get('license'),'archive_sha256':digest(archive.read_bytes()),'all_tree_blobs_present':True}
        reports.append(report);print(json.dumps(report,ensure_ascii=False),flush=True)
    write(root/'PASS1_REPOSITORIES.json',reports);return reports

def raw_tagged_lines(text):
    """Independent line scan,not the nested-template walker used by116.

    Retain the complete raw line and offsets: formatting/row delimiters are not
    silently interpreted as game-visible prose. These are candidates,not keys.
    """
    offset=0
    for lineno,line in enumerate(text.splitlines(keepends=True),1):
        for match in TAG.finditer(line):
            yield {'line':lineno,'offset':offset+match.start(),'raw_line':line.rstrip('\r\n'),'raw_tagged_tail':line[match.start():].rstrip('\r\n')}
        offset+=len(line)

def source_audits(root):
    known=exact_reference();write(root/'EXACT_WORKTREE_REFERENCE.json',known)
    old_tagged={r['source'] for r in rows(PUBLIC/'NPC_DIALOGUE_REVIEW_QUEUE.jsonl')};priority=list(rows(PUBLIC/'REPORTED_NPC_PRIORITY_QUEUE.jsonl'))
    by_source={};by_speaker=defaultdict(set);page_counts=Counter();raw_hits=[];pages_selected=[];visible_templates=Counter();visible_parameters=Counter();case_counts=Counter()
    for p in rows(PUBLIC/'source_pages.jsonl'):
        raw=p['raw_wikitext'];assert digest(raw)==p['raw_sha256'];page_counts['total']+=1
        tags=list(raw_tagged_lines(raw))
        if tags:page_counts['pages_with_raw_speaker_tags']+=1
        for hit in tags:
            hit.update(page=p['title'],revision=p['revision'],source_url=p['source_url'],raw_sha256=p['raw_sha256']);raw_hits.append(hit)
        if p['title'] in ('Ryan','Melody','Melody/UI','Harp',"Melody's Harp",'Quests','The Rebuild','Kelly','Romero','Juliette'):
            pages_selected.append(p)
        # A completely separate raw parameter/line scan preserves context outside Dialogue.
        for lineno,line in enumerate(raw.splitlines(),1):
            if re.search(r'\b(?:Collect |Find the |Travel to |Talk to |Speak to |Return to |Claim |Select an option:)',line):
                s=line.strip();by_source.setdefault(s,[]).append({'page':p['title'],'line':lineno,'source_url':p['source_url']})
        # Counts only,not an alternate lossy renderer.
        for name in re.findall(r'\{\{\s*([^{}|\n]+)',raw):visible_templates[name.strip().casefold()]+=1
        for name in re.findall(r'^\s*\|\s*([^=|{}\n]{1,80})\s*=',raw,re.M):visible_parameters[name.strip().casefold()]+=1
    jsonl(root/'PASS2_INDEPENDENT_RAW_SPEAKER_SCAN.jsonl',raw_hits)
    jsonl(root/'PASS2_RAW_QUEST_CONTEXTS.jsonl',({'raw_line':s,'origins':v,'not_certified_game_string':True} for s,v in sorted(by_source.items())))
    write(root/'PASS2_SOURCE_STRUCTURE.json',{'pages':dict(page_counts),'raw_tag_occurrences':len(raw_hits),'quest_context_unique_raw_lines':len(by_source),'templates':dict(visible_templates.most_common()),'named_parameters':dict(visible_parameters.most_common()),'independent_from116_template_parser':True,'all_lines_retained_in_frozen_full_source':True})
    for r in priority:
        speaker=r['npc'];by_speaker[speaker].add(r['source']);case_counts[speaker+'_exact']+=r['source'] in known
    jsonl(root/'PASS3_PRIORITY_FULL_SOURCE.jsonl',pages_selected)
    jsonl(root/'PASS3_NPC_EDITORIAL_REVIEW.jsonl',({**r,'exact_worktree_records':known.get(r['source'],[]),'review_required':True} for r in priority))
    write(root/'PASS3_REPORTED_NPC_COUNTS.json',{'published_unique_messages':{s:len(v) for s,v in by_speaker.items()},'separate_exact_records':dict(case_counts),'review_messages':len(priority),'variable_layout_needs_runtime_test':True,'melody_live_missing_literal_not_supplied':True,'not_all_NPCs_declared_translated':True})
    # Cross-source item/lore strings,with no technical identifier silently certified.
    candidates={};stats=Counter()
    for p in (root/'repositories').glob('*/TEXT_WITH_PROVENANCE.jsonl'):
        for r in rows(p):
            stats['all_occurrences']+=1
            # Re-evaluate from the saved JSON path,so adding an observed visible
            # field does not require re-downloading the identical pinned archive.
            if not visible_field(tuple(r['json_path'])):continue
            s=COLOR.sub('',r['source']);stats['likely_visible_occurrences']+=1
            candidates.setdefault(s,[]).append(r)
    for s,origins in candidates.items():
        stats['unique_likely_visible']+=1;stats['with_exact_key']+=s in known
    jsonl(root/'PASS4_REPOSITORY_REVIEW_QUEUE.jsonl',({'source':s,'translation_ru':'','origins':v,'exact_worktree_records':known.get(s,[]),'status':'provisional_visible_field_not_runtime_coverage_proof','automatically_added_to_mod':False} for s,v in sorted(candidates.items())))
    stats['without_separate_exact_key']=stats['unique_likely_visible']-stats['with_exact_key']
    write(root/'PASS4_COUNTS.json',dict(stats))
    # Existing targets: flag,do not rewrite proper names or intentional formatting.
    issues=[];conflicts=[]
    for s,refs in known.items():
        targets={r['translation'] for r in refs}
        if len(targets)>1:conflicts.append({'source':s,'targets':refs,'status':'precedence_or_context_review_not_automatically_wrong'})
        for ref in refs:
            t=ref['translation'];flags=[]
            if '\ufffd' in t:flags.append('unicode_replacement_character')
            # Preserve numeric facts;translation may intentionally change representation.
            if Counter(re.findall(r'(?<![A-Za-z])[0-9]+(?:[.,][0-9]+)*',s))!=Counter(re.findall(r'(?<![A-Za-z])[0-9]+(?:[.,][0-9]+)*',t)):flags.append('numeric_tokens_differ_review')
            source_vars=Counter(re.findall(r'\{[A-Za-z_][A-Za-z_0-9]*\}|<[A-Za-z_][^<>\n]*>',s));target_vars=Counter(re.findall(r'\{[A-Za-z_][A-Za-z_0-9]*\}|<[A-Za-z_][^<>\n]*>',t))
            if source_vars!=target_vars:flags.append('variable_markers_differ_review')
            if re.search(r'(?i)§(?![0-9a-fk-or])',t):flags.append('unrecognised_legacy_format_review')
            if flags:issues.append({'source':s,**ref,'flags':flags,'not_proven_error':True})
    jsonl(root/'PASS5_EXISTING_TARGET_FLAGS.jsonl',issues);jsonl(root/'PASS5_MULTIPLE_TARGETS.jsonl',conflicts)
    write(root/'PASS5_STATIC_COUNTS.json',{'exact_sources':len(known),'flagged_target_records':len(issues),'sources_with_multiple_targets':len(conflicts),'flags':dict(Counter(f for r in issues for f in r['flags'])),'editorial_review_not_automatic_error_count':True,'game_fps_measured':False})
    print(json.dumps({'source_pages':page_counts['total'],'raw_tag_occurrences':len(raw_hits),'repository_review':dict(stats),'static_flags':len(issues)},ensure_ascii=False),flush=True)

def main():
    ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);ap.add_argument('--sources-only',action='store_true');a=ap.parse_args();a.root.mkdir(parents=True,exist_ok=True)
    capture_repositories(a.root)
    if not a.sources_only:source_audits(a.root)
    write(a.root/'AUDIT_SCOPE.json',{'time_utc':stamp(),'passes':['pinned_independent_repositories','independent_raw_wiki_scan','reported_NPC_full_context','cross_repository_visible_fields','existing_translation_static_checks'],'public116_source_unchanged':True,'all_private_live_server_scripts_available':False,'all_targets_manually_reviewed':False,'runtime_automatically_modified':False,'installed_game_modified':False})

if __name__=='__main__':main()
