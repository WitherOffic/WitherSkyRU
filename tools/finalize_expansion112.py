"""Evidence-gated112 delivery. Percent is exact frozen keys,not live-game completeness."""
import argparse,hashlib,json,math,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,rows,sha,write
from prepare_expansion112 import BASE,load
from audit_expansion112 import PREVIOUS
DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_9')

def performance(path,digest,size):
    d=load(path/'RAW.json');assert d['runtime_jar_sha256']==digest and len(d['summary'])==size
    assert [r['version'] for r in d['records']]==['0.15.111-dev','0.15.112-dev','0.15.112-dev','0.15.111-dev']
    for r in d['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.111-dev' else digest)
    for m in d['summary']:
        values=[r['results'][m['mode']]['useful_visible_outputs'] for r in d['records']];assert m['equal_useful_output'] and values[0]==values[1]==values[2]==values[3]
        assert len(m['orders'])==2 and all(math.isfinite(o['wall_change_percent']) for o in m['orders'])
    slow=[m['mode'] for m in d['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])];assert not slow,('Investigate persistent slowdown,retain all raw',slow)
    return {'scope':'Isolated chat receipt,not FPS/frame time/0.1% low. New useful translations are not equal-work baseline calls.','modes':size,'both_orders_slower_over5':slow,'min_wall_change_percent':min(o['wall_change_percent'] for m in d['summary'] for o in m['orders']),'max_wall_change_percent':max(o['wall_change_percent'] for m in d['summary'] for o in m['orders']),'max_absolute_allocation_delta_bytes':max(abs(o['after_bytes']-o['before_bytes']) for m in d['summary'] for o in m['orders']),'all_raw_retained':True},d

def perf_table(d):
    lines=['| Режим | Изменение времени, порядок 1 | Порядок 2 | Разница выделения памяти, байт/вызов |','| --- | ---: | ---: | ---: |']
    for m in d['summary']:
        a,b=m['orders'];lines.append(f"| {m['mode']} | {a['wall_change_percent']:+.2f}% | {b['wall_change_percent']:+.2f}% | {a['after_bytes']-a['before_bytes']:+.2f} / {b['after_bytes']-b['before_bytes']:+.2f} |")
    return lines

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','audit','benchmark','long'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.112-dev.jar';digest=sha(jar);audit=load(a.audit/'AUDIT.json');assert audit['runtime_jar_sha256']==digest and audit['fresh_test_groups']==185 and audit['all_receipt_and_render_algorithms_unchanged']
    perf,short_raw=performance(a.benchmark,digest,15);long,long_raw=performance(a.long,digest,3);assert {m['mode'] for m in long_raw['summary']}==set(load(SOURCE/'EXPANSION_PERFORMANCE_CONTROL_0.15.112.json')['long_modes'])
    evidence=load(SOURCE/'EXPANSION_SOURCES_0.15.112.json');fixed={r['source']:r for r in evidence['records']};assert len(fixed)==482
    snapshot={r['source']:r for r in load(a.snapshots/'0.15.112-dev/SNAPSHOTS.json')['records']};work=rows(SOURCE/'NPC_WORKLIST_0.15.111.jsonl')
    for r in work:
        s=r['source'];r['exact_literal112']=s in fixed
        if s in fixed:
            assert not r['exact_registration'];v=fixed[s];r['exact_registration']=True;r['review_status']='authored_full_literal_primary_and_published_styles_tested_not_live_certified';r['registered_targets_not_automatically_certified']=[{'resource':'npc-expansion112.json' if v['tag']=='NPC' else 'system-expansion112.json','translation':v['translation']}];r['runtime_flat_snapshot_not_quality_proof']=snapshot[s]['flat'];r['runtime_published_outputs112_not_quality_proof']=snapshot[s]['layouts']
    exact=sum(r['exact_registration'] for r in work);remaining=len(work)-exact;assert (len(work),exact,remaining)==(16244,13744,2500)
    (SOURCE/'NPC_WORKLIST_0.15.112.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8');percent=100*exact/len(work);full,tail=divmod(round(percent*30*8/100),8);bar='█'*full+['','▏','▎','▍','▌','▋','▊','▉'][tail]+'░'*(30-full-int(tail>0))
    progress={'version':'0.15.112-dev','runtime_jar_sha256':digest,'exact_keys':exact,'total_frozen':16244,'exact_percent':percent,'without_exact_key':remaining,'new_exact_keys':482,'new_npc_literals':2,'new_system_literals':480,'new_cosmetic_notifications':295,'new_other_messages':187,'new_archival_system_literals':5,'static_choice_groups_unchanged':345,'prior_name_templates103':135,'fresh_test_groups':185,'parser_tests':15,'new_assertions':audit['new_assertions'],'primary_cases':482,'published_layouts':507,'pinned_pages':85,'historical_test_sources_unchanged':audit['historical_test_sources_unchanged'],'isolated_expectation_files':36,'fixed_pet_literal_negative_upgrades':2,'byte_identical_runtime_classes':124,'startup_method_edits_only':audit['method_differences'],'hot_methods_unchanged':True,'performance_not_fps':perf,'long_performance_not_fps':long,'installed_pack_modified':False,'game_fps_measured':False};write(SOURCE/'NPC_PROGRESS_0.15.112.json',progress)
    speakers=defaultdict(Counter);tags=defaultdict(Counter)
    for r in work:
        for c in (speakers[r['speaker'] or '(без говорящего)'],tags[r['tag'] or 'SYSTEM / без тега']):c['total']+=1;c['exact']+=int(r['exact_registration']);c['new']+=int(r['exact_literal112'])
    table=['# Остаток по обозначениям персонажей — 0.15.112','','Это доля отдельных точных ключей выгруженной очереди. Шаблоны с именами игроков учитываются отдельно, не подменяют точные ключи.','','| Обозначение | Всего | Точных | Доля | Новых ключей | Без точной записи |','| --- | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(speakers.items(),key=lambda x:x[0].casefold()):table.append(f"| {name.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['new']} | {c['total']-c['exact']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.112_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    table=['# 482 новых полных сообщения — 0.15.112','','295 конкретных уведомлений об обликах, 185 других уведомлений и 2 реплики укротителя пауков. Не 482 NPC. Названия обликов и цветовые сочетания раскрыты заранее, только для зафиксированных строк.','','| № | Тип | Облик | Архивный заголовок | Исходник | Перевод | Цветных фрагментов |','| ---: | --- | --- | --- | --- | --- | ---: |']
    for n,r in enumerate(fixed.values(),1):table.append(f"| {n} | {r['tag']} | {'да' if r['cosmetic'] else 'нет'} | {'да' if r['archival'] else 'нет'} | {r['source'].replace('|','&#124;')} | {r['translation'].replace('|','&#124;')} | {len(r['style_anchors'])} |")
    (SOURCE/'EXPANSION_TRANSLATIONS_0.15.112_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    report=['# Перевод публичной базы — 0.15.112','',f'`{bar}` **{percent:.2f}%** точных записей. Полоса округлена.','','| Состояние зафиксированной очереди | Строк | Доля |','| --- | ---: | ---: |',f'| С отдельной точной записью | 13 744 | {percent:.2f}% |',f'| Без отдельной точной записи | 2 500 | {100-percent:.2f}% |','| Всего | 16 244 | 100% |','','Добавлено **482 полных сообщения: 2 реплики NPC и 480 уведомлений**. Из уведомлений 295 относятся к конкретным обликам; ещё 185 — к механикам, событиям и обучению. Пять сообщений имеют архивный заголовок в источнике; они не выдаются за новую действующую ветку NPC.','','Включены предупреждения и стадии Бакте, направления леденца Падшей звезды, обучение безопасности Бригетты, карнавал, награды за экспорт урожая, контакты Абифона, Разлом и другие игровые состояния. Две реплики укротителя пауков содержат фиксированный уровень питомца, а не подставляемую переменную.','','Облики: спинозавр, кошкодевушки, костёр, очки иссушителя, милая медуза, аксолотль, мороженое и шлем жнеца. Проверены окончания прилагательных, сочетания цветов, имена и названия. Только реальные опубликованные сочетания развёрнуты офлайн в конечные точные записи. Во время игры нет генерации названий, новых грамматических правил или запросов к API.','','[Все 482 исходника и перевода](EXPANSION_TRANSLATIONS_0.15.112_RU.md).','','| Тип источника | Всего | Точных | Без точного ключа |','| --- | ---: | ---: | ---: |']
    for tag,c in sorted(tags.items()):report.append(f"| {tag} | {c['total']} | {c['exact']} | {c['total']-c['exact']} |")
    report+=['','## Проверки качества','',f"Заново пройдены **185 групп проверок и 15 тестов разбора базы** на окончательном JAR. Новый набор: **{audit['new_assertions']:,} утверждений**, 482 основных и 507 опубликованных оформлений. Проверены все буквы перевода и их текущие цвета, действия по нажатию, подсказки, разбиение компонентов, числа, время, счётчики, пользовательские замены и защита подписанного/пользовательского чата.".replace(f"{audit['new_assertions']:,}",f"{audit['new_assertions']:,}".replace(',',' ')),f"Контроль прежних 11 252 реплик: все обычные и цветные результаты совпадают с 111. {audit['historical_test_sources_unchanged']} прежних Java-исходников не изменены. В 36 изолированных файлах сохранены ранее обозначенные правки ожиданий 105/111 и обновлены количества таблиц; в одной копии отдельно разрешены две точные фразы с фиксированным [Lvl 1] Tarantula. Остальные отрицательные случаи и все прежние положительные переводы/цветовые эталоны сохранены.",'','Источники — 85 зафиксированных публичных страниц с ревизиями и полными блоками, не закрытые серверные сценарии. Среди оставшихся строк есть обозначения переменных, нейтральные строки, фрагменты многострочных публикаций и подписи таблиц. Они не регистрируются вслепую ради процента. Уже работающие общие правила и 135 шаблонов с именем игрока могут покрывать часть остатка, но не прибавлены к точным ключам.','', '## Производительность','','124 из 125 исполняемых классов совпадают с 111 побайтно. В оставшемся изменены только два метода подключения таблиц при запуске. Обработка входящих сообщений, кэши и отрисовка не менялись. Нет новой покадровой работы, новых сетевых запросов или дополнительного прохода переводчиков. Таблицы больше и занимают память; полезные новые переводы не имеют нулевой стоимости.','','Сравнение 111→112→112→111: четыре отдельных процесса, 15 равных по видимому результату режимов. Минимум 300 000 прогревочных вызовов и четыре раунда минимум по 200 000. Все результаты сохранены:','']+perf_table(short_raw)+['','Удлинённый контроль трёх заранее выбранных режимов: минимум 1 000 000 прогревочных вызовов и четыре раунда минимум по 1 000 000.','']+perf_table(long_raw)+['','В этих сериях нет режима с замедлением более 5% в обоих порядках. Это не доказательство отсутствия меньшей регрессии или накладных расходов. Изменения выделения памяти показаны явно; память таблиц при запуске здесь не измерялась.','','**Замеры вне игры не являются игровым FPS, временем кадра или 0,1% low. Нулевое влияние на FPS не гарантируется. Игра и установленная сборка не изменялись.**','','## Как читать процент','',f'{percent:.2f}% — доля отдельных точных ключей в зафиксированной очереди, не процент идеального перевода всей игры. 345 групп ответов и 135 шаблонов имён сохранены отдельно и не прибавлены к числителю. Не все живые варианты текстов и оформлений доступны в публичной базе.','','[Остаток по обозначениям NPC](NPC_ALL_SPEAKERS_0.15.112_RU.md).','',f'SHA-256 JAR: `{digest}`']
    at=report.index('## Производительность');report[at:at]=['Перед выдачей исправлены три формулировки нового черновика: мука и нарост согласованы с прежними названиями предметов, а вариант очков описан как облик, а не новый вид существа. Черновой JAR и его проверки сохранены отдельно; для окончательного JAR весь набор выполнен заново. Это не изменение прежних переводов или положительных эталонов.','']
    (SOURCE/'NPC_PROGRESS_0.15.112_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
    assert not DEST.exists();archive=DEST.parent/'NPC_TRANSLATION_0.15.112.zip';target=DEST.parent/jar.name;assert not archive.exists() and not target.exists();DEST.mkdir()
    def cp(p,rel):q=DEST/rel;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
    cp(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java','src/main/resources/data/witherskyru/npc-expansion112.json','src/main/resources/data/witherskyru/system-expansion112.json','src/main/resources/META-INF/licenses/EXPANSION112-SOURCES.txt'):cp(ROOT/rel,rel)
    for p in SOURCE.glob('*0.15.112*'):cp(p,'translation-source/'+p.name)
    for p in (ROOT/'tools').glob('*expansion112*'):cp(p,'tools/'+p.name)
    for p in (ROOT/'tests').glob('*.java'):cp(p,'tests/'+p.name)
    cp(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    copies=ROOT/'build/expansion112-test-sources';cp(copies/'EXPECTATION_UPDATES.json','isolated-test-sources/EXPECTATION_UPDATES.json')
    for r in load(copies/'EXPECTATION_UPDATES.json')['files']:cp(copies/r['file'],'isolated-test-sources/'+r['file'])
    sup=ROOT/'translation-source-regression112-v2';cp(sup/'EXPECTATION_SUPERSESSION105.json','expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(sup/'EXPECTATION_SUPERSESSION105.json')['changed_files']:cp(sup/r['file'],'expectation-supersession/'+r['file'])
    for name in ('EXPANSION_COVERAGE_ADDITIONS_0.15.111.json','EXPANSION_COVERAGE_SUPERSESSION_0.15.111.json'):cp(SOURCE/name,'prior-coverage-notices/'+name)
    for p in a.checks.glob('*-output.txt'):cp(p,'checks/'+p.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):cp(a.checks/name,'checks/'+name)
    for name,folder in [('audit',a.audit),('benchmark',a.benchmark),('long-control',a.long)]:
        for p in folder.rglob('*'):
            if p.is_file() and p.suffix in ('.json','.txt') and not any(s.startswith('isolated') for s in p.relative_to(folder).parts):cp(p,'evidence/'+name+'/'+p.relative_to(folder).as_posix())
    for version in ('0.15.111-dev','0.15.112-dev'):
        cp(a.snapshots/version/'SNAPSHOTS.json','evidence/snapshots/'+version+'/SNAPSHOTS.json')
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):cp(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    cp(a.dispatch/'RUNTIME_SCOPE.json','evidence/dispatch/RUNTIME_SCOPE.json')
    cp(ROOT/'build/expansion112-snapshots-3bb29a277d934369a43fae926d5b0301/0.15.111-dev/SNAPSHOTS.json','evidence/initial-probe111/SNAPSHOTS.json')
    editorial=load(SOURCE/'EXPANSION_EDITORIAL_SUPERSESSION_0.15.112.json')
    for p in (ROOT/editorial['retained_preliminary_sources']).iterdir():
        if p.is_file():cp(p,'evidence/preliminary-editorial112/retained/'+p.name+('.superseded-disabled' if p.suffix=='.jar' else ''))
    for p in (ROOT/editorial['preliminary_checks']).glob('*'):
        if p.is_file() and p.suffix in ('.json','.txt'):cp(p,'evidence/preliminary-editorial112/checks/'+p.name)
    (DEST/'README.md').write_text('# WitherSkyRU 0.15.112\n\n482 новых сообщения: 2 реплики NPC и 480 уведомлений, включая 295 уведомлений об обликах. Полная таблица и отчёт находятся в translation-source.\n\nЭто частичный пакет изменений и доказательств проверок, не самостоятельный исходный проект: нужны прежний проект, зафиксированная база и зависимости PrismLauncher. HASHES.json содержит хеши всех файлов кроме самого манифеста. Прежний пакет сохранён.\n\nОригинальные прежние Java-тесты и положительные эталоны не переписаны. Обозначенные изменения ожиданий 105/111 сохранены; две прежние отрицательные проверки для фиксированного уровня тарантула явно обновлены в изолированной копии.\n\nJAR автоматически не установлен. Новая покадровая работа не добавлена. Замеры обработки сообщений вне игры не являются FPS/временем кадра/0,1% low; нулевая стоимость не гарантируется.\n',encoding='utf-8')
    manifest={p.relative_to(DEST).as_posix():sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',manifest)
    for rel,h in manifest.items():assert sha(DEST/rel)==h
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(DEST.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(manifest)+1
        for rel,h in manifest.items():assert hashlib.sha256(z.read(rel)).hexdigest().upper()==h
        assert z.read('HASHES.json')==(DEST/'HASHES.json').read_bytes()
    shutil.copy2(jar,target);assert sha(target)==digest
    for rel,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/rel)==h
    result={'jar':str(target),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'progress':progress,'installed_pack_modified':False};write(ROOT/'build/EXPANSION112_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
