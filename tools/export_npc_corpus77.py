"""Read-only public MediaWiki corpus export. No game access; no machine translation.

The server's private scripts are not published by this API. A closed traversal of
the published inventory is deliberately NOT called every live NPC/branch.
Raw revision text and complete Dialogue blocks remain authoritative; parsed rows
are an explicitly provisional translation queue, never automatic runtime keys.
"""
from __future__ import annotations
import argparse
import csv
import hashlib
import html
import json
from pathlib import Path
import re
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timezone

PROJECT = Path(__file__).resolve().parents[1]
API = "https://hypixelskyblock.minecraft.wiki/api.php"
UA = "WitherSkyRU-public-dialogue-export/1.0 (offline Russian localization research; read-only)"

def stamp():
    return datetime.now(timezone.utc).isoformat()

def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def digest(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()

class Capture:
    def __init__(self, root, offline=False):
        self.root = root
        self.last = 0.0
        self.requests = 0
        self.cache_hits = 0
        self.offline = offline
        self.listing_stats = {}
        (root / "requests").mkdir(parents=True, exist_ok=True)
    def query(self, **params):
        p = {"action": "query", "format": "json", "formatversion": "2", "maxlag": "5", **params}
        query = urllib.parse.urlencode(sorted(p.items()))
        key = digest(query)
        path = self.root / "requests" / (key + ".json")
        if path.exists():
            saved = json.loads(path.read_text(encoding="utf-8"))
            assert saved["query"] == p and saved["body_sha256"] == digest(saved["body"])
            self.cache_hits += 1
            return json.loads(saved["body"])
        if self.offline:
            raise RuntimeError(f"No captured API response for offline traversal: {p}")
        for attempt in range(6):
            time.sleep(max(0.0, 1.05 - (time.monotonic() - self.last)))
            try:
                self.last = time.monotonic()
                request = urllib.request.Request(API + "?" + query, headers={"User-Agent": UA, "Accept": "application/json"})
                with urllib.request.urlopen(request, timeout=45) as response:
                    body = response.read().decode("utf-8")
                parsed = json.loads(body)
                if "error" in parsed:
                    if parsed["error"].get("code") in ("maxlag", "ratelimited", "readonly"):
                        time.sleep(min(30, 2 ** (attempt + 1)))
                        continue
                    raise RuntimeError(parsed["error"])
                write_json(path, {"url": API + "?" + query, "query": p, "retrieved_utc": stamp(), "body_sha256": digest(body), "body": body})
                self.requests += 1
                return parsed
            except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
                if attempt == 5:
                    raise RuntimeError(f"Public API request failed after retries: {p}") from error
                time.sleep(min(30, 2 ** (attempt + 1)))
        raise RuntimeError(f"Public API never cleared its lag/rate limit: {p}")
    def listed(self, name, **params):
        continuation = {}
        result = []
        seen = set()
        reported=None;requests=0
        while True:
            response = self.query(list=name, **params, **continuation)
            requests+=1
            if name=="search": reported=response.get("query",{}).get("searchinfo",{}).get("totalhits",reported)
            result.extend(response["query"][name])
            if "continue" not in response:
                stat_key=name+":"+json.dumps(params,sort_keys=True)
                self.listing_stats[stat_key]={"entries":len(result),"pages":requests,"reported_total_hits":reported,"continuation_exhausted":True,"reported_hits_exceed_returned":reported is not None and reported>len(result)}
                return result
            continuation = response["continue"]
            key = json.dumps(continuation, sort_keys=True)
            if key in seen:
                raise RuntimeError("Repeated pagination token")
            seen.add(key)

def pages_of(capture, titles, origins):
    out = []
    for start in range(0, len(titles), 35):
        names = titles[start:start+35]
        response = capture.query(prop="revisions|info", rvprop="ids|timestamp|content", rvslots="main", inprop="url", redirects="1", titles="|".join(names))
        for warning in response.get("warnings",{}).values():
            if re.search(r"(?i)truncat|too (?:many|large)",str(warning)):
                raise RuntimeError("API reported possible truncation; raw cache retained")
        for mapping in response["query"].get("normalized",[])+response["query"].get("redirects",[]):
            origins.setdefault(mapping["to"],set()).update(origins.get(mapping["from"],set()))
        out.extend(response["query"]["pages"])
        print(f"Fetched revision batch {start+len(names)}/{len(titles)}", flush=True)
    return out

def templates(text):
    """Balanced template spans; returns nested and parent templates without loss."""
    stack = []
    i = 0
    while i < len(text)-1:
        if text.startswith("<!--", i):
            end = text.find("-->", i+4)
            i = len(text) if end < 0 else end+3
            continue
        if text.startswith("{{{", i):
            end = text.find("}}}", i+3)
            i = len(text) if end < 0 else end+3
            continue
        if text.startswith("{{", i):
            stack.append(i)
            i += 2
        elif text.startswith("}}", i) and stack:
            start = stack.pop()
            raw = text[start:i+2]
            name = re.split(r"[|\n]", raw[2:-2], maxsplit=1)[0].strip()
            yield start, i+2, name, raw
            i += 2
        else:
            i += 1

def split_parameters(text):
    parts = []
    start = depth = links = 0
    i = 0
    while i < len(text):
        if text.startswith("{{", i): depth += 1; i += 2; continue
        if text.startswith("}}", i): depth -= 1; i += 2; continue
        if text.startswith("[[", i): links += 1; i += 2; continue
        if text.startswith("]]", i): links -= 1; i += 2; continue
        if text[i] == "|" and depth == 0 and links == 0:
            parts.append(text[start:i]); start = i+1
        i += 1
    parts.append(text[start:])
    return parts

COLOR = re.compile(r"(?i)[&§][0-9a-fk-or]")
FORMATS = {"black":"0","dark blue":"1","dark green":"2","dark aqua":"3","dark red":"4","dark purple":"5","gold":"6","gray":"7","grey":"7","dark gray":"8","blue":"9","green":"a","aqua":"b","red":"c","light purple":"d","yellow":"e","white":"f"}
def provisional_text(raw):
    text = re.sub(r"<!--.*?-->", "", raw, flags=re.S)
    # Only expand known presentation wrappers. Unknown mechanics/stat/item
    # templates are retained verbatim and explicitly require markup review.
    for _ in range(12):
        edits = []
        for a,b,name,value in templates(text):
            if "{{" in value[2:-2]: continue
            p = split_parameters(value[2:-2])
            key = name.casefold()
            replacement = None
            if key in FORMATS and len(p)==2:
                replacement = "§"+FORMATS[key]+p[1]+"§r"
            elif key in ("rmt", "minecraft", "nowrap", "nobr") and len(p)==2:
                replacement = p[1]
            elif key == "color" and len(p)==3 and p[1].strip().casefold() in FORMATS:
                replacement = "§"+FORMATS[p[1].strip().casefold()]+p[2]+"§r"
            elif key in ("!", "pipe") and len(p)==1:
                replacement = "|"
            elif key == "player" and len(p)==1:
                replacement = "<player>"
            if replacement is not None: edits.append((a,b,replacement))
        if not edits: break
        for a,b,replacement in sorted(edits, reverse=True): text=text[:a]+replacement+text[b:]
    text = re.sub(r"\[\[([^\[\]]+)\]\]", lambda m: m[1].split("|")[-1].split("#")[0], text)
    text = re.sub(r"\[(https?://\S+) ([^\]]+)\]", lambda m:m[2], text)
    text = re.sub(r"<br\s*/?>", "\n", text, flags=re.I)
    text = re.sub(r"</?(?:span|div|small|big|b|i|strong|em|code|p|nowiki)(?:\s[^>]*)?>", "", text, flags=re.I)
    # Explicit HTML entities keep their meaning. Otherwise legacy colour codes
    # take precedence: &centrance is red "entrance", not HTML's semicolon-less
    # &cent followed by "rance". Do not rewrite frozen exports in place.
    text = re.sub(r"&(?:#[0-9]+|#x[0-9a-f]+|[a-z][a-z0-9]+);|&([0-9a-fk-or])",
                  lambda m: "§"+m[1] if m[1] else m[0], text, flags=re.I)
    text = html.unescape(text).replace("\\/", "/")
    text = text.replace("'''", "").replace("''", "")
    return text

def runtime_translations():
    known = {}
    data = PROJECT / "src/main/resources/data/witherskyru"
    for path in sorted(data.glob("*.json")):
        value = json.loads(path.read_text(encoding="utf-8-sig"))
        if not isinstance(value,dict): continue
        for row in value.get("records",[]):
            source = row.get("source", "")
            if source.startswith(("[NPC] ", "[BOSS] ", "[SECURITY] ")):
                known.setdefault(source, []).append({"translation": row.get("translation", ""), "resource":path.name})
    return known

def split_visible_dialogue(text):
    # [NPC] can also occur *inside* an utterance as a placeholder. Only a
    # complete speaker prefix may start a second message on the same line.
    return [part.strip() for part in re.split(r"(?=\[(?:NPC|BOSS|SECURITY)\] [^:\n]{1,100}:)",text) if part.strip()]

def rows_for_block(block, known, start):
    rows=[]
    for line_no,line in enumerate(block["provisional_legacy_text"].splitlines()):
        stripped=COLOR.sub("",line).strip()
        if not stripped: continue
        for text in split_visible_dialogue(stripped):
            voice=re.match(r"^\[(NPC|BOSS|SECURITY)\] ([^:\n]{1,100}):\s*(.*)$",text)
            dynamic=bool(re.search(r"<[^>]+>|\[(?:[Pp]layer|[Uu]sername|[Pp]et|[Nn]ame|[Nn]umber)[^\]]*\]|\{[^}]+\}|#[a-zA-Z]|\b(?:X+|Y+)\b",text))
            if voice and re.search(r"\[NPC\]",voice[3]): dynamic=True
            unresolved="{{" in text or "}}" in text or bool(re.search(r"</?(?:table|ref|onlyinclude|includeonly|noinclude)\b",text))
            nonstandard=not voice and bool(re.match(r"^\[(?:NPC|BOSS|SECURITY)\]",text))
            status="markup_review" if unresolved else "variable_definition" if dynamic else "exact_resource_match_not_runtime_proof" if text in known else "needs_translation_review" if voice else "nonstandard_tag_review" if nonstandard else "unprefixed_or_choice_review"
            rows.append({"occurrence_id":block["block_id"]+f"-l{line_no}-f{start+len(rows)}","block_id":block["block_id"],"page":block["page"],"source_url":block["source_url"],"revision":block["revision"],"headings":block["headings"],"historical_heading":block["historical_heading"],"alpha_page":block["alpha_page"],"tag":voice[1] if voice else None,"speaker":voice[2] if voice else None,"kind":"tagged_dialogue" if voice else "unprefixed_or_choice","source_kind":block.get("kind","Dialogue_template"),"source":text,"source_legacy_line":line,"payload_line":line_no,"status":status,"variable_definition":dynamic,"nonstandard_tag_layout":nonstandard,"unresolved_markup":unresolved,"existing_resources":known.get(text,[]),"translation_ru":""})
    return rows

def outside_payloads(line):
    """Only complete quoted messages, never the following article narration.
    Missing colons/speakers are deliberately not invented. Raw outside lines
    still go to the manual-review appendix whether extraction succeeds or not.
    """
    out=[]
    for a,b,name,value in sorted(templates(line)):
        if name.casefold() not in {"rmt","color",*FORMATS}: continue
        visible=provisional_text(value)
        plain=COLOR.sub("",visible).strip()
        if re.match(r"^\[(?:NPC|BOSS|SECURITY)\] [^:\n]{1,100}: .+",plain):
            out.append((a,b,visible,"quoted_template"))
        elif re.fullmatch(r"\[(?:NPC|BOSS|SECURITY)\] [^:\n]{1,100}",plain):
            tail=line[b:]
            if re.fullmatch(r":\s*[^{}<>\n]+",tail):
                out.append((a,len(line),visible+provisional_text(tail),"colored_speaker_with_literal_tail"))
    if not out:
        visible=provisional_text(line);plain=COLOR.sub("",visible).strip()
        if re.match(r"^\[(?:NPC|BOSS|SECURITY)\] [^:\n]{1,100}: .+",plain):out.append((0,len(line),visible,"unwrapped_literal_line"))
    return out

def heading_at(raw, offset):
    heads = []
    for m in re.finditer(r"(?m)^(={2,6})\s*(.*?)\s*\1\s*$",raw[:offset]):
        level = len(m[1]); heads = [h for h in heads if h[0]<level]; heads.append((level,m[2]))
    return [h[1] for h in heads]

def template_title(name, page):
    if name.startswith(":"): return name[1:].strip()
    if name.startswith("/"): return page+name
    return name if name.startswith("Template:") else "Template:"+name

def dialogue_context(pages):
    texts={name:p["revisions"][0]["slots"]["main"]["content"] for name,p in pages.items()}
    bearing={name for name,text in texts.items() if name.startswith("Template:") and re.search(r"(?i)\{\{\s*Dialogue\s*\|",text)}
    for _ in range(8):
        extra={name for name,text in texts.items() if name.startswith("Template:") and any(template_title(n,name) in bearing for a,b,n,v in templates(text))}
        if extra<=bearing: break
        bearing.update(extra)
    return texts,bearing

def expand_shared_dialogues(page, pages, context):
    """Resolve captured dialogue-bearing transclusions, never evaluate Lua/code.

    Every dependency is an immutable saved revision. Any unsupported syntax is
    preserved and queued for manual review rather than guessed as server text.
    """
    texts,bearing=context
    provenance={}
    def expand(text, title, chain):
        edits=[];end=-1
        for a,b,name,value in sorted(templates(text),key=lambda t:(t[0],-t[1])):
            target=template_title(name,title)
            if target not in bearing or target in chain or a<end: continue
            fields=split_parameters(value[2:-2]);parameters={};position=1
            for field in fields[1:]:
                match=re.match(r"^\s*([A-Za-z0-9_ -]+)=(.*)$",field,re.S)
                if match: parameters[match[1].strip()]=match[2]
                else: parameters[str(position)]=field;position+=1
            dependency=pages[target];body=texts[target]
            only=re.findall(r"<onlyinclude>(.*?)</onlyinclude>",body,flags=re.I|re.S)
            if only: body="".join(only)
            body=re.sub(r"<noinclude>.*?</noinclude>","",body,flags=re.I|re.S)
            body=re.sub(r"</?includeonly>","",body,flags=re.I)
            body=re.sub(r"\{\{\{([^{}|]+)(?:\|([^{}]*))?\}\}\}",lambda m:parameters.get(m[1].strip(),m[2] if m[2] is not None else m[0]),body)
            if len(chain)>=12: raise RuntimeError("Shared dialogue expansion depth limit reached")
            body=expand(body,target,chain+(target,))
            provenance[target]=dependency["revisions"][0]["revid"]
            edits.append((a,b,body));end=b
        for a,b,body in reversed(edits): text=text[:a]+body+text[b:]
        return text
    source=texts[page]
    return expand(source,page,(page,)),provenance

def export(capture, pages, inventories, origins, missing, missing_dependencies):
    root=capture.root
    reference=root/"runtime_exact_reference.json"
    known=json.loads(reference.read_text(encoding="utf-8")) if capture.offline and reference.exists() else runtime_translations()
    write_json(root/"runtime_exact_reference.json",known)
    blocks=[]; rows=[]; registry=[]; sources=[]; unique={};views=[];outside=[];supplemental=[];book_sections=[]
    expansion_context=dialogue_context(pages)
    for page in sorted(pages.values(), key=lambda p:p["title"]):
        if "revisions" not in page: continue
        rev=page["revisions"][0]; raw=rev["slots"]["main"]["content"]
        title=page["title"]
        url="https://hypixelskyblock.minecraft.wiki/w/"+urllib.parse.quote(title.replace(" ","_"),safe="/:()")+"?oldid="+str(rev["revid"])
        sources.append({"pageid":page["pageid"],"title":title,"revision":rev["revid"],"parent_revision":rev["parentid"],"timestamp":rev["timestamp"],"source_url":url,"raw_sha256":digest(raw),"origins":sorted(origins.get(title,set())),"raw_wikitext":raw})
        raw,expansions=expand_shared_dialogues(title,pages,expansion_context)
        views.append({"title":title,"revision":rev["revid"],"expanded_wikitext":raw,"expanded_sha256":digest(raw),"shared_dialogue_revisions":expansions})
        page_blocks=page_rows=0
        page_historical=bool(re.search(r"(?i)\{\{\s*(?:Historical article|Removed(?: content)?)\b",raw))
        alpha_page=bool(re.search(r"(?i)/Alpha(?:$|/)",title))
        if title=="Journal":
            for a,b,name,value in sorted(templates(raw)):
                if name.casefold()!="textsection":continue
                fields=split_parameters(value[2:-2])
                text=next((field.strip()[5:] for field in fields[1:] if field.strip().startswith("text=")),None)
                if text is None:raise RuntimeError("Journal TextSection without text parameter")
                book_sections.append({"section_id":f"p{page['pageid']}-r{rev['revid']}-book{a}","page":title,"revision":rev["revid"],"source_url":url,"headings":heading_at(raw,a),"char_start":a,"char_end":b,"template_raw":value,"text_raw":text,"provisional_legacy_text":provisional_text(text),"scope":"Published journal entry, not verified live NBT pagination or every book in SkyBlock"})
        for a,b,name,value in sorted(templates(raw)):
            if name.strip().casefold() not in ("dialogue","dialog"): continue
            fields=split_parameters(value[2:-2])
            options={}
            payload_parts=[]
            for field in fields[1:]:
                option=re.match(r"^\s*(class|style|align)=(.*)$",field,re.S)
                if option: options[option[1]]=option[2]
                else: payload_parts.append(field)
            payload="|".join(payload_parts)
            headings=heading_at(raw,a)
            historical=page_historical or bool(re.search(r"(?i)removed|historical|old dialogue|previous dialogue|former|unused", " / ".join(headings)))
            blockid=f"p{page['pageid']}-r{rev['revid']}-o{a}"
            boundary=raw.rfind("|-",0,a)
            context=raw[max(0,boundary if boundary>=0 else a-1800):a]
            plain=provisional_text(payload)
            block={"block_id":blockid,"page":title,"source_url":url,"revision":rev["revid"],"coordinate_space":"expanded_source_pages.jsonl","shared_dialogue_revisions":expansions,"char_start":a,"char_end":b,"line_start":raw.count("\n",0,a)+1,"headings":headings,"historical_heading":historical,"alpha_page":alpha_page,"condition_context_raw":context,"template_options":options,"template_raw":value,"payload_raw":payload,"provisional_legacy_text":plain,"unresolved_markup": "{{" in plain or "}}" in plain}
            blocks.append(block); page_blocks+=1
            # Keep every nonblank line, including choices, unprefixed
            # continuations, objectives, sounds and hypothetical examples.
            added=rows_for_block(block,known,len(rows))
            rows.extend(added);page_rows+=len(added)
            for record in added: unique.setdefault(record["source"],[]).append(record["occurrence_id"])
        spans=[(block["char_start"],block["char_end"]) for block in blocks if block["page"]==title]
        comments=[(match.start(),match.end()) for match in re.finditer(r"<!--.*?(?:-->|\Z)",raw,re.S)]
        for match in re.finditer(r"(?m)^.*(?<!\[)\[(?:NPC|BOSS|SECURITY)\](?!\]).*$",raw):
            if not any(match.start()<b and match.end()>a for a,b in spans):
                commented=any(match.start()<b and match.end()>a for a,b in comments)
                entry={"page":title,"revision":rev["revid"],"source_url":url,"raw_line":match[0],"offset":match.start(),"headings":heading_at(raw,match.start()),"commented_out_in_publication":commented,"status":"outside_Dialogue_template_requires_review","extracted_block_ids":[]}
                if not commented:
                    for a,b,visible,kind in outside_payloads(match[0]):
                        offset=match.start()+a;end=match.start()+b
                        extra={"block_id":f"p{page['pageid']}-r{rev['revid']}-outside{offset}","page":title,"source_url":url,"revision":rev["revid"],"kind":kind,"coordinate_space":"expanded_source_pages.jsonl","char_start":offset,"char_end":end,"template_raw":raw[offset:end],"payload_raw":raw[offset:end],"provisional_legacy_text":visible,"headings":entry["headings"],"historical_heading":page_historical,"alpha_page":alpha_page,"condition_context_raw":match[0],"shared_dialogue_revisions":expansions}
                        supplemental.append(extra);entry["extracted_block_ids"].append(extra["block_id"])
                        added=rows_for_block(extra,known,len(rows));rows.extend(added);page_rows+=len(added)
                        for record in added: unique.setdefault(record["source"],[]).append(record["occurrence_id"])
                outside.append(entry)
        registry.append({"page":title,"revision":rev["revid"],"origins":sorted(origins.get(title,set())),"historical_page":page_historical,"alpha_page":alpha_page,"dialogue_blocks":page_blocks,"extracted_line_occurrences":page_rows,"has_unextracted_dialogue_heading":bool(re.search(r"(?im)^==+.*dialogue",raw)) and not page_blocks,"source_url":url})
    for filename,records in (("source_pages.jsonl",sources),("expanded_source_pages.jsonl",views),("dialogue_blocks.jsonl",blocks),("supplemental_dialogue_blocks.jsonl",supplemental),("book_text_sections.jsonl",book_sections),("dialogue_occurrences.jsonl",rows),("page_registry.jsonl",registry),("outside_dialogue_tagged_lines.jsonl",outside)):
        with (root/filename).open("w",encoding="utf-8",newline="\n") as stream:
            for record in records: stream.write(json.dumps(record,ensure_ascii=False)+"\n")
    by_source={}
    for row in rows:
        if row["source"] not in by_source:
            record=dict(row);record["occurrence_ids"]=unique[row["source"]];by_source[row["source"]]=record
    queue=list(by_source.values())
    readable=["# Опубликованные диалоги — корпус для перевода","","Оригинал: Hypixel SkyBlock Wiki. Точные ревизии указаны у каждой страницы.","Это не выгрузка закрытых серверных скриптов. Исходная разметка и цвета находятся в JSONL.","Общие шаблоны развёрнуты только из сохранённых ревизий; неподдержанная разметка не выдумывается.","Условия ниже предварительные: у таблиц с rowspan полный контекст нужно сверять в source_pages.jsonl.",""]
    blocks_by_page={}
    for block in blocks+supplemental: blocks_by_page.setdefault(block["page"],[]).append(block)
    (root/"pages").mkdir(exist_ok=True)
    page_index=["# Страницы опубликованных диалогов","","Это индекс сохранённого публичного корпуса, не полный серверный реестр.",""]
    for source in sources:
        selected=blocks_by_page.get(source["title"],[])
        if not selected: continue
        lines=["## "+source["title"],"",f"[Ревизия {source['revision']}]({source['source_url']})","",f"Блоков и отдельно извлечённых цитат: {len(selected)}.",""]
        for number,block in enumerate(selected,1):
            lines.extend(["### Блок "+str(number),"", "Ветка: "+(" / ".join(block["headings"]) or "без заголовка"),"Статус публикации: "+("историческая" if block["historical_heading"] else "Alpha-страница" if block["alpha_page"] else "не классифицирована как историческая/Alpha"),""])
            context=COLOR.sub("",provisional_text(block["condition_context_raw"])).strip()
            if context: lines.extend(["Контекст перед блоком (разметка вики):","",*['> '+line for line in context.splitlines()],""])
            lines.extend(['> '+line for line in COLOR.sub("",block["provisional_legacy_text"]).strip().splitlines()]+[""])
        (root/"pages"/(str(source["pageid"])+".md")).write_text("\n".join(lines)+"\n",encoding="utf-8")
        readable.extend(lines)
        page_index.append(f"- [{source['title']}](pages/{source['pageid']}.md): {len(selected)} блоков")
    (root/"NPC_DIALOGUES_EN.md").write_text("\n".join(readable)+"\n",encoding="utf-8")
    (root/"PAGE_INDEX_RU.md").write_text("\n".join(page_index)+"\n",encoding="utf-8")
    appendix=["# Дополнительные строки вне действующих блоков Dialogue","","Это не готовые серверные ключи. Включены закомментированные наброски публикации и цитаты в повествовании.","Все слова сохранены; неизвестные границы требуют ручной проверки.",""]
    for row in outside:
        appendix.extend(["## "+row["page"],"",f"[Исходная ревизия]({row['source_url']})","", "Закомментировано в публикации: "+("да" if row["commented_out_in_publication"] else "нет"),"","> "+row["raw_line"],""])
    (root/"OUTSIDE_DIALOGUE_REVIEW_RU.md").write_text("\n".join(appendix)+"\n",encoding="utf-8")
    book_reader=["# Дневники подземелий — опубликованные тексты","","Оригинал: Hypixel SkyBlock Wiki. Лицензия и авторство указаны в LICENSE_SOURCES.md.","Это полные опубликованные записи со страницы Journal, не доказательство полноты всех книг игры или оригинальной разбивки на страницы в NBT.",""]
    for section in book_sections:
        book_reader.extend(["## "+" / ".join(section["headings"]),"",f"[Исходная ревизия]({section['source_url']})","",COLOR.sub("",section["provisional_legacy_text"]),""])
    (root/"BOOKS_EN.md").write_text("\n".join(book_reader)+"\n",encoding="utf-8")
    with (root/"translation_queue.jsonl").open("w",encoding="utf-8",newline="\n") as stream:
        for row in queue: stream.write(json.dumps(row,ensure_ascii=False)+"\n")
    fields=["status","kind","tag","speaker","page","historical_heading","alpha_page","variable_definition","unresolved_markup","source","existing_translation_ru","translation_ru","source_url","occurrence_count","block_id"]
    with (root/"translation_queue.tsv").open("w",encoding="utf-8-sig",newline="") as stream:
        writer=csv.DictWriter(stream,fields,delimiter="\t");writer.writeheader()
        for row in queue:
            data={k:row.get(k,"") for k in fields};data["existing_translation_ru"]=" || ".join(dict.fromkeys(r["translation"] for r in row["existing_resources"]));data["occurrence_count"]=len(row["occurrence_ids"]);writer.writerow(data)
    counts={status:sum(r["status"]==status for r in queue) for status in sorted({r["status"] for r in queue})}
    manifest={"scope":"Closed read-only traversal of specified public MediaWiki inventories and referenced relative/Dialogue subpages. NOT private Hypixel server scripts, every live NPC/branch/packet, or a completed translation.","date_utc":stamp(),"source_api":API,"wiki_license":"CC BY-NC-SA 3.0; see LICENSE_SOURCES.md","inventories":inventories,"successful_live_requests_this_run":capture.requests,"verified_request_cache_hits":capture.cache_hits,"captured_pages":len(sources),"dialogue_blocks":len(blocks),"line_occurrences":len(rows),"unique_provisional_lines":len(queue),"unique_tagged_dialogue_lines":sum(r["kind"]=="tagged_dialogue" for r in queue),"unique_speakers":len({r["speaker"] for r in rows if r["speaker"]}),"unique_status_counts":counts,"pages_with_unextracted_dialogue_heading":[r["page"] for r in registry if r["has_unextracted_dialogue_heading"]],"missing_or_deleted_inventory_pages":sorted(missing),"missing_relative_or_dialogue_dependencies":sorted(missing_dependencies),"all_live_server_dialogue_known":False,"runtime_files_modified_by_export":False,"game_started":False,"runtime_keys_automatically_imported":False}
    write_json(root/"manifest.json",manifest)
    manifest["literal_speaker_names_excluding_definition_placeholders"]=len({r["speaker"] for r in rows if r["speaker"] and not re.search(r"[{}<>]",r["speaker"])})
    publication={}
    for row in rows:
        flags=publication.setdefault(row["source"],{"historical":True,"alpha":True})
        flags["historical"] &= row["historical_heading"]
        flags["alpha"] &= row["alpha_page"]
    manifest["historical_only_unique_line_candidates"]=sum(v["historical"] for v in publication.values())
    manifest["alpha_only_unique_line_candidates"]=sum(v["alpha"] for v in publication.values())
    manifest["outside_dialogue_tagged_line_candidates"]=len(outside)
    manifest["outside_dialogue_commented_candidates"]=sum(r["commented_out_in_publication"] for r in outside)
    manifest["supplemental_quoted_dialogue_blocks"]=len(supplemental)
    manifest["published_journal_text_sections"]=len(book_sections)
    manifest["unique_nonstandard_tag_rows"]=sum(q["nonstandard_tag_layout"] for q in queue)
    manifest["listing_completion"]=capture.listing_stats
    manifest["search_total_mismatch_warnings"]=[k for k,v in capture.listing_stats.items() if v["reported_hits_exceed_returned"]]
    manifest["pages_with_expanded_shared_dialogue_templates"]=sum(bool(v["shared_dialogue_revisions"]) for v in views)
    write_json(root/"manifest.json",manifest)
    write_json(root/"inventory_origins.json",{k:sorted(v) for k,v in sorted(origins.items())})
    (root/"LICENSE_SOURCES.md").write_text("# Public source attribution\n\nText: contributors to Hypixel SkyBlock Wiki, https://hypixelskyblock.minecraft.wiki/ .\nLicense: Creative Commons Attribution-NonCommercial-ShareAlike 3.0, https://creativecommons.org/licenses/by-nc-sa/3.0/ .\nEach source record includes page, exact revision URL, revision timestamp and raw text SHA-256.\nGame dialogue belongs to Hypixel and its respective authors.\nThe provisional normalization/extraction is not an official Hypixel database.\nAuthor/player-written Island NPC content, private scripts and undisclosed server variants are outside the public inventory.\n",encoding="utf-8")
    report=["# Выгрузка опубликованных диалогов NPC — 2026-10-16","","Это корпус для дальнейшей вычитки и перевода, не обещание полного охвата закрытых серверных скриптов.","Исходные страницы и целые блоки Dialogue сохранены без потери текста. Разбор строк предварительный.","","| Показатель | Количество |","| --- | ---: |",f"| Страниц с зафиксированной ревизией | {len(sources)} |",f"| Полных блоков Dialogue | {len(blocks)} |",f"| Вхождений непустых строк | {len(rows)} |",f"| Уникальных предварительно разобранных строк | {len(queue)} |",f"| Уникальных строк с тегом говорящего | {manifest['unique_tagged_dialogue_lines']} |",f"| Различных имён говорящих | {manifest['unique_speakers']} |",""]
    labels={"exact_resource_match_not_runtime_proof":"Найден точный ключ в ресурсах (не доказательство вывода в игре)","markup_review":"Нужна проверка разметки","needs_translation_review":"Нет точного ключа; проверить перевод и динамический обработчик","nonstandard_tag_review":"Нестандартный тег: нельзя выдумывать имя или двоеточие","unprefixed_or_choice_review":"Ответы игрока, продолжения и служебные строки без тега","variable_definition":"Определения с переменными; не готовые точные ключи"}
    report += ["| Статус очереди | Количество |","| --- | ---: |"]+[f"| {labels[k]} | {v} |" for k,v in counts.items()]
    report += ["",f"Буквальных имён говорящих без обозначений переменных: {manifest['literal_speaker_names_excluding_definition_placeholders']}. Включены исторические и Alpha-материалы, это не число действующих NPC.",f"Развёрнуты общие шаблоны на {manifest['pages_with_expanded_shared_dialogue_templates']} страницах. Вне блоков отдельно сохранено {len(outside)} строк-кандидатов, включая закомментированные материалы.","","## Зафиксированные пробелы источника","","- Baking Specialist: историческая статья содержит заголовок Dialogue, но вместо реплик стоит InfoNeeded.","- Teddy/UI: отсутствующая в вики интерфейсная подстраница; диалоги самого Teddy получены.","- Неопубликованные серверные ветки, случайные варианты и пользовательские Island NPC не объявляются выгруженными."]
    report += ["","Совпадение с ресурсом не доказывает корректность маршрута, цвета и вывода в игре. Отсутствие точного ключа не исключает динамического переводчика.","Переменные, неразобранные шаблоны, ответы игрока, исторические ветки и повторы условий сохранены отдельно.","","## Файлы","","- `NPC_DIALOGUES_EN.md`: читаемая книга оригинальных диалогов; отдельные страницы доступны через `PAGE_INDEX_RU.md`.","- `source_pages.jsonl`: исходный текст всех сохранённых ревизий целиком.","- `expanded_source_pages.jsonl`: воспроизводимый текст с развёрнутыми общими шаблонами и номерами их ревизий.","- `dialogue_blocks.jsonl`: целые блоки, включая цветовые коды и контекст условий.","- `dialogue_occurrences.jsonl`: все вхождения предварительно разобранных строк.","- `translation_queue.tsv` и `.jsonl`: уникальные строки с пустой колонкой для нового русского перевода и имеющимся точным ресурсом, если найден.","- `OUTSIDE_DIALOGUE_REVIEW_RU.md`: цитаты и закомментированные наброски вне обычных блоков.","- `page_registry.jsonl`: страницы без Dialogue тоже включены; отсутствие блока не означает отсутствия реплик в игре.","- `requests/`: исходные ответы API, параметры, время получения и контрольная сумма тела.","- `manifest.json`: счётчики и явно сохранённые ограничения.","","В игровой JAR эта выгрузка автоматически не добавляется. Замеров FPS здесь нет."]
    (root/"README_RU.md").write_text("\n".join(report)+"\n",encoding="utf-8")
    print(json.dumps({k:v for k,v in manifest.items() if k not in ("inventories","pages_with_unextracted_dialogue_heading","listing_completion")},ensure_ascii=False,indent=2),flush=True)

def main():
    parser=argparse.ArgumentParser();parser.add_argument("--output",type=Path);parser.add_argument("--offline",action="store_true");args=parser.parse_args()
    root=args.output or PROJECT/"build"/("npc-corpus77-"+uuid.uuid4().hex)
    root.mkdir(parents=True,exist_ok=True)
    print("CORPUS ROOT: "+str(root),flush=True)
    capture=Capture(root,args.offline);origins={};inventories={};wanted=set();missing=set();missing_dependencies=set()
    def add(title,origin):
        title=html.unescape(title).strip().replace("_"," ")
        if not title or "|" in title or "#" in title or "{" in title or "}" in title: return
        wanted.add(title);origins.setdefault(title,set()).add(origin)
    for title in ("NPC","NPC/List","Template:Nav/Characters"):
        add(title,"root_inventory")
    capture.query(meta="siteinfo",siprop="general|rightsinfo")
    categories=["Category:Non-player characters","Category:Quests","Category:Events","Category:Books"];visited=set()
    while categories:
        category=categories.pop(0)
        if category in visited: continue
        visited.add(category)
        entries=capture.listed("categorymembers",cmtitle=category,cmlimit="500",cmtype="page|subcat")
        inventories[category]=entries
        for entry in entries:
            if entry["ns"]==14: categories.append(entry["title"])
            elif entry["ns"] in (0,10): add(entry["title"],category)
    for prefix in ("NPC/List",):
        entries=capture.listed("allpages",apnamespace="0",apprefix=prefix,aplimit="500")
        inventories["prefix:"+prefix]=entries
        for entry in entries: add(entry["title"],"prefix:"+prefix)
    templates_found=capture.listed("allpages",apnamespace="10",apprefix="Dialog",aplimit="500")
    template_names={"Template:Dialogue","Template:Infobox/Character"}|{e["title"] for e in templates_found}
    for template in sorted(template_names):
        add(template,"dialogue_template_definition")
        entries=capture.listed("embeddedin",eititle=template,eilimit="500",einamespace="0|10")
        inventories["transcludes:"+template]=entries
        for entry in entries: add(entry["title"],"transcludes:"+template)
    # Independent whole-wiki source searches catch quotations outside Dialogue,
    # NPC category omissions, boss speech and quest answer/objective messages.
    for expression in (r"insource:/\[NPC\]/",r"insource:/\[BOSS\]/",r"insource:/\[SECURITY\]/",r"insource:/Select an option:/",r"insource:/NEW OBJECTIVE/","Journal","Diary"):
        entries=capture.listed("search",srsearch=expression,srnamespace="0|10",srlimit="500",srprop="timestamp|size")
        inventories["search:"+expression]=entries
        for entry in entries:add(entry["title"],"search:"+expression)
    old=PROJECT/"translation-source/NPC_INDEX_0.15.74.json"
    if old.exists():
        for group in json.loads(old.read_text(encoding="utf-8-sig"))["groups"]:
            for name in group["names"]: add(name,"frozen_navigation_2026-10-15:"+group["group"])
    pages={};attempted=set();wave=0
    while wanted-attempted:
        todo=sorted(wanted-attempted);attempted.update(todo);wave+=1
        if wave>12: raise RuntimeError("Dependency traversal did not converge; raw cache retained")
        print(f"Inventory/dependency wave {wave}: {len(todo)} requested titles",flush=True)
        newpages=pages_of(capture,todo,origins)
        for page in newpages:
            if page.get("missing") or "revisions" not in page:
                if any(v.startswith("relative:") for v in origins.get(page["title"],set())): missing_dependencies.add(page["title"])
                else: missing.add(page["title"])
                continue
            title=page["title"];pages[title]=page
            raw=page["revisions"][0]["slots"]["main"]["content"]
            # Complete raw snapshots include UI, books, historical and Alpha
            # sections. Follow relative transclusions; never execute wikitext.
            for a,b,name,value in templates(raw):
                if name.startswith("/"): add(title+name,"relative:"+title)
                elif name.startswith(":") and not name.startswith(":File:"): add(name[1:],"absolute_transclusion:"+title)
                elif name.startswith(":NPC/List/"): add(name[1:],"list_transclusion:"+title)
                elif re.search(r"(?i)(?:^|/)(?:dialogues?|transcripts?|cutscenes?)(?:$|/)",name) and not name.startswith("#"):
                    add(name[1:] if name.startswith(":") else name if name.startswith("Template:") else "Template:"+name,"dialogue_transclusion:"+title)
            for match in re.finditer(r"\|-\|:([^|\n{}]+)",raw):add(match[1],"tabview_transclusion:"+title)
            if title=="Template:Nav/Characters" or title.startswith("NPC/List/"):
                for m in re.finditer(r"\[\[([^\]|#]+)(?:\|[^\]]*)?\]\]",raw):
                    if not m[1].startswith(("File:","Category:","Template:")): add(m[1],"navigation_link:"+title)
                for m in re.finditer(r"\{\{(?:NPCSprite|NPC)\|([^|}]+)",raw,re.I): add(m[1],"navigation_npc:"+title)
                for m in re.finditer(r"(?:^|\n)\|-\|:([^|\n]+)",raw): add(m[1],"list_tab:"+title)
        print(f"Saved {len(pages)} canonical revisions; pending {len(wanted-attempted)}",flush=True)
    export(capture,pages,inventories,origins,missing,missing_dependencies)

if __name__=="__main__": main()
