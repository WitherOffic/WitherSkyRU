"""Package reviewed style-only repairs; exact-key percentages deliberately stay unchanged."""
import argparse,hashlib,json,math,re,shutil,zipfile
from pathlib import Path
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,SOURCE,sha,rows,write
from audit_npc104 import PREVIOUS,BASE

DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_1')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
    parser=argparse.ArgumentParser()
    for name in ('checks','dispatch','audit','benchmark','focused','hud-focused'):parser.add_argument('--'+name,type=Path,required=True)
    a=parser.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.104-dev.jar';digest=sha(jar)
    audit=load(a.audit/'AUDIT.json');assert audit['runtime_jar_sha256']==digest and audit['new_style_assertions']==78163
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==digest and len(checks['tests_passed'])==176
    evidence=load(SOURCE/'NPC_STYLE_SOURCES_0.15.104.json');assert len(evidence['records'])==67 and len(evidence['pending'])==5
    benchmark=load(a.benchmark/'RAW.json');focused=load(a.focused/'RAW.json');hud_focused=load(a.hud_focused/'RAW.json');assert benchmark['runtime_jar_sha256']==focused['runtime_jar_sha256']==hud_focused['runtime_jar_sha256']==digest
    metrics={}
    for family,data in list(benchmark['families'].items())+[('focused',focused),('hud_focused',hud_focused)]:
        assert [r['version'] for r in data['records']]==['0.15.103-dev','0.15.104-dev','0.15.104-dev','0.15.103-dev']
        for record in data['records']:assert record['jar_sha256']==(BASE if record['version']=='0.15.103-dev' else digest)
        assert len(data['summary'])=={'receipt':38,'hud':10,'mixed':5,'focused':10,'hud_focused':2}[family]
        equal=[m for m in data['summary'] if m['equal_useful_output']]
        if family in ('receipt','focused'):
            # Actual useful-output equality, not a label inferred solely from a mode name.
            for m in equal:
                actual=[r['results'][m['mode']]['useful_visible_outputs'] for r in data['records']]
                assert actual[0]==actual[1]==actual[2]==actual[3],(family,m['mode'])
        percentages=[]
        for mode in equal:
            assert len(mode['orders'])==2
            for order in mode['orders']:
                assert all(math.isfinite(order[k]) and order[k]>0 for k in ('before_ns','after_ns'))
                percentages.append(order['wall_change_percent'])
        metrics[family]={'equal_modes':len(equal),'unequal_modes':len(data['summary'])-len(equal),'min_change_percent':min(percentages),'max_change_percent':max(percentages),'both_orders_slower_over5':[m['mode'] for m in equal if all(o['wall_change_percent']>5 for o in m['orders'])]}
    assert metrics['receipt']['equal_modes']==37 and metrics['receipt']['unequal_modes']==1
    fixed={r['source']:r for r in evidence['records']};newrows={r['source']:r for r in rows(a.dispatch/'0.15.104-dev/DISPATCH_ROWS.jsonl')}
    work=rows(SOURCE/'NPC_WORKLIST_0.15.103.jsonl');assert len(work)==16244 and sum(r['exact_registration'] for r in work)==11092
    pending_reasons={0:'В переводе не выражено выделенное lot («много»).',3:'В переводе отсутствует special («особенный»).',13:'Нет отдельного местоимения для выделенного you («вам»).',25:'Нужна редактура идиомы Crazy stuff, прежде чем назначать её выделение.',52:'В переводе не выражен усилительный смысл do.'}
    pending={r['snapshot']['source']:pending_reasons[r['index']] for r in evidence['pending']}
    for row in work:
        row['published_style_repair104']=row['source'] in fixed
        if row['source'] in fixed:row['runtime_colored_snapshot_not_quality_proof']=newrows[row['source']]['published_style_result']
        if row['source'] in pending:row['wording_review104']=pending[row['source']]
    assert sum(r['published_style_repair104'] for r in work)==67
    (SOURCE/'NPC_WORKLIST_0.15.104.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8')
    per=defaultdict(Counter)
    for row in work:
        c=per[row['speaker'] or '(без говорящего)'];c['total']+=1;c['exact']+=int(row['exact_registration']);c['named103']+=int(row.get('single_name_template103',False));c['style104']+=int(row['published_style_repair104'])
    all_table=['# Публичная очередь: обозначения говорящего — 0.15.104','','Не процент готовности всей игры или каждого NPC. Точные записи, шаблоны с ником и исправления оформления показаны раздельно.','','| Обозначение | Строк | Точных ключей | Доля точных | Шаблонов с ником в 103 | Исправлений оформления в 104 | Без точного ключа |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    for name,c in sorted(per.items(),key=lambda x:x[0].casefold()):all_table.append(f"| {name.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['named103']} | {c['style104']} | {c['total']-c['exact']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.104_RU.md').write_text('\n'.join(all_table)+'\n',encoding='utf-8')
    latin=[]
    for record in evidence['records']:
        body=record['translation'].split(': ',1)[1].replace('I love the warmth of the Holidays despite the snow','')
        words=sorted(set(re.findall('[A-Za-z]+',body))-{'SkyBlock'})
        if words:latin.append({'source':record['source'],'translation':record['translation'],'remaining_body_tokens':words,'reason':'Preexisting proper names retained; this batch changes semantic style plans only,not wording.'})
    write(SOURCE/'NPC_WORDING_PENDING_0.15.104.json',{'style_pending':[{'source':s,'reason':r} for s,r in pending.items()],'preexisting_latin_name_rows_in_repaired_set':latin,'unsupported_counter_scope':'Two Tia relay lines retain the existing base-provider restriction: extra repeat suffix not enabled.','server_input_exception':'The St. Jerry holiday phrase deliberately stays English for exact server input; Russian instruction already present.'})
    counts=Counter(r['queue_row']['speaker'] for r in evidence['records'])
    progress={'version':'0.15.104-dev','runtime_jar_sha256':digest,'exact_keys':11092,'total_frozen':16244,'exact_percent':100*11092/16244,'without_exact_key':5152,'prior_name_templates103':135,'style_repairs':67,'style_gap_before':72,'style_gap_remaining':5,'style_gap_repair_percent':100*67/72,'style_speaker_labels':len(counts),'pinned_pages':14,'fresh_test_groups':176,'parser_tests':15,'new_style_assertions':78163,'old_test_sources_unchanged':audit['previous_test_sources_unchanged'],'unchanged_runtime_classes':124,'preexisting_latin_name_rows_in_repaired_set':len(latin),'performance_not_fps':metrics,'installed_pack_modified':False,'game_fps_or_frame_times_measured':False}
    write(SOURCE/'NPC_PROGRESS_0.15.104.json',progress)
    report=['# Перевод NPC — 0.15.104','','`████████████████████████████░░` **67 / 72 · 93,06%** известных проблем цветного оформления исправлено. Полоса округлена.','','| Показатель | До | После |','| --- | ---: | ---: |','| Проблемные опубликованные оформления в выбранной группе | 72 | 5 |','| Исправлено в этом проходе | — | 67 |','| Обозначений NPC в исправленном наборе | — | 14 |','| Точные ключи публичной очереди | 11 092 / 16 244 | 11 092 / 16 244 · 68,28% |','| Без отдельного точного ключа | 5 152 | 5 152 |','','Это не 93,06% всей игры. Новых точных ключей здесь нет: исправлены соответствия цветов и действий для существующих переводов. Прежние формулировки и основные планы оформления сохранены; 135 шаблонов с ником из 103 также сохранены. Закрытые серверные скрипты и все возможные живые оформления не считаются известными.','','## Проверки','','Все 176 групп тестов запущены заново на итоговом JAR, плюс 15 проверок разбора базы. Новая проверка — 78 163 утверждения. Проверены каждая буква перевода, цвета, курсив, текущие команды/подсказки/вставки, числа, время, разбиение компонентов, пользовательские замены, настройки и защита текста игроков. Все 261 прежний тестовый исходник оставлен без изменений.','','В общем скрининге 11 252 входа: все прежние результаты обычного текста совпадают с 103; в цветном тексте изменились только выбранные 67 источников. Остальные 11 185 результатов цветного текста совпадают с 103. Пять оставшихся случаев сохранены как незавершённые, без подмены смысла ради цвета.','','## Производительность','','Новая таблица читается один раз при загрузке и использует существующий поиск точных фраз. Дополнительных проходов или правил каждый кадр не добавлено. 124 прежних исполняемых класса побайтно совпадают с 103; изменён только загрузчик таблиц оформления. Исполняемый код обработки чата, HUD, отрисовки, кэшей и шаблонов с ником не менялся. Это не гарантия нулевой цены более длинного текста на экране.','','| Изолированный замер | Сопоставимых режимов | Разница времени, оба порядка |','| --- | ---: | ---: |']
    names={'receipt':'Получение чата','hud':'Подготовленные HUD-строки','mixed':'Изменяющиеся HUD-строки','focused':'Удлинённый повтор чата','hud_focused':'Удлинённый повтор цветов/действий HUD'}
    for family in ('receipt','hud','mixed','focused','hud_focused'):
        m=metrics[family];report.append(f"| {names[family]} | {m['equal_modes']} | {m['min_change_percent']:+.2f}…{m['max_change_percent']:+.2f}% |")
    flagged={family:m['both_orders_slower_over5'] for family,m in metrics.items() if m['both_orders_slower_over5']}
    if not metrics['focused']['both_orders_slower_over5'] and not metrics['hud_focused']['both_orders_slower_over5']:
        report+=['','Удлинённые повторы чата и HUD не показали замедления более 5% в обоих порядках. Короткое отклонение HUD не подтвердилось: в длинном режиме меняющихся цветов/действий разница около +0,04…+0,10%, в варианте с чтением готовой строки замедления нет. Исходные короткие результаты не удалены.']
    report+=['','Двенадцать отдельных процессов для трёх семейств плюс по четыре для удлинённых проверок чата и HUD, порядок 103 → 104 → 104 → 103. Повтор HUD выбран из-за разброса короткого прохода: два режима, по два миллиона прогревочных операций и четыре раунда по два миллиона операций. Равенство полезных результатов чата проверено по сохранённым спискам выводов, а не только названиям режимов. Новый режим с 67 исправленными оформлениями делает разную полезную работу и исключён из сравнения одинаковой нагрузки. Все раунды сохранены.',f'','Режимы с замедлением более 5% в обоих порядках по каждому этапу: '+(json.dumps(flagged,ensure_ascii=False) if flagged else 'не обнаружены в выбранных замерах.') ,'','Это не игровой FPS, не время кадра и не замер 0,1% low. Проверка в игре нужна отдельно; нулевое влияние на FPS не заявляется.','','## Пять оставшихся случаев','','| NPC | Исходная реплика | Почему требуется новая формулировка |','| --- | --- | --- |']
    for record in evidence['pending']:
        source=record['snapshot']['source'];report.append(f"| {record['queue_row']['speaker']} | {source.split(': ',1)[1]} | {pending[source]} |")
    report+=['',f'В {len(latin)} прежних формулировках этого набора также остаются латинские имена внутри реплик. Они вынесены в `NPC_WORDING_PENDING_0.15.104.json`; исправление оформления не означает полного редакторского завершения этих текстов. Имена в служебном NPC-префиксе не переименовывались.','','Две реплики Тии из базового словаря по-прежнему не поддерживают добавочный счётчик повторов: этот проход не расширял их грамматику. Английская праздничная фраза Сент-Джерри сохранена намеренно для точного ввода на сервере; русская инструкция и оформление исправлены.','','## Исправления по NPC','','| Обозначение NPC | Исправленных оформлений |','| --- | ---: |']
    report += [f'| {name} | {count} |' for name,count in sorted(counts.items(),key=lambda x:x[0].casefold())]
    report += ['','JAR подготовлен отдельно. Установленная сборка и игра не изменялись.','',f'SHA-256 JAR: `{digest}`']
    (SOURCE/'NPC_PROGRESS_0.15.104_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
    assert not DEST.exists();DEST.mkdir()
    def copy(path,rel):
        out=DEST/rel;out.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,out)
    copy(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java','src/main/java/ru/wither/skyru/ChatNotificationTranslator.java','src/main/java/ru/wither/skyru/NamedNpcTemplates.java','src/main/resources/data/witherskyru/npc-styles104.json','src/main/resources/META-INF/licenses/NPC104-SOURCES.txt'):copy(ROOT/rel,rel)
    for path in SOURCE.glob('*0.15.104*'):copy(path,'translation-source/'+path.name)
    for path in (ROOT/'tools').glob('*npc104*'):copy(path,'tools/'+path.name)
    for path in (ROOT/'tests').glob('*.java'):copy(path,'tests/'+path.name)
    copy(ROOT/'tests/test_npc_corpus77.py','tests/test_npc_corpus77.py')
    for path in a.checks.glob('*-output.txt'):copy(path,'checks/'+path.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):copy(a.checks/name,'checks/'+name)
    for name,directory in [('audit',a.audit),('benchmark',a.benchmark),('focused',a.focused),('hud-focused',a.hud_focused)]:
        for path in directory.rglob('*'):
            if path.is_file() and path.suffix in ('.json','.txt') and not any(part.startswith('isolated') for part in path.relative_to(directory).parts):copy(path,'evidence/'+name+'/'+path.relative_to(directory).as_posix())
    for version in ('0.15.103-dev','0.15.104-dev'):
        for name in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):copy(a.dispatch/version/name,'evidence/dispatch/'+version+'/'+name)
    (DEST/'README.md').write_text('# NPC-перевод 0.15.104\n\n67 исправлений цветного оформления;пять случаев требуют правки формулировок. Начните с `translation-source/NPC_PROGRESS_0.15.104_RU.md`.\n\nJAR не установлен автоматически. Это пакет изменений и доказательств, не самостоятельная копия проекта: для сборки и полного повторения старых проверок нужны исходный проект, его зафиксированные входные данные и зависимости PrismLauncher. Старые формулировки, основные планы и тестовые исходники не переписывались.\n\n`HASHES.json` содержит SHA-256 всех файлов кроме самого манифеста. Предыдущие пакеты не изменены. Реальный FPS и время кадра здесь не измерялись.\n',encoding='utf-8')
    manifest={p.relative_to(DEST).as_posix():sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',manifest)
    for rel,h in manifest.items():assert sha(DEST/rel)==h
    archive=DEST.parent/'NPC_TRANSLATION_0.15.104.zip';assert not archive.exists()
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for path in sorted(DEST.rglob('*')):
            if path.is_file():z.write(path,path.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(manifest)+1
        for rel,h in manifest.items():assert hashlib.sha256(z.read(rel)).hexdigest().upper()==h
    destination=DEST.parent/jar.name;assert not destination.exists();shutil.copy2(jar,destination);assert sha(destination)==digest
    for rel,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/rel)==h
    result={'jar':str(destination),'jar_sha256':digest,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'installed_pack_modified':False,'progress':progress}
    write(ROOT/'build/NPC104_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
