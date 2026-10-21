"""Evidence-gated114 release:224 queue fragments vs1344 finite complete envelopes."""
import argparse,hashlib,json,math,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,rows,sha,write
from prepare_eggs114 import BASE,load
from audit_eggs114 import PREVIOUS
from finalize_npc113 import table
DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_11')

def performance(path,digest,size):
    d=load(path/'RAW.json');assert d['runtime_jar_sha256']==digest and len(d['summary'])==size
    assert [r['version'] for r in d['records']]==['0.15.113-dev','0.15.114-dev','0.15.114-dev','0.15.113-dev']
    for r in d['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.113-dev' else digest)
    for m in d['summary']:
        values=[r['results'][m['mode']]['useful_visible_outputs'] for r in d['records']];assert m['equal_useful_output'] and values[0]==values[1]==values[2]==values[3]
        assert len(m['orders'])==2 and all(math.isfinite(o['wall_change_percent']) for o in m['orders'])
    slow=[m['mode'] for m in d['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])];assert not slow,('Investigate persistent slowdown;all raw retained',slow)
    return {'scope':'Isolated receipt,not FPS/frame-time/0.1%low;startup/additional useful work not measured.','modes':size,'both_orders_slower_over5':slow,'min_wall_change_percent':min(o['wall_change_percent'] for m in d['summary'] for o in m['orders']),'max_wall_change_percent':max(o['wall_change_percent'] for m in d['summary'] for o in m['orders']),'max_absolute_allocation_delta_bytes':max(abs(o['after_bytes']-o['before_bytes']) for m in d['summary'] for o in m['orders']),'all_raw_retained':True},d

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','audit','benchmark','long'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.114-dev.jar';digest=sha(jar);audit=load(a.audit/'AUDIT.json');assert audit['runtime_jar_sha256']==digest and audit['fresh_test_groups']==187 and audit['all_receipt_and_render_algorithms_unchanged']
    perf,short=performance(a.benchmark,digest,17);long,longraw=performance(a.long,digest,6);assert {m['mode'] for m in longraw['summary']}==set(load(SOURCE/'EGG_PERFORMANCE_CONTROL_0.15.114.json')['long_modes'])
    evidence=load(SOURCE/'EGG_SOURCES_0.15.114.json');selected=evidence['records'];locations={r['source']:r for r in selected if r['kind']=='location'};assert len(locations)==224
    snapshot={r['source']:r for r in load(a.snapshots/'0.15.114-dev/SNAPSHOTS.json')['records']};work=rows(SOURCE/'NPC_WORKLIST_0.15.113.jsonl')
    for r in work:
        r['exact_literal114']=r['source'] in locations
        if r['exact_literal114']:
            v=locations[r['source']];assert not r['exact_registration'];r['exact_registration']=True;r['review_status']='authored_location_fragment_plus_six_offline_complete_chat_envelopes_not_live_certification';r['registered_targets_not_automatically_certified']=[{'resource':'eggs-expansion114.json','translation':v['translation']}];r['runtime_flat_snapshot_not_quality_proof']=snapshot[r['source']]['flat']
    exact=sum(r['exact_registration'] for r in work);remaining=len(work)-exact;assert (len(work),exact,remaining)==(16244,13969,2275)
    (SOURCE/'NPC_WORKLIST_0.15.114.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8')
    percent=100*exact/len(work);full,tail=divmod(round(percent*30*8/100),8);bar='█'*full+['','▏','▎','▍','▌','▋','▊','▉'][tail]+'░'*(30-full-int(tail>0))
    progress={'version':'0.15.114-dev','runtime_jar_sha256':digest,'exact_keys':exact,'total_frozen':16244,'exact_percent':percent,'without_exact_key':remaining,'new_exact_queue_keys':224,'new_destination_fragments':224,'new_complete_compositions':1344,'total_new_runtime_records':1568,'egg_types':6,'new_NPC_speakers':0,'prior_bounded_templates':230,'prior_choice_groups':345,'fresh_test_groups':187,'parser_tests':15,'new_assertions':audit['new_assertions'],'new_fixtures':4480,'source_pages':2,'historical_test_sources_unchanged':audit['historical_test_sources_unchanged'],'isolated_expectation_files':37,'new_negative_upgrades':0,'byte_identical_runtime_classes':124,'startup_method_edits_only':audit['method_differences'],'performance_not_fps':perf,'long_performance_not_fps':long,'unverified_terminal_punctuation_compositions':18,'installed_pack_modified':False,'game_fps_measured':False};write(SOURCE/'NPC_PROGRESS_0.15.114.json',progress)
    lines=['# 224 описания мест охоты Хоппити — 0.15.114','','Каждое место используется в шести заранее подготовленных полных сообщениях о находке. Это не 224 новых NPC. Цветные фрагменты перенесены по смыслу, в том числе при изменении порядка слов.','','| № очереди | Исходник | Перевод |','| ---: | --- | --- |']
    for r in locations.values():lines.append(f"| {r['location_id']} | {r['source']} | {r['translation']} |")
    lines+=['','## Шесть типов яиц','','| Исходное название | Прежнее название предмета в моде, сохранено |','| --- | --- |']
    for source,target in evidence['headers']['egg_names'].items():lines.append(f'| {source} | {target} |')
    lines+=['','Lunch и Déjeuner сохраняют прежнее общее русское название; это не слияние исходных ключей. Имя NPC Хани в притяжательном контексте не переводится как материал «мёд».','', '[Все 1 568 записи с исходниками, переводами, цветными сегментами и контекстом](EGG_SOURCES_0.15.114.json).']
    (SOURCE/'EGG_TRANSLATIONS_0.15.114_RU.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    tags=defaultdict(Counter);speakers=defaultdict(Counter)
    for r in work:
        for c in (tags[r['tag'] or 'SYSTEM / без тега'],speakers[r['speaker'] or '(без говорящего)']):c['total']+=1;c['exact']+=int(r['exact_registration']);c['new']+=int(r['exact_literal114'])
    lines=['# Покрытие по обозначениям персонажей — 0.15.114','','Точный ключ не равен доказанному идеальному переводу всей игры. Шаблоны и составные сообщения не прибавляются к числу строк зафиксированной очереди. В этой пачке расширены уведомления события, не список NPC.','','| Обозначение | Всего | Точных | Доля | Без отдельной точной записи | Новых ключей |','| --- | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(speakers.items(),key=lambda x:x[0].casefold()):lines.append(f"| {name.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['total']-c['exact']} | {c['new']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.114_RU.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    report=['# Перевод публичной базы — 0.15.114','',f'`{bar}` **{percent:.2f}%** отдельных точных записей. Полоса округлена.','','| Зафиксированная очередь | Строк | Доля |','| --- | ---: | ---: |',f'| С отдельной точной записью | 13 969 | {percent:.2f}% |',f'| Без отдельной точной записи | 2 275 | {100-percent:.2f}% |','| Всего | 16 244 | 100% |','','## Новая пачка','','Переведены **224 опубликованных описания мест охоты Хоппити** и подготовлены **1 344 полных варианта сообщения о находке**: шесть конкретных типов шоколадных яиц × 224 места. Всего 1 568 новых записей в таблице. Только 224 исходные строки мест входят в зафиксированную очередь и прибавлены к её числителю. 1 344 составных сообщения не выданы за 1 344 новые строки выгрузки или NPC.','','Это расширение **перевода чата**. Для полных сообщений недостаточно перевода отдельной подписи места: исходник заранее объединён с опубликованным заголовком и названием яйца. Чат игрока и подписанные сообщения не меняются. Перевод других поверхностей интерфейса здесь не сертифицируется.','','Охвачены Хаб, фермерские и шахтёрские острова, Паучье логово, Край, Багровый остров, Парк, Хаб подземелий, Разлом и Галатея. Падежи и порядок слов заданы заранее; цветной фрагмент «Пороховые шахты» остаётся при своём названии, даже когда «подъёмник» перемещён перед ним в русском. Шесть названий яиц согласованы с прежним словарём мода.','','[Таблица всех 224 мест и названий яиц](EGG_TRANSLATIONS_0.15.114_RU.md).','','## Проверки','',f"Заново пройдены **187 групп проверок и 15 тестов разбора базы** на окончательном JAR. Новый набор: **{audit['new_assertions']:,} утверждений**, 4 480 вариантов оформления — первичные независимые цвета, унаследованный цвет родительского сообщения и цветные фрагменты опубликованного места. Цели отдельно восстановлены из авторского файла, а не скопированы из упакованного плана.".replace(f"{audit['new_assertions']:,}",f"{audit['new_assertions']:,}".replace(',',' ')),f"Проверены все буквы перевода, текущие цвета и действия, подсказки, разбиение компонентов, время, счётчики, пользовательские замены, переключатели и защита пользовательского/подписанного чата. Старые 11 252 контрольные реплики дают прежние обычные и цветные результаты. {audit['historical_test_sources_unchanged']} прежних Java-исходников сохранены; в 37 изолированных файлах удержаны прошлые явные изменения ожиданий и обновлено только количество SYSTEM-записей. Новых ослаблений отрицательных случаев или изменений прежних положительных эталонов нет.",'','Первый общий прогон остановлен из-за лишнего повторного создания словаря в новом тесте. Восемь одинаковых неизменяемых тестовых словарей теперь могут переиспользоваться для строго одинаковой пары «ключ замены/значение». Сообщения, стили и действия не кэшируются. Все случаи, циклы и утверждения сохранены; полный набор запущен заново, прежние успехи не использованы. Это оптимизация тестового инструмента, не изменение мода.','','До окончательных проверок уточнены два новых перевода: направление взгляда на мельницу и положение наверху заставы. Предварительный JAR и исходные формулировки сохранены отдельно, не выданы как установленная сборка.','','## Производительность','','**124 из 125 исполняемых классов побайтно совпадают с 113.** В оставшемся изменён только метод подключения таблицы SYSTEM при запуске. Все алгоритмы обработки сообщений, кэши, HUD и отрисовка неизменны. Нет новой покадровой работы, дополнительных регулярных выражений, сетевых запросов или генерации названий и падежей в игре. Дополнительные таблицы всё же требуют памяти и времени загрузки.','','Сравнение 113→114→114→113: 17 равных по видимому результату режимов, четыре отдельных процесса, от 300 000 прогревочных вызовов и четыре раунда от 200 000.','']+table(short)+['','Длинная серия шести заранее выбранных режимов: от 1 000 000 прогревочных вызовов и четыре раунда от 1 000 000.','']+table(longraw)+['','Все исходные замеры сохранены. Нет режима с замедлением более 5% в обоих порядках. Это не доказывает отсутствие меньших накладных расходов. Дополнительная полезная работа по переводу новых сообщений не сравнивалась с оставлением английского текста как с равной работой.','','**Изолированные замеры не являются игровым FPS, временем кадра или 0,1% low. Нулевое влияние на FPS не гарантируется. Игра и установленная сборка не изменялись.**','','## Границы источника и процента','','Использованы две зафиксированные публичные страницы: шесть оболочек сообщения и 224 описания мест. Полные сообщения — конечное офлайн-раскрытие опубликованных шаблонов, не перехваченные серверные пакеты. Пунктуация сохранена дословно: три описания заканчиваются восклицательным знаком, поэтому 18 комбинаций имеют два завершающих знака. Возможные серверные варианты со свёрнутой пунктуацией не объявляются проверенными. Неизвестные форматы остаются исходными, а не угадываются.','','Процент относится к отдельным точным ключам замороженной очереди, не к полноте всего живого SkyBlock. 345 групп ответов и 230 ограниченных шаблонов учитываются отдельно. Остаток включает переменные, нейтральные строки, фрагменты и уже обрабатываемые общими правилами варианты.','','| Тип | Всего | Точных | Без точного ключа |','| --- | ---: | ---: | ---: |']
    for name,c in sorted(tags.items()):report.append(f"| {name} | {c['total']} | {c['exact']} | {c['total']-c['exact']} |")
    report+=['','[Таблица по всем обозначениям персонажей](NPC_ALL_SPEAKERS_0.15.114_RU.md).','',f'SHA-256 JAR: `{digest}`']
    (SOURCE/'NPC_PROGRESS_0.15.114_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
    archive=DEST.parent/'NPC_TRANSLATION_0.15.114.zip';target=DEST.parent/jar.name;assert not any(p.exists() for p in (DEST,archive,target));DEST.mkdir()
    def cp(p,rel):q=DEST/rel;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
    cp(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java','src/main/resources/data/witherskyru/eggs-expansion114.json','src/main/resources/META-INF/licenses/EGGS114-SOURCES.txt'):cp(ROOT/rel,rel)
    for p in SOURCE.glob('*0.15.114*'):cp(p,'translation-source/'+p.name)
    for p in (ROOT/'tools').glob('*eggs114*'):cp(p,'tools/'+p.name)
    for name in ('prepare_expansion114.py','derive_expansion114_probe.py'):cp(ROOT/'tools'/name,'tools/'+name)
    for p in (ROOT/'tests').glob('*.java'):cp(p,'tests/'+p.name)
    cp(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    copies=ROOT/'build/eggs114-test-sources';cp(copies/'EXPECTATION_UPDATES.json','isolated-test-sources/EXPECTATION_UPDATES.json')
    for r in load(copies/'EXPECTATION_UPDATES.json')['files']:cp(copies/r['file'],'isolated-test-sources/'+r['file'])
    sup=ROOT/'translation-source-regression114';cp(sup/'EXPECTATION_SUPERSESSION105.json','expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(sup/'EXPECTATION_SUPERSESSION105.json')['changed_files']:cp(sup/r['file'],'expectation-supersession/'+r['file'])
    for p in a.checks.glob('*-output.txt'):cp(p,'checks/'+p.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):cp(a.checks/name,'checks/'+name)
    for name,folder in [('audit',a.audit),('benchmark',a.benchmark),('long-control',a.long)]:
        for p in folder.rglob('*'):
            if p.is_file() and p.suffix in ('.json','.txt') and not any(part.startswith('isolated') for part in p.relative_to(folder).parts):cp(p,'evidence/'+name+'/'+p.relative_to(folder).as_posix())
    for version in ('0.15.113-dev','0.15.114-dev'):
        cp(a.snapshots/version/'SNAPSHOTS.json','evidence/snapshots/'+version+'/SNAPSHOTS.json')
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):cp(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    cp(a.dispatch/'RUNTIME_SCOPE.json','evidence/dispatch/RUNTIME_SCOPE.json')
    cp(ROOT/'build/eggs114-snapshots-ca255c45160e470a9fd78747245859d7/0.15.113-dev/SNAPSHOTS.json','evidence/initial-probe113/SNAPSHOTS.json')
    interrupted=ROOT/'build/eggs114-checks-4469738127994ff2be8d59f5b117fe46'
    for p in interrupted.iterdir():
        if p.is_file() and p.suffix in ('.txt','.java','.json'):cp(p,'evidence/interrupted-test-run/'+p.name)
    for p in (ROOT/'build/eggs114-editorial-draft').iterdir():
        if p.is_file():cp(p,'evidence/editorial-draft/'+p.name+('.superseded-disabled' if p.suffix=='.jar' else ''))
    (DEST/'README.md').write_text('# WitherSkyRU 0.15.114\n\n224 описания мест охоты Хоппити и1344 полных офлайн-составленных сообщения о шести типах яиц. Это расширение перевода чата,не1568 новых NPC. В процент очереди включены только224 исходных фрагмента. Отчёт и таблица в translation-source.\n\nПакет изменений и проверок,не самостоятельный проект: нужны прежний проект,зафиксированная база и зависимости. HASHES.json содержит все файлы,кроме самого манифеста. Предыдущий пакет113 сохранён.\n\nИсторические Java-тесты и текстовые/цветовые эталоны не переписаны. Новый полный прогон выполнен заново после устранения повторной загрузки одинаковых тестовых словарей;все4480 вариантов оформления/утверждения сохранены.\n\nJAR автоматически не установлен. Алгоритмы отрисовки/обработки неизменны. Замеры вне игры не являются FPS/временем кадра/0,1%low;нулевое влияние не гарантируется. Точность неперехваченных живых пунктуационных вариантов не сертифицируется.\n',encoding='utf-8')
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
    result={'jar':str(target),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'progress':progress,'installed_pack_modified':False};write(ROOT/'build/EGGS114_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
