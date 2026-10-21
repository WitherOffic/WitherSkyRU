"""Publish a verified source corpus, not a mod JAR. No installed game writes."""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path
import re
import shutil
import zipfile

PROJECT=Path(__file__).resolve().parents[1]
def rows(path):return [json.loads(s) for s in path.read_text(encoding="utf-8-sig").splitlines() if s.strip()]
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest().upper()
def escape(value):return str(value).replace("|","\\|").replace("\n"," ")

def main():
    parser=argparse.ArgumentParser();parser.add_argument("root",type=Path);parser.add_argument("--destination",type=Path,required=True);args=parser.parse_args()
    root=args.root.resolve();destination=args.destination.resolve()
    if destination.exists():raise RuntimeError("Refusing to overwrite a delivered corpus directory")
    manifest=json.loads((root/"manifest.json").read_text(encoding="utf-8"));verified=json.loads((root/"VERIFICATION.json").read_text(encoding="utf-8"))
    if not verified["all_passed"] or verified["unique_queue_rows"]!=manifest["unique_provisional_lines"]:raise RuntimeError("Final verification does not match corpus")
    queue=rows(root/"translation_queue.jsonl");occurrences=rows(root/"dialogue_occurrences.jsonl");pages=rows(root/"page_registry.jsonl")
    grouped=collections.defaultdict(dict);page_origins=collections.defaultdict(set)
    for row in occurrences:
        if row["speaker"] and not re.search(r"[{}<>]",row["speaker"]):
            grouped[row["speaker"]][row["source"]]=row;page_origins[row["speaker"]].add(row["page"])
    records=[]
    for name,items in sorted(grouped.items(),key=lambda x:x[0].casefold()):
        values=list(items.values());status=collections.Counter(r["status"] for r in values)
        records.append({"speaker":name,"unique_rows":len(items),"exact_resource_keys":status["exact_resource_match_not_runtime_proof"],"without_exact_key_review":status["needs_translation_review"],"variables":status["variable_definition"],"markup":status["markup_review"],"source_pages":len(page_origins[name]),"all_occurrences_historical":all(r["historical_heading"] for r in occurrences if r["speaker"]==name),"source_page_names":"; ".join(sorted(page_origins[name]))})
    with (root/"NPC_COVERAGE_TABLE.tsv").open("w",encoding="utf-8-sig",newline="") as stream:
        writer=csv.DictWriter(stream,records[0].keys(),delimiter="\t");writer.writeheader();writer.writerows(records)
    table=["# Охват реплик по обозначениям говорящих","","Точный ключ в ресурсе не равен полному или качественному переводу. Динамические правила здесь не доказаны и не объявлены отсутствующими.","Обозначения не являются серверными ID: одно имя может относиться к нескольким версиям персонажа. Сохранены также условные обозначения [name], [Eye], ---- и пробелы исходника; это не четыре подтверждённых самостоятельных NPC.","","| Обозначение говорящего | Уникальных строк | Точный ключ | Проверить перевод | Переменные | Разметка | Страниц |","| --- | ---: | ---: | ---: | ---: | ---: | ---: |"]
    for r in records:table.append("| "+" | ".join(escape(r[k]) for k in ("speaker","unique_rows","exact_resource_keys","without_exact_key_review","variables","markup","source_pages"))+" |")
    (root/"NPC_COVERAGE_TABLE_RU.md").write_text("\n".join(table)+"\n",encoding="utf-8")
    inventory_table=["# Проверка полноты публичного обхода","","Все количества относятся к сохранённым ответам API, а не к закрытым скриптам Hypixel.","","| Источник поиска | Найдено записей |","| --- | ---: |"]
    for key,entries in manifest["inventories"].items():inventory_table.append(f"| {escape(key)} | {len(entries)} |")
    inventory_table += ["","Пагинация каждого использованного списка проверена по исходным ответам API. Полные тела ответов находятся в requests/.","Расширенный обход включает категории NPC, квестов, событий и книг; поиски по тегам NPC/BOSS/SECURITY, вариантам ответов и новым целям."]
    (root/"INVENTORY_COVERAGE_RU.md").write_text("\n".join(inventory_table)+"\n",encoding="utf-8")
    oldroot=PROJECT/"build/npc-corpus77-38418bf339584f219e78071b501404ff"
    old=json.loads((oldroot/"manifest.json").read_text(encoding="utf-8"))
    prior={r["source"] for r in rows(oldroot/"translation_queue.jsonl")};current={r["source"] for r in queue}
    added=[r for r in queue if r["source"] not in prior]
    removed=sorted(prior-current)
    broken_hendrik={"[NPC] Hendrik: Thank goodness I don't have to see","[NPC] anymore..."}
    retained=[{**r,"retention_status":"prior_snapshot_variant_not_proven_current"} for r in rows(oldroot/"translation_queue.jsonl") if r["source"] in removed and r["source"] not in broken_hendrik]
    (root/"prior_snapshot").mkdir(exist_ok=True)
    shutil.copy2(oldroot/"source_pages.jsonl",root/"prior_snapshot/source_pages.jsonl")
    shutil.copy2(oldroot/"manifest.json",root/"prior_snapshot/manifest.json")
    with (root/"prior_snapshot/retained_variants.jsonl").open("w",encoding="utf-8",newline="\n") as stream:
        for record in retained:stream.write(json.dumps(record,ensure_ascii=False)+"\n")
    diff={"scope":"Raw saved-corpus comparison, not newly translated runtime keys","baseline_pages":old["captured_pages"],"current_pages":manifest["captured_pages"],"baseline_unique_rows":len(prior),"current_unique_rows":len(current),"new_unique_rows":added,"old_rows_not_in_new_queue":removed,"installed_modpack_modified":False}
    (root/"DIFFERENTIAL.json").write_text(json.dumps(diff,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    top=sorted(records,key=lambda r:r["without_exact_key_review"],reverse=True)[:12]
    summary=["# Публичная база диалогов — 16 октября 2026","","Готова выгрузка доступного публичного корпуса для последующего перевода. Это не серверная база Hypixel и не готовая переведённая версия мода.","","## Масштаб","","| Показатель | Было | В новой выгрузке |","| --- | ---: | ---: |",f"| Страниц с зафиксированной ревизией | {old['captured_pages']:,} | {manifest['captured_pages']:,} |",f"| Блоков Dialogue | {old['dialogue_blocks']:,} | {manifest['dialogue_blocks']:,} |",f"| Уникальных строк очереди | {old['unique_provisional_lines']:,} | {manifest['unique_provisional_lines']:,} |",f"| Буквальных имён говорящих | {old['literal_speaker_names_excluding_definition_placeholders']:,} | {manifest['literal_speaker_names_excluding_definition_placeholders']:,} |","",f"Новых уникальных строк относительно прежней очереди: {len(added)}. Старых строк, не вошедших в новую очередь: {len(removed)}; список и причины требуют сверки в DIFFERENTIAL.json. Это не число переведённых строк.","","## Состояние перевода","","| Статус | Строк |","| --- | ---: |"]
    labels={"exact_resource_match_not_runtime_proof":"Есть точный ключ в ресурсах; корректность в игре не сертифицирована","needs_translation_review":"Нет точного ключа: проверить перевод и динамическое правило","variable_definition":"Переменные и шаблонные определения","markup_review":"Требуется разбор разметки","nonstandard_tag_review":"Нестандартный тег или отсутствующее имя/двоеточие","unprefixed_or_choice_review":"Ответы игрока, продолжения и служебные сообщения"}
    summary += [f"| {labels[key]} | {value:,} |" for key,value in manifest["unique_status_counts"].items()]
    summary += ["","## Приоритетные большие ветки","","Это кандидаты без точного ключа, а не доказательство отсутствия любого динамического перевода.","","| Говорящий | Всего уникальных строк | Проверить без точного ключа |","| --- | ---: | ---: |"]+[f"| {escape(r['speaker'])} | {r['unique_rows']} | {r['without_exact_key_review']} |" for r in top]
    summary += ["","## Проверка и исправления учёта","",f"- Пройдено {verified['total_assertions']:,} проверок целостности; отдельные граничные тесты разбора приложены.","- Реплика Артура вне стандартного блока добавлена в очередь с привязкой к исходной цитате.","- Фраза Хендрика с `[NPC]` внутри восстановлена целиком; два ошибочных фрагмента не оставлены как самостоятельные реплики.","- Цитаты вне Dialogue не захватывают следующий за ними авторский текст статьи.","- Пропущенные имена/двоеточия в исходнике не выдумываются: такие строки выделены отдельно.","- Сохранены оригинальные цветовые коды, полные исходники, условия, ответы, повторные вхождения и ревизии общих шаблонов.","","## Границы достоверности","","- Непубличные, скрытые и ещё не описанные ветки сервера не объявляются выгруженными.","- Baking Specialist: историческая страница не публикует сами реплики (InfoNeeded).","- Teddy/UI: отсутствующая в вики подстраница; реплики самого Teddy сохранены.","- Исторические и Alpha-материалы включены с отдельными признаками, не объявлены действующими.","- Некоторые наследуемые условия таблиц rowspan нужно читать по полной исходной странице.","- Это экспорт данных. Игровой JAR не обновлялся, мод автоматически не устанавливался; FPS здесь не измерялся.","","## Что открыть","","- `NPC_DIALOGUES_EN.md` — читаемый оригинал диалогов.","- `NPC_COVERAGE_TABLE_RU.md` — полная таблица говорящих персонажей.","- `translation_queue.tsv` — очередь с английским исходником, найденным прежним переводом и пустым полем нового перевода.","- `INVENTORY_COVERAGE_RU.md` — перечень всех API-обходов.","- `source_pages.jsonl` — первичные исходники без потери текста.","- `VERIFICATION.json` и `HASHES.json` — доказательства целостности.","","Лицензия и авторство: см. LICENSE_SOURCES.md. Архив не содержит игровых логов, токенов, JFR или пользовательских настроек."]
    summary += ["","## Сохранение предыдущих вариантов","",f"В `prior_snapshot/` сохранены исходные страницы прежней выгрузки и {len(retained)} заменённых вариантов строк. Они не объявлены действующими и не смешаны с актуальной очередью. Два ошибочных обрывка Хендрика исключены из списка вариантов.","",f"Отдельно сохранены {manifest.get('published_journal_text_sections',0)} опубликованные записи дневников подземелий: `BOOKS_EN.md` и `book_text_sections.jsonl`. Оригинальная серверная разбивка на страницы NBT и все прочие книги игры этим не сертифицируются."]
    summary=[line.replace("Буквальных имён говорящих","Различных обозначений говорящего").replace("полная таблица говорящих персонажей","таблица обозначений говорящих, включая условные имена") for line in summary]
    summary += ["","В счётчике обозначений сохранены [name], [Eye], ---- и вариант имени St. Jerry с пробелом в исходнике. Он не равен числу уникальных действующих NPC."]
    (root/"SUMMARY_RU.md").write_text(re.sub(r"(?<=\d),(?=\d{3}(?:\D|$))"," ","\n".join(summary))+"\n",encoding="utf-8")
    # Reproducible offline validation tools are data-only additions to this
    # archive, never copied into an installed Minecraft instance.
    (root/"reproduction/tools").mkdir(parents=True,exist_ok=True);(root/"reproduction/tests").mkdir(parents=True,exist_ok=True)
    for name in ("export_npc_corpus77.py","verify_npc_corpus77.py","package_npc_corpus77.py"):shutil.copy2(PROJECT/"tools"/name,root/"reproduction/tools"/name)
    shutil.copy2(PROJECT/"tests/test_npc_corpus77.py",root/"reproduction/tests/test_npc_corpus77.py")
    (root/"reproduction/translation-source").mkdir(parents=True,exist_ok=True)
    shutil.copy2(PROJECT/"translation-source/NPC_INDEX_0.15.74.json",root/"reproduction/translation-source/NPC_INDEX_0.15.74.json")
    files=sorted(p for p in root.rglob("*") if p.is_file() and p.name not in ("HASHES.json",) and "__pycache__" not in p.parts)
    hashes={str(p.relative_to(root)).replace("\\","/"):sha(p) for p in files}
    (root/"HASHES.json").write_text(json.dumps(hashes,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    files.append(root/"HASHES.json")
    destination.mkdir(parents=True)
    archive=destination/"NPC_CORPUS_2026-10-16_FULL.zip"
    with zipfile.ZipFile(archive,"x",compression=zipfile.ZIP_DEFLATED,compresslevel=9) as package:
        for path in files:package.write(path,str(path.relative_to(root)).replace("\\","/"))
    with zipfile.ZipFile(archive) as package:
        if package.testzip() is not None:raise RuntimeError("ZIP CRC validation failed")
        for name,value in hashes.items():
            if hashlib.sha256(package.read(name)).hexdigest().upper()!=value:raise RuntimeError("ZIP entry SHA mismatch: "+name)
    for name in ("SUMMARY_RU.md","NPC_COVERAGE_TABLE_RU.md","NPC_COVERAGE_TABLE.tsv","translation_queue.tsv","NPC_DIALOGUES_EN.md","PAGE_INDEX_RU.md","BOOKS_EN.md"):
        shutil.copy2(root/name,destination/name)
        if sha(root/name)!=sha(destination/name):raise RuntimeError("Delivered copy mismatch")
    result={"archive":str(archive),"archive_sha256":sha(archive),"archive_bytes":archive.stat().st_size,"verified_entries":len(files),"source_root":str(root),"new_unique_rows_vs_previous":len(added),"removed_rows_vs_previous":removed,"installed_game_modified":False}
    (destination/"DELIVERY.json").write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(result,ensure_ascii=False,indent=2))

if __name__=="__main__":main()
