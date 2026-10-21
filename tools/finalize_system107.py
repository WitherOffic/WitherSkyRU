"""Evidence-gated107 package and denominator-stable public-corpus progress."""
import argparse,hashlib,json,math,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,sha,rows,write
from audit_system107 import BASE,PREVIOUS

DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_4')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','audit','benchmark'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.107-dev.jar';digest=sha(jar)
    audit=load(a.audit/'AUDIT.json');assert audit['runtime_jar_sha256']==digest and audit['all_receipt_and_render_algorithms_unchanged'] and audit['fresh_test_groups']==180
    bench=load(a.benchmark/'RAW.json');assert bench['runtime_jar_sha256']==digest and len(bench['summary'])==12
    assert [r['version'] for r in bench['records']]==['0.15.106-dev','0.15.107-dev','0.15.107-dev','0.15.106-dev']
    for r in bench['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.106-dev' else digest)
    for mode in bench['summary']:
        assert mode['equal_useful_output'] and len(mode['orders'])==2
        outputs=[r['results'][mode['mode']]['useful_visible_outputs'] for r in bench['records']];assert outputs[0]==outputs[1]==outputs[2]==outputs[3]
        assert all(math.isfinite(o['wall_change_percent']) for o in mode['orders'])
    changes=[o['wall_change_percent'] for m in bench['summary'] for o in m['orders']]
    slow=[m['mode'] for m in bench['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])]
    assert not slow,('Investigate/repeat before packaging,retain all raw data',slow)
    performance={'scope':'Isolated chat receipt,not game FPS/frame times/0.1% lows. New119 useful translations verified separately,not treated as equal work.','equal_modes':12,'order':['106','107','107','106'],'min_wall_change_percent':min(changes),'max_wall_change_percent':max(changes),'both_orders_slower_over5':slow}
    evidence=load(SOURCE/'SYSTEM_SOURCES_0.15.107.json');fixed={r['source']:r for r in evidence['records']};assert len(fixed)==119
    snap=load(a.snapshots/'0.15.107-dev/SNAPSHOTS.json');snapshot={r['source']:r for r in snap['records']}
    work=rows(SOURCE/'NPC_WORKLIST_0.15.106.jsonl')
    for r in work:
        source=r['source'];r['exact_system_notification107']=source in fixed
        if source in fixed:
            assert not r['exact_registration'] and r['tag'] is None
            r['exact_registration']=True;r['review_status']='authored_system_primary_and_publication_layout_tested_not_live_certified'
            r['registered_targets_not_automatically_certified']=[{'resource':'system-expansion107.json','translation':fixed[source]['translation']}]
            r['runtime_flat_snapshot_not_quality_proof']=snapshot[source]['flat']
            r['runtime_published_outputs107_not_quality_proof']=snapshot[source]['layouts']
    total=len(work);exact=sum(r['exact_registration'] for r in work);remaining=total-exact
    assert (total,exact,remaining)==(16244,11314,4930) and sum(r['exact_system_notification107'] for r in work)==119
    (SOURCE/'NPC_WORKLIST_0.15.107.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8')
    percent=100*exact/total;rest=100-percent
    progress={'version':'0.15.107-dev','runtime_jar_sha256':digest,'exact_keys':exact,'total_frozen':total,'exact_percent':percent,'without_exact_key':remaining,'without_exact_percent':rest,'new_exact_keys':119,'new_system_notifications':119,'new_npc_keys':0,'new_archival_keys':0,'static_choice_groups_unchanged':345,'prior_name_templates103':135,'selected_style_repaired_cumulative105':72,'selected_style_gaps_remaining':0,'fresh_test_groups':180,'parser_tests':15,'new_system_assertions':632925,'primary_cases':119,'published_layouts':119,'byte_identical_runtime_classes':124,'startup_method_edits_only':audit['method_differences'],'hot_method_instructions_unchanged':True,'historical_test_sources_unchanged':audit['historical_test_sources_unchanged'],'isolated_count_assertion_updates':29,'pinned_pages':21,'performance_not_fps':performance,'installed_pack_modified':False,'game_fps_measured':False}
    write(SOURCE/'NPC_PROGRESS_0.15.107.json',progress)
    by=defaultdict(Counter)
    for r in work:
        c=by[r['speaker'] or '(без говорящего)'];c['total']+=1;c['exact']+=int(r['exact_registration']);c['system107']+=int(r['exact_system_notification107'])
    table=['# Публичная очередь — 0.15.107','','Это доля отдельных точных ключей, а не оценка качества или полноты всей игры. Безымянные уведомления отделены от реплик персонажей.','','| Обозначение | Строк | Точных переводов | Доля точных | Новых уведомлений107 | Без точной записи |','| --- | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(by.items(),key=lambda x:x[0].casefold()):table.append(f"| {name.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['system107']} | {c['total']-c['exact']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.107_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    bar='█'*round(percent*30/100)+'░'*(30-round(percent*30/100))
    tags=Counter(r['tag'] or 'SYSTEM/UNTAGGED' for r in work);tag_exact=Counter(r['tag'] or 'SYSTEM/UNTAGGED' for r in work if r['exact_registration'])
    tag_rows=[f'| {tag} | {n} | {tag_exact[tag]} | {n-tag_exact[tag]} |' for tag,n in tags.items()]
    pages=Counter(r['published_row']['page'] for r in fixed.values())
    page_rows=[f'| {page} | {n} |' for page,n in sorted(pages.items())]
    report=['# Перевод публичной базы — 0.15.107','',f'`{bar}` **{percent:.2f}%** точных записей; полоса округлена.','','| Состояние зафиксированной выгрузки | Строк | Доля |','| --- | ---: | ---: |',f'| С отдельным точным переводом | 11 314 | {percent:.2f}% |',f'| Без отдельной точной записи | 4 930 | {rest:.2f}% |','| Всего | 16 244 | 100% |','','Рост с 68,92% до 69,65%: **119 новых системных уведомлений**. Число точных NPC-реплик в этом проходе не увеличивалось. Это не процент всей игры, не подтверждение всех живых веток и не процент редакторской проверки. Остаток включает переменные реплики и варианты выбора, часть которых уже переводится отдельными обработчиками. 4 930 — не число гарантированно английских строк в игре.','','## Состав очереди','','| Тип источника | Всего | Точных записей | Без точной записи |','| --- | ---: | ---: | ---: |']+tag_rows+['','SYSTEM/UNTAGGED включает сообщения, ответы и другие строки без говорящего. 345 прежних групп ответов и 135 шаблонов имён сохранены; они не прибавлены к проценту точных ключей. Исправленные ранее 72 выбранные проблемы оформления не означают проверку всех оформлений игры.','','## Добавлено','','| Блок | Новых сообщений |','| --- | ---: |','| Абифон: лимиты, причины отказа, уже добавленный контакт, Агата | 12 |','| Алсина: 21 блюдо и 9 эффектов напитков | 30 |','| Смена обликов: древний золотой дракон, пробуждённый шлем иссушителя, маленький нефритовый дракон | 53 |','| Событие порошка, Арахна, питомец Броненосец, водные существа, муравьи и другие уведомления | 24 |','','Это полные известные строки, а не глобальная замена отдельных английских слов. Списки блюд и обликов развёрнуты в точные записи до сборки: в игре не добавлены ни регулярные выражения, ни новые проходы перевода. Частичные абзацы обучения, сомнительная разметка, переменные имена и надписи в мире не выдаются за исправленные сообщения чата.','','## Проверки','','На итоговом JAR заново пройдены **180 групп тестов и 15 проверок разбора базы**. Новый набор содержит **632 925 утверждений**, 119 основных и 119 опубликованных оформлений. Проверены русские цели, цвет каждой буквы, курсив/жирность, многоцветный значок звонка, числа и счётчики, время, разбиение компонентов, пользовательские замены, команды/подсказки/вставки, настройки и защищённые каналы. Неизвестные оформления безопасно остаются исходными. Это модельные проверки, а не гарантия всех живых пакетов.','','В отдельных процессах 106/107 подтверждено: все 119 новых строк раньше оставались исходными, а теперь дают независимо заданные русские цели в обычном и опубликованном цветном оформлении. Прежние **11 252** проверяемые строки дают прежние результаты в обоих оформлениях.','','272 прежних Java-исходника тестов не переписаны. В 29 изолированных копиях обновлено только ожидаемое число ранее расширенных NPC-записей 10525→10628. Отдельный снимок 11 редактур из 105 с 24 затронутыми эталонными файлами сохранён явно, остальные ожидания не заменялись.','','## Нагрузка','','124 из 125 исполняемых классов побайтно совпадают с 106. В оставшемся классе отличается только регистрация новой таблицы при запуске; инструкции обработки сообщений, кэшей и отрисовки не менялись. Новых операций каждый кадр и сетевых запросов в игре нет. Это не означает нулевой стоимости: дополнительные записи занимают память, а перевод новых сообщений выполняет полезную работу.','','Контроль нагрузки: четыре отдельных процесса 106→107→107→106, 12 режимов с одинаковыми сохранёнными результатами, минимум 300 000 прогревочных вызовов и четыре раунда минимум по 200 000 вызовов на режим. Новые 119 переводов проверяются отдельно и не выдаются за равную работу.',f'Изменение времени в выбранном замере: **{min(changes):+.2f}…{max(changes):+.2f}%**. Режимов с замедлением более 5% в обоих порядках не обнаружено. Все раунды и показатели выделения памяти сохранены.','','**Это не игровой FPS, время кадра или 0,1% low. Нулевое влияние на FPS не гарантируется. Игра и установленная сборка не менялись.**','','## Источники','','21 зафиксированная публичная страница с ревизиями, условиями и полными блоками. Английский источник не менялся. Эта выгрузка не является полным набором закрытых серверных скриптов.','','| Страница | Новых точных сообщений |','| --- | ---: |']+page_rows+['','[Остаток по обозначениям NPC](NPC_ALL_SPEAKERS_0.15.107_RU.md).','',f'SHA-256 JAR: `{digest}`']
    (SOURCE/'NPC_PROGRESS_0.15.107_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
    assert not DEST.exists();DEST.mkdir()
    def cp(path,rel):
        target=DEST/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
    cp(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/resources/data/witherskyru/system-expansion107.json','src/main/resources/META-INF/licenses/SYSTEM107-SOURCES.txt','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java'):cp(ROOT/rel,rel)
    for pattern in ('*0.15.107*',):
        for p in SOURCE.glob(pattern):cp(p,'translation-source/'+p.name)
    for p in (ROOT/'tools').glob('*system107*'):cp(p,'tools/'+p.name)
    for p in (ROOT/'tests').glob('*.java'):cp(p,'tests/'+p.name)
    cp(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    count_dir=ROOT/'build/system107-test-sources-v2';cp(count_dir/'COUNT_EXPECTATION_UPDATES.json','isolated-test-sources/COUNT_EXPECTATION_UPDATES.json')
    for r in load(count_dir/'COUNT_EXPECTATION_UPDATES.json')['files']:cp(count_dir/r['file'],'isolated-test-sources/'+r['file'])
    supersession=ROOT/'translation-source-regression107-v2';cp(supersession/'EXPECTATION_SUPERSESSION105.json','expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(supersession/'EXPECTATION_SUPERSESSION105.json')['changed_files']:cp(supersession/r['file'],'expectation-supersession/'+r['file'])
    for p in a.checks.glob('*-output.txt'):cp(p,'checks/'+p.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):cp(a.checks/name,'checks/'+name)
    for name,directory in [('audit',a.audit),('benchmark',a.benchmark)]:
        for p in directory.rglob('*'):
            if p.is_file() and p.suffix in ('.json','.txt') and not any(s.startswith('isolated') for s in p.relative_to(directory).parts):cp(p,'evidence/'+name+'/'+p.relative_to(directory).as_posix())
    for version in ('0.15.106-dev','0.15.107-dev'):
        cp(a.snapshots/version/'SNAPSHOTS.json','evidence/snapshots/'+version+'/SNAPSHOTS.json')
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):cp(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    (DEST/'README.md').write_text('# WitherSkyRU 0.15.107\n\n119 новых уведомлений. Прогресс публичной очереди: 11 314 / 16 244 точных записей, 69,65%; без точной записи 4 930. Подробности: `translation-source/NPC_PROGRESS_0.15.107_RU.md`.\n\nJAR автоматически не установлен. Это частичный пакет изменений и проверок, не самостоятельный проект: для повторения нужны прежний проект, зафиксированный корпус и зависимости PrismLauncher. 272 прежних Java-теста сохранены; 29 изолированных копий меняют только ожидаемый общий размер NPC-набора. Редактуры 105 обозначены отдельно.\n\nНовых операций каждый кадр нет. Нагрузка проверена вне игры; FPS и время кадра не измерялись, нулевая стоимость не гарантируется.\n\nHASHES.json содержит SHA-256 всех файлов кроме самого манифеста. Предыдущие пакеты не изменены.\n',encoding='utf-8')
    manifest={p.relative_to(DEST).as_posix():sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',manifest)
    for rel,h in manifest.items():assert sha(DEST/rel)==h
    archive=DEST.parent/'NPC_TRANSLATION_0.15.107.zip';assert not archive.exists()
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(DEST.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(manifest)+1
        for rel,h in manifest.items():assert hashlib.sha256(z.read(rel)).hexdigest().upper()==h
    target=DEST.parent/jar.name;assert not target.exists();shutil.copy2(jar,target);assert sha(target)==digest
    for rel,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/rel)==h
    result={'jar':str(target),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'progress':progress,'installed_pack_modified':False}
    write(ROOT/'build/SYSTEM107_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
