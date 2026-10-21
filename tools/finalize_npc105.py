"""Evidence-gated wording release; preserve exact-key denominator and historical artifacts."""
import argparse,hashlib,json,math,re,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,sha,rows,write
from audit_npc105 import BASE,PREVIOUS

DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_2')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','audit','benchmark'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.105-dev.jar';digest=sha(jar)
    audit=load(a.audit/'AUDIT.json');assert audit['runtime_jar_sha256']==digest and audit['all_125_runtime_classes_byte_identical']
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==digest and len(checks['tests_passed'])==177
    bench=load(a.benchmark/'RAW.json');assert bench['runtime_jar_sha256']==digest
    assert [r['version'] for r in bench['records']]==['0.15.104-dev','0.15.105-dev','0.15.105-dev','0.15.104-dev']
    assert len(bench['summary'])==10
    for r in bench['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.104-dev' else digest)
    for mode in bench['summary']:
        assert mode['equal_useful_output'] and len(mode['orders'])==2
        outputs=[r['results'][mode['mode']]['useful_visible_outputs'] for r in bench['records']]
        assert outputs[0]==outputs[1]==outputs[2]==outputs[3],mode['mode']
        assert all(math.isfinite(o['wall_change_percent']) for o in mode['orders'])
    changes=[o['wall_change_percent'] for m in bench['summary'] for o in m['orders']]
    performance={'scope':'Isolated equal-output receipt benchmark;not FPS/frametime/0.1% low. Revised eleven outputs are not presented as equal work.','equal_modes':10,'four_jvm_order':['104','105','105','104'],'min_wall_change_percent':min(changes),'max_wall_change_percent':max(changes),'both_orders_slower_over5':[m['mode'] for m in bench['summary'] if all(o['wall_change_percent']>5 for o in m['orders'])]}
    evidence=load(SOURCE/'NPC_WORDING_SOURCES_0.15.105.json');fixed={r['source']:r for r in evidence['records']};assert len(fixed)==11
    newrows={r['source']:r for r in rows(a.dispatch/'0.15.105-dev/DISPATCH_ROWS.jsonl')}
    work=rows(SOURCE/'NPC_WORKLIST_0.15.104.jsonl')
    for r in work:
        r['wording_revision105']=r['source'] in fixed
        if r['source'] in fixed:
            r.pop('wording_review104',None);r['wording_review105']='explicit_revised_target_primary_and_published_layout_verified'
            r['runtime_colored_snapshot_not_quality_proof']=newrows[r['source']]['published_style_result']
            r['runtime_flat_snapshot105_not_quality_proof']=newrows[r['source']]['flat_result']
    assert len(work)==16244 and sum(r['exact_registration'] for r in work)==11092 and sum(r['wording_revision105'] for r in work)==11
    (SOURCE/'NPC_WORKLIST_0.15.105.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8')
    counts=Counter(r['queue_row']['speaker'] for r in fixed.values())
    progress={'version':'0.15.105-dev','runtime_jar_sha256':digest,'exact_keys':11092,'total_frozen':16244,'exact_percent':100*11092/16244,'without_exact_key':5152,'new_exact_keys':0,'prior_name_templates103':135,'wording_revisions':11,'new_style_repairs':5,'selected_style_gap_before103':72,'selected_style_repaired_cumulative105':72,'selected_style_gap_remaining':0,'selected_style_repair_percent':100.0,'selected_latin_name_revisions':6,'fresh_test_groups':177,'parser_tests':15,'new_wording_assertions':25769,'unchanged_runtime_classes':125,'historical_test_sources_unchanged':audit['historical_test_sources_unchanged'],'historical_lookup_test_intentionally_updated':True,'isolated_expectation_supersession_files':audit['isolated_expectation_supersession_files'],'performance_not_fps':performance,'installed_pack_modified':False,'game_fps_or_frame_times_measured':False}
    write(SOURCE/'NPC_PROGRESS_0.15.105.json',progress)
    write(SOURCE/'NPC_WORDING_PENDING_0.15.105.json',{'selected_five_wording_rows_pending':[],'selected_six_latin_name_rows_pending':[],'not_all_game_translation_complete':True,'retained_exceptions':['Two base-provider Tia relay lines do not accept an additional repeat counter.','St. Jerry holiday phrase remains English for required exact server input.','English names in protocol NPC prefixes are retained.'],'remaining_without_exact_key_in_frozen_queue':5152})
    per=defaultdict(Counter)
    for r in work:
        c=per[r['speaker'] or '(без говорящего)'];c['total']+=1;c['exact']+=int(r['exact_registration']);c['names']+=int(r.get('single_name_template103',False));c['wording']+=int(r['wording_revision105'])
    table=['# Публичная очередь NPC — 0.15.105','','Не процент готовности всей игры. Точные записи и шаблоны с ником считаются раздельно.','','| Обозначение | Строк | Точных ключей | Доля точных | Шаблонов с ником103 | Редактур105 | Без точного ключа |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(per.items(),key=lambda p:p[0].casefold()):table.append(f"| {name.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['names']} | {c['wording']} | {c['total']-c['exact']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.105_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    report=['# Перевод NPC — 0.15.105','','Известные проблемы выбранной группы цветного текста:','','`██████████████████████████████` **72 / 72 · 100%** исправлено суммарно в104–105.','','Это не 100% игры и не полная проверка всех живых серверных оформлений. В105 закрыты пять оставшихся случаев; дополнительно исправлены шесть строк с латинскими именами внутри русских реплик.','','Публичная очередь точных ключей:','','`████████████████████░░░░░░░░░░` **11 092 / 16 244 · 68,28%**. Полоса округлена; новых ключей в этом проходе нет. Без отдельного точного ключа — **5 152**. Это регистрация в зафиксированной очереди, не доказательство качества каждой строки. 135 шаблонов с ником из103 сохраняются отдельно.','','## Что изменено','','| Показатель | Результат |','| --- | ---: |','| Отредактировано реплик | 11 |','| Дополнительно исправлено опубликованных цветных оформлений | 5 |','| Оставшихся проблем в выбранной группе из72 | 0 |','| Латинских имён исправлено в выбранных шести репликах | 6 |','| Проверенных входов общего скрининга | 11 252 |','| Других результатов, не изменившихся в обычном и цветном тексте | 11 241 |','','## Новые формулировки','','| NPC | Русская реплика |','| --- | --- |']
    for r in evidence['records']:report.append(f"| {r['queue_row']['speaker']} | {r['translation'].split(': ',1)[1]} |")
    report+=['','Исправлены соответствия выделенных «множество», «особенный», «вам», «безумие», «действительно». Основные английские смысловые интервалы прежнего оформления сохранены, их русские соответствия обновлены. В альтернативном опубликованном оформлении имена и другие выделения проверены отдельно. Служебные NPC-префиксы не переименовывались.','','## Проверки','','На итоговом JAR заново выполнены **177 групп тестов и15 проверок разбора базы**. Новая проверка — **25 769 утверждений**, 11 реплик × два оформления: прежнее основное и опубликованное. Проверены каждая буква, цвета/курсив, текущие команды и подсказки, вставки, числа, время, счётчики повторов, фрагментация, настройки, пользовательские замены, неизменность исходного текста и защита текста игроков.','','Исторические эталонные файлы в `translation-source` не переписаны. Для регрессии создан отдельный снимок с явно перечисленными заменами11 целей/цветовых соответствий. Один прежний сравнительный тест намеренно обновлён: старый JAR60 не служит эталоном старой формулировки для этих11 строк; остальные сравнения сохранены, новые цели проверяются независимыми случаями105. Поэтому это не заявление о неизменности всех старых ожиданий. У старого стенда104 в выводе осталась историческая надпись «old targets intact»; для105 она не является утверждением, актуальный перечень замен — `NPC_WORDING_SOURCES_0.15.105.json`.','','## Производительность','','**Все125 исполняемых классов побайтно совпадают с104.** Изменены только три таблицы перевода, номер версии и атрибуция. Ни обработчик чата, ни HUD, ни отрисовка, ни кэши не менялись. Новых регулярных выражений, дополнительных проходов и обработки каждый кадр не добавлено. Планы и русский текст по-прежнему подготавливаются при загрузке, а текущие стили/команды берутся из полученного сообщения.','','Изолированный повтор: четыре процесса104 →105 →105 →104, десять режимов одинаковой полезной работы; равенство сохранённых выводов проверено. Для каждого режима — 300 000 прогревочных операций и четыре раунда минимум по200 000 вызовов. Изменённые11 результатов не выдаются за одинаковую работу.',f"Разница времени отдельных сопоставимых режимов: **{min(changes):+.2f}…{max(changes):+.2f}%**. Режимы с замедлением более5% в обоих порядках: "+(', '.join(performance['both_orders_slower_over5']) or 'не обнаружены в выбранном замере')+'.','','Это не игровой FPS, не время кадра и не0,1% low. Более длинный русский текст может иметь другую стоимость вывода; нулевое влияние на FPS не гарантируется.','','## Сохранённые границы','','Две реплики Тии из базового провайдера по-прежнему не поддерживают добавочный счётчик повторов. Английская праздничная фраза Сент-Джерри намеренно оставлена для точного ввода на сервере. Закрытые серверные скрипты и все возможные динамические реплики не объявляются полностью выгруженными или переведёнными.','','JAR подготовлен отдельно. Установленная сборка и игра не изменены.','',f'SHA-256 JAR: `{digest}`']
    (SOURCE/'NPC_PROGRESS_0.15.105_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
    assert not DEST.exists();DEST.mkdir()
    def cp(path,rel):
        target=DEST/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
    cp(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/resources/data/witherskyru/story-dialogues.json','src/main/resources/data/witherskyru/npc-expansion74.json','src/main/resources/data/witherskyru/npc-styles104.json','src/main/resources/META-INF/licenses/NPC105-SOURCES.txt','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java','src/main/java/ru/wither/skyru/ChatNotificationTranslator.java','src/main/java/ru/wither/skyru/NamedNpcTemplates.java'):cp(ROOT/rel,rel)
    for path in SOURCE.glob('*0.15.105*'):cp(path,'translation-source/'+path.name)
    cp(SOURCE/'NPC_STYLE_SOURCES_0.15.104.json','translation-source/NPC_STYLE_SOURCES_0.15.104.json')
    cp(SOURCE/'NPC_STYLES_0.15.104_REVIEWED.tsv','translation-source/NPC_STYLES_0.15.104_REVIEWED.tsv')
    for path in (ROOT/'tools').glob('*npc105*'):cp(path,'tools/'+path.name)
    for path in (ROOT/'tests').glob('*.java'):cp(path,'tests/'+path.name)
    cp(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    for path in a.checks.glob('*-output.txt'):cp(path,'checks/'+path.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):cp(a.checks/name,'checks/'+name)
    supersession=load(ROOT/'translation-source-regression105/EXPECTATION_SUPERSESSION105.json')
    cp(ROOT/'translation-source-regression105/EXPECTATION_SUPERSESSION105.json','expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for item in supersession['changed_files']:cp(ROOT/'translation-source-regression105'/item['file'],'expectation-supersession/'+item['file'])
    for name,directory in [('audit',a.audit),('benchmark',a.benchmark)]:
        for path in directory.rglob('*'):
            if path.is_file() and path.suffix in ('.json','.txt') and not any(part.startswith('isolated') for part in path.relative_to(directory).parts):cp(path,'evidence/'+name+'/'+path.relative_to(directory).as_posix())
    for version in ('0.15.104-dev','0.15.105-dev'):
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):cp(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    (DEST/'README.md').write_text('# NPC-перевод0.15.105\n\n11 редактур, пять оставшихся цветных оформлений исправлены. Начните с `translation-source/NPC_PROGRESS_0.15.105_RU.md`.\n\nJAR не установлен автоматически. Это частичный пакет изменений/доказательств, а не самостоятельный проект: нужны прежний проект, зафиксированные данные и зависимости PrismLauncher. Исторические эталоны сохранены; отдельный снимок ожиданий явно заменяет11 целей/соответствий. Один старый сравнительный тест обновлён для этих намеренных редактур.\n\nВсе125 исполняемых классов совпадают с104. Это не гарантия нулевой стоимости вывода более длинного текста и не замер FPS.\n\n`HASHES.json` содержит SHA-256 всех файлов кроме самого манифеста. Предыдущие пакеты не изменены.\n',encoding='utf-8')
    manifest={p.relative_to(DEST).as_posix():sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',manifest)
    for rel,h in manifest.items():assert sha(DEST/rel)==h
    archive=DEST.parent/'NPC_TRANSLATION_0.15.105.zip';assert not archive.exists()
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(DEST.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(manifest)+1
        for rel,h in manifest.items():assert hashlib.sha256(z.read(rel)).hexdigest().upper()==h
    target=DEST.parent/jar.name;assert not target.exists();shutil.copy2(jar,target);assert sha(target)==digest
    for rel,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/rel)==h
    result={'jar':str(target),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'progress':progress,'installed_pack_modified':False}
    write(ROOT/'build/NPC105_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
