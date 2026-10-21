"""Package an evidence-gated106 release with clear,denominator-stable progress bars."""
import argparse,hashlib,json,math,re,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,sha,rows,write
from audit_npc106 import BASE,PREVIOUS

DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_3')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','audit','benchmark','long'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.106-dev.jar';digest=sha(jar)
    audit=load(a.audit/'AUDIT.json');assert audit['runtime_jar_sha256']==digest and audit['all_receipt_and_render_algorithms_unchanged'] and audit['fresh_test_groups']==179
    bench=load(a.benchmark/'RAW.json');assert bench['runtime_jar_sha256']==digest and len(bench['summary'])==10
    assert [r['version'] for r in bench['records']]==['0.15.105-dev','0.15.106-dev','0.15.106-dev','0.15.105-dev']
    for r in bench['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.105-dev' else digest)
    for mode in bench['summary']:
        assert mode['equal_useful_output'] and len(mode['orders'])==2
        outputs=[r['results'][mode['mode']]['useful_visible_outputs'] for r in bench['records']];assert outputs[0]==outputs[1]==outputs[2]==outputs[3]
        assert all(math.isfinite(o['wall_change_percent']) for o in mode['orders'])
    changes=[o['wall_change_percent'] for m in bench['summary'] for o in m['orders']]
    performance={'scope':'Isolated equal-output receipt test;not game FPS/frametime/0.1% low. New103 NPC literals and56 groups are intentionally unequal work,verified separately.','equal_modes':10,'order':['105','106','106','105'],'min_wall_change_percent':min(changes),'max_wall_change_percent':max(changes),'both_orders_slower_over5':[m['mode'] for m in bench['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])]}
    long=load(a.long/'RAW.json');assert long['runtime_jar_sha256']==digest and len(long['summary'])==3
    assert [r['version'] for r in long['records']]==['0.15.105-dev','0.15.106-dev','0.15.106-dev','0.15.105-dev']
    for r in long['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.105-dev' else digest)
    for mode in long['summary']:
        assert mode['equal_useful_output'] and len(mode['orders'])==2
        outputs=[r['results'][mode['mode']]['useful_visible_outputs'] for r in long['records']];assert outputs[0]==outputs[1]==outputs[2]==outputs[3]
        assert all(math.isfinite(o['wall_change_percent']) for o in mode['orders'])
    long_changes=[o['wall_change_percent'] for m in long['summary'] for o in m['orders']]
    performance['long_repeat']={'scope':'Predeclared noisy choices87 plus near-threshold existing_style/existing_dynamic;one million warmup and four one-million-call rounds;all raw results retained.','equal_modes':3,'min_wall_change_percent':min(long_changes),'max_wall_change_percent':max(long_changes),'both_orders_slower_over5':[m['mode'] for m in long['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])]}
    evidence=load(SOURCE/'NPC_SOURCES_0.15.106.json');fixed={r['runtime_record']['source']:r['runtime_record'] for r in evidence['records']}
    choices=load(SOURCE/'NPC_CHOICES_SOURCES_0.15.106.json')['records'];choice_sources={o['source'] for r in choices for o in r['all_occurrences']}
    snap=load(a.snapshots/'0.15.106-dev/NEW_ROWS.json');npc_snap={r['source']:r for r in snap['npc']};choice_snap={r['source']:r for r in snap['choices']}
    work=rows(SOURCE/'NPC_WORKLIST_0.15.105.jsonl')
    for r in work:
        s=r['source'];r['archival_exact_translation106']=s in fixed;r['ordered_choice_group106']=s in choice_sources
        if s in fixed:
            assert not r['exact_registration'] and r['historical_heading']
            r['exact_registration']=True;r['review_status']='authored_archival_primary_and_publication_layout_tested_not_live_certified'
            r['registered_targets_not_automatically_certified']=[{'resource':'npc-expansion106.json','translation':fixed[s]['translation']}]
            r['runtime_flat_snapshot_not_quality_proof']=npc_snap[s]['flat'];r['runtime_colored_snapshot_not_quality_proof']=npc_snap[s]['styled']
        if s in choice_sources:r['runtime_ordered_choice_snapshot106_not_quality_proof']=choice_snap[s]['styled']
    assert len(work)==16244 and sum(r['exact_registration'] for r in work)==11195 and sum(r['archival_exact_translation106'] for r in work)==103
    (SOURCE/'NPC_WORKLIST_0.15.106.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8')
    percent=100*11195/16244;remaining_percent=100-percent
    progress={'version':'0.15.106-dev','runtime_jar_sha256':digest,'exact_keys':11195,'total_frozen':16244,'exact_percent':percent,'without_exact_key':5049,'without_exact_percent':remaining_percent,'new_exact_keys':103,'new_exact_keys_are_archival':True,'new_current_choice_groups':56,'new_current_label_slots':75,'new_current_choice_source_rows':len(choice_sources),'static_choice_groups_before':289,'static_choice_groups_after':345,'prior_name_templates103':135,'selected_style_repaired_cumulative105':72,'selected_style_gaps_remaining':0,'fresh_test_groups':179,'parser_tests':15,'new_npc_assertions':217703,'new_choice_assertions':81691,'byte_identical_runtime_classes':120,'hot_method_instructions_unchanged':True,'startup_method_edits_only':audit['method_differences'],'historical_test_sources_unchanged':audit['historical_test_sources_unchanged'],'isolated_count_assertion_updates':29,'pinned_pages':len(evidence['source_pages']),'performance_not_fps':performance,'installed_pack_modified':False,'game_fps_measured':False}
    write(SOURCE/'NPC_PROGRESS_0.15.106.json',progress)
    by=defaultdict(Counter)
    for r in work:
        c=by[r['speaker'] or '(без говорящего)'];c['total']+=1;c['exact']+=int(r['exact_registration']);c['archival106']+=int(r['archival_exact_translation106']);c['replies106']+=int(r['ordered_choice_group106'])
    table=['# Публичная очередь — 0.15.106','','Точные ключи,архивные добавления и варианты ответа разделены. Это не процент качества/готовности всей игры.','','| Обозначение | Строк | Точных переводов | Доля точных | Архивных добавлений106 | Строк выбора106 | Без точной записи |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(by.items(),key=lambda x:x[0].casefold()):table.append(f"| {name.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['archival106']} | {c['replies106']} | {c['total']-c['exact']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.106_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    bar='█'*round(percent*30/100)+'░'*(30-round(percent*30/100))
    report=['# Перевод базы NPC — 0.15.106','',f'`{bar}` **{percent:.2f}%** точных записей. Полоса округлена.','','| Состояние публичной выгрузки | Строк | Доля |','| --- | ---: | ---: |',f'| С отдельным точным переводом | 11 195 | {percent:.2f}% |',f'| Без отдельной точной записи | 5 049 | {remaining_percent:.2f}% |','| Всего в зафиксированной очереди | 16 244 | 100% |','','Показатель вырос с68,28% до68,92% за счёт103 архивных реплик Громилы. Это не процент всей игры или качества всех строк. Остаток включает переменные реплики,варианты выбора и нейтральный текст: часть такого текста уже обрабатывается отдельными правилами,а не точными записями. Поэтому5 049 не означает5 049 полностью английских живых реплик.','','## Сделано в этом проходе','','| Блок | Добавлено | Проверенные оформления |','| --- | ---: | ---: |','| Архивные реплики Goon / Громилы | 103 | 206:основное и опубликованное |','| Текущие группы ответов | 56 | 58 опубликованных оформлений |','| Отдельные варианты ответа | 75 | В составе56 полных упорядоченных групп |','','Ответы охватывают Рифт,Барри,Дамию,Кэт,Сьюзан,Волшебника,Мортона и других персонажей,а также Великий ужас. Кнопки с одинаковыми словами не переводятся глобально: принимаются только известные полные группы. Команды не исполнялись и не подменялись. Архивные ветки Ауры/Данте явно отделены от текущих: этот проход не подтверждает их доступность сейчас.','','## Проверки','','Все **179 групп тестов и15 проверок разбора базы** запущены заново на итоговом JAR. Новые проверки: **217 703 утверждения** для NPC и **81 691** для ответов. Проверены каждая буква,цвета/курсив,многоцветные ранги и неизменные ники,текущие команды/подсказки/вставки,цифры,счётчики,время,разбиение компонентов,пользовательские замены,настройки и защита подписанного/игрового чата.','','Отдельные запуски105 и106 подтверждают: все206 новых NPC-оформлений и58 оформлений выбора раньше оставались исходными,а теперь дают независимо заданные русские цели. В прежнем скрининге11 252 входа все обычные и цветные результаты совпадают с105. Новые исторические источники не подмешивались в этот текущий скрининг.','','Прежние Java-исходники тестов и эталонные тексты не переписаны. В изолированных копиях29 проверок изменено только ожидаемое общее число записей10525→10628. Отдельный явно обозначенный снимок11 редактур из105 сохранён; это не замена остальных ожидаемых строк.','','## Производительность','','Алгоритмы обработки сообщений,кэшей и отрисовки не менялись. Дизассемблированные методы сравнены: изменены только статическое заполнение списка ответов и регистрация нового файла в загрузчике NPC. Три вложенных класса отличаются только отладочными номерами строк;120 исполняемых классов совпадают побайтно. Новых проверок каждый кадр,регулярных выражений и сетевых запросов в игре нет. Дополнительные записи загружаются один раз.','','Контрольный замер: четыре отдельных процесса105→106→106→105,десять режимов с одинаковыми сохранёнными результатами;по300 000 прогревочных вызовов и четыре раунда минимум по200 000 вызовов на режим. Новые переводы делают больше полезной работы,поэтому не выдаются за равную нагрузку.',f'Разница времени отдельных режимов: **{min(changes):+.2f}…{max(changes):+.2f}%**. Режимы с замедлением более5% в обоих порядках: '+(', '.join(performance['both_orders_slower_over5']) or 'не обнаружены в выбранном замере')+'.','','Это не игровой FPS,не время кадра и не0,1% low. Нулевое влияние на FPS не гарантируется: размер перевода и данные поиска тоже имеют стоимость. Установленная сборка и игра не менялись.','','## Источники','','25 зафиксированных публичных страниц с сохранёнными ревизиями,условиями и полными блоками;копия английского текста остаётся неизменной. Закрытые серверные скрипты и все возможные живые ветки не объявляются полностью выгруженными.','','[Таблица по обозначениям NPC](NPC_ALL_SPEAKERS_0.15.106_RU.md) содержит полный остаток по зафиксированной очереди.','',f'SHA-256 JAR:`{digest}`']
    tag_totals=Counter(r['tag'] or 'SYSTEM/UNTAGGED' for r in work)
    tag_exact=Counter(r['tag'] or 'SYSTEM/UNTAGGED' for r in work if r['exact_registration'])
    categories=['## Из чего состоит очередь','','| Тип источника | Всего | Точных записей | Без точной записи |','| --- | ---: | ---: | ---: |']
    for tag,total in tag_totals.items():categories.append(f'| {tag} | {total} | {tag_exact[tag]} | {total-tag_exact[tag]} |')
    categories+=['','SYSTEM/UNTAGGED включает уведомления и варианты ответа; не все эти строки являются репликами NPC. Отдельные обработчики и шаблоны не включены в долю точных записей.','']
    report[report.index('## Сделано в этом проходе'):report.index('## Сделано в этом проходе')]=categories
    note=['Удлинённый повтор трёх заранее выбранных режимов: по1 000 000 прогревочных вызовов и четыре раунда по1 000 000. '+f'Диапазон времени: **{min(long_changes):+.2f}…{max(long_changes):+.2f}%**. Режимы с замедлением более5% в обоих порядках: '+(', '.join(performance['long_repeat']['both_orders_slower_over5']) or 'не обнаружены в выбранном повторе')+'. Исходный короткий результат с+14,2% не удалён.','']
    at=next(i for i,text in enumerate(report) if text.startswith('Это не игровой FPS'))
    report[at:at]=note
    report_text='\n'.join(report).replace('Мортона','Морта').replace('JAR:`','JAR: `')
    report_text=re.sub(r'([А-Яа-яЁё])(?=\d)',r'\1 ',report_text)
    report_text=re.sub(r'(\d)(?=[А-Яа-яЁё])',r'\1 ',report_text)
    report_text=re.sub(r',(?=[А-Яа-яЁё])',', ',report_text)
    report_text=re.sub(r':(?=[А-Яа-яЁё])',': ',report_text)
    (SOURCE/'NPC_PROGRESS_0.15.106_RU.md').write_text(report_text+'\n',encoding='utf-8')
    assert not DEST.exists();DEST.mkdir()
    def cp(path,rel):
        target=DEST/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
    cp(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/resources/data/witherskyru/npc-expansion106.json','src/main/resources/META-INF/licenses/NPC106-SOURCES.txt','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java','src/main/java/ru/wither/skyru/ChatNotificationTranslator.java','src/main/java/ru/wither/skyru/NamedNpcTemplates.java'):cp(ROOT/rel,rel)
    for p in SOURCE.glob('*0.15.106*'):cp(p,'translation-source/'+p.name)
    for p in (ROOT/'tools').glob('*npc106*'):cp(p,'tools/'+p.name)
    for p in (ROOT/'tests').glob('*.java'):cp(p,'tests/'+p.name)
    cp(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    count_manifest=load(ROOT/'build/npc106-test-sources/COUNT_EXPECTATION_UPDATES.json')
    cp(ROOT/'build/npc106-test-sources/COUNT_EXPECTATION_UPDATES.json','isolated-test-sources/COUNT_EXPECTATION_UPDATES.json')
    for r in count_manifest['files']:cp(ROOT/'build/npc106-test-sources'/r['file'],'isolated-test-sources/'+r['file'])
    cp(ROOT/'translation-source-regression106/EXPECTATION_SUPERSESSION105.json','expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(ROOT/'translation-source-regression106/EXPECTATION_SUPERSESSION105.json')['changed_files']:cp(ROOT/'translation-source-regression106'/r['file'],'expectation-supersession/'+r['file'])
    for p in a.checks.glob('*-output.txt'):cp(p,'checks/'+p.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):cp(a.checks/name,'checks/'+name)
    for name,directory in [('audit',a.audit),('benchmark',a.benchmark),('long-repeat',a.long)]:
        for p in directory.rglob('*'):
            if p.is_file() and p.suffix in ('.json','.txt') and not any(s.startswith('isolated') for s in p.relative_to(directory).parts):cp(p,'evidence/'+name+'/'+p.relative_to(directory).as_posix())
    for version in ('0.15.105-dev','0.15.106-dev'):
        cp(a.snapshots/version/'NEW_ROWS.json','evidence/snapshots/'+version+'/NEW_ROWS.json')
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):cp(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    (DEST/'README.md').write_text('# Перевод NPC0.15.106\n\n103 архивные реплики и56 текущих групп ответов/75 кнопок. Прогресс:11 195/16 244 точных записей;68,92%. Начните с `translation-source/NPC_PROGRESS_0.15.106_RU.md`.\n\nJAR не установлен автоматически. Это частичный пакет изменений/проверок,не самостоятельный проект:для повторения нужны прежний проект,зафиксированные данные и зависимости PrismLauncher. Прежние Java-тесты сохранены;29 изолированных копий обновляют только ожидаемый размер общего набора,редактуры105 явно обозначены отдельно.\n\nАлгоритмы отрисовки/обработки сообщений не менялись. Реальные FPS/время кадра не измерялись.\n\n`HASHES.json` содержит SHA-256 всех файлов кроме манифеста. Предыдущие пакеты не изменены.\n',encoding='utf-8')
    manifest={p.relative_to(DEST).as_posix():sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',manifest)
    for rel,h in manifest.items():assert sha(DEST/rel)==h
    archive=DEST.parent/'NPC_TRANSLATION_0.15.106.zip';assert not archive.exists()
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(DEST.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(manifest)+1
        for rel,h in manifest.items():assert hashlib.sha256(z.read(rel)).hexdigest().upper()==h
    target=DEST.parent/jar.name;assert not target.exists();shutil.copy2(jar,target);assert sha(target)==digest
    for rel,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/rel)==h
    result={'jar':str(target),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'progress':progress,'installed_pack_modified':False}
    write(ROOT/'build/NPC106_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
