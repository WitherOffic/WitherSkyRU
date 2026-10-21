"""Evidence-gated113 delivery;patterns never masquerade as exact queue keys."""
import argparse,hashlib,json,math,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,rows,sha,write
from prepare_npc113 import BASE,load
from audit_npc113 import PREVIOUS
DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_10')

def performance(path,digest,size):
    d=load(path/'RAW.json');assert d['runtime_jar_sha256']==digest and len(d['summary'])==size
    assert [r['version'] for r in d['records']]==['0.15.112-dev','0.15.113-dev','0.15.113-dev','0.15.112-dev']
    for r in d['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.112-dev' else digest)
    for m in d['summary']:
        values=[r['results'][m['mode']]['useful_visible_outputs'] for r in d['records']];assert m['equal_useful_output'] and values[0]==values[1]==values[2]==values[3]
        assert len(m['orders'])==2 and all(math.isfinite(o['wall_change_percent']) for o in m['orders'])
    slow=[m['mode'] for m in d['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])]
    assert not slow,('Investigate persistent slowdown;retain all raw',slow)
    return {'scope':'Isolated receipt,not FPS/frame-time/0.1%low;startup and added useful work not measured.','modes':size,'both_orders_slower_over5':slow,'min_wall_change_percent':min(o['wall_change_percent'] for m in d['summary'] for o in m['orders']),'max_wall_change_percent':max(o['wall_change_percent'] for m in d['summary'] for o in m['orders']),'max_absolute_allocation_delta_bytes':max(abs(o['after_bytes']-o['before_bytes']) for m in d['summary'] for o in m['orders']),'all_raw_retained':True},d

def table(d):
    lines=['| Режим | Время, порядок 1 | Порядок 2 | Разница памяти, байт/вызов |','| --- | ---: | ---: | ---: |']
    for m in d['summary']:
        a,b=m['orders'];lines.append(f"| {m['mode']} | {a['wall_change_percent']:+.2f}% | {b['wall_change_percent']:+.2f}% | {a['after_bytes']-a['before_bytes']:+.2f} / {b['after_bytes']-b['before_bytes']:+.2f} |")
    return lines

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','audit','benchmark','long'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.113-dev.jar';digest=sha(jar);audit=load(a.audit/'AUDIT.json')
    assert audit['runtime_jar_sha256']==digest and audit['fresh_test_groups']==186 and audit['render_hud_and_client_dispatch_byte_identical']
    perf,short=performance(a.benchmark,digest,17);long,longraw=performance(a.long,digest,6)
    assert {m['mode'] for m in longraw['summary']}==set(load(SOURCE/'NPC_PERFORMANCE_CONTROL_0.15.113.json')['long_modes'])
    evidence=load(SOURCE/'NPC_STRUCTURED_SOURCES_0.15.113.json');patterns=evidence['records'];literal=evidence['literal_records'][0]
    work=rows(SOURCE/'NPC_WORKLIST_0.15.112.jsonl');by_id={int(r['id']):r for r in patterns};snapshot={r['source']:r for r in load(a.snapshots/'0.15.113-dev/SNAPSHOTS.json')['records']}
    for i,r in enumerate(work):
        r['exact_literal113']=r['source']==literal['source'];r['bounded_pattern113']=i in by_id
        if i in by_id:
            v=by_id[i];assert not r['exact_registration'] and r['source']==v['queue_row']['source']
            r['structured_runtime113']={'canonical_source':v['source'],'translation':v['translation'],'capture_type':v['capture_type'],'archival':v['archival'],'color_repair_not_new_wording':v['color_fix'],'exact_key_claim':False}
        if r['exact_literal113']:
            assert not r['exact_registration'];r['exact_registration']=True;r['review_status']='fixed_Duke_X_proper_name_full_block_reviewed_old_X_heuristic_false_positive';r['registered_targets_not_automatically_certified']=[{'resource':'npc-literals113.json','translation':literal['translation']}];r['runtime_flat_snapshot_not_quality_proof']=snapshot[literal['source']]['flat']
    exact=sum(r['exact_registration'] for r in work);remaining=len(work)-exact;assert (len(work),exact,remaining)==(16244,13745,2499)
    (SOURCE/'NPC_WORKLIST_0.15.113.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8')
    percent=100*exact/len(work);full,tail=divmod(round(percent*30*8/100),8);bar='█'*full+['','▏','▎','▍','▌','▋','▊','▉'][tail]+'░'*(30-full-int(tail>0))
    progress={'version':'0.15.113-dev','runtime_jar_sha256':digest,'exact_keys':exact,'total_frozen':16244,'exact_percent':percent,'without_exact_key':remaining,'new_exact_keys':1,'new_bounded_patterns':93,'published_color_repairs':2,'new_template_resource_rules':95,'new_numeric_rules':79,'new_username_rules':16,'prior_name_rules':135,'combined_rules':230,'archival_rules_in_new_resource':4,'retained_old_patterns_not_new':13,'speaker_labels':44,'fresh_test_groups':186,'parser_tests':15,'new_assertions':audit['new_assertions'],'primary_cases':96,'published_layouts':audit['published_layouts'],'pinned_pages':45,'historical_test_sources_unchanged':audit['historical_test_sources_unchanged'],'isolated_expectation_files':37,'byte_identical_runtime_classes':120,'render_hud_and_client_dispatch_byte_identical':True,'method_differences':audit['method_differences'],'performance_not_fps':perf,'long_performance_not_fps':long,'installed_pack_modified':False,'game_fps_measured':False}
    write(SOURCE/'NPC_PROGRESS_0.15.113.json',progress)
    lines=['# Полные исходники и переводы — 0.15.113','','95 закрытых шаблонов: 93 новых и 2 исправления цветного оформления прежнего перевода. Число или имя {value} не переводится. В конце — одна отдельная точная реплика. Четыре шаблона отмечены архивными по заголовку источника. Это не перечень всех живых серверных форматов.','','| № очереди | Персонаж | Тип | Архив | Исходник | Перевод |','| ---: | --- | --- | --- | --- | --- |']
    for r in patterns:lines.append(f"| {r['id']} | {r['speaker']} | {'цветовое исправление' if r['color_fix'] else r['capture_type']} | {'да' if r['archival'] else 'нет'} | {r['source'].replace('|','&#124;')} | {r['translation'].replace('|','&#124;')} |")
    lines+=['', '## Отдельная точная реплика','',literal['source'],'',literal['translation'],'','Duke X — постоянное имя персонажа, а не переменная. Это подтверждено полным исходным блоком.']
    (SOURCE/'NPC_TRANSLATIONS_0.15.113_RU.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    tags=defaultdict(Counter);speakers=defaultdict(Counter)
    for r in work:
        for c in (tags[r['tag'] or 'SYSTEM / без тега'],speakers[r['speaker'] or '(без говорящего)']):c['total']+=1;c['exact']+=int(r['exact_registration']);c['pattern113']+=int(r['bounded_pattern113'])
    lines=['# Покрытие по обозначениям персонажей — 0.15.113','','Точные записи и шаблоны не складываются в один процент. Шаблоны113 относятся к строкам без точной записи. Предыдущие общие правила и шаблоны могут покрывать дополнительную часть остатка.','','| Обозначение | Всего | Точных | Доля точных | Без точной записи | Шаблонов113 отдельно |','| --- | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(speakers.items(),key=lambda x:x[0].casefold()):lines.append(f"| {name.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['total']-c['exact']} | {c['pattern113']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.113_RU.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    report=['# Перевод публичной базы — 0.15.113','',f'`{bar}` **{percent:.2f}%** отдельных точных записей. Полоса округлена.','','| Зафиксированная очередь | Строк | Доля |','| --- | ---: | ---: |',f'| С отдельной точной записью | 13 745 | {percent:.2f}% |',f'| Без отдельной точной записи | 2 499 | {100-percent:.2f}% |','| Всего | 16 244 | 100% |','','## Новые переводы NPC','','**93 новых ограниченных шаблона**, **2 исправления цветного текста** и **1 точная реплика**. 95 шаблонов нового ресурса относятся к 44 обозначениям NPC; 4 отмечены архивными в публикации. 79 шаблонов с числом, 16 с именем игрока. Они не прибавлены к числителю точных записей. 13 обнаруженных уже работающих шаблонов оставлены без изменений и не выданы за новые.','','Добавлены ответы об интеллекте для 18 конкретно опубликованных NPC, количество ресурсов у Бейкера, испытания синего пламени, оставшиеся подарки Джерри, контакты Абифона, аукцион, личный рекорд и обращения к игроку. Строки Жюльетты и Ромеро сохраняют прежнюю русскую формулировку, но теперь корректно обрабатывают опубликованные цветные компоненты. Одна точная реплика Дина о Герцоге X восстановлена после проверки полного блока: X здесь часть имени.','','[Все исходники и переводы](NPC_TRANSLATIONS_0.15.113_RU.md).','','## Проверки','',f"На окончательном JAR заново пройдены **186 групп проверок и 15 тестов разбора базы**. Новый набор — **{audit['new_assertions']:,} утверждений**: все буквы перевода, актуальные цвета, действия по нажатию, подсказки, неизменность каждого символа числа/имени, разбиение компонентов, время, счётчики, пользовательские замены, защита пользовательского и подписанного чата.".replace(f"{audit['new_assertions']:,}",f"{audit['new_assertions']:,}".replace(',',' ')),f"Прежние 11 252 контрольные строки дают побуквенно прежние обычные и опубликованные цветные результаты. {audit['historical_test_sources_unchanged']} прежних Java-исходников сохранены. В 37 изолированных файлах удержаны прежние изменения ожиданий и обновлено число точных реплик; только один прежний отрицательный случай — фиксированное имя Duke X — явно переведён в положительную проверку. Остальные отрицательные случаи и прежние положительные цветовые/текстовые эталоны не ослаблены.",'','Первая целевая проверка выявила ошибку подключения словаря в самом тесте: новый словарь устанавливался в клиент, но не в экземпляр переводчика. Исходник и неуспешный запуск сохранены; подключение исправлено, утверждения не удалены. Целевой и полный наборы выполнены заново.','','## Производительность','','120 из 125 классов исполняемого JAR совпадают с 112 побайтно. Ещё у одного вложенного класса изменились только отладочные номера строк: после исключения таблицы номеров все остальные байты совпадают. Классы отрисовки, HUD, клиентского распределения сообщений и словаря не изменены. Расширена существующая таблица поиска по префиксу NPC, отдельного общего прохода переводчиков нет. Две прежние специализированные ветки направлены в подготовленный план для сохранения цветов.','','Для каждой закрытой фразы планы смещений на 16 допустимых длин подготавливаются при запуске. В рабочем пути проверяются только неизменная оболочка и ограниченные символы числа/имени; нет новых регулярных выражений, разбора чисел, генерации русских окончаний, сетевых запросов или покадровой обработки. Стили и действия берутся из текущего сообщения, не кэшируются вместе с живым именем. Новые таблицы всё же занимают память и имеют ненулевую стоимость при запуске и переводе нового сообщения.','','Сравнение 112→113→113→112, четыре отдельных процесса: 17 равных по видимому результату режимов, от 300 000 прогревочных вызовов и четыре раунда от 200 000 вызовов.','']+table(short)+['','Удлинённый заранее заданный контроль шести режимов, включая все 135 прежних шаблонов имён: от 1 000 000 прогревочных вызовов и четыре раунда от 1 000 000 вызовов.','']+table(longraw)+['','Все исходные результаты сохранены. Нет режима с замедлением более 5% в обоих порядках. Это не исключает меньших накладных расходов и не означает нулевую стоимость. Новые полезные переводы не сравнивались с оставлением английского текста как с равной работой.','','**Эти замеры вне игры не являются FPS, временем кадра или 0,1% low. Отсутствие влияния на игровой FPS не гарантируется. Мод не установлен автоматически, игра не менялась.**','','## Границы процента','','Процент относится к точным ключам конкретной зафиксированной публичной очереди, не к идеальному переводу всей игры. 345 групп ответов и 230 ограниченных шаблонов учитываются отдельно. 2 499 строк без точного ключа — не обязательно 2 499 полностью непереведённых живых реплик: среди них переменные, нейтральные строки, фрагменты публикаций и уже обрабатываемые шаблоны. Непроверенные обозначения не регистрируются ради роста процента.','','| Тип | Всего | Точных | Без точного ключа |','| --- | ---: | ---: | ---: |']
    for name,c in sorted(tags.items()):report.append(f"| {name} | {c['total']} | {c['exact']} | {c['total']-c['exact']} |")
    report+=['','[Таблица по всем обозначениям персонажей](NPC_ALL_SPEAKERS_0.15.113_RU.md).','',f'SHA-256 JAR: `{digest}`']
    (SOURCE/'NPC_PROGRESS_0.15.113_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
    target=DEST.parent/jar.name;archive=DEST.parent/'NPC_TRANSLATION_0.15.113.zip';assert not any(p.exists() for p in (DEST,target,archive));DEST.mkdir()
    def cp(p,rel):q=DEST/rel;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
    cp(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java','src/main/java/ru/wither/skyru/NamedNpcTemplates.java','src/main/java/ru/wither/skyru/ChatNotificationTranslator.java','src/main/resources/data/witherskyru/npc-structured113.json','src/main/resources/data/witherskyru/npc-literals113.json','src/main/resources/META-INF/licenses/NPC113-SOURCES.txt'):cp(ROOT/rel,rel)
    for p in SOURCE.iterdir():
        if p.is_file() and ('0.15.113' in p.name or p.name in ('NPC113_TEST_FIXTURE_CORRECTION.json','NPC113_CLASSFILE_AUDIT_CORRECTION.json')):cp(p,'translation-source/'+p.name)
    for p in (ROOT/'tools').glob('*npc113*'):cp(p,'tools/'+p.name)
    for p in (ROOT/'tests').glob('*.java'):cp(p,'tests/'+p.name)
    cp(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    copies=ROOT/'build/npc113-test-sources';cp(copies/'EXPECTATION_UPDATES.json','isolated-test-sources/EXPECTATION_UPDATES.json')
    for r in load(copies/'EXPECTATION_UPDATES.json')['files']:cp(copies/r['file'],'isolated-test-sources/'+r['file'])
    sup=ROOT/'translation-source-regression113';cp(sup/'EXPECTATION_SUPERSESSION105.json','expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(sup/'EXPECTATION_SUPERSESSION105.json')['changed_files']:cp(sup/r['file'],'expectation-supersession/'+r['file'])
    for p in a.checks.glob('*-output.txt'):cp(p,'checks/'+p.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):cp(a.checks/name,'checks/'+name)
    for name,folder in [('audit',a.audit),('benchmark',a.benchmark),('long-control',a.long)]:
        for p in folder.rglob('*'):
            if p.is_file() and p.suffix in ('.json','.txt') and not any(part.startswith('isolated') for part in p.relative_to(folder).parts):cp(p,'evidence/'+name+'/'+p.relative_to(folder).as_posix())
    for version in ('0.15.112-dev','0.15.113-dev'):
        cp(a.snapshots/version/'SNAPSHOTS.json','evidence/snapshots/'+version+'/SNAPSHOTS.json')
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):cp(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    cp(a.dispatch/'RUNTIME_SCOPE.json','evidence/dispatch/RUNTIME_SCOPE.json')
    cp(ROOT/'build/npc113-snapshots-b83aa758568d47feb1d17fbe5944333a/0.15.112-dev/SNAPSHOTS.json','evidence/initial-probe112/SNAPSHOTS.json')
    for folder in ('npc113-targeted-1c05e0d7ff194cb1968a5243661c17f5','npc113-targeted-1add86edcb9f48af9bbae6fe95fdade0'):
        for name in ('TestNpc113-output.txt','TestNpc113-before-fixture-repair.java','SCOPE.json'):
            p=ROOT/'build'/folder/name
            if p.exists():cp(p,'evidence/targeted/'+folder+'/'+name)
    (DEST/'README.md').write_text('# WitherSkyRU 0.15.113\n\n93 новых закрытых шаблона NPC,2 исправления цветного оформления,1 новая точная реплика. Полный отчёт и таблицы в translation-source.\n\nПакет изменений и проверок,не самостоятельный исходный проект: нужны прежний проект,зафиксированная база и зависимости. HASHES.json содержит все файлы,кроме самого манифеста. Предыдущий пакет112 сохранён.\n\nИсторические положительные текстовые и цветовые эталоны сохранены;единственное новое исключение из прежних отрицательных случаев — подтверждённое постоянное имя Duke X. Фикстура словаря в новом тесте исправлена с сохранением неуспешного запуска.\n\nJAR автоматически не установлен. Новая покадровая работа не добавлена. Замеры вне игры не являются FPS/временем кадра/0,1%low;нулевое влияние не гарантируется.\n',encoding='utf-8')
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
    result={'jar':str(target),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'progress':progress,'installed_pack_modified':False};write(ROOT/'build/NPC113_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
