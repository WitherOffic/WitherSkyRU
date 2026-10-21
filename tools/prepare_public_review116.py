"""Turn the public dump into explicit review queues,never automatic mod keys."""
import argparse,json,re,hashlib,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from export_public_text116 import ROOT,rows,write,jsonl,digest,stamp,templates,split_parameters,provisional_text

DATA=ROOT/'src/main/resources/data/witherskyru'
OLD=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
COLOR=re.compile(r'(?i)§[0-9a-fk-or]')

def exact_reference():
    known=defaultdict(list)
    java=(ROOT/'src/main/java/ru/wither/skyru/OfflineDictionary.java').read_text(encoding='utf-8')
    constructor=java[java.index('for (String resource : List.of('):];constructor=constructor[:constructor.index(')) {')]
    loaded=re.findall(r'"([^"\n]+\.tsv)"',constructor);assert len(loaded)==14
    # Exclude internal grammatical genders,patterns,and other metadata TSVs.
    for filename in loaded:
        p=DATA/filename
        for line in p.read_text(encoding='utf-8-sig').splitlines():
            if line and not line.startswith('#') and '\t' in line:
                a,b=line.split('\t',1)
                if a and b:known[a].append({'resource':p.name,'translation':b})
    for p in DATA.glob('*.json'):
        d=json.loads(p.read_text(encoding='utf-8-sig'))
        if not isinstance(d,dict):continue
        for r in d.get('records',[]):
            if r.get('source') and r.get('translation'):known[r['source']].append({'resource':p.name,'translation':r['translation']})
    return dict(known)

