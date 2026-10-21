"""Evidence-gated110 delivery;stable public-corpus denominator,not all-game certification."""
import argparse,hashlib,json,math,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,sha,rows,write
from audit_game110 import BASE,PREVIOUS

DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_7')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))

def performance(path,digest,size):
    data=load(path/'RAW.json');assert data['runtime_jar_sha256']==digest and len(data['summary'])==size
    assert [r['version'] for r in data['records']]==['0.15.109-dev','0.15.110-dev','0.15.110-dev','0.15.109-dev']
    for r in data['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.109-dev' else digest)
    for mode in data['summary']:
        assert mode['equal_useful_output'] and len(mode['orders'])==2
        outputs=[r['results'][mode['mode']]['useful_visible_outputs'] for r in data['records']];assert outputs[0]==outputs[1]==outputs[2]==outputs[3]
        assert all(math.isfinite(o['wall_change_percent']) for o in mode['orders'])
    changes=[o['wall_change_percent'] for m in data['summary'] for o in m['orders']]
    slow=[m['mode'] for m in data['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])]
    assert not slow,('Investigate persistent change before delivery;do not discard raw runs',slow)
    return {'scope':'Isolated chat receipt,not game FPS/frame times/0.1% low;new540 translations are not treated as equal old work.','modes':size,'order':['109','110','110','109'],'min_wall_change_percent':min(changes),'max_wall_change_percent':max(changes),'max_abs_allocation_delta_bytes':max(abs(o['after_bytes']-o['before_bytes']) for m in data['summary'] for o in m['orders']),'both_orders_slower_over5':slow,'all_raw_retained':True}

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','audit','benchmark','long'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.110-dev.jar';digest=sha(jar)
    audit=load(a.audit/'AUDIT.json');assert audit['runtime_jar_sha256']==digest and audit['all_receipt_and_render_algorithms_unchanged'] and audit['fresh_test_groups']==183
    perf=performance(a.benchmark,digest,15);long=performance(a.long,digest,3)
    assert {m['mode'] for m in load(a.long/'RAW.json')['summary']}==set(load(SOURCE/'GAME_PERFORMANCE_CONTROL_0.15.110.json')['modes'])
    evidence=load(SOURCE/'GAME_SOURCES_0.15.110.json');fixed={r['source']:r for r in evidence['records']};assert len(fixed)==540
    snap=load(a.snapshots/'0.15.110-dev/SNAPSHOTS.json');snapshot={r['source']:r for r in snap['records']}
    work=rows(SOURCE/'NPC_WORKLIST_0.15.109.jsonl')
    for r in work:
        source=r['source'];r['exact_gameplay_notification110']=source in fixed
        if source in fixed:
            assert not r['exact_registration'] and r['tag'] is None;r['exact_registration']=True
            r['review_status']='authored_gameplay_primary_and_published_styles_tested_not_live_certified'
            r['registered_targets_not_automatically_certified']=[{'resource':'game-expansion110.json','translation':fixed[source]['translation']}]
            r['runtime_flat_snapshot_not_quality_proof']=snapshot[source]['flat'];r['runtime_published_outputs110_not_quality_proof']=snapshot[source]['layouts']
    total=len(work);exact=sum(r['exact_registration'] for r in work);remaining=total-exact;assert (total,exact,remaining)==(16244,12760,3484)
    (SOURCE/'NPC_WORKLIST_0.15.110.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8')
    percent=100*exact/total;rest=100-percent;bar='█'*round(percent*30/100)+'░'*(30-round(percent*30/100))
    categories={'phone_messages':sum(s.startswith('✆ ') for s in fixed),'contact_added':sum("has been added to your Abiphone's contacts!" in s for s in fixed),'thorn_crowd':sum(s.startswith('CROWD: ') for s in fixed),'vargul_spoken':sum(s.startswith('[Vargul]: ') for s in fixed)}
    progress={'version':'0.15.110-dev','runtime_jar_sha256':digest,'exact_keys':exact,'total_frozen':total,'exact_percent':percent,'without_exact_key':remaining,'without_exact_percent':rest,'new_exact_keys':540,'new_gameplay_messages':540,'new_npc_tagged_keys':0,'message_subsets_not_additive':categories,'static_choice_groups_unchanged':345,'prior_name_templates103':135,'selected_style_repaired_cumulative105':72,'fresh_test_groups':183,'parser_tests':15,'new_gameplay_assertions':audit['new_gameplay_assertions'],'primary_cases':540,'published_layouts':541,'historical_test_sources_unchanged':audit['historical_test_sources_unchanged'],'isolated_count_assertion_updates':33,'pinned_pages':254,'byte_identical_runtime_classes':124,'startup_method_edits_only':audit['method_differences'],'hot_methods_unchanged':True,'performance_not_fps':perf,'long_performance_not_fps':long,'installed_pack_modified':False,'game_fps_measured':False};write(SOURCE/'NPC_PROGRESS_0.15.110.json',progress)
    by=defaultdict(Counter);tags=defaultdict(Counter)
    for r in work:
        c=by[r['speaker'] or '(без говорящего)'];c['total']+=1;c['exact']+=int(r['exact_registration']);c['new']+=int(r['exact_gameplay_notification110']);c=tags[r['tag'] or 'SYSTEM / без тега'];c['total']+=1;c['exact']+=int(r['exact_registration'])
    speaker=['# Остаток публичной очереди — 0.15.110','','Процент отдельных точных ключей, не оценка полноты всей игры. Системные сообщения, включая зрителей и Варгула, не считаются новыми NPC.','','| Обозначение | Всего | Точных | Доля | Новых ключей110 | Без точной записи |','| --- | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(by.items(),key=lambda x:x[0].casefold()):speaker.append(f"| {name.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['new']} | {c['total']-c['exact']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.110_RU.md').write_text('\n'.join(speaker)+'\n',encoding='utf-8')
    full=['# 540 проверенных сообщений — 0.15.110','','Полные зафиксированные источники, русские цели и количество явных смысловых цветных фрагментов. Один ключ считается один раз. Не утверждение о всех живых вариантах.','','| № | Английский источник | Русский перевод | Цветных фрагментов |','| ---: | --- | --- | ---: |']
    for n,r in enumerate(fixed.values(),1):full.append(f"| {n} | {r['source'].replace('|','&#124;')} | {r['translation'].replace('|','&#124;')} | {len(r['style_anchors'])} |")
    (SOURCE/'GAME_TRANSLATIONS_0.15.110_RU.md').write_text('\n'.join(full)+'\n',encoding='utf-8')
    report=['# Перевод публичной базы — 0.15.110','',f'`{bar}` **{percent:.2f}%** точных записей. Полоса округлена.','','| Состояние зафиксированной очереди | Строк | Доля |','| --- | ---: | ---: |',f'| С отдельным точным переводом | 12 760 | {percent:.2f}% |',f'| Без отдельной точной записи | 3 484 | {rest:.2f}% |','| Всего | 16 244 | 100% |','','**+540 полноценных игровых сообщений**, не 540 новых NPC. В этой пачке нет массового наполнения уведомлениями об обликах. Сюда вошли рыбалка и охота, ограничения предметов и испытаний додзё, событийные объявления, сцены заданий, Абифон, зрители у Торна и Варгул.','',f"Абифон: {categories['phone_messages']} сообщений, включая {categories['contact_added']} добавления контактов; зрители Торна: {categories['thorn_crowd']}; произносимые реплики Варгула: {categories['vargul_spoken']}. Это пересекающиеся подмножества 540 ключей, не дополнительные строки.",'','[Полная таблица исходников и переводов](GAME_TRANSLATIONS_0.15.110_RU.md).','','## Состав очереди','','| Тип источника | Всего | Точных ключей | Без точного ключа |','| --- | ---: | ---: | ---: |']
    report=[line.replace(' В этой пачке нет массового наполнения уведомлениями об обликах.','') for line in report]
    for tag,c in sorted(tags.items()):report.append(f"| {tag} | {c['total']} | {c['exact']} | {c['total']-c['exact']} |")
    report+=['','## Проверка качества','','540 вручную написанных полных русских сообщений с 254 зафиксированных публичных страниц. Сначала 1 161 кандидат проверен старым JAR; в перевод не включены уже изменяемые строки, незавершённые абзацы, переменные и неподтверждённые фразы из другого канала. Включены только выбранные строки, которые раньше полностью оставались исходными. Это не новая полная выгрузка закрытых серверных скриптов.','','Названия существ сверены со словарём; отдельно вычитаны полные инструкции, запреты, состояния до/после действия, числовые условия и каламбуры звонков. Цветные смысловые фрагменты заданы заранее, включая названия, жирные заголовки, кнопки меню, временные значения и оформление скобок Варгула. Эти правила развёрнуты в ресурсы офлайн, не вычисляются по английской грамматике в игре.','',f"Заново пройдены **183 группы проверок и 15 проверок разбора базы**. Новый набор содержит **{audit['new_gameplay_assertions']:,} утверждений** на 540 основных и 541 опубликованном оформлении: текст каждой буквы и её цвет, текущие команды/подсказки/вставки, дробление компонентов, числовые условия, время, счётчики, настройки, пользовательские замены и защита подписанного и пользовательского чата. Неизвестные оформления не угадываются.",'','Раздельные запуски 109 и 110 подтвердили все 540 русских целей во всех сохранённых вариантах. Прежние **11 252** проверяемые реплики NPC и боссов сохранили результаты в обычном и цветном виде.','',f"{audit['historical_test_sources_unchanged']} прежних Java-исходников проверок не изменены. 33 изолированные копии меняют только ожидания количества ключей; старые эталоны текста и цветов не заменены. Ранее обозначенные 11 редактур версии105 и 24 затронутых файла эталонов сохранены отдельно.",'','## Производительность','','124 из 125 исполняемых классов совпадают с109 побайтно. В оставшемся изменено только подключение таблицы при запуске. Обработка сообщений, кэши и отрисовка не изменились. Нового прохода перевода, покадровой работы и сетевых обращений нет. Таблица больше и занимает память; перевод новых сообщений всё равно требует работы.','',f"Сравнение нагрузки: 109→110→110→109 в четырёх отдельных процессах, **15 режимов с одинаковыми видимыми результатами**. Минимум 300 000 прогревочных вызовов и четыре раунда минимум по 200 000 на режим. Диапазон изменения времени: **{perf['min_wall_change_percent']:+.2f}…{perf['max_wall_change_percent']:+.2f}%**.",'',f"Дополнительно заранее выбран удлинённый контроль трёх режимов — динамические реплики, неизвестные системные сообщения, прежние уведомления об обликах. В каждом минимум 1 000 000 прогревочных вызовов и четыре раунда минимум по 1 000 000. Диапазон: **{long['min_wall_change_percent']:+.2f}…{long['max_wall_change_percent']:+.2f}%**. Ни в основном, ни в удлинённом наборе нет режима с замедлением более 5% в обоих порядках. Все раунды и показатели выделения памяти сохранены.",'','**Это не игровой FPS, время кадра или 0,1% low. Нулевое влияние на FPS не гарантируется. Игра и установленная сборка не менялись.**','','## Что означает процент','','78,55% — доля отдельных точных ключей в прежней очереди из16 244 строк, не процент идеального перевода всей игры. В остатке есть переменные, нейтральные строки и ответы, часть которых уже переводится общими правилами. 345 прежних групп ответов и 135 шаблонов имён сохранены, но не прибавлены к числителю. Исправленные ранее 72 выбранные проблемы оформления не означают проверку всех оформлений игры.','','[Остаток по обозначениям персонажей](NPC_ALL_SPEAKERS_0.15.110_RU.md).','',f'SHA-256 JAR: `{digest}`']
    at=next(i for i,line in enumerate(report) if line.startswith('**Это не игровой FPS'))
    report[at:at]=[f"Максимальное абсолютное различие выделения памяти на вызов в контрольных режимах: {perf['max_abs_allocation_delta_bytes']:.2f} байта в основном наборе и {long['max_abs_allocation_delta_bytes']:.2f} байта в удлинённом. Это не оценка памяти новой таблицы при запуске или всей игры.",'']
    (SOURCE/'NPC_PROGRESS_0.15.110_RU.md').write_text('\n'.join(report).replace(f"{audit['new_gameplay_assertions']:,}",f"{audit['new_gameplay_assertions']:,}".replace(',',' '))+'\n',encoding='utf-8')
    assert not DEST.exists();DEST.mkdir()
    def cp(path,rel):
        target=DEST/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
    cp(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/resources/data/witherskyru/game-expansion110.json','src/main/resources/META-INF/licenses/GAME110-SOURCES.txt','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java'):cp(ROOT/rel,rel)
    for p in SOURCE.glob('*0.15.110*'):cp(p,'translation-source/'+p.name)
    for p in (ROOT/'tools').glob('*game110*'):cp(p,'tools/'+p.name)
    for p in (ROOT/'tests').glob('*.java'):cp(p,'tests/'+p.name)
    cp(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    count=ROOT/'build/game110-test-sources-v2';cp(count/'COUNT_EXPECTATION_UPDATES.json','isolated-test-sources/COUNT_EXPECTATION_UPDATES.json')
    for r in load(count/'COUNT_EXPECTATION_UPDATES.json')['files']:cp(count/r['file'],'isolated-test-sources/'+r['file'])
    supersession=ROOT/'translation-source-regression110-v2';cp(supersession/'EXPECTATION_SUPERSESSION105.json','expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(supersession/'EXPECTATION_SUPERSESSION105.json')['changed_files']:cp(supersession/r['file'],'expectation-supersession/'+r['file'])
    for p in a.checks.glob('*-output.txt'):cp(p,'checks/'+p.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):cp(a.checks/name,'checks/'+name)
    for name,directory in [('audit',a.audit),('benchmark',a.benchmark),('long-control',a.long)]:
        for p in directory.rglob('*'):
            if p.is_file() and p.suffix in ('.json','.txt') and not any(s.startswith('isolated') for s in p.relative_to(directory).parts):cp(p,'evidence/'+name+'/'+p.relative_to(directory).as_posix())
    # Keep the pre-authorship probe too;never replace its109 result with the new selected output.
    initial=ROOT/'build/game110-snapshots-d2025d44a02445f08fb60087a919ce62/0.15.109-dev/SNAPSHOTS.json';cp(initial,'evidence/initial-probe109/SNAPSHOTS.json')
    for version in ('0.15.109-dev','0.15.110-dev'):
        cp(a.snapshots/version/'SNAPSHOTS.json','evidence/snapshots/'+version+'/SNAPSHOTS.json')
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):cp(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    superseded=load(SOURCE/'GAME_SUPERSESSION_0.15.110.json')
    for p in (ROOT/superseded['retained_candidate']).iterdir():
        if p.is_file():cp(p,'evidence/superseded-not-final/candidate/'+p.name)
    assert sha(ROOT/superseded['retained_candidate']/jar.name)==superseded['superseded_jar_sha256']
    for key,folder in [('retained_checks','checks'),('retained_audit','audit'),('interrupted_benchmark_not_final_evidence','interrupted-benchmark')]:
        for p in (ROOT/superseded[key]).glob('*'):
            if p.is_file() and p.suffix in ('.json','.txt'):cp(p,'evidence/superseded-not-final/'+folder+'/'+p.name)
    for version in ('0.15.109-dev','0.15.110-dev'):
        cp(ROOT/superseded['retained_snapshots']/version/'SNAPSHOTS.json','evidence/superseded-not-final/snapshots/'+version+'/SNAPSHOTS.json')
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):cp(ROOT/superseded['retained_dispatch']/version/name,'evidence/superseded-not-final/dispatch/'+version+'/'+name)
    (DEST/'README.md').write_text('# WitherSkyRU 0.15.110\n\n540 новых полных игровых сообщений. 12 760 / 16 244 точных ключей публичной очереди:78,55%; без отдельного ключа3484. См. таблицу исходников/переводов и отчёт в translation-source. Это не540 новых NPC и не заявление о полной готовности игры.\n\nJAR автоматически не установлен. Это частичный пакет изменений и проверок, не самостоятельный проект: нужны прежний проект, замороженный корпус и зависимости PrismLauncher. Прежние исходники тестов сохранены;33 изолированные копии меняют только ожидания количества, редактуры105 отмечены отдельно.\n\nНет новой покадровой обработки. Нагрузка проверена вне игры: FPS, время кадра и0,1% low не измерялись, нулевое влияние не гарантируется.\n\nHASHES.json содержит SHA-256 всех файлов кроме самого манифеста. Предыдущие пакеты сохранены.\n',encoding='utf-8')
    manifest={p.relative_to(DEST).as_posix():sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',manifest)
    for rel,h in manifest.items():assert sha(DEST/rel)==h
    archive=DEST.parent/'NPC_TRANSLATION_0.15.110.zip';assert not archive.exists()
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(DEST.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(manifest)+1
        for rel,h in manifest.items():assert hashlib.sha256(z.read(rel)).hexdigest().upper()==h
        assert z.read('HASHES.json')==(DEST/'HASHES.json').read_bytes()
    target=DEST.parent/jar.name;assert not target.exists();shutil.copy2(jar,target);assert sha(target)==digest
    for rel,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/rel)==h
    result={'jar':str(target),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'progress':progress,'installed_pack_modified':False};write(ROOT/'build/GAME110_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()
