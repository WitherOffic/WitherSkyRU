"""Extract actual UI slot title/lore lines from the pinned public wiki source.

No prose, inferred visual wrapping, inherited aliases, or executable Lua are imported.
Module:UI revision836871 documents comma escaping/slot fields; slash rows are explicit.
Run only to prepare this batch, not on each future continuation.
"""
import sys, json, re, hashlib, zipfile, html
from pathlib import Path
from collections import Counter
from export_npc_corpus77 import templates, split_parameters

ROOT=Path(__file__).resolve().parents[1]
WIKI=ROOT/'../../rebuild/WitherSkyRU/build/public-text116-6ee5c58738c54ede81623ec912a75105/source_pages.jsonl'
OUT=ROOT/'build/bulk117002'
COLOR=re.compile(r'(?i)§[0-9a-fk-or]')
def plain(s): return COLOR.sub('',s).strip()
def sha(s): return hashlib.sha256(s.encode()).hexdigest()
def decode(s):
    s=html.unescape(s)
    s=re.sub(r'\\([,/])',lambda m:'\uefff' if m[1]=='/' else ',',s)
    # Unknown wiki/JS expansion is not certified as an actual game literal.
    if '\\' in s or any(t in s for t in ('{{','}}','[[',']]','<','>','%inherit%','$n','{0}')): return None
    return re.sub(r'(?i)&([0-9a-fk-or])',r'§\1',s)
def slot(value):
    parts=re.split(r'(?<!\\),\s*',value.strip())
    if len(parts)!=4: return None
    title=decode(parts[2]);lore=decode(parts[3].replace('\n','/'))
    if title is None or lore is None or title.strip() in ('none','') or lore.strip()=='none': return None
    return [title.replace('\uefff','/')]+[s.replace('\uefff','/') for s in lore.split('/')]

def main():
    assert slot('Paper, none, &aTitle, &7Hello\\, world/&7At 1\\/2.')==['§aTitle','§7Hello, world','§7At 1/2.']
    assert slot('Paper, none, %inherit%, &7Anything') is None
    assert slot('Paper, none, &aA, &7Unknown\\stat{Health}') is None
    OUT.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(ROOT/'translation-source/coverage117-001/exports.zip') as z:
        exact={r['source'].strip().lower() for r in map(json.loads,z.read('runtime-inventory.jsonl').decode().splitlines())}
    selected={};contexts={};counts=Counter();modules={}
    for line in WIKI.open(encoding='utf-8'):
        page=json.loads(line);title=page['title']
        if title in ('Module:UI','Module:UIText','Module:Inventory slot'):
            modules[title]={k:page[k] for k in ('source_url','revision','raw_sha256')}
        if page['namespace']!=0 or not ('{{UI' in page['raw_wikitext'] or '{{Shop UI' in page['raw_wikitext']): continue
        raw=page['raw_wikitext'];assert sha(raw).lower()==page['raw_sha256'].lower()
        # Remove comments without changing offsets or exposing obsolete commented UI slots.
        active=re.sub(r'<!--[\s\S]*?-->',lambda m:' '*(m.end()-m.start()),raw)
        for start,end,name,block in templates(active):
            if name.strip().casefold() not in ('ui','shop ui'): continue
            args=split_parameters(block[2:-2]);menu=args[1].strip() if len(args)>1 else ''
            for n,arg in enumerate(args[2:],2):
                arg=arg.strip();key=str(n)
                if '=' in arg:
                    k,v=arg.split('=',1)
                    if not re.fullmatch(r'\d+\s*,\s*\d+',k.strip()): continue
                    key=k.strip();arg=v
                context=slot(arg)
                if not context:continue
                context_id=sha(title+'\0'+str(start)+'\0'+key)
                origin={'source_url':page['source_url'],'page':title,'revision':page['revision'],'raw_sha256':page['raw_sha256'],'template_start':start,'template_end':end,'template_name':name,'template_raw_sha256':sha(raw[start:end]),'slot':key,'menu':menu,'slot_raw':arg}
                contexts[context_id]={'origin':origin,'lines':context}
                for index,legacy in enumerate(context):
                    source=plain(legacy)
                    if len(source)<5 or len(source)>160 or not re.search('[A-Za-z]{3}',source):continue
                    if source.lower() in exact or source.startswith(('/', 'http:', 'https:')):continue
                    if any(t in source for t in ('%inherit%','{','}','[player]','[name]')):continue
                    # Prefer genuine literal UI instructions, not separate numeric values/shapes.
                    shape=re.sub(r'\d+(?:[.,]\d+)*','#',source).lower()
                    row=selected.setdefault(shape,{'source':source,'legacy':legacy,'source_id':sha(source),'context_id':context_id,'context_row_index':index,'origins':[],'kind':'wiki_ui_literal','status':'awaiting_baseline_and_editorial_review'})
                    row['origins'].append(origin|{'context_id':context_id,'context_row_index':index})
                    counts['occurrences']+=1
    values=list(selected.values())
    # Uniform semantic style rows are useful actual slots, not synthetic color variants.
    values.sort(key=lambda r:(len(re.findall(r'§[0-9a-fk-or]',r['legacy'][2:])),r['origins'][0]['page'],r['context_id'],r['context_row_index']))
    (OUT/'pool.json').write_text(json.dumps({'modules':modules,'contexts':contexts,'records':values,'counts':dict(counts)},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    grouped={}
    for r in values:grouped.setdefault(r['context_id'],[]).append(r)
    with (OUT/'probes.jsonl').open('w',encoding='utf-8') as f:
        for cid,rs in grouped.items():
            context=contexts[cid]['lines'];ids=[None]*len(context)
            for r in rs:ids[r['context_row_index']]=r['source_id']
            f.write(json.dumps({'surface':'tooltip','ids':ids,'lines':context},ensure_ascii=False)+'\n')
    print('UI candidates',len(values),'contexts',len(grouped),'uniform',sum(not re.search('§[0-9a-fk-or]',r['legacy'][2:]) for r in values))
    print(json.dumps(values[:12],ensure_ascii=False)[:2500])
if __name__=='__main__':main()
