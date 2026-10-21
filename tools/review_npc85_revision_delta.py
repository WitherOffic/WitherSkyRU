"""Save an exact public revision delta for manual review; never silently bless a changed page."""
import difflib,json,urllib.parse,urllib.request
from prepare_npc85 import SOURCE,CORPUS,rows,write
check=json.loads((SOURCE/'NPC_0.15.85_PUBLIC_REVISION.json').read_text(encoding='utf-8'))
old={r['title']:r for r in rows(CORPUS/'source_pages.jsonl')};records=[]
for title,versions in check['differences'].items():
    url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','revids':versions['current'],'prop':'revisions','rvprop':'ids|timestamp|content','rvslots':'main','format':'json','formatversion':'2'})
    with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-review/0.15.85 (read-only)'}),timeout=45) as stream:page=json.load(stream)['query']['pages'][0]
    revision=page['revisions'][0];raw=revision['slots']['main']['content'];diff='\n'.join(difflib.unified_diff(old[title]['raw_wikitext'].splitlines(),raw.splitlines(),fromfile=str(versions['pinned']),tofile=str(versions['current']),n=4))
    records.append({'title':title,'pinned_revision':versions['pinned'],'current_revision':versions['current'],'api_url':url,'current_page':page,'unified_diff':diff});print(title+'\n'+diff)
write(SOURCE/'NPC_0.15.85_REVISION_DELTA.json',{'scope':'Full exact revision contents and diff; requires manual review, not approval by extraction script','records':records})
