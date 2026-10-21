"""Evidence-gated108 package; archive-aware, fixed-denominator progress."""
import argparse,hashlib,json,math,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,sha,rows,write
from audit_boss108 import BASE,PREVIOUS
DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_5')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','audit','benchmark'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.108-dev.jar';digest=sha(jar);audit=load(a.audit/'AUDIT.json')
    assert audit['runtime_jar_sha256']==digest and audit['all_receipt_and_render_algorithms_unchanged'] and audit['fresh_test_groups']==181
    bench=load(a.benchmark/'RAW.json');assert bench['runtime_jar_sha256']==digest and len(bench['summary'])==14
    assert [r['version'] for r in bench['records']]==['0.15.107-dev','0.15.108-dev','0.15.108-dev','0.15.107-dev']
    for r in bench['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.107-dev' else digest)
    for mode in bench['summary']:
        assert mode['equal_useful_output'] and len(mode['orders'])==2
        outputs=[r['results'][mode['mode']]['useful_visible_outputs'] for r in bench['records']];assert outputs[0]==outputs[1]==outputs[2]==outputs[3]
        assert all(math.isfinite(o['wall_change_percent']) for o in mode['orders'])
    changes=[o['wall_change_percent'] for m in bench['summary'] for o in m['orders']];slow=[m['mode'] for m in bench['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])]
    assert not slow,('Investigate before packaging;retain every raw round',slow)
    performance={'scope':'Isolated equal-output chat receipt,not game FPS/frame times/0.1% lows. New102 useful translations verified separately.','equal_modes':14,'order':['107','108','108','107'],'min_wall_change_percent':min(changes),'max_wall_change_percent':max(changes),'both_orders_slower_over5':slow}
    evidence=load(SOURCE/'BOSS_SOURCES_0.15.108.json');fixed={r['source']:r for r in evidence['records']};assert len(fixed)==102
    snapshot={r['source']:r for r in load(a.snapshots/'0.15.108-dev/SNAPSHOTS.json')['records']};work=rows(SOURCE/'NPC_WORKLIST_0.15.107.jsonl')
    for r in work:
        s=r['source'];r['exact_boss_translation108']=s in fixed;r['archival_dante_translation108']=s in fixed and fixed[s]['archive_branch']
        if s in fixed:
            assert not r['exact_registration'] and r['tag']=='BOSS';r['exact_registration']=True
            r['review_status']='authored_archival_dante_primary_and_publication_layout_tested_not_live_certified' if fixed[s]['archive_branch'] else 'authored_boss_primary_and_publication_layout_tested_not_live_certified'
            r['registered_targets_not_automatically_certified']=[{'resource':'boss-expansion108.json','translation':fixed[s]['translation']}]
            r['runtime_flat_snapshot_not_quality_proof']=snapshot[s]['flat'];r['runtime_published_outputs108_not_quality_proof']=snapshot[s]['layouts']
    exact=sum(r['exact_registration'] for r in work);total=len(work);remaining=total-exact;percent=100*exact/total
    assert (total,exact,remaining)==(16244,11416,4828) and sum(r['exact_boss_translation108'] for r in work)==102 and sum(r['archival_dante_translation108'] for r in work)==61
    (SOURCE/'NPC_WORKLIST_0.15.108.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8')
    boss_remaining=[r for r in work if r['tag']=='BOSS' and not r['exact_registration']];assert len(boss_remaining)==38
    write(SOURCE/'BOSS_REMAINING_0.15.108.json',{'records':boss_remaining,'scope':'38 frozen keys without separate exact registration. Includes placeholders and language-neutral ellipsis;not38 live English lines.'})
    remaining_table=['# Остаток BOSS — 0.15.108','','38 ключей без отдельной точной записи. Заглушки источника не должны попадать в мод как буквальные сообщения. Это не список из 38 гарантированно английских живых реплик.','','| Источник | Причина отдельной проверки | Исходная строка |','| --- | --- | --- |']
    for r in boss_remaining:
        reason='Нейтральное многоточие, перевод не требуется' if r['source']=='[BOSS] Goldor: ...' else 'Переменное имя босса' if r['speaker']=='[name]' else 'Редакторская заглушка или неполный фрагмент' if r['review_status'].startswith('editorial_') or '<modifier line>' in r['source'] else 'Переменный игрок или структурная подстановка'
        remaining_table.append('| '+r['speaker']+' | '+reason+' | '+r['source'].replace('|','&#124;').replace('<','&lt;').replace('>','&gt;')+' |')
    (SOURCE/'BOSS_REMAINING_0.15.108_RU.md').write_text('\n'.join(remaining_table)+'\n',encoding='utf-8')
    progress={'version':'0.15.108-dev','runtime_jar_sha256':digest,'exact_keys':exact,'total_frozen':total,'exact_percent':percent,'without_exact_key':remaining,'without_exact_percent':100-percent,'new_exact_keys':102,'new_boss_literals':102,'new_non_dante_boss_literals':41,'new_archival_dante_literals':61,'new_npc_tagged_keys':0,'static_choice_groups_unchanged':345,'prior_name_templates103':135,'selected_style_repaired_cumulative105':72,'selected_style_gaps_remaining':0,'fresh_test_groups':181,'parser_tests':15,'new_boss_assertions':audit['new_boss_assertions'],'primary_cases':102,'published_layouts':102,'byte_identical_runtime_classes':124,'startup_method_edits_only':audit['method_differences'],'hot_method_instructions_unchanged':True,'historical_test_sources_unchanged':audit['historical_test_sources_unchanged'],'isolated_count_assertion_updates':31,'pinned_pages':4,'performance_not_fps':performance,'installed_pack_modified':False,'game_fps_measured':False}
    write(SOURCE/'NPC_PROGRESS_0.15.108.json',progress)
    by=defaultdict(Counter)
    for r in work:
        c=by[r['speaker'] or '(без говорящего)'];c['total']+=1;c['exact']+=int(r['exact_registration']);c['boss108']+=int(r['exact_boss_translation108']);c['archive108']+=int(r['archival_dante_translation108'])
    table=['# Публичная очередь — 0.15.108','','Доля точных ключей не равна готовности/качеству всей игры. Архивный Данте выделен отдельно.','','| Обозначение | Строк | Точных переводов | Доля точных | Новых108 | Из них архив Данте | Без точной записи |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(by.items(),key=lambda x:x[0].casefold()):table.append(f"| {name.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['boss108']} | {c['archive108']} | {c['total']-c['exact']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.108_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    tags=Counter(r['tag'] or 'SYSTEM/UNTAGGED' for r in work);tag_exact=Counter(r['tag'] or 'SYSTEM/UNTAGGED' for r in work if r['exact_registration']);bar='█'*round(percent*30/100)+'░'*(30-round(percent*30/100))
    report=['# Перевод публичной базы — 0.15.108','',f'`{bar}` **{percent:.2f}%** точных записей; полоса округлена.','','| Состояние зафиксированной выгрузки | Строк | Доля |','| --- | ---: | ---: |',f'| С отдельным точным переводом | 11 416 | {percent:.2f}% |',f'| Без отдельной точной записи | 4 828 | {100-percent:.2f}% |','| Всего | 16 244 | 100% |','','Рост с 69,65% до 70,28% — **102 новые реплики боссов**, в том числе **61 архивная реплика Данте**. Это не процент всей игры или качества всех строк. Публичная очередь содержит также переменные, варианты выбора и нейтральный текст; часть остатка уже переводится шаблонами. 4 828 — не количество гарантированно английских живых реплик.','','## Состав очереди','','| Тип источника | Всего | Точных записей | Без точной записи |','| --- | ---: | ---: | ---: |']+[f'| {tag} | {n} | {tag_exact[tag]} | {n-tag_exact[tag]} |' for tag,n in tags.items()]+['','## Добавлено','','| Персонаж | Новых реплик | Статус источника |','| --- | ---: | --- |','| Арахна | 34 | Зафиксированная публичная страница |','| Бал | 4 | Зафиксированная публичная страница |','| Ливид | 3 | Зафиксированная публичная страница |','| Данте | 61 | Архивная событийная ветка, не подтверждение доступности сейчас |','','Исходные имена и префиксы сообщений не подменяются; переводится содержимое реплик. Сохранены эмоции, прописные буквы, числа и жирное оформление Данте. Фрагменты с редакторскими заглушками, переменными именами и повреждённой разметкой не включены. 345 прежних групп ответов и 135 шаблонов имён сохранены, но не добавлены к проценту точных записей. Ранее исправленные 72 выбранные проблемы оформления не означают проверку всех оформлений игры.','','## Проверки','',f'На итоговом JAR заново пройдены **181 группа тестов и 15 проверок разбора базы**. Новый набор: **{audit["new_boss_assertions"]:,} утверждения**, 102 основных и 102 опубликованных оформления. Проверены каждая буква/цвет, жирность, числа, время, разбиение компонентов, команды/подсказки/вставки, пользовательские замены, настройки и защищённый чат. Счётчики повтора, которые прежний обработчик боссов не принимал, по-прежнему не переводятся — контракт не расширен ради теста.','','Раздельные запуски 107 и 108 подтвердили: все 102 выбранные строки раньше оставались исходными, теперь дают независимо заданные русские цели в обоих оформлениях. В скрининге 11 252 строк изменились ровно эти 102, остальные **11 150** сохранили прежний результат. Новые архивные строки явно обозначены даже там, где заголовок выгрузки не помечен историческим.','',f'{audit["historical_test_sources_unchanged"]} прежних Java-исходников проверок не переписаны. В 31 изолированной копии изменены только ожидания количества: 29 прежних проверок размера NPC-набора и две проверки размера обработчика боссов. Снимок 11 редактур из 105 с 24 затронутыми эталонными файлами сохранён отдельно; остальные тексты и ожидания оформления не заменялись.','','## Производительность','','124 из 125 исполняемых классов совпадают с 107 побайтно. В оставшемся изменилось только подключение новой таблицы при запуске. Инструкции обработки сообщений, кэшей и отрисовки не менялись. Нет нового прохода по сообщениям, операций каждый кадр или сетевых запросов в игре. Дополнительные записи всё же занимают память, а перевод новых сообщений имеет стоимость.','','Контроль нагрузки: четыре отдельных процесса 107→108→108→107, **14 режимов с одинаковыми результатами**, включая прежние реплики боссов и неизвестные сообщения. На режим — минимум 300 000 прогревочных вызовов и четыре раунда минимум по 200 000. Новые переводы проверены отдельно и не выдаются за равную работу.',f'Диапазон изменения времени выбранных режимов: **{min(changes):+.2f}…{max(changes):+.2f}%**. Режимов с замедлением более 5% в обоих порядках не обнаружено. Все раунды и показатели выделения памяти сохранены.','','**Это не игровой FPS, время кадра или 0,1% low. Нулевое влияние на FPS не гарантируется. Игра и установленная сборка не менялись.**','','## Источники','','Четыре зафиксированные публичные страницы с ревизиями и полными блоками. Английский источник сохранён. Это не полный набор закрытых серверных скриптов.','','[Остаток по обозначениям NPC](NPC_ALL_SPEAKERS_0.15.108_RU.md).','',f'SHA-256 JAR: `{digest}`']
    (SOURCE/'NPC_PROGRESS_0.15.108_RU.md').write_text('\n'.join(report).replace(f'{audit["new_boss_assertions"]:,}',f'{audit["new_boss_assertions"]:,}'.replace(',',' '))+'\n',encoding='utf-8')
    assert not DEST.exists();DEST.mkdir()
    def cp(path,rel):
        target=DEST/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
    cp(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/resources/data/witherskyru/boss-expansion108.json','src/main/resources/META-INF/licenses/BOSS108-SOURCES.txt','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java'):cp(ROOT/rel,rel)
    for p in SOURCE.glob('*0.15.108*'):cp(p,'translation-source/'+p.name)
    for p in (ROOT/'tools').glob('*boss108*'):cp(p,'tools/'+p.name)
    for p in (ROOT/'tests').glob('*.java'):cp(p,'tests/'+p.name)
    cp(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    counts=ROOT/'build/boss108-test-sources';cp(counts/'COUNT_EXPECTATION_UPDATES.json','isolated-test-sources/COUNT_EXPECTATION_UPDATES.json')
    for r in load(counts/'COUNT_EXPECTATION_UPDATES.json')['files']:cp(counts/r['file'],'isolated-test-sources/'+r['file'])
    sup=ROOT/'translation-source-regression108';cp(sup/'EXPECTATION_SUPERSESSION105.json','expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(sup/'EXPECTATION_SUPERSESSION105.json')['changed_files']:cp(sup/r['file'],'expectation-supersession/'+r['file'])
    for p in a.checks.glob('*-output.txt'):cp(p,'checks/'+p.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):cp(a.checks/name,'checks/'+name)
    for name,directory in [('audit',a.audit),('benchmark',a.benchmark)]:
        for p in directory.rglob('*'):
            if p.is_file() and p.suffix in ('.json','.txt') and not any(s.startswith('isolated') for s in p.relative_to(directory).parts):cp(p,'evidence/'+name+'/'+p.relative_to(directory).as_posix())
    for version in ('0.15.107-dev','0.15.108-dev'):
        cp(a.snapshots/version/'SNAPSHOTS.json','evidence/snapshots/'+version+'/SNAPSHOTS.json')
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):cp(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    (DEST/'README.md').write_text('# WitherSkyRU 0.15.108\n\n102 новые реплики боссов: 41 Арахны/Бала/Ливида и 61 архивная Данте. Прогресс: 11 416 / 16 244 точных записей публичной очереди, 70,28%; без отдельной точной записи 4 828. Подробности: `translation-source/NPC_PROGRESS_0.15.108_RU.md`.\n\nJAR автоматически не установлен. Это частичный пакет изменений/проверок, не самостоятельный проект. Для повторения нужны прежний проект, зафиксированный корпус и зависимости PrismLauncher. Исходники прежних проверок сохранены; 31 изолированная копия обновляет только общие количества записей. Редактуры 105 обозначены отдельно.\n\nНовых операций каждый кадр нет. Нагрузка проверена вне игры, игровой FPS не измерялся; нулевая стоимость не гарантируется.\n\nHASHES.json содержит SHA-256 всех файлов кроме самого манифеста. Предыдущие пакеты сохранены.\n',encoding='utf-8')
    manifest={p.relative_to(DEST).as_posix():sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',manifest)
    for rel,h in manifest.items():assert sha(DEST/rel)==h
    archive=DEST.parent/'NPC_TRANSLATION_0.15.108.zip';assert not archive.exists()
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(DEST.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(manifest)+1
        for rel,h in manifest.items():assert hashlib.sha256(z.read(rel)).hexdigest().upper()==h
    target=DEST.parent/jar.name;assert not target.exists();shutil.copy2(jar,target);assert sha(target)==digest
    for rel,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/rel)==h
    result={'jar':str(target),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'progress':progress,'installed_pack_modified':False}
    write(ROOT/'build/BOSS108_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()
