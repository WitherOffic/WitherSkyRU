"""Evidence-gated115 report and recoverable delivery;never installs the JAR."""
import argparse,json,math,shutil,zipfile,hashlib
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,rows,sha,write
from prepare_npc115 import BASE,load
from audit_npc115 import PREVIOUS

DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-21_1')

def performance(path,digest,count):
    data=load(path/'RAW.json');assert data['runtime_jar_sha256']==digest and len(data['summary'])==count
    assert [r['version'] for r in data['records']]==['0.15.114-dev','0.15.115-dev','0.15.115-dev','0.15.114-dev']
    for r in data['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.114-dev' else digest)
    for m in data['summary']:
        values=[r['results'][m['mode']]['useful_visible_outputs'] for r in data['records']];assert m['equal_useful_output'] and values[0]==values[1]==values[2]==values[3]
        assert all(math.isfinite(o['wall_change_percent']) for o in m['orders'])
    slow=[m['mode'] for m in data['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])]
    assert not slow,('Investigate persistent slowdown,all raw retained',slow)
    return {'modes':count,'min_wall_change_percent':min(o['wall_change_percent'] for m in data['summary'] for o in m['orders']),'max_wall_change_percent':max(o['wall_change_percent'] for m in data['summary'] for o in m['orders']),'max_absolute_allocation_delta_bytes':max(abs(o['after_bytes']-o['before_bytes']) for m in data['summary'] for o in m['orders']),'both_orders_slower_over5':slow,'scope':'Isolated receipt,not FPS,frame-time,0.1%low,or proof of zero cost.'},data

def perf_table(d):
    lines=['| Режим | Порядок A | Порядок B |','| --- | ---: | ---: |']
    for m in d['summary']:lines.append(f"| {m['mode']} | {m['orders'][0]['wall_change_percent']:+.2f}% | {m['orders'][1]['wall_change_percent']:+.2f}% |")
    return lines

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','audit','snapshots','dispatch','benchmark','long'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.115-dev.jar';digest=sha(jar);audit=load(a.audit/'AUDIT.json')
    assert audit['runtime_jar_sha256']==digest and audit['fresh_test_groups']==189 and audit['receipt_and_render_algorithms_unchanged']
    perf,short=performance(a.benchmark,digest,17);long,longraw=performance(a.long,digest,6)
    assert {m['mode'] for m in longraw['summary']}==set(load(SOURCE/'NPC_PERFORMANCE_CONTROL_0.15.115.json')['long_modes'])
    evidence=load(SOURCE/'NPC_RECOVERY_SOURCES_0.15.115.json');selected=evidence['records'];literal={r['source']:r for r in selected if r['kind']=='literal'}
    snapshots=load(a.snapshots/'0.15.115-dev/SNAPSHOTS.json');snaps={r['source']:r for r in snapshots['records']};work=rows(SOURCE/'NPC_WORKLIST_0.15.114.jsonl');numbers={r['queue_index'] for r in selected if r['kind']=='integer'}
    for i,r in enumerate(work):
        r['exact_literal115']=r['source'] in literal;r['bounded_pattern115']=i in numbers
        if r['exact_literal115']:
            v=literal[r['source']];assert not r['exact_registration'];r['exact_registration']=True;r['review_status']='authored_source_backed_recovery_not_current_live_completeness';r['registered_targets_not_automatically_certified']=[{'resource':'npc-recovery115.json','translation':v['translation']}];r['runtime_flat_snapshot_not_quality_proof']=snaps[r['source']]['flat']
    exact=sum(r['exact_registration'] for r in work);assert (len(work),exact,sum(r['exact_literal115'] for r in work),len(numbers))==(16244,13984,15,11)
    (SOURCE/'NPC_WORKLIST_0.15.115.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8')
    percent=100*exact/len(work);full,tail=divmod(round(percent*30*8/100),8);bar='█'*full+['','▏','▎','▍','▌','▋','▊','▉'][tail]+'░'*(30-full-int(tail>0))
    progress={'version':'0.15.115-dev','runtime_jar_sha256':digest,'exact_keys':exact,'total_frozen':16244,'exact_percent':percent,'without_exact_key':16244-exact,'new_exact_queue_keys':15,'new_normalized_keys_outside_frozen_queue':20,'new_literals':35,'new_integer_shapes':13,'original_numeric_queue_patterns':11,'bounded_templates_total':243,'choice_groups':345,'archival_records':33,'source_pages':12,'fresh_test_groups':189,'parser_tests':15,'new_assertions':audit['new_assertions'],'unchanged_dispatch_rows':11252,'byte_identical_runtime_classes':audit['byte_identical_runtime_classes'],'runtime_classes':audit['runtime_classes'],'startup_method_edits_only':audit['method_differences'],'performance_not_fps':perf,'long_performance_not_fps':long,'installed_pack_modified':False,'game_fps_measured':False};write(SOURCE/'NPC_PROGRESS_0.15.115.json',progress)
    progress['unchanged_dispatch_rows']=audit['unchanged_dispatch_rows'];progress['explicit_dispatch_coverage_upgrades']=len(audit['explicit_dispatch_coverage_upgrades']);write(SOURCE/'NPC_PROGRESS_0.15.115.json',progress)
    def cell(s):return s.replace('|','&#124;').replace('\n',' ')
    lines=['# Реплики NPC и обучение — 0.15.115','','35 полных реплик и13 ограниченных числовых форм. Исторические реплики отмечены отдельно. Это не48 новых точных строк замороженной очереди и не48 новых NPC.','','| ID | Персонаж | Тип | Источник | Перевод |','| --- | --- | --- | --- | --- |']
    for r in selected:
        kind=('архивная; ' if r['archival'] else '')+('число 1–16 символов' if r['kind']=='integer' else 'точная реплика')
        lines.append(f"| {r['id']} | {cell(r['speaker'])} | {kind} | {cell(r['source'])} | {cell(r['translation'])} |")
    lines+=['','## Отдельные результаты проверки','','- Frosty: фраза о покупке снежной пушки уже переводилась. Пустой параметр `text=` страницы не добавлен в игровой ключ и не выдан за новый перевод.','- В архивных уроках разделителем считается только `/` перед следующим полным цветным префиксом NPC. Команда `/coop` сохранена.','- У Nancy символ `|` явно защищён `nowiki` в публикации. Он сохранён как видимый символ, а не удалён как разметка. Это не утверждение, что NPC-заглушка доступен в каждой текущей сессии.','- Дополнительный пробел Brynmor, ошибочное `&!` Vex и нестандартный обфусцированный префикс Lost Adventurer не исправлены догадкой. Эти три источника сохранены в списке исключений.','- Единицы `мин.`, `с`, `г.` используются с динамическими числами без неверного выбора русского окончания. Числа и их текущий цвет копируются, а не пересобираются.','', '[Полные блоки, зафиксированные страницы, преобразования и цветовые соответствия](NPC_RECOVERY_SOURCES_0.15.115.json).']
    (SOURCE/'NPC_TRANSLATIONS_0.15.115_RU.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    tags=defaultdict(Counter);speakers=defaultdict(Counter)
    for r in work:
        for c in (tags[r['tag'] or 'SYSTEM'],speakers[r['speaker'] or '(без говорящего)']):c['total']+=1;c['exact']+=int(r['exact_registration']);c['new']+=int(r['exact_literal115'])
    lines=['# Точные ключи по обозначениям персонажей — 0.15.115','','Наличие точного ключа не равно доказанной полноте живой игры. Числовые шаблоны учитываются отдельно.','','| Персонаж/обозначение | Всего | Точных | Доля | Без точного ключа | Новых |','| --- | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(speakers.items(),key=lambda p:p[0].casefold()):lines.append(f"| {cell(name)} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['total']-c['exact']} | {c['new']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.115_RU.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    lines=['# Перевод публичной базы — 0.15.115','',f'`{bar}` **{percent:.2f}%** отдельных точных ключей. Полоса округлена.','','| Зафиксированная очередь | Строк | Доля |','| --- | ---: | ---: |',f'| С отдельной точной записью | 13 984 | {percent:.2f}% |',f'| Без отдельной точной записи | 2 260 | {100-percent:.2f}% |','| Всего | 16 244 | 100% |','','## Что добавлено','','**35 полных реплик NPC и13 ограниченных числовых форм**, охватывающих11 исходных шаблонов с числами. Восстановлены архивные уроки жителей Хаба: Andrew, Jack, Jamie, Tom, Leo, Felix, Duke, Stella, Vex и Liam. Добавлены два сообщения Jerry со счётчиками частей, приглашение Generow забрать награду и явно опубликованное приветствие Mayor Nancy. Числовые сообщения относятся к Ikrus, Researcher Timmy, Rezzus, Simon, Tia the Fairy, Trevor и Wizard.','','33 из48 записей/форм относятся к архивным разделам, включая31 урок Хаба. Их исторический статус не скрывается. Это дополнение уже существующих персонажей, не заявление о48 новых NPC. Одна найденная реплика Frosty уже обрабатывалась и не посчитана повторно.','','В счётчик замороженной очереди прибавлены только **15 совпадающих точных исходников**. Ещё20 очищенных от ошибочного межсообщенческого разделителя ключей полезны для перевода, но в исходной очереди записаны иначе и в процент не включены. Числовые формы также не выданы за точные ключи; всего ограниченных шаблонов теперь243, групп ответов345.','','[Таблица всех новых реплик](NPC_TRANSLATIONS_0.15.115_RU.md).','','## Качество и проверки','',f"Заново пройдены **189 Java-групп и15 тестов разбора выгрузки**. Новые проверки полных реплик: {audit['new_assertions']['TestNpcLiterals115']:,} утверждений; числовых форм: {audit['new_assertions']['TestNpcNumbers115']:,}.".replace(',',' '),'','Проверяется полный авторский текст, а не просто появление кириллицы. Каждый символ проверен на сохранение текущего смыслового цвета и действий. Проверены числовые значения, разбиение компонентов, время, счётчики повторов, пользовательские замены, переключатели, чат игроков и подписанные сообщения. Допустимые и ошибочные числовые формы проверены отдельно. Авторские цели сверены с независимым исходным файлом.','','Все11 252 прежних контрольных реплики сохранили прежние обычные и цветные результаты. Старые Java-тесты и положительные эталоны не переписаны. В39 изолированных копиях сохранены предыдущие явные изменения ожиданий и обновлено только количество story-записей10764→10799; новых ослаблений отрицательных случаев нет. Замороженная выгрузка и предыдущий пакет114 проверены по контрольным суммам.','','## Оптимизация','',f"**{audit['byte_identical_runtime_classes']} из{audit['runtime_classes']} исполняемых классов побайтно совпадают с114**. Смысловые изменения кода ограничены подключением новых таблиц при запуске. Алгоритмы приёма сообщений, HUD, отрисовка, кэши и числовой валидатор неизменны. Есть дополнительная память/загрузка таблиц и несколько новых правил для соответствующих NPC; нулевые накладные расходы не обещаются.",'','Числа не перебираются заранее и не кэшируются вместе с сообщениями: переиспользуются16 неизменяемых планов смещений для ограниченной длины значения. Текущие цвета/события берутся из текущего сообщения. Исправление разделителей выполняется в подготовке базы, не каждый кадр и не в игре. Новых покадровых проходов, регулярных выражений или сетевых запросов нет.','','Сравнение114→115→115→114 на равном полезном результате,17 режимов:','']+perf_table(short)+['','Длинная серия шести заранее выбранных режимов:','']+perf_table(longraw)+['','Все исходные замеры сохранены. Проверка отсутствия замедления более5% в обоих порядках пройдена. Это ограниченный контроль, а не доказательство отсутствия меньшей разницы. Новые переводы не сравниваются с оставлением английского текста как с равной полезной работой.','','**Это не игровые FPS, время кадра или0,1%low. Отсутствие влияния на FPS не гарантируется. Установленная сборка и игра не менялись.**','','## Остаток и ограничения','','Процент относится к точным ключам зафиксированной публичной очереди, не к каждому слову текущего сервера. Остаток включает переменные, нейтральные строки, части таблиц и уже покрываемые общими правилами варианты. Нестандартные или повреждённые источники не угадываются. Новые данные сертифицированы для чата, не для всех поверхностей мира/меню.','','| Тип | Всего | Точных | Без точного ключа |','| --- | ---: | ---: | ---: |']
    for name,c in sorted(tags.items()):lines.append(f"| {name} | {c['total']} | {c['exact']} | {c['total']-c['exact']} |")
    lines=[v.replace('Все11 252 прежних контрольных реплики сохранили прежние обычные и цветные результаты.','В11 251 из11 252 контрольных реплик обычный и цветной результаты полностью сохранены. Единственное ожидаемое расширение — прежде английское приветствие Mayor Nancy теперь переведено.') for v in lines]
    assert audit['new_negative_upgrades']==6 and len(audit['explicit_negative_to_positive_sources'])==5
    previous_expectation_text='В39 изолированных копиях сохранены предыдущие явные изменения ожиданий и обновлено только количество story-записей10764→10799; новых ослаблений отрицательных случаев нет.'
    revised_expectation_text='В39 изолированных копиях обновлено количество story-записей10764→10799. В трёх из них шесть прежних отрицательных проверок пяти точных строк явно заменены проверками конкретного авторского перевода: Nancy с защищённым символом |, полное приглашение Generow, полная вторая реплика Andrew и две полные части сообщения Jerry. Повреждённые варианты с разделителем и остальные отрицательные случаи сохранены; исходные исторические тесты не переписаны. Все изменения ожиданий перечислены в EXPECTATION_UPDATES.json.'
    assert sum(previous_expectation_text in v for v in lines)==1
    lines=[v.replace(previous_expectation_text,revised_expectation_text) for v in lines]
    lines+=['','## Исправленное пересечение до выпуска','','Первый кандидат перехватывал уже существующие точные поздравления Simon для5,6,7 лет. Сравнение прежних результатов выявило это до выпуска. В новые шаблоны добавлены явные исключения для прежних точных ключей; прежние тексты не изменены. Код проверки чисел и выбора правила не менялся. Предварительный JAR и прерванный общий прогон сохранены отдельно как незавершённые доказательства,не как успешный результат. После исправления весь набор189 групп выполнен заново на окончательном JAR.','', '[Таблица по всем обозначениям персонажей](NPC_ALL_SPEAKERS_0.15.115_RU.md).','',f'SHA-256 JAR: `{digest}`']
    (SOURCE/'NPC_PROGRESS_0.15.115_RU.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    archive=DEST.parent/'NPC_TRANSLATION_0.15.115.zip';target=DEST.parent/jar.name;assert not any(p.exists() for p in (DEST,archive,target));DEST.mkdir()
    def cp(p,rel):q=DEST/rel;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
    cp(jar,jar.name)
    for name in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java','src/main/java/ru/wither/skyru/NamedNpcTemplates.java','src/main/resources/data/witherskyru/npc-recovery115.json','src/main/resources/data/witherskyru/npc-structured115.json','src/main/resources/META-INF/licenses/NPC115-SOURCES.txt'):cp(ROOT/name,name)
    for p in SOURCE.glob('*0.15.115*'):cp(p,'translation-source/'+p.name)
    for p in (ROOT/'tools').glob('*npc115*'):cp(p,'tools/'+p.name)
    for p in (ROOT/'tests').glob('*.java'):cp(p,'tests/'+p.name)
    cp(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    copies=ROOT/'build/npc115-test-sources';cp(copies/'EXPECTATION_UPDATES.json','isolated-test-sources/EXPECTATION_UPDATES.json')
    for r in load(copies/'EXPECTATION_UPDATES.json')['files']:cp(copies/r['file'],'isolated-test-sources/'+r['file'])
    regression=ROOT/'translation-source-regression115';cp(regression/'EXPECTATION_SUPERSESSION105.json','expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(regression/'EXPECTATION_SUPERSESSION105.json')['changed_files']:cp(regression/r['file'],'expectation-supersession/'+r['file'])
    for p in a.checks.glob('*-output.txt'):cp(p,'checks/'+p.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):cp(a.checks/name,'checks/'+name)
    for name,folder in [('audit',a.audit),('benchmark',a.benchmark),('long-control',a.long)]:
        for p in folder.rglob('*'):
            if p.is_file() and p.suffix in ('.json','.txt') and not any(x.startswith('isolated') for x in p.relative_to(folder).parts):cp(p,'evidence/'+name+'/'+p.relative_to(folder).as_posix())
    for version in ('0.15.114-dev','0.15.115-dev'):
        cp(a.snapshots/version/'SNAPSHOTS.json','evidence/snapshots/'+version+'/SNAPSHOTS.json')
        for name in ('DISPATCH_ROWS.jsonl','DISPATCH_SUMMARY.json'):cp(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    cp(a.dispatch/'RUNTIME_SCOPE.json','evidence/dispatch/RUNTIME_SCOPE.json')
    cp(ROOT/'build/npc115-snapshots-1995c683e6cc4bf683088f6e62e0f74d/0.15.114-dev/SNAPSHOTS.json','evidence/initial-probe114/SNAPSHOTS.json')
    interrupted=ROOT/'build/npc115-checks-c9dd96ca7b6c49109d8c362646c8b9d1'
    for p in interrupted.iterdir():
        if p.is_file() and p.suffix in ('.json','.txt'):cp(p,'evidence/interrupted-draft-checks/'+p.name)
    for p in (ROOT/'build/npc115-superseded-draft').iterdir():
        if p.is_file():cp(p,'evidence/superseded-draft/'+p.name+('.disabled' if p.suffix=='.jar' else ''))
    (DEST/'README.md').write_text('# WitherSkyRU0.15.115\n\n35 полных реплик и13 числовых форм. Новых точных ключей замороженной очереди15;20 исправленных ключей и числовые формы в процент не включены. Архивные уроки отмечены отдельно.\n\nПакет изменений/проверок,не самостоятельная исходная сборка. Предыдущие файлы сохранены. Полный отчёт и таблицы в translation-source. Предварительный кандидат с пересечением трёх старых реплик сохранён только как.disabled;окончательная сборка прошла полный повторный прогон.\n\nJAR автоматически не установлен. Отрисовка и алгоритмы неизменны,но новые таблицы требуют памяти/загрузки. Изолированные замеры не являются FPS/0,1%low;нулевая стоимость не гарантируется.\n',encoding='utf-8')
    manifest={p.relative_to(DEST).as_posix():sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',manifest)
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(DEST.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(manifest)+1
        for rel,h in manifest.items():assert sha(DEST/rel)==h and hashlib.sha256(z.read(rel)).hexdigest().upper()==h
        assert z.read('HASHES.json')==(DEST/'HASHES.json').read_bytes()
    shutil.copy2(jar,target);assert sha(target)==digest
    for rel,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/rel)==h
    result={'jar':str(target),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'progress':progress,'installed_pack_modified':False};write(ROOT/'build/NPC115_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
