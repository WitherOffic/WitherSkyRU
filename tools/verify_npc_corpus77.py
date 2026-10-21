"""Independent saved-file consistency gates; no network, no Minecraft."""
import argparse
import collections
import csv
import hashlib
import importlib.util
import json
from pathlib import Path
import re

def read_rows(path):
    return [json.loads(line) for line in path.read_text(encoding="utf-8-sig").splitlines() if line.strip()]

def sha(text):return hashlib.sha256(text.encode("utf-8")).hexdigest()

def main():
    parser=argparse.ArgumentParser();parser.add_argument("root",type=Path);args=parser.parse_args();root=args.root
    checks=collections.Counter()
    def check(ok,message,group):
        if not ok:raise AssertionError(message)
        checks[group]+=1
    requests=[];captured={};aliases={};missing=set();request_keys=set()
    for path in sorted((root/"requests").glob("*.json")):
        record=json.loads(path.read_text(encoding="utf-8"));check(sha(record["body"])==record["body_sha256"],str(path),"response_hashes")
        body=json.loads(record["body"]);check("error" not in body,"API error captured","responses")
        requests.append(record)
        query=record["query"]
        request_keys.add(json.dumps(query,sort_keys=True))
        data=body.get("query",{})
        for entry in data.get("normalized",[])+data.get("redirects",[]):aliases[entry["from"]]=entry["to"]
        for page in data.get("pages",[]):
            if page.get("missing"):missing.add(page["title"])
            if "revisions" in page:
                revision=page["revisions"][0];captured[(page["title"],revision["revid"])]=revision["slots"]["main"]["content"]
        if "continue" in body:
            continued={**query,**body["continue"]}
            # Checked after every request is loaded; pagination may be captured
            # in an earlier/later traversal order without losing its endpoint.
    for record in requests:
        body=json.loads(record["body"])
        if "continue" in body:
            follow={**record["query"],**body["continue"]}
            check(json.dumps(follow,sort_keys=True) in request_keys,"Pagination not closed: "+str(follow),"closed_pagination")
    sources=read_rows(root/"source_pages.jsonl");views=read_rows(root/"expanded_source_pages.jsonl");blocks=read_rows(root/"dialogue_blocks.jsonl");rows=read_rows(root/"dialogue_occurrences.jsonl");queue=read_rows(root/"translation_queue.jsonl");registry=read_rows(root/"page_registry.jsonl");outside=read_rows(root/"outside_dialogue_tagged_lines.jsonl")
    supplemental=read_rows(root/"supplemental_dialogue_blocks.jsonl")
    books=read_rows(root/"book_text_sections.jsonl")
    source_by_title={r["title"]:r for r in sources};view_by_title={r["title"]:r for r in views};block_by_id={r["block_id"]:r for r in blocks+supplemental}
    check(len(source_by_title)==len(sources)>1800,"Source count/duplicates","source_revisions")
    check(len(view_by_title)==len(views)==len(sources),"Views dropped pages","source_revisions")
    for source in sources:
        key=(source["title"],source["revision"])
        check(captured.get(key)==source["raw_wikitext"],"Saved API revision mismatch "+source["title"],"source_revisions")
        check(sha(source["raw_wikitext"])==source["raw_sha256"],"Raw SHA mismatch","source_revisions")
        check(source["source_url"].endswith("?oldid="+str(source["revision"])),"Unpinned URL","source_revisions")
    for view in views:
        check(sha(view["expanded_wikitext"])==view["expanded_sha256"],"Expanded SHA mismatch","expanded_revisions")
        for name,revision in view["shared_dialogue_revisions"].items():check((name,revision) in captured,"Unknown expanded dependency","expanded_revisions")
    check(len(block_by_id)==len(blocks)+len(supplemental) and len(blocks)>8000,"Block duplicates","blocks")
    by_page=collections.defaultdict(list)
    for block in blocks:
        view=view_by_title[block["page"]]
        check(view["expanded_wikitext"][block["char_start"]:block["char_end"]]==block["template_raw"],"Truncated/mislocated block "+block["block_id"],"blocks")
        check(block["revision"]==view["revision"],"Block revision mismatch","blocks")
        by_page[block["page"]].append(block)
    for block in supplemental:
        text=view_by_title[block["page"]]["expanded_wikitext"]
        check(text[block["char_start"]:block["char_end"]]==block["template_raw"],"Supplemental quote offset mismatch","supplemental")
        check(block["kind"] in {"quoted_template","colored_speaker_with_literal_tail","unwrapped_literal_line"},"Unsupported quote type","supplemental")
    for section in books:
        text=view_by_title[section["page"]]["expanded_wikitext"]
        check(text[section["char_start"]:section["char_end"]]==section["template_raw"],"Truncated journal section","books")
        check(bool(section["text_raw"].strip()) and bool(section["provisional_legacy_text"].strip()),"Empty journal content","books")
    check(len(books)==24,"Expected all 24 published Journal entries in this snapshot","books")
    occurrence_ids=set();grouped=collections.defaultdict(list)
    for row in rows:
        check(row["occurrence_id"] not in occurrence_ids,"Duplicate occurrence","occurrences");occurrence_ids.add(row["occurrence_id"])
        block=block_by_id[row["block_id"]]
        check(row["page"]==block["page"] and row["revision"]==block["revision"],"Row reference mismatch","occurrences")
        check(row["source"].strip()==row["source"] and bool(row["source"]),"Blank/unstable row","occurrences")
        grouped[row["source"]].append(row)
    check(len(grouped)==len(queue)==len({r["source"] for r in queue}),"Deduplication dropped or invented rows","queue")
    known=json.loads((root/"runtime_exact_reference.json").read_text(encoding="utf-8"))
    for row in queue:
        check(row["occurrence_ids"]==[r["occurrence_id"] for r in grouped[row["source"]]],"Missing duplicate conditions","queue")
        check(row["existing_resources"]==known.get(row["source"],[]),"Unjustified translation match","queue")
        check(row["translation_ru"]=="","Export has invented translations","queue")
    with (root/"translation_queue.tsv").open(encoding="utf-8-sig",newline="") as stream:tsv=list(csv.DictReader(stream,delimiter="\t"))
    check([r["source"] for r in tsv]==[r["source"] for r in queue],"TSV loss/corruption","tsv")
    for a,b in zip(tsv,queue):check(int(a["occurrence_count"])==len(b["occurrence_ids"]),"TSV occurrence mismatch","tsv")
    for r in registry:
        check(r["page"] in source_by_title,"Unknown registry entry","registry")
        check(r["dialogue_blocks"]==len(by_page[r["page"]]),"Registry count mismatch","registry")
        if r["dialogue_blocks"]:
            page=root/"pages"/(str(source_by_title[r["page"]]["pageid"])+".md")
            check(page.exists() and r["source_url"] in page.read_text(encoding="utf-8"),"Missing readable page","readable")
    def resolve(title):
        seen=set()
        while title in aliases:
            check(title not in seen,"Redirect cycle","discovery");seen.add(title);title=aliases[title]
        return title
    origins=json.loads((root/"inventory_origins.json").read_text(encoding="utf-8"))
    unresolved=[]
    for title in origins:
        final=resolve(title)
        if final not in source_by_title and final not in missing:unresolved.append(title)
    check(not unresolved,"Requested titles neither saved nor explicitly missing: "+str(unresolved),"discovery")
    # Independently verify every raw tagged-line occurrence is either in a
    # complete block or in the explicit outside/manual-review file.
    outside_keys={(r["page"],r["offset"],r["raw_line"]) for r in outside}
    for title,view in view_by_title.items():
        text=view["expanded_wikitext"]
        spans=[(r["char_start"],r["char_end"]) for r in by_page[title]]
        for match in re.finditer(r"(?m)^.*(?<!\[)\[(?:NPC|BOSS|SECURITY)\](?!\]).*$",text):
            covered=any(match.start()<b and match.end()>a for a,b in spans)
            check(covered or (title,match.start(),match[0]) in outside_keys,"Lost tagged source text "+title,"tagged_line_coverage")
        comments=[(m.start(),m.end()) for m in re.finditer(r"<!--.*?(?:-->|\Z)",text,re.S)]
        for match in re.finditer(r"\{\{\s*Dialogue\s*\|",text,re.I):
            if any(a<=match.start()<b for a,b in comments):continue
            check(any(b["char_start"]==match.start() for b in by_page[title]),"Opening missing from balanced blocks "+title,"independent_opening_scan")
    manifest=json.loads((root/"manifest.json").read_text(encoding="utf-8"))
    for key,count in (("captured_pages",len(sources)),("dialogue_blocks",len(blocks)),("line_occurrences",len(rows)),("unique_provisional_lines",len(queue))):check(manifest[key]==count,"Manifest count mismatch "+key,"manifest")
    check(not manifest["all_live_server_dialogue_known"] and not manifest["runtime_keys_automatically_imported"],"Unsupported completeness/installation claim","manifest")
    status_counts=collections.Counter(r["status"] for r in queue)
    check(dict(status_counts)==manifest["unique_status_counts"],"Status totals mismatch","manifest")
    check(not manifest["search_total_mismatch_warnings"],"Search result totals not fully exhausted","search_completion")
    for key,entries in manifest["inventories"].items():
        for entry in entries:
            if entry.get("ns",0) not in (0,10):continue
            title=resolve(entry["title"])
            check(title in source_by_title or title in missing,"Inventory entry not captured: "+key+":"+title,"inventory_coverage")
    arthur="[NPC] Arthur: Sorry but that's mine!"
    hendrik="[NPC] Hendrik: Thank goodness I don't have to see [NPC] anymore..."
    check(arthur in grouped,"Arthur narrative quote missing","regressions")
    check(hendrik in grouped,"Hendrik utterance split at placeholder","regressions")
    check("[NPC] Hendrik: Thank goodness I don't have to see" not in grouped,"Old truncated Hendrik key retained","regressions")
    check("[NPC] anymore..." not in grouped,"Invented speaker fragment retained","regressions")
    # A separate balanced-parser fixture suite handles nested/default template
    # boundaries; this gate does not count using the generator as an oracle.
    result={"checks":dict(checks),"total_assertions":sum(checks.values()),"all_passed":True,"source_pages":len(sources),"complete_blocks":len(blocks),"unique_queue_rows":len(queue),"outside_candidates":len(outside),"read_only_offline_verification":True,"live_server_completeness_proven":False,"fps_measured":False}
    (root/"VERIFICATION.json").write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(result,ensure_ascii=False,indent=2))

if __name__=="__main__":main()