def main():
    ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);args=ap.parse_args();root=args.root
    verification=json.loads((root/'VERIFICATION.json').read_text(encoding='utf-8'));assert verification['all_passed'];known=exact_reference();write(root/'WORKTREE_EXACT_REFERENCE.json',known)
    frozen_hash=digest((OLD/'translation_queue.jsonl').read_bytes());assert frozen_hash=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    old_pages={p['title']:p for p in rows(OLD/'source_pages.jsonl')};old_queue={r['source'] for r in rows(OLD/'translation_queue.jsonl')};new_pages={};raw_hits=defaultdict(list);counts=Counter();ryan=None
    observed=json.loads((ROOT/'translation-source/OBSERVED_MISSING_2026-10-21.json').read_text(encoding='utf-8'));observed_sources=[r.get('source') for r in observed['observations'] if r.get('source')]
    for p in rows(root/'source_pages.jsonl'):
        new_pages[p['title']]={'revision':p['revision'],'source_url':p['source_url'],'raw_sha256':p['raw_sha256'],'namespace':p['namespace']}
        if p['title']=='Ryan':ryan=p
        for source in observed_sources:
            if source in p['raw_wikitext']:raw_hits[source].append({'page':p['title'],'source_url':p['source_url']})
    all_review=0;quality=[]
    with (root/'ADDITIONAL_REVIEW_QUEUE.jsonl').open('w',encoding='utf-8',newline='\n') as output:
        for r in rows(root/'candidate_index.jsonl'):
            source=r['source'];present=source in known;counts['with_separate_worktree_exact_key']+=int(present);counts['without_separate_worktree_exact_key']+=int(not present)
            if not present:
                entry=r|{'status':'requires_context_and_runtime_review_not_proven_untranslated_game_text','in_old_dialogue_queue':source in old_queue,'contains_markup_or_variable':bool(re.search(r'\{\{|\}\}|<[^>]+>|\{[^}]+\}',source)),'automatically_added_to_mod':False};output.write(json.dumps(entry,ensure_ascii=False)+'\n');all_review+=1
            else:
                candidates=[]
                for ref in known[source]:
                    body=re.sub(r'^\[(?:NPC|BOSS|SECURITY)\] [^:]+:\s*','',COLOR.sub('',ref['translation']))
                    latin=[w for w in re.findall('[A-Za-z]{2,}',body) if w not in ('SkyBlock','Minecraft','NPC') and not re.fullmatch('[IVXLCDM]+',w)]
                    if latin:candidates.append(ref|{'remaining_latin_tokens':latin})
                if candidates:quality.append({'source':source,'candidate_occurrence_ids':r['occurrence_ids'],'existing_worktree_targets':candidates,'status':'remaining_latin_requires_editorial_review_may_be_correct_proper_names','not_proof_of_translation_error':True,'automatically_added_to_mod':False})
    jsonl(root/'EXISTING_TRANSLATION_REVIEW.jsonl',quality)
    # All publicly captured tagged messages,including number/name placeholders.
    messages=defaultdict(list);blocks=0;ryan_messages=defaultdict(list);ui_lines=defaultdict(list)
    for p in rows(root/'source_pages.jsonl'):
        for start,end,name,raw in templates(p['raw_wikitext']):
            if name.strip().casefold()!='dialogue':continue
            blocks+=1;parts=split_parameters(raw[2:-2]);value='|'.join(parts[1:]);legacy=provisional_text(value)
            for lineno,line in enumerate(legacy.splitlines()):
                # Split only at a full next speaker prefix. Raw full block stays authoritative.
                line=COLOR.sub('',line).strip()
                if line and (line.startswith(('Select an option:','NEW OBJECTIVE','QUEST COMPLETE','OBJECTIVE COMPLETE'))):ui_lines[line].append({'page':p['title'],'source_url':p['source_url'],'block_start':start,'block_end':end,'line':lineno})
                for text in re.split(r'/(?=\[(?:NPC|BOSS|SECURITY)\] [^:\n]{1,100}:)',line):
                    if not re.match(r'^\[(?:NPC|BOSS|SECURITY)\] [^:\n]{1,100}:',text):continue
                    ref={'page':p['title'],'source_url':p['source_url'],'revision':p['revision'],'block_start':start,'block_end':end,'line':lineno,'block_sha256':digest(raw)};messages[text].append(ref)
                    if p['title']=='Ryan':ryan_messages[text].append(ref)
    tagged_new=0
    with (root/'NPC_DIALOGUE_REVIEW_QUEUE.jsonl').open('w',encoding='utf-8',newline='\n') as output:
        for source,origins in sorted(messages.items()):
            if source in known:continue
            body=source.split(':',1)[1];variable=bool(re.search(r'<[^>]+>|\{[^}]+\}|\[[^]]+\]|#[a-zA-Z]|\b(?:X+|Y+)\b',body));tagged_new+=1
            output.write(json.dumps({'source':source,'translation_ru':'','origins':origins,'variable_or_markup_review':variable,'in_old_dialogue_queue':source in old_queue,'status':'no_separate_exact_worktree_record_not_proof_of_runtime_failure','automatically_added_to_mod':False},ensure_ascii=False)+'\n')
    resource_texts=[]
    def visit(value,path,endpoint):
        if isinstance(value,dict):
            for k,v in value.items():
                if isinstance(v,str) and k.casefold() in ('name','description','lore','text','fullore','fulllore','title','displayname','label') and re.search('[A-Za-z]',v):resource_texts.append({'source':v,'endpoint':endpoint,'json_path':path+[k],'exact_worktree_record_present':v in known,'translation_ru':'','automatically_added_to_mod':False})
                elif isinstance(v,(dict,list)):visit(v,path+[k],endpoint)
        elif isinstance(value,list):
            for i,v in enumerate(value):
                if isinstance(v,str) and path and str(path[-1]).casefold() in ('lore','fulllore','description','text') and re.search('[A-Za-z]',v):resource_texts.append({'source':v,'endpoint':endpoint,'json_path':path+[i],'exact_worktree_record_present':v in known,'translation_ru':'','automatically_added_to_mod':False})
                elif isinstance(v,(dict,list)):visit(v,path+[i],endpoint)
    for p in (root/'hypixel-resources').glob('*.json'):visit(json.loads(p.read_text(encoding='utf-8')),[],p.stem)
    if (root/'RESOURCE_PACK_TEXTS.jsonl').exists():
        for r in rows(root/'RESOURCE_PACK_TEXTS.jsonl'):resource_texts.append({'source':r['source'],'endpoint':r['url'],'json_path':[r['archive_member']]+r['json_path'],'exact_worktree_record_present':r['source'] in known,'translation_ru':'','automatically_added_to_mod':False})
    jsonl(root/'HYPIXEL_RESOURCE_TEXTS.jsonl',resource_texts)
    for r in observed['observations']:
        source=r.get('source');r['found_in_new_raw_wiki']=raw_hits.get(source,[]) if source else [];r['separate_exact_worktree_record_present']=source in known if source else None;r['in_old_dialogue_queue']=source in old_queue if source else None
    write(root/'OBSERVED_MISSING.json',observed)
    master={}
    def review(source,kind,origin):
        if source not in master:master[source]={'source':source,'translation_ru':'','origins':[],'kinds':[],'status':'translation_or_runtime_or_game_provenance_review_required','automatically_added_to_mod':False}
        row=master[source];row['origins'].append(origin)
        if kind not in row['kinds']:row['kinds'].append(kind)
    for r in rows(root/'ADDITIONAL_REVIEW_QUEUE.jsonl'):review(r['source'],'unclassified_public_wiki_candidate',{'candidate_occurrence_ids':r['occurrence_ids']})
    for r in rows(root/'NPC_DIALOGUE_REVIEW_QUEUE.jsonl'):review(r['source'],'public_tagged_dialogue',{'dialogue_origins':r['origins'],'variable_or_markup_review':r['variable_or_markup_review']})
    old_missing=0
    for r in rows(OLD/'translation_queue.jsonl'):
        if r['source'] not in known:review(r['source'],'prior_frozen_dialogue_queue',{'prior_source_url':r['source_url'],'prior_occurrence_ids':r['occurrence_ids'],'not_necessarily_current_server_text':True});old_missing+=1
    for source,origins in ui_lines.items():
        if source not in known:review(source,'public_choice_or_objective_notification',{'origins':origins})
    for r in quality:review(r['source'],'existing_translation_remaining_latin_not_necessarily_error',r)
    for r in resource_texts:
        if not r['exact_worktree_record_present']:review(r['source'],'public_hypixel_resource_text',{'endpoint':r['endpoint'],'json_path':r['json_path']})
    for r in observed['observations']:
        source=r.get('source',r.get('source_pattern'));review(source,'user_reported_missing',r)
        if r.get('observed_counter'):review(source+' '+r['observed_counter'],'user_reported_missing_with_counter',r)
    ui_candidates=0
    with (root/'OBJECTIVE_AND_UI_REVIEW.jsonl').open('w',encoding='utf-8',newline='\n') as output:
        for r in rows(root/'text_candidates.jsonl'):
            s=r['provisional_plain_text']
            if re.search(r'objective|goal|task|tooltip|option|instruction',r['parameter'],re.I) or re.match(r'^(?:Collect |Find the |Travel to |Talk to |Speak to |Visit |Return to |Defeat |Complete |Select an option:)',s):
                if s in known:continue
                entry={'source':s,'candidate_id':r['id'],'source_url':r['source_url'],'parameter':r['parameter'],'not_certified_game_text':True};output.write(json.dumps(entry,ensure_ascii=False)+'\n');ui_candidates+=1;review(s,'objective_or_ui_candidate',entry)
    jsonl(root/'MASTER_REVIEW_QUEUE.jsonl',(master[k] for k in sorted(master)));write(root/'MASTER_REVIEW_STATUS.json',{'unique_sources_or_patterns':len(master),'unclassified_wiki_candidates':all_review,'public_tagged_dialogue_without_exact_key':tagged_new,'observed_user_cases':len(observed['observations']),'not_an_all_game_untranslated_line_count':True,'runtime_automatically_modified':False})
    assert ryan;fixed=dynamic=registered=0;lines=['# Ryan: полный опубликованный диалог','','Это проверка источника и отдельных ключей рабочей папки,не доказательство полного перевода текущей игры. Включены все блоки Dialogue страницы и переменные.','',f"Источник: [{ryan['title']}, ревизия{ryan['revision']}]({ryan['source_url']}).",'', '| Реплика | Переменные/разметка | Отдельный точный ключ в рабочей папке |','| --- | --- | --- |']
    for source,origins in sorted(ryan_messages.items()):
        var=bool(re.search(r'<[^>]+>|\{[^}]+\}',source));dynamic+=int(var);fixed+=int(not var);registered+=int(source in known)
        lines.append('| '+source.replace('|','&#124;')+' | '+('да' if var else 'нет')+' | '+('есть,качество/достижимость отдельно' if source in known else 'нет;проверить шаблон или добавить')+' |')
    lines+=['','Скриншот с головой игрока перед Wither_101 отличается от простого имени. Наличие старого шаблона обращения без головы не подтверждает перевод такого оформления. Цели,сообщения о награде и меню проверяются отдельно от реплик.']
    (root/'RYAN_AUDIT_RU.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    old_only=sorted(old_pages.keys()-new_pages.keys());new_only=sorted(new_pages.keys()-old_pages.keys());changed=[name for name in old_pages.keys()&new_pages.keys() if old_pages[name]['raw_sha256'].upper()!=new_pages[name]['raw_sha256']];write(root/'SOURCE_DIFFERENTIAL.json',{'old_pages':len(old_pages),'new_pages':len(new_pages),'new_page_titles':new_only,'old_titles_not_in_current_explicit_namespaces':old_only,'changed_revision_text_titles':sorted(changed),'frozen_queue_sha256_unchanged':frozen_hash,'old_progress_denominator_not_replaced':16244})
    summary={'captured_pages':len(new_pages),'old_capture_pages':len(old_pages),'additional_page_titles':len(new_only),'old_titles_not_in_new_inventory':len(old_only),'changed_page_texts':len(changed),'all_public_page_inventory_fetched':verification['missing_pages']==0,'unique_provisional_review_candidates':verification['unique_review_candidates'],'unregistered_provisional_review_candidates':all_review,'all_dialogue_blocks':blocks,'unique_tagged_messages':len(messages),'tagged_without_separate_exact_record':tagged_new,'ryan_unique_messages':len(ryan_messages),'ryan_fixed_messages':fixed,'ryan_variable_messages':dynamic,'ryan_separate_exact_records':registered,'hypixel_resource_text_occurrences':len(resource_texts),'observed_cases':len(observed['observations']),'runtime_automatically_modified':False,'all_live_server_text_known':False,'profile_api_settings_verified':False,'translation_percentage_for_this_mixed_corpus_available':False,'exact_key_reference_scope':'Current source resources,not proof of published JAR reachability,quality,or runtime template coverage.'};write(root/'REVIEW_SUMMARY.json',summary)
    namespaces=json.loads((root/'LISTING_COMPLETION.json').read_text(encoding='utf-8'));nsnames={0:'Игровые статьи и перенаправления',8:'Сообщения интерфейса wiki',10:'Шаблоны,включая данные предметов и заданий',14:'Категории',112:'Игровые калькуляторы',114:'Обучающие страницы',828:'Модули',9592:'Табличные данные Bucket'}
    lines=['# Расширенная выгрузка публичных текстов — 21 октября2026','', '**Сохранены все текущие публичные страницы в7 явно перечисленных пространствах wiki. Это не частные серверные скрипты Hypixel и не гарантия каждого слова живого сервера.**','', '`██████████████████████████████` **100% найденного публичного списка страниц**. Это полоса выгрузки,не перевода.','', '| Раздел источника | Страниц | Обход завершён |','| --- | ---: | --- |']
    for n in namespaces:lines.append(f"| {nsnames[n['namespace']]} | {n['pages']} | да |")
    lines+=['','| Результат | Количество |','| --- | ---: |',f"| Сохранённые полные страницы | {len(new_pages):,} |",f"| Прежняя выгрузка | {len(old_pages):,} |",f"| Дополнительные названия страниц | {len(new_only):,} |",f"| Все блоки Dialogue | {blocks:,} |",f"| Уникальные строки с префиксом NPC/BOSS/SECURITY | {len(messages):,} |",f"| Из них без отдельного точного ключа | {tagged_new:,} |",f"| Все предварительные кандидаты из параметров | {verification['unique_review_candidates']:,} |",f"| Без отдельного точного ключа,требуют разбора | {all_review:,} |",f"| Текстовые вхождения из8 официальных ресурсов API | {len(resource_texts):,} |",f"| Зафиксированные примеры пользователя | {len(observed['observations'])} |",'','## Что изменилось относительно старой базы','','Раньше очередь строилась преимущественно из блоков Dialogue. Теперь полные исходники всех перечисленных страниц сохранены без вырезания описаний,условий таблиц и параметров шаблонов. `Collect Spruce Logs` действительно присутствовало в сырой странице Kelly,но отсутствовало в старой очереди. Ошибка прежнего ответа об отсутствии во всей выгрузке уточнена: отсутствовал отдельный ключ,а не сырой текст страницы.','','[Новая очередь диалогов NPC](NPC_DIALOGUE_REVIEW_QUEUE.jsonl),[широкая очередь дополнительных кандидатов](ADDITIONAL_REVIEW_QUEUE.jsonl),[наблюдения по скриншотам](OBSERVED_MISSING.json),[проверка всей страницы Ryan](RYAN_AUDIT_RU.md).','','В широкую очередь включены также неподтверждённые параметры: среди них могут быть справочный текст wiki,технические идентификаторы и повторяющиеся части. Они сохранены для полноты,но не объявлены тысячами непереведённых игровых реплик. Отсутствие точного ключа не исключает существующий динамический перевод.','','## Официальный API и Wither_101','','Успешно сохранены8 публичных ресурсов Hypixel: предметы,коллекции,навыки,выборы,бинго,достижения,общие задания и список ресурс-паков. Ключ API и профиль игрока не использовались. Это не проверка настройки API именно профиля Wither_101. Открытие API профиля не предоставляет частную базу диалогов или серверных скриптов. Общие задания API не равны полному каталогу сюжетных целей SkyBlock. Список ресурс-паков сохранён,но сами архивы паков на этом проходе не объявлены выгруженными.','','## Целостность и ограничения','',f"Независимая проверка: **{verification['total_assertions']:,} утверждений**, контрольные суммы ответов и текстов,полные цепочки продолжения API,соответствие каждого сохранённого pageid списку,точные ревизии,границы блоков и все ссылки дедупликации.",'','Прежняя замороженная очередь16 244 строк не переписана. Процент её перевода не перенесён на эту более широкую базу. Статистика новой базы не является процентом перевода всей игры. Снимок получен серией запросов,не атомарной копией в один момент;каждая страница имеет собственную ревизию и время.','','Выгружены текущие публичные ревизии,в том числе архивные и Alpha-разделы в их составе. Не выгружена вся история ревизий,изображения и их OCR,личные страницы/обсуждения игроков,частные скрипты сервера,все игровые пакеты или все строки сторонних модов. Такие границы перечислены явно,а не выданы за100% живой игры.','','**Сырые строки не добавлены в исполняемый мод. Игра и установленная сборка не изменялись. Новая выгрузка не создаёт покадровой нагрузки. Перевод и проверка динамических форм — следующий отдельный этап.**']
    lines+=['','## Единая очередь','',f"Все дополнительные источники объединены в[MASTER_REVIEW_QUEUE.jsonl](MASTER_REVIEW_QUEUE.jsonl): **{len(master):,} уникальных строк или шаблонов**. Сохранены происхождение,повторы,сообщения NPC,публичные ресурсы API и7 форм по6 пользовательским примерам,включая вариант со счётчиком. Это очередь проверки,а не заявление о стольких доказанно непереведённых игровых репликах.",'',f"Отдельно добавлены **{len(quality):,} строк с существующими переводами и оставшейся латиницей**: [редакторская проверка](EXISTING_TRANSLATION_REVIEW.jsonl). Среди них есть корректные собственные имена;латиница сама по себе не доказывает ошибку. Такие случаи не потеряны лишь потому,что отдельный ключ уже существует."]
    if (root/'RESOURCE_PACK_CAPTURE.json').exists():
        packs=json.loads((root/'RESOURCE_PACK_CAPTURE.json').read_text(encoding='utf-8'));summary['official_resource_pack_versions']=len(packs['packs']);summary['official_resource_pack_language_files']=sum(p['language_files'] for p in packs['packs'])
        lines=[v.replace('в7 явно перечисленных пространствах','в8 явно перечисленных пространствах').replace('Список ресурс-паков сохранён,но сами архивы паков на этом проходе не объявлены выгруженными.','Также сохранены все3 версии официального ресурс-пака,объявленные текущим API. Их SHA-1 сверены с API;языковые файлы и метаданные извлечены отдельно. Это не частные серверные диалоги.') for v in lines]
        lines+=['','[Сведения об официальных ресурс-паках](RESOURCE_PACK_CAPTURE.json).']
    summary['master_review_unique_sources_or_patterns']=len(master);summary['existing_translation_latin_review_not_error_count']=len(quality);write(root/'REVIEW_SUMMARY.json',summary)
    summary['prior_frozen_rows_without_exact_record_retained']=old_missing;summary['objective_ui_candidate_occurrences']=ui_candidates;write(root/'REVIEW_SUMMARY.json',summary)
    (root/'SUMMARY_RU.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    print(json.dumps(summary,ensure_ascii=False))

if __name__=='__main__':main()
