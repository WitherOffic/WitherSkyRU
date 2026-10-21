"""Publish a non-installed JAR, reviewed-source report and hash-verified evidence bundle."""
import argparse,json,math,re,shutil,zipfile
from collections import Counter,defaultdict
from pathlib import Path
from prepare_npc78 import ROOT,DATA,SOURCE,sha,rows,write
from audit_npc103 import BASE,PREVIOUS

DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-19_6')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
    p=argparse.ArgumentParser()
    for name in ('checks','dispatch','compare','benchmark','focused','audit'):p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.103-dev.jar';fingerprint=sha(jar)
    audit=load(a.audit/'AUDIT.json');assert audit['runtime_jar_sha256']==fingerprint and audit['new_template_assertions']==329566
    assert load(a.checks/'CHECK_SCOPE.json')['runtime_jar_sha256']==fingerprint
    previous_rows=rows(a.dispatch/'0.15.103-dev/DISPATCH_ROWS.jsonl')
    old_style_gaps=sum(r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof'] for r in previous_rows)
    assert old_style_gaps==72
    evidence=load(SOURCE/'NPC_NAMES_SOURCES_0.15.103.json');assert len(evidence['records'])==135
    compare=load(a.compare/'SCOPE.json');assert compare['fixture_sha256']==sha(ROOT/'tests/TestNpcNames103.java') and compare['reviewed_tsv_sha256']==sha(SOURCE/'NPC_NAMES_0.15.103_REVIEWED.tsv')
    snapshots=[]
    for record in compare['records']:
        version=record['version'];path=a.compare/record['results_file'];assert sha(path)==record['results_sha256']
        assert record['runtime_jar_sha256']==(BASE if version=='0.15.102-dev' else fingerprint)
        snapshots.append({r['source']:r for r in load(path)})
    assert len(snapshots)==2
    for record in evidence['records']:
        s=record['source'];english=s.replace('{name}','Wither_101');russian=record['translation'].replace('{name}','Wither_101')
        assert snapshots[0][s]['flat']==english and snapshots[1][s]['flat']==russian
        assert snapshots[0][s]['published']==[english]*len(record['layouts']) and snapshots[1][s]['published']==[russian]*len(record['layouts'])
    bench=load(a.benchmark/'RAW.json');focus=load(a.focused/'RAW.json');assert bench['runtime_jar_sha256']==focus['runtime_jar_sha256']==fingerprint
    measurements={}
    for family,data in list(bench['families'].items())+[('focused',focus)]:
        assert [r['version'] for r in data['records']]==['0.15.102-dev','0.15.103-dev','0.15.103-dev','0.15.102-dev']
        for r in data['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.102-dev' else fingerprint)
        expected={'receipt':37,'hud':10,'mixed':5,'focused':10}[family];assert len(data['summary'])==expected,(family,len(data['summary']))
        for mode in data['summary']:
            assert len(mode['orders'])==2
            for order in mode['orders']:assert all(math.isfinite(order[k]) and order[k]>0 for k in ('before_ns','after_ns'))
        equal=[s for s in data['summary'] if s['equal_useful_output']];values=[o['wall_change_percent'] for s in equal for o in s['orders']]
        measurements[family]={'equal_modes':len(equal),'unequal_modes':expected-len(equal),'min_change_percent':min(values),'max_change_percent':max(values),'both_orders_slower_over5':[s['mode'] for s in equal if all(o['wall_change_percent']>5 for o in s['orders'])]}
    assert measurements['receipt']['equal_modes']==36 and measurements['receipt']['unequal_modes']==1
    assert not measurements['focused']['both_orders_slower_over5'],'Review persistent repeated-receipt regressions before delivery'
    # Keep exact registration and new dynamic-template evidence as separate metrics.
    work=rows(SOURCE/'NPC_WORKLIST_0.15.102.jsonl');assert len(work)==16244 and sum(r['exact_registration'] for r in work)==11092
    dynamic={r['source']:r for r in evidence['records']};seen=set()
    from prepare_npc103 import canonical
    for row in work:
        key=canonical(row['source']);row['single_name_template103']=key in dynamic
        if row['single_name_template103']:
            assert not row['exact_registration'];seen.add(key)
            row['name_template_review_scope']='ASCII username1..16; frozen publication layout; no heads; not a universal dynamic-variable claim'
    assert seen==dynamic.keys() and sum(r['single_name_template103'] for r in work)==135
    (SOURCE/'NPC_WORKLIST_0.15.103.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in work),encoding='utf-8')
    per=defaultdict(Counter)
    for r in work:
        c=per[r['speaker'] or '(без говорящего)'];c['total']+=1;c['exact']+=int(r['exact_registration']);c['named103']+=int(r['single_name_template103'])
    table=['# Публичная очередь: все обозначения говорящего — 0.15.103','','Точные записи и новые динамические шаблоны показаны раздельно. Это не готовность каждого NPC в игре; в очереди также есть варианты ответов, исторические и редакторские строки.','','| Обозначение в источнике | Строк | Точных ключей | Доля точных | Шаблонов с ником в 103 | Без точного ключа |','| --- | ---: | ---: | ---: | ---: | ---: |']
    for speaker,c in sorted(per.items(),key=lambda x:x[0].casefold()):table.append(f"| {speaker.replace('|','&#124;')} | {c['total']} | {c['exact']} | {100*c['exact']/c['total']:.2f}% | {c['named103']} | {c['total']-c['exact']} |")
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.103_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    counts=Counter(r['speaker'] for r in evidence['records']);phones=sum(': ✆ ' in r['source'] for r in evidence['records'])
    progress={'version':'0.15.103-dev','runtime_jar_sha256':fingerprint,'exact_keys':11092,'total_frozen':16244,'exact_percent':100*11092/16244,'without_exact_key':5152,'new_name_templates':135,'new_name_speaker_labels':92,'new_phone_templates':phones,'published_layouts':136,'pinned_pages':93,'fresh_test_groups':175,'parser_tests':15,'new_template_assertions':329566,'all_11252_previous_literal_outputs_unchanged':True,'preexisting_style_gaps_not_fixed_this_batch':old_style_gaps,'measurements_not_fps':measurements,'game_fps_or_frame_times_measured':False,'installed_pack_modified':False}
    write(SOURCE/'NPC_PROGRESS_0.15.103.json',progress)
    report=['# Перевод NPC — 0.15.103','','`████████████████████░░░░░░░░░░` **68,28% точных ключей** зафиксированной публичной очереди. Полоса округлена.','','| Показатель | Результат |','| --- | ---: |','| Точные ключи | 11 092 / 16 244 · 68,28% |','| Без отдельного точного ключа | 5 152 · 31,72% |','| Новых шаблонов с реальным ником | 135 |','| Обозначений NPC в этом проходе | 92 |',f'| Из них телефонных шаблонов | {phones} |','| Проверенных оформлений из публикаций | 136 |','| Страниц зафиксированных источников | 93 |','','Динамические шаблоны не прибавлены к числу точных ключей. Все 135 выбранных форм с тестовым ником раньше оставались английскими; теперь совпадают с авторским русским переводом, в том числе во всех 136 опубликованных оформлениях. Это не означает, что оставшиеся 5 152 строки целиком непереведены: часть старых динамических обработчиков работает без точного ключа.','','## Качество и совместимость','','175 групп проверок запущены заново на итоговом JAR, плюс 15 проверок разбора корпуса. Новая проверка: 329 566 утверждений. Проверены короткие/длинные ники, посимвольные стили имени, цветные термины, команды/подсказки/вставки, разбиение компонентов, время, счётчики, повторная обработка, настройки и пользовательские замены. Подписанные сообщения, текст игроков и клиентские сообщения защищены. Результаты 11 252 прежних строк идентичны 102; исправлен конфликт фиксированного приветствия `Hey adventurer!`.','','Ограничения этого прохода: только один ник из ASCII-букв, цифр и `_`, длиной 1–16. Иконки голов, произвольные числа, названия предметов и другие переменные не угадываются. Неизвестные оформления со смешанными смысловыми стилями оставляются исходными. Имена в служебном NPC-префиксе и таблички над NPC этим проходом не переименовываются. Источники привязаны к сохранённым ревизиям, а не заявлены как полная копия текущего сервера.','','## Производительность','','Новые планы текста создаются один раз при загрузке. Выбор идёт по точному префиксу NPC, максимум семь шаблонов на префикс. Готовые смещения зависят только от длины ника; живые сообщения, имена, стили и команды не кэшируются. Обработка — при получении чата, не каждый кадр. 119 прежних исполняемых классов, включая HUD и отрисовку, побайтно совпадают с 102. В обработчике чата сохранены прежние правила и добавлен закрытый резервный поиск.','','| Изолированный замер | Сопоставимых режимов | Разница времени, оба порядка |','| --- | ---: | ---: |']
    labels={'receipt':'Получение чата','hud':'Подготовленные HUD-строки','mixed':'Изменяющиеся HUD-строки','focused':'Удлинённый повторный замер'}
    for family in ('receipt','hud','mixed','focused'):
        m=measurements[family];report.append(f"| {labels[family]} | {m['equal_modes']} | {m['min_change_percent']:+.2f}…{m['max_change_percent']:+.2f}% |")
    report+=['','В коротком проходе три режима были медленнее более чем на 5% в обоих порядках. В удлинённой проверке десяти режимов, включая эти три, такого повторяемого результата нет. Это не доказательство нулевой нагрузки: небольшие различия времени обработки чата остаются, а игровой кадр здесь не измерялся.']
    report+=['','Двенадцать отдельных процессов для трёх семейств плюс четыре для удлинённой проверки, порядок 102 → 103 → 103 → 102. Повторная проверка включает три режима с подозрительным замедлением в коротком проходе. Один новый режим с 135 именованными репликами выполняет разный объём полезной работы на 102/103 и исключён из сравнения одинаковой нагрузки. Все исходные раунды сохранены. Это не игровой FPS, не время кадра и не замер 0,1% low. Нулевое влияние на FPS не обещается без проверки в игре.','','## Оставшиеся ограничения','','В прежнем скрининге сохраняются 72 случая, где плоская строка меняется, а опубликованное цветное оформление не переводится. Этот проход их не исправлял; они не скрыты за процентом точных ключей. Остальные переменные, книги, описания и имена над персонажами также требуют дальнейших отдельных проходов.','','## Новые шаблоны по NPC','','| Обозначение NPC | Шаблонов |','| --- | ---: |']
    report += [f'| {speaker} | {count} |' for speaker,count in sorted(counts.items(),key=lambda x:x[0].casefold())]
    report += ['','JAR подготовлен отдельно; установленная сборка и работающая игра не изменялись.','',f'SHA-256 JAR: `{fingerprint}`']
    (SOURCE/'NPC_PROGRESS_0.15.103_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
    assert not DEST.exists();DEST.mkdir()
    def copy(path,relative):
        target=DEST/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
    copy(jar,jar.name)
    for rel in ('build.gradle','build-local.ps1','src/main/resources/fabric.mod.json','src/main/java/ru/wither/skyru/ChatNotificationTranslator.java','src/main/java/ru/wither/skyru/NamedNpcTemplates.java','src/main/java/ru/wither/skyru/NpcDialogueTranslator.java','src/main/resources/data/witherskyru/npc-names103.json','src/main/resources/META-INF/licenses/NPC103-SOURCES.txt'):
        copy(ROOT/rel,rel)
    for path in SOURCE.glob('*0.15.103*'):copy(path,'translation-source/'+path.name)
    for path in (ROOT/'tools').glob('*npc103*'):copy(path,'tools/'+path.name)
    copy(ROOT/'tools/compare_npc_names103.ps1','tools/compare_npc_names103.ps1')
    for path in (ROOT/'tests').glob('*.java'):copy(path,'tests/'+path.name)
    for path in (ROOT/'tests').glob('test_npc_corpus77.py'):copy(path,'tests/'+path.name)
    for path in a.checks.glob('*-output.txt'):copy(path,'checks/'+path.name)
    for name in ('CHECK_SCOPE.json','compile-arguments.txt'):copy(a.checks/name,'checks/'+name)
    for name,directory in [('audit',a.audit),('benchmark',a.benchmark),('focused',a.focused),('names-compare',a.compare)]:
        for path in directory.rglob('*'):
            if path.is_file() and path.suffix in ('.json','.txt') and not any(p.startswith('isolated') or p=='config' for p in path.relative_to(directory).parts):copy(path,'evidence/'+name+'/'+path.relative_to(directory).as_posix())
    for version in ('0.15.102-dev','0.15.103-dev'):
        for filename in ('DISPATCH_SUMMARY.json','DISPATCH_ROWS.jsonl'):copy(a.dispatch/version/filename,'evidence/dispatch/'+version+'/'+filename)
    readme='# NPC-перевод 0.15.103\n\n135 новых шаблонов с именем игрока для 92 обозначений NPC. JAR не установлен автоматически.\n\nНачните с `translation-source/NPC_PROGRESS_0.15.103_RU.md`.\n\nВ архиве итоговый JAR, изменённые исходники/ресурсы, авторские переводы, привязка к публичным ревизиям, тестовые исходники и свежие результаты. Это пакет изменений и доказательств, не самостоятельная копия всего проекта: для сборки/полного повторения старых проверок нужен исходный проект, его зафиксированные входные данные и зависимости PrismLauncher.\n\n`HASHES.json` содержит SHA-256 каждого файла кроме самого манифеста. Предыдущий пакет102 не изменён. Реальный FPS/время кадра не измерялись.\n'
    (DEST/'README.md').write_text(readme,encoding='utf-8')
    manifest={path.relative_to(DEST).as_posix():sha(path) for path in sorted(DEST.rglob('*')) if path.is_file()}
    write(DEST/'HASHES.json',manifest)
    for rel,digest in manifest.items():assert sha(DEST/rel)==digest,rel
    archive=DEST.parent/'NPC_TRANSLATION_0.15.103.zip';assert not archive.exists()
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for path in sorted(DEST.rglob('*')):
            if path.is_file():z.write(path,path.relative_to(DEST).as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None and len(z.namelist())==len(manifest)+1
        import hashlib
        for rel,digest in manifest.items():assert hashlib.sha256(z.read(rel)).hexdigest().upper()==digest
    destination=DEST.parent/jar.name;assert not destination.exists();shutil.copy2(jar,destination);assert sha(destination)==fingerprint
    for rel,digest in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/rel)==digest
    result={'jar':str(destination),'jar_sha256':fingerprint,'delivery':str(DEST),'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(manifest),'zip_entries':len(manifest)+1,'installed_pack_modified':False,'progress':progress}
    write(ROOT/'build/NPC103_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
