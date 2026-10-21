"""Evidence-gated109 package and denominator-stable public-corpus progress."""
import argparse,hashlib,json,math,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,sha,rows,write
from audit_skins109 import BASE,PREVIOUS

DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_6')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','audit','benchmark'):ap.add_argument('--'+name,type=Path,required=True)
    ap.add_argument('--long',type=Path,required=True)
    a=ap.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.109-dev.jar';digest=sha(jar)
    audit=load(a.audit/'AUDIT.json');assert audit['runtime_jar_sha256']==digest and audit['all_receipt_and_render_algorithms_unchanged'] and audit['fresh_test_groups']==182
    bench=load(a.benchmark/'RAW.json');assert bench['runtime_jar_sha256']==digest and len(bench['summary'])==15
    assert [r['version'] for r in bench['records']]==['0.15.108-dev','0.15.109-dev','0.15.109-dev','0.15.108-dev']
    for r in bench['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.108-dev' else digest)
    for mode in bench['summary']:
        assert mode['equal_useful_output'] and len(mode['orders'])==2
        outputs=[r['results'][mode['mode']]['useful_visible_outputs'] for r in bench['records']];assert outputs[0]==outputs[1]==outputs[2]==outputs[3]
        assert all(math.isfinite(o['wall_change_percent']) for o in mode['orders'])
    changes=[o['wall_change_percent'] for m in bench['summary'] for o in m['orders']]
    slow=[m['mode'] for m in bench['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])]
    assert not slow,('Investigate/repeat before packaging,retain all raw data',slow)
    performance={'scope':'Isolated chat receipt,not game FPS/frame times/0.1% lows. New804 useful translations verified separately,not treated as equal work.','equal_modes':15,'order':['108','109','109','108'],'min_wall_change_percent':min(changes),'max_wall_change_percent':max(changes),'both_orders_slower_over5':slow}
    long=load(a.long/'RAW.json');assert long['runtime_jar_sha256']==digest and len(long['summary'])==3
    assert [r['version'] for r in long['records']]==['0.15.108-dev','0.15.109-dev','0.15.109-dev','0.15.108-dev']
    assert {m['mode'] for m in long['summary']}=={'existing_dynamic','unknown_system_notifications','retained_cosmetic_notifications'}
    for r in long['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.108-dev' else digest)
    for mode in long['summary']:
        assert mode['equal_useful_output'] and len(mode['orders'])==2
        outputs=[r['results'][mode['mode']]['useful_visible_outputs'] for r in long['records']];assert outputs[0]==outputs[1]==outputs[2]==outputs[3]
        assert all(math.isfinite(o['wall_change_percent']) for o in mode['orders'])
    long_changes=[o['wall_change_percent'] for m in long['summary'] for o in m['orders']]
    long_slow=[m['mode'] for m in long['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])];assert not long_slow,('Investigate repeat',long_slow)
    performance['long_repeat']={'modes':3,'warmup_calls_at_least':1000000,'rounds':4,'calls_per_round_at_least':1000000,'min_wall_change_percent':min(long_changes),'max_wall_change_percent':max(long_changes),'both_orders_slower_over5':long_slow,'original_short_data_retained':True}
    evidence=load(SOURCE/'SKINS_SOURCES_0.15.109.json');fixed={r['source']:r for r in evidence['records']};assert len(fixed)==804
    snap=load(a.snapshots/'0.15.109-dev/SNAPSHOTS.json');snapshot={r['source']:r for r in snap['records']}
    work=rows(SOURCE/'NPC_WORKLIST_0.15.108.jsonl')
    for r in work:
        source=r['source'];r['exact_cosmetic_notification109']=source in fixed
        if source in fixed:
            assert not r['exact_registration'] and r['tag'] is None
            r['exact_registration']=True;r['review_status']='authored_system_primary_and_publication_layout_tested_not_live_certified'
            r['registered_targets_not_automatically_certified']=[{'resource':'skins-expansion109.json','translation':fixed[source]['translation']}]
            r['runtime_flat_snapshot_not_quality_proof']=snapshot[source]['flat']
            r['runtime_published_outputs109_not_quality_proof']=snapshot[source]['layouts']
    total=len(work);exact=sum(r['exact_registration'] for r in work);remaining=total-exact
    assert (total,exact,remaining)==(16244,12220,4024) and sum(r['exact_cosmetic_notification109'] for r in work)==804
    (SOURCE/'NPC_WORKLIST_0.15.109.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8')
    percent=100*exact/total;rest=100-percent
    progress={'version':'0.15.109-dev','runtime_jar_sha256':digest,'exact_keys':exact,'total_frozen':total,'exact_percent':percent,'without_exact_key':remaining,'without_exact_percent':rest,'new_exact_keys':804,'new_current_choice_groups':0,'new_cosmetic_notifications':804,'new_npc_keys':0,'new_archival_keys':0,'static_choice_groups_unchanged':345,'prior_name_templates103':135,'selected_style_repaired_cumulative105':72,'selected_style_gaps_remaining':0,'fresh_test_groups':182,'parser_tests':15,'new_cosmetic_assertions':audit['new_cosmetic_assertions'],'primary_cases':804,'published_layouts':810,'byte_identical_runtime_classes':124,'startup_method_edits_only':audit['method_differences'],'hot_method_instructions_unchanged':True,'historical_test_sources_unchanged':audit['historical_test_sources_unchanged'],'isolated_count_assertion_updates':32,'primary_pages':50,'attribution_pages':52,'performance_not_fps':performance,'installed_pack_modified':False,'game_fps_measured':False}
    write(SOURCE/'NPC_PROGRESS_0.15.109.json',progress)
    by=defaultdict(Counter)
    for r in work:
        c=by[r['speaker'] or '(без говорящего)'];c['total']+=1;c['exact']+=int(r['exact_registration']);c['skins109']+=int(r['exact_cosmetic_notification109'])
    table=['# Публичная очередь — 0.15.109','','Это доля отдельных точных ключей, а не оценка качества или полноты всей игры. Безымянные уведомления отделены от реплик персонажей.','','| Обозначение | Строк | Точных переводов | Доля точных | Новых уведомлений109 | Без точной записи |','| --- | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(by.items(),key=lambda x:x[0].casefold()):table.append(f"| {name.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['skins109']} | {c['total']-c['exact']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.109_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    bar='█'*round(percent*30/100)+'░'*(30-round(percent*30/100))
    tags=Counter(r['tag'] or 'SYSTEM/UNTAGGED' for r in work);tag_exact=Counter(r['tag'] or 'SYSTEM/UNTAGGED' for r in work if r['exact_registration'])
    tag_rows=[f'| {tag} | {n} | {tag_exact[tag]} | {n-tag_exact[tag]} |' for tag,n in tags.items()]
    pages=Counter(r['published_row']['page'] for r in fixed.values())
    page_rows=[f'| {page} | {n} |' for page,n in sorted(pages.items())]
    pairs=load(SOURCE/'SKINS_LABEL_REVIEW_0.15.109.json')['records'];assert len(pairs)==804
    labels_table=['# Проверенные уведомления об обликах — 0.15.109','','Каждая строка — один точный ключ уведомления, а не NPC. Общие сообщения худи не дублируются.','','| Страница | Английское название | Русское название |','| --- | --- | --- |']
    for r in pairs:labels_table.append('| '+r['page']+' | '+r['source_label']+' | '+r['target_label']+' |')
    (SOURCE/'SKINS_LABELS_0.15.109_RU.md').write_text('\n'.join(labels_table)+'\n',encoding='utf-8')
    report=['# Перевод публичной базы — 0.15.109','',f'`{bar}` **{percent:.2f}%** точных записей; полоса округлена.','','| Состояние зафиксированной выгрузки | Строк | Доля |','| --- | ---: | ---: |',f'| С отдельным точным переводом | 12 220 | {percent:.2f}% |',f'| Без отдельной точной записи | 4 024 | {rest:.2f}% |','| Всего | 16 244 | 100% |','','В этой пачке **804 новых уведомления** — почти в восемь раз больше прежних 102 строк. Это уведомления о смене обликов, **не 804 новых NPC**. Рост доли точных ключей с 70,28% до 75,23% не означает такой же процент готовности всей игры или редакторской проверки всех текстов. Остаток содержит также переменные, ответы и нейтральные строки; часть уже переводится шаблонами.','','## Состав очереди','','| Тип источника | Всего | Точных записей | Без точной записи |','| --- | ---: | ---: | ---: |']+tag_rows+['','## Размер и качество пачки','','804 уникальных сообщения с 50 основных страниц. Английские названия и все конкретные русские цели сохранены отдельно. Для каждого составного названия использованы вручную заданные термины и формы рода: «чёрный волк», «чёрная призрачная косатка», «большая белая акула». Контекстные варианты вроде розы/розового оттенка и биомов проверены отдельно. Повторяющаяся структура развёрнута заранее в точные записи: это не автоматический перевод английских слов в игре.','','В атрибуции 52 страницы: 20 общих сообщений худи опубликованы также для пингвина и тюленя. Они посчитаны один раз, а все источники и оформления сохранены. Это не утверждение о переводе всех уведомлений каждой из этих страниц. Имена/варианты из восьми более сложных семейств не включены в эту пачку.','','[Полная таблица 804 названий](SKINS_LABELS_0.15.109_RU.md).','','## Проверки','',f'Заново пройдены **182 группы тестов и 15 проверок разбора базы**. Новый набор содержит **{audit["new_cosmetic_assertions"]:,} утверждений**, 804 основных и 810 опубликованных оформлений. Проверены каждая буква и её цвет, жирность/курсив, текущие команды, подсказки и вставки, разбиение компонентов, время, счётчики, пользовательские замены, настройки и защита подписанного/игрового чата. Неизвестные оформления не угадываются. Это модельная проверка, не гарантия всех живых пакетов.','','Раздельные запуски 108/109 подтверждают: все 804 выбранные строки раньше оставались исходными, теперь дают заданные русские цели в обычном и всех сохранённых цветных оформлениях. Прежние **11 252** проверяемые реплики NPC/боссов сохранили оба результата.','',f'{audit["historical_test_sources_unchanged"]} прежних Java-исходников проверок не переписаны. 32 изолированные копии обновляют только ожидания общего количества записей; из них одна учитывает увеличение прежнего обработчика уведомлений со 148 до 952 ключей. Отдельный снимок 11 редактур из 105 с 24 затронутыми эталонными файлами сохранён, остальные цели и ожидания цветов не заменены.','','## Производительность','','124 из 125 исполняемых классов совпадают с 108 побайтно. В оставшемся отличается только подключение новой таблицы при запуске. Алгоритмы обработки сообщений, кэшей и отрисовки не менялись. Нет нового прохода перевода, покадровых операций или сетевых запросов в игре. Конечные названия уже находятся в ресурсах JAR. Это не нулевая стоимость: таблица больше, занимает память, а перевод новых уведомлений требует работы.','','Контроль нагрузки: четыре отдельных процесса 108→109→109→108, **15 режимов с одинаковыми результатами**, включая прежние уведомления об обликах, сообщения боссов и неизвестные уведомления. Минимум 300 000 прогревочных вызовов и четыре раунда минимум по 200 000 вызовов на режим. Новые 804 перевода не выдаются за равную работу.',f'Диапазон изменения времени выбранных режимов: **{min(changes):+.2f}…{max(changes):+.2f}%**. Режимов с замедлением более 5% в обоих порядках не обнаружено. Все раунды и показатели выделения памяти сохранены.','','**Это не игровой FPS, время кадра или 0,1% low. Нулевое влияние на FPS не гарантируется. Игра и установленная сборка не менялись.**','','## Источники','','52 зафиксированные публичные страницы с ревизиями и полными блоками. Английский источник не переписан. Это не полная выгрузка закрытых серверных скриптов. 345 прежних групп ответов и 135 шаблонов имён сохранены, но не добавлены к проценту точных ключей. 72 ранее исправленные выбранные проблемы оформления не означают проверку всех оформлений игры.','','[Остаток по обозначениям NPC](NPC_ALL_SPEAKERS_0.15.109_RU.md).','',f'SHA-256 JAR: `{digest}`']
    at=next(i for i,text in enumerate(report) if text.startswith('**Это не игровой FPS'))
    report[at:at]=['Удлинённый повтор трёх заранее выбранных режимов: динамические реплики, неизвестные системные уведомления и прежние уведомления об обликах. В каждом — минимум 1 000 000 прогревочных вызовов и четыре раунда минимум по 1 000 000. '+f'Диапазон изменения времени: **{min(long_changes):+.2f}…{max(long_changes):+.2f}%**; режимов с замедлением более 5% в обоих порядках не обнаружено. Первоначальное отклонение +12,86% в прямом порядке и +0,89% в обратном не удалено и не выдаётся за изменение FPS.','']
    (SOURCE/'NPC_PROGRESS_0.15.109_RU.md').write_text('\n'.join(report).replace(f'{audit["new_cosmetic_assertions"]:,}',f'{audit["new_cosmetic_assertions"]:,}'.replace(',',' '))+'\n',encoding='utf-8')
    assert not DEST.exists();DEST.mkdir()
    def cp(path,rel):
        target=DEST/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
    cp(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/resources/data/witherskyru/skins-expansion109.json','src/main/resources/META-INF/licenses/SKINS109-SOURCES.txt','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java'):cp(ROOT/rel,rel)
    for pattern in ('*0.15.109*',):
        for p in SOURCE.glob(pattern):cp(p,'translation-source/'+p.name)
    for p in (ROOT/'tools').glob('*skins109*'):cp(p,'tools/'+p.name)
    for p in (ROOT/'tests').glob('*.java'):cp(p,'tests/'+p.name)
    cp(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    count_dir=ROOT/'build/skins109-test-sources-v3';cp(count_dir/'COUNT_EXPECTATION_UPDATES.json','isolated-test-sources/COUNT_EXPECTATION_UPDATES.json')
    for r in load(count_dir/'COUNT_EXPECTATION_UPDATES.json')['files']:cp(count_dir/r['file'],'isolated-test-sources/'+r['file'])
    supersession=ROOT/'translation-source-regression109-v3';cp(supersession/'EXPECTATION_SUPERSESSION105.json','expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(supersession/'EXPECTATION_SUPERSESSION105.json')['changed_files']:cp(supersession/r['file'],'expectation-supersession/'+r['file'])
    for p in a.checks.glob('*-output.txt'):cp(p,'checks/'+p.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):cp(a.checks/name,'checks/'+name)
    for name,directory in [('audit',a.audit),('benchmark',a.benchmark),('long-repeat',a.long)]:
        for p in directory.rglob('*'):
            if p.is_file() and p.suffix in ('.json','.txt') and not any(s.startswith('isolated') for s in p.relative_to(directory).parts):cp(p,'evidence/'+name+'/'+p.relative_to(directory).as_posix())
    for version in ('0.15.108-dev','0.15.109-dev'):
        cp(a.snapshots/version/'SNAPSHOTS.json','evidence/snapshots/'+version+'/SNAPSHOTS.json')
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):cp(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    (DEST/'README.md').write_text('# WitherSkyRU 0.15.109\n\n804 новых уведомления об обликах, не NPC. 12 220 / 16 244 точных записей публичной очереди, 75,23%; без отдельной записи 4 024. См. `translation-source/NPC_PROGRESS_0.15.109_RU.md` и таблицу 804 названий.\n\nJAR автоматически не установлен. Это частичный пакет изменений и проверок, не самостоятельный проект: нужны прежний проект, зафиксированный корпус и зависимости PrismLauncher. Прежние исходники проверок сохранены, 32 изолированные копии меняют только ожидаемые количества, редактуры 105 обозначены отдельно.\n\nНет новой покадровой обработки. Нагрузка проверена вне игры; игровой FPS и время кадра не измерялись, нулевая стоимость не гарантируется.\n\nHASHES.json содержит SHA-256 всех файлов кроме манифеста. Предыдущие пакеты сохранены.\n',encoding='utf-8')
    manifest={p.relative_to(DEST).as_posix():sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',manifest)
    for rel,h in manifest.items():assert sha(DEST/rel)==h
    archive=DEST.parent/'NPC_TRANSLATION_0.15.109.zip';assert not archive.exists()
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(DEST.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(manifest)+1
        for rel,h in manifest.items():assert hashlib.sha256(z.read(rel)).hexdigest().upper()==h
    target=DEST.parent/jar.name;assert not target.exists();shutil.copy2(jar,target);assert sha(target)==digest
    for rel,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/rel)==h
    result={'jar':str(target),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'progress':progress,'installed_pack_modified':False}
    write(ROOT/'build/SKINS109_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
