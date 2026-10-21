"""Evidence-gated111 delivery;archival NPC literals are separate from current dialogue."""
import argparse,hashlib,json,math,re,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,rows,sha,write
from audit_expansion111 import BASE,PREVIOUS
DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_8')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def polish(text):
    text=text.replace('35 изолированных копий обновляют только ожидания количества записей.','35 изолированных копий обновляют ожидания количества записей; в6 из них дополнительно перечислено31 осознанное расширение отрицательных проверок:28 архивных реплик,две фиксированные ячейки Фанна и настоящее имя [Eye]. Остальные отрицательные случаи сохранены,в том числе42 оставшиеся записи с переменными в наборе102. Положительные эталоны прежних переводов и цветов не переписаны.')
    text=re.sub(r'([,;:])(?=[А-Яа-яЁёA-Za-z])',r'\1 ',text)
    return re.sub(r'(?<=[А-Яа-яЁё])(?=[0-9])|(?<=[0-9])(?=[А-Яа-яЁё])',' ',text)
def performance(path,digest,size):
    d=load(path/'RAW.json');assert d['runtime_jar_sha256']==digest and len(d['summary'])==size and [r['version'] for r in d['records']]==['0.15.110-dev','0.15.111-dev','0.15.111-dev','0.15.110-dev']
    for r in d['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.110-dev' else digest)
    for m in d['summary']:
        assert m['equal_useful_output'] and len(m['orders'])==2
        outputs=[r['results'][m['mode']]['useful_visible_outputs'] for r in d['records']];assert outputs[0]==outputs[1]==outputs[2]==outputs[3]
        assert all(math.isfinite(o['wall_change_percent']) for o in m['orders'])
    changes=[o['wall_change_percent'] for m in d['summary'] for o in m['orders']];slow=[m['mode'] for m in d['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])];assert not slow,('Investigate persistent change;retain raw data',slow)
    return {'scope':'Isolated receipt;not FPS/frame time/0.1% low. New502 useful translations are not treated as equal old work.','modes':size,'order':['110','111','111','110'],'min_wall_change_percent':min(changes),'max_wall_change_percent':max(changes),'max_abs_allocation_delta_bytes':max(abs(o['after_bytes']-o['before_bytes']) for m in d['summary'] for o in m['orders']),'both_orders_slower_over5':slow,'all_raw_retained':True}

def diagnostic(path,digest):
    d=load(path/'RAW.json');assert d['runtime_jar_sha256']==digest and d['replaces_previous_results'] is False
    runs=d['records'];assert len(runs)==6
    assert [r['version'] for r in runs]==['0.15.110-dev','0.15.111-dev','0.15.111-dev','0.15.110-dev','0.15.110-dev','0.15.111-dev']
    assert [r['escape_analysis'] for r in runs]==[True,True,True,True,False,False]
    modes=['existing_dynamic','unknown_system_notifications','retained_cosmetic_notifications','retained_boss_dialogues'];results=[]
    for r in runs:assert r['jar_sha256']==(BASE if r['version']=='0.15.110-dev' else digest)
    for mode in modes:
        values=[r['results'][mode] for r in runs]
        assert all(v['equal_useful_output'] for v in values)
        assert all(v['useful_visible_outputs']==values[0]['useful_visible_outputs'] for v in values)
        changes=[100*(values[b]['ns_per_message']/values[a]['ns_per_message']-1) for a,b in [(0,1),(3,2)]]
        assert all(math.isfinite(v) for v in changes)
        assert not all(v>5 for v in changes),('Investigate post-hoc persistent slowdown; retain all raw',mode,changes)
        results.append({'mode':mode,'normal_wall_changes_percent':changes,'normal_allocation_deltas_bytes':[values[b]['bytes_per_message']-values[a]['bytes_per_message'] for a,b in [(0,1),(3,2)]],'without_escape_analysis_allocation_delta_bytes':values[5]['bytes_per_message']-values[4]['bytes_per_message']})
    return {'scope':'Post-hoc diagnosis,not replacement of short/predeclared long controls or game FPS. Escape-analysis-disabled pair is an attribution aid,not proof of allocation site.','results':results,'all_raw_retained':True}
def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','audit','benchmark','long','diagnostic'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.111-dev.jar';digest=sha(jar);audit=load(a.audit/'AUDIT.json');assert audit['runtime_jar_sha256']==digest and audit['fresh_test_groups']==184 and audit['all_receipt_and_render_algorithms_unchanged']
    perf=performance(a.benchmark,digest,15);long=performance(a.long,digest,3);diag=diagnostic(a.diagnostic,digest);assert {m['mode'] for m in load(a.long/'RAW.json')['summary']}==set(load(SOURCE/'EXPANSION_PERFORMANCE_CONTROL_0.15.111.json')['long_modes'])
    evidence=load(SOURCE/'EXPANSION_SOURCES_0.15.111.json');fixed={r['source']:r for r in evidence['records']};assert len(fixed)==502
    snapshot={r['source']:r for r in load(a.snapshots/'0.15.111-dev/SNAPSHOTS.json')['records']};work=rows(SOURCE/'NPC_WORKLIST_0.15.110.jsonl')
    for r in work:
        s=r['source'];r['exact_literal111']=s in fixed
        if s in fixed:
            assert not r['exact_registration'];v=fixed[s];r['exact_registration']=True;r['review_status']='authored_full_literal_primary_and_published_styles_tested_not_live_certified';r['registered_targets_not_automatically_certified']=[{'resource':'npc-expansion111.json' if v['tag']=='NPC' else 'system-expansion111.json','translation':v['translation']}];r['runtime_flat_snapshot_not_quality_proof']=snapshot[s]['flat'];r['runtime_published_outputs111_not_quality_proof']=snapshot[s]['layouts']
    exact=sum(r['exact_registration'] for r in work);remaining=len(work)-exact;assert (len(work),exact,remaining)==(16244,13262,2982)
    (SOURCE/'NPC_WORKLIST_0.15.111.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8');percent=100*exact/len(work);rest=100-percent;bar='█'*round(percent*30/100)+'░'*(30-round(percent*30/100))
    full,tail=divmod(round(percent*30*8/100),8);bar='█'*full+['','▏','▎','▍','▌','▋','▊','▉'][tail]+'░'*(30-full-int(tail>0))
    progress={'version':'0.15.111-dev','runtime_jar_sha256':digest,'exact_keys':exact,'total_frozen':16244,'exact_percent':percent,'without_exact_key':remaining,'without_exact_percent':rest,'new_exact_keys':502,'new_npc_literals':133,'new_archival_npc_literals':130,'new_nonarchival_npc_literals':3,'new_system_literals':369,'previous_changed_candidates_not_new':3,'static_choice_groups_unchanged':345,'prior_name_templates103':135,'selected_style_repaired_cumulative105':72,'fresh_test_groups':184,'parser_tests':15,'new_assertions':audit['new_assertions'],'primary_cases':502,'published_layouts':509,'pinned_pages':212,'historical_test_sources_unchanged':audit['historical_test_sources_unchanged'],'isolated_count_assertion_updates':35,'byte_identical_runtime_classes':124,'startup_method_edits_only':audit['method_differences'],'hot_methods_unchanged':True,'performance_not_fps':perf,'long_performance_not_fps':long,'installed_pack_modified':False,'game_fps_measured':False};write(SOURCE/'NPC_PROGRESS_0.15.111.json',progress)
    speakers=defaultdict(Counter);tags=defaultdict(Counter)
    for r in work:
        for c in (speakers[r['speaker'] or '(без говорящего)'],tags[r['tag'] or 'SYSTEM / без тега']):c['total']+=1;c['exact']+=int(r['exact_registration']);c['new']+=int(r['exact_literal111'])
    table=['# Остаток по обозначениям персонажей — 0.15.111','','Доля отдельных точных ключей,не процент полной готовности игры. Архивные реплики включены в общий корпус и обозначены в отчёте.','','| Обозначение | Всего | Точных | Доля | Новых ключей111 | Без точной записи |','| --- | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(speakers.items(),key=lambda x:x[0].casefold()):table.append(f"| {name.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['new']} | {c['total']-c['exact']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.111_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    table=['# 502 проверенных перевода — 0.15.111','','Архивные заголовки сохранены,не выдаются за действующих персонажей. Имена в техническом префиксе NPC сохранены как в источнике.','','| № | Тип | Архив | Исходник | Русский перевод | Цветных фрагментов |','| ---: | --- | --- | --- | --- | ---: |']
    for n,r in enumerate(fixed.values(),1):table.append(f"| {n} | {r['tag']} | {'да' if r['archival'] else 'нет'} | {r['source'].replace('|','&#124;')} | {r['translation'].replace('|','&#124;')} | {len(r['style_anchors'])} |")
    (SOURCE/'EXPANSION_TRANSLATIONS_0.15.111_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    report=['# Перевод публичной базы — 0.15.111','',f'`{bar}` **{percent:.2f}%** точных записей. Полоса округлена.','','| Состояние зафиксированной очереди | Строк | Доля |','| --- | ---: | ---: |',f'| С отдельным точным переводом | 13 262 | {percent:.2f}% |',f'| Без отдельной точной записи | 2 982 | {rest:.2f}% |','| Всего | 16 244 | 100% |','','**502 новых полных сообщения:133 реплики NPC и369 игровых уведомлений.** Среди NPC **130 реплик имеют архивный заголовок**,3 — неархивные. Это не133 новых действующих NPC. В остатке буквальных реплик много исторических текстов;переменные и служебные обозначения не превращены в буквальные фразы ради увеличения счётчика.','','Архивная часть включает старые реплики Барри,агентов,стражников,городских помощников и потерявшегося искателя приключений. Неархивные точные добавления — две фразы Фанна о ячейках обучения питомцев и реплика Ока. Игровая часть — Разлом,события,охота,рыбалка,руны,предметы,погода,награды и ограничения.','','[Полная таблица всех502 переводов](EXPANSION_TRANSLATIONS_0.15.111_RU.md).','','| Тип источника | Всего | Точных | Без точного ключа |','| --- | ---: | ---: | ---: |']
    for tag,c in sorted(tags.items()):report.append(f"| {tag} | {c['total']} | {c['exact']} | {c['total']-c['exact']} |")
    report+=['','## Качество и происхождение','','Проверены полные источники и русские цели,повелительные формы,запреты,числовые условия и состояния до/после действия. Названия существ и предметов сверены со словарём. Для каждого сохранённого цветного варианта смысловые фрагменты размечены явно:названия,кнопки,заголовки,редкости,числа,курсив и пунктуация. Составные заголовки и конечные повторяющиеся формы развёрнуты офлайн в точные записи. Во время игры нет нового перевода английских слов по грамматике или обращения к API.','','Источники —212 зафиксированных публичных страниц с ревизиями и полными блоками,не закрытые серверные сценарии и не все живые варианты. Три подготовленные строки уже переводились раньше:они сохранены как непринятые кандидаты и не включены в502 новых ключа.','',f"На окончательном JAR заново пройдены **184 группы проверок и15 проверок разбора базы**. Новый набор — **{audit['new_assertions']:,} утверждений**,502 основных и509 опубликованных оформлений. Проверены каждая буква и её цвет/действия,разбиение компонентов,команды и подсказки,время,счётчики,пользовательские замены,настройки и защита подписанного/пользовательского чата.",'','Новый смешанный тест учитывает прежний договор обработки:NPC принимает обычный префикс или время с одним пробелом,а дополнительные отступы явно проверяются как неподдерживаемые. Строка замены,начинающаяся с #,по прежнему формату является комментарием:для таких ключей проверено отсутствие замены,для остальных смысловых ключей и полных исходников —приоритет пользовательского текста. Два ошибочных предположения первоначального нового теста исправлены без изменений мода и целей перевода;неудачные запуски и исходники сохранены отдельно.','',f"Контроль прежних11 252 реплик:{audit['unchanged_dispatch_rows']} сохранили обычный и цветной результаты. Единственные допустимые изменения — новые выбранные точные фразы;они перечислены в AUDIT.json. Уже переводившиеся строки не переписаны.",f"{audit['historical_test_sources_unchanged']} прежних Java-исходников проверок не изменены.35 изолированных копий обновляют только ожидания количества записей. Ранее обозначенные11 редактур версии105 и24 затронутых эталонных файла сохранены отдельно.",'','## Производительность','','124 из125 исполняемых классов совпадают с110 побайтно. В оставшемся изменены только два метода подключения таблиц при запуске. Алгоритмы обработки сообщений,кэшей и отрисовки не менялись. Нет нового прохода перевода,покадровых операций или сетевых запросов. Таблицы больше и занимают память,а новые полезные переводы требуют работы;нулевая стоимость не заявляется.','',f"Основной контроль:110→111→111→110,четыре отдельных процесса,15 режимов с одинаковыми видимыми результатами,минимум300 000 прогревочных вызовов и четыре раунда минимум по200 000. Диапазон изменения времени:**{perf['min_wall_change_percent']:+.2f}…{perf['max_wall_change_percent']:+.2f}%**.",f"Заранее заданный удлинённый контроль трёх режимов:минимум1 000 000 прогревочных вызовов и четыре раунда минимум по1 000 000. Диапазон:**{long['min_wall_change_percent']:+.2f}…{long['max_wall_change_percent']:+.2f}%**. В обоих наборах нет режима с замедлением более5% в обоих порядках. Все раунды и выделение памяти сохранены.",f"Максимальное абсолютное различие выделения памяти на вызов:{perf['max_abs_allocation_delta_bytes']:.2f} байта в основном и{long['max_abs_allocation_delta_bytes']:.2f} байта в удлинённом наборе. Это не оценка памяти таблиц при запуске.",'','**Это не игровой FPS,время кадра или0,1% low. Нулевое влияние на FPS не гарантируется. Игра и установленная сборка не менялись.**','','## Как читать процент','','81,64% — доля отдельных точных ключей в зафиксированной очереди,не процент идеального перевода всей игры. В остатке есть переменные,нейтральные строки и ответы,часть которых уже переводится общими правилами.345 групп ответов и135 шаблонов имён сохранены,но не прибавлены к числителю. Ранее исправленные72 выбранные проблемы оформления не означают проверку всех оформлений игры.','','[Остаток по обозначениям NPC](NPC_ALL_SPEAKERS_0.15.111_RU.md).','',f'SHA-256 JAR:`{digest}`']
    progress['posthoc_performance_diagnostic_not_fps']=diag;write(SOURCE/'NPC_PROGRESS_0.15.111.json',progress)
    diagnostic_lines=['## Дополнительная диагностика нагрузки','','Короткий контроль дал разнонаправленные результаты для прежних реплик боссов, включая один скачок замедления. Исходные раунды не удалены и не заменены. После этого отдельно выполнены четыре обычных процесса110→111→111→110 и два процесса с отключённым устранением временных объектов Java. Это последующая диагностическая проверка,а не заранее заданный контроль.','','| Режим | Изменение времени,порядок1 | Порядок2 | Дополнительные байты на вызов,обычный режим | Разница без escape analysis |','| --- | ---: | ---: | ---: | ---: |']
    for r in diag['results']:
        changes=r['normal_wall_changes_percent'];alloc=r['normal_allocation_deltas_bytes']
        diagnostic_lines.append(f"| {r['mode']} | {changes[0]:+.2f}% | {changes[1]:+.2f}% | {alloc[0]:+.2f} / {alloc[1]:+.2f} | {r['without_escape_analysis_allocation_delta_bytes']:+.2f} |")
    diagnostic_lines+=['','Различие выделения памяти в обычном режиме не скрыто:нулевые накладные расходы не заявляются. Пара с отключённой оптимизацией помогает отделить влияние компиляции Java,но сама по себе не устанавливает конкретное место выделения объекта. Это обработка входящих сообщений,не покадровый рендер. Измерений игрового FPS и микролагов в этой серии нет.','']
    at=report.index('## Как читать процент');report[at:at]=diagnostic_lines
    (SOURCE/'NPC_PROGRESS_0.15.111_RU.md').write_text(polish('\n'.join(report).replace(f"{audit['new_assertions']:,}",f"{audit['new_assertions']:,}".replace(',',' ')))+'\n',encoding='utf-8')
    assert not DEST.exists();DEST.mkdir()
    def cp(p,rel):
        q=DEST/rel;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
    cp(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java','src/main/resources/data/witherskyru/npc-expansion111.json','src/main/resources/data/witherskyru/system-expansion111.json','src/main/resources/META-INF/licenses/EXPANSION111-SOURCES.txt'):cp(ROOT/rel,rel)
    for p in SOURCE.glob('*0.15.111*'):cp(p,'translation-source/'+p.name)
    for p in (ROOT/'tools').glob('*expansion111*'):cp(p,'tools/'+p.name)
    for p in (ROOT/'tests').glob('*.java'):cp(p,'tests/'+p.name)
    cp(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    count=ROOT/'build/expansion111-test-sources-v5';cp(count/'COUNT_EXPECTATION_UPDATES.json','isolated-test-sources/COUNT_EXPECTATION_UPDATES.json')
    for r in load(count/'COUNT_EXPECTATION_UPDATES.json')['files']:cp(count/r['file'],'isolated-test-sources/'+r['file'])
    sup=ROOT/'translation-source-regression111-v5';cp(sup/'EXPECTATION_SUPERSESSION105.json','expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(sup/'EXPECTATION_SUPERSESSION105.json')['changed_files']:cp(sup/r['file'],'expectation-supersession/'+r['file'])
    for p in a.checks.glob('*-output.txt'):cp(p,'checks/'+p.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):cp(a.checks/name,'checks/'+name)
    for name,folder in [('audit',a.audit),('benchmark',a.benchmark),('long-control',a.long),('posthoc-diagnostic',a.diagnostic)]:
        for p in folder.rglob('*'):
            if p.is_file() and p.suffix in ('.json','.txt','.java') and not any(s.startswith('isolated') for s in p.relative_to(folder).parts):cp(p,'evidence/'+name+'/'+p.relative_to(folder).as_posix())
    for version in ('0.15.110-dev','0.15.111-dev'):
        cp(a.snapshots/version/'SNAPSHOTS.json','evidence/snapshots/'+version+'/SNAPSHOTS.json')
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):cp(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    cp(ROOT/'build/expansion111-snapshots-6c3264d7e33d4a83a681cc8ffb7db812/0.15.110-dev/SNAPSHOTS.json','evidence/initial-probe110/SNAPSHOTS.json')
    scope=load(SOURCE/'EXPANSION_TEST_SCOPE_0.15.111.json')
    for key,folder in [('initial_failed_run','initial-failed-run'),('second_failed_run','second-failed-run')]:
        for p in (ROOT/scope[key]).glob('*'):
            if p.is_file() and p.suffix in ('.txt','.json'):cp(p,'evidence/fixture-scope-corrections/'+folder+'/'+p.name)
    for key,folder in [('retained_initial_test_source','initial'),('retained_second_test_source','second')]:cp(ROOT/scope[key],'evidence/fixture-scope-corrections/'+folder+'/TestExpansion111.java')
    coverage=load(SOURCE/'EXPANSION_COVERAGE_SUPERSESSION_0.15.111.json')
    for p in (ROOT/coverage['failed_pre_supersession_run']).glob('*'):
        if p.is_file() and p.suffix in ('.json','.txt'):cp(p,'evidence/coverage-supersession111/failed-run/'+p.name)
    cp(ROOT/coverage['retained_pre_supersession_test_source'],'evidence/coverage-supersession111/previous-derived-TestNpc102.java')
    upgrades=load(SOURCE/'EXPANSION_COVERAGE_ADDITIONS_0.15.111.json')
    for p in (ROOT/upgrades['earlier_failed_run_before_full_scope_audit']).glob('*'):
        if p.is_file() and p.suffix in ('.json','.txt'):cp(p,'evidence/coverage-additions111/failed-run/'+p.name)
    cp(ROOT/upgrades['retained_earlier_test_source'],'evidence/coverage-additions111/previous-derived-TestNpc99.java')
    (DEST/'README.md').write_text(polish('# WitherSkyRU 0.15.111\n\n502 новых сообщения:133 реплики NPC (130 архивных,3 неархивные) и369 игровых уведомлений.13 262/16 244 точных ключей публичной очереди:81,64%;без отдельного ключа2982. Полная таблица и отчёт находятся в translation-source.\n\nJAR автоматически не установлен. Это частичный пакет изменений и проверок,не самостоятельный проект:нужны прежний проект,зафиксированная база и зависимости PrismLauncher. Прежние исходники тестов не переписаны.35 изолированных копий обновляют только ожидания количества записей. Редактуры105,две поправки договора нового теста и исключение отрицательной проверки для настоящего имени [Eye] обозначены отдельно.\n\nНовой покадровой обработки нет. Замеры вне игры не являются замером FPS,времени кадра или0,1% low,нулевая стоимость не гарантируется. HASHES.json содержит хеши всех файлов кроме самого манифеста. Прежний пакет сохранён.\n'),encoding='utf-8')
    manifest={p.relative_to(DEST).as_posix():sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',manifest)
    for rel,h in manifest.items():assert sha(DEST/rel)==h
    archive=DEST.parent/'NPC_TRANSLATION_0.15.111.zip';assert not archive.exists()
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(DEST.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(manifest)+1
        for rel,h in manifest.items():assert hashlib.sha256(z.read(rel)).hexdigest().upper()==h
        assert z.read('HASHES.json')==(DEST/'HASHES.json').read_bytes()
    target=DEST.parent/jar.name;assert not target.exists();shutil.copy2(jar,target);assert sha(target)==digest
    for rel,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/rel)==h
    result={'jar':str(target),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'progress':progress,'installed_pack_modified':False};write(ROOT/'build/EXPANSION111_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()
