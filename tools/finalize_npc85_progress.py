"""Bind evidence to one tested JAR, update a full ledger and publish without installing."""
import argparse,hashlib,html,json,re,shutil,zipfile
from collections import Counter,defaultdict
from pathlib import Path
from prepare_npc78 import rows,sha,write
from prepare_npc85 import SPEAKERS,ROOT,SOURCE,DATA,CORPUS
BASE='89E2D4AFC38FB6BB11F28A41CF5B37F844149489E2E0C5161D9F79068517A74D'
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def number(x):return f'{x:,}'.replace(',','\u202f')
def main():
    p=argparse.ArgumentParser()
    for name in ('checks','audit','benchmark','destination','jar-directory'):p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();destination=a.destination.resolve();assert not destination.exists(),'Never overwrite a published package';assert a.jar_directory.is_dir()
    jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.85-dev.jar';baseline=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.84-dev.jar';fingerprint=sha(jar);assert sha(baseline)==BASE
    scope=load(a.checks/'CHECK_SCOPE.json');tests=scope['tests_passed'];assert scope['runtime_jar_sha256']==fingerprint and not scope['targeted_only'] and not scope['reused_successes'] and len(tests)==len(set(tests))==141
    assert {'TestNpc85','TestNpc84','TestNpcChoices84','TestNpcLookup61','TestHudMixins','TestFrameNoops71','TestEditorialQuality'}<=set(tests)
    for name in tests:assert (a.checks/(name+'-output.txt')).is_file()
    new_test=(a.checks/'TestNpc85-output.txt').read_text(encoding='utf-8-sig');assert '689 new authored literals/689 published layouts' in new_test
    assertions=int(re.search(r'PASS: NPC85 (\d+) assertions',new_test)[1])
    batch=load(DATA/'npc-expansion85.json')['records'];new={r['source']:r['translation'] for r in batch};assert len(new)==len(batch)==689 and Counter(r['speaker'] for r in batch)==SPEAKERS
    authored={};authoring_paths=[]
    for speaker in SPEAKERS:
        path=SOURCE/('NPC85_'+re.sub('[^A-Z0-9]+','_',speaker.upper())+'_REVIEWED.tsv');authoring_paths.append(path)
        for line in path.read_text(encoding='utf-8-sig').splitlines():
            if not line or line.startswith('#'):continue
            f=line.split('|');source=f'[NPC] {f[0]}: {f[1]}';assert source not in authored;authored[source]=f'[NPC] {f[0]}: {f[2]}'
    assert authored==new
    evidence=load(SOURCE/'NPC_SOURCES_0.15.85.json');assert len(evidence['records'])==808 and len(evidence['source_pages'])==23 and len(evidence['retained_originals'])==77 and len(evidence['pending_variables'])==31 and len(evidence['neutral_unchanged'])==4 and len(evidence['unsafe_excluded'])==7
    revision=load(SOURCE/'NPC_0.15.85_PUBLIC_REVISION.json');assert not revision['matches_pinned_corpus'] and revision['dialogue_unchanged_after_exact_manual_delta_review'] and len(revision['pages'])==23
    assert revision['differences']=={'Cult of the Fallen Star':{'pinned':843725,'current':859622}}
    delta=load(SOURCE/'NPC_0.15.85_REVISION_DELTA.json')['records'];assert len(delta)==1 and delta[0]['current_revision']==859622
    pinned=next(r['raw_wikitext'] for r in evidence['source_pages'] if r['title']=='Cult of the Fallen Star');removed='{{Stub|Need to add the countdown on [[Events#Timers]] {{TODOstate|no}}}}\n\n'
    assert pinned.startswith(removed) and delta[0]['current_page']['revisions'][0]['slots']['main']['content']==pinned[len(removed):]
    before=load(a.audit/'0.15.84-dev/DISPATCH_SUMMARY.json');after=load(a.audit/'0.15.85-dev/DISPATCH_SUMMARY.json');assert before['runtime_jar_sha256']==BASE and after['runtime_jar_sha256']==fingerprint
    assert before['queue_sha256']==after['queue_sha256']==evidence['queue_sha256']==sha(CORPUS/'translation_queue.jsonl') and before['tested_literal_rows']==after['tested_literal_rows']==11247
    old={r['source']:r for r in rows(a.audit/'0.15.84-dev/DISPATCH_ROWS.jsonl')};current={r['source']:r for r in rows(a.audit/'0.15.85-dev/DISPATCH_ROWS.jsonl')};assert old.keys()==current.keys()
    for source,r in current.items():
        if source in new:assert r['flat_result']==r['published_style_result']==new[source],source
        else:assert all(r[k]==old[source][k] for k in ('flat_result','published_style_result')),source
    assert len(new.keys()&current.keys())==689
    gaps=[r for r in current.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']];assert len(gaps)==72
    benchmark=load(a.benchmark/'RAW.json');assert [r['version'] for r in benchmark['records']]==['0.15.84-dev','0.15.85-dev','0.15.85-dev','0.15.84-dev'] and len(benchmark['summary'])==15
    for record in benchmark['records']:assert record['jar_sha256']==(BASE if record['version']=='0.15.84-dev' else fingerprint)
    changed=[];added=[]
    with zipfile.ZipFile(baseline) as x,zipfile.ZipFile(jar) as y:
        assert y.testzip() is None;old_names,new_names=set(x.namelist()),set(y.namelist());assert not old_names-new_names
        changed=sorted(k for k in old_names&new_names if x.read(k)!=y.read(k));added=sorted(new_names-old_names)
        assert changed==['fabric.mod.json','ru/wither/skyru/NpcDialogueTranslator.class'],changed
        assert added==['META-INF/licenses/NPC85-SOURCES.txt','data/witherskyru/npc-expansion85.json'],added
        assert y.read('data/witherskyru/npc-expansion85.json')==(DATA/'npc-expansion85.json').read_bytes()
    stats=load(SOURCE/'NPC_PROGRESS_0.15.85.json');assert stats['exact_resource_after']==3987 and stats['new_resource_sha256']==sha(DATA/'npc-expansion85.json')
    stats.update({'runtime_validation':'141_fresh_groups_passed','fresh_test_groups':141,'npc85_assertions':assertions,'runtime_jar_sha256':fingerprint,'runtime_changed_files':changed,'runtime_added_files':added,'other_executable_classes_byte_identical':True,'render_hud_f3_classes_byte_identical':True,'outside_selected_dispatch_text_unchanged':True,'new_rows_in_conservative_bulk_dispatch':689,'compatible_published_npc_layouts_checked':689,'remaining_original_style_gap':72,'dispatch_before':before,'dispatch_after':after,'benchmark_summary':benchmark['summary'],'current_revision_check':'22 exact revisions plus one manually reviewed editorial-only delta','parser_tests_passed':13})
    write(SOURCE/'NPC_PROGRESS_0.15.85.json',stats);write(SOURCE/'NPC_STYLE_REMAINING_0.15.85.json',{'scope':'72 older candidates unchanged by85, not certified fixed','records':gaps})
    exact=defaultdict(set)
    for path in sorted(DATA.glob('*.json')):
        for r in load(path).get('records',[]):
            if r.get('source') and r.get('translation'):exact[r['source']].add(r['translation'])
    worklist=rows(SOURCE/'NPC_WORKLIST_0.15.84.jsonl');queue=rows(CORPUS/'translation_queue.jsonl');assert len(worklist)==len(queue)==16244 and [r['source'] for r in worklist]==[r['source'] for r in queue]
    pending={r['source'] for r in evidence['pending_variables']};neutral={r['source'] for r in evidence['neutral_unchanged']};excluded={r['source'] for r in evidence['unsafe_excluded']};by_speaker=defaultdict(list)
    for r in worklist:
        source=r['source'];r['exact_registration']=bool(exact.get(source));r['registered_targets_not_automatically_certified']=sorted(exact.get(source,set()))
        if source in new:r['review_status']='authored_and_fixture_checked85';r['all_published_npc_layouts_checked85']=True
        if source in pending:r['structural_review_pending85']=True;r['template_coverage_certified85']=False
        if source in neutral:r['review_status']='neutral_no_language_translation_needed85'
        if source in excluded:r['review_status']='historical_source_excluded85'
        if source in current:r['runtime_flat_snapshot_not_quality_proof']=current[source]['flat_result'];r['runtime_colored_snapshot_not_quality_proof']=current[source]['published_style_result']
        by_speaker[r['speaker'] or '(без указанного говорящего)'].append(r)
    assert sum(r['exact_registration'] for r in worklist)==3987
    (SOURCE/'NPC_WORKLIST_0.15.85.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in worklist),encoding='utf-8')
    gap_keys={r['source'] for r in gaps};table=['# Вся очередь NPC — 0.15.85','','Публичная выгрузка, не полный реестр действующих NPC. Наличие точного ключа не сертифицирует весь перевод; динамические строки, нейтральные записи и варианты ответов учитываются отдельно.','', '| Говорящий | Всего | Точных ключей | Доля | Новых в 85 | Цветных кандидатов | Структурных признаков |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    for speaker,values in sorted(by_speaker.items()):
        count=sum(r['exact_registration'] for r in values);safe=html.escape(speaker).replace('|','\\|')
        table.append(f'| {safe} | {len(values)} | {count} | {100*count/len(values):.2f}% | {sum(r["source"] in new for r in values)} | {sum(r["source"] in gap_keys for r in values)} | {sum(bool(r["variable_definition"]) for r in values)} |')
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.85_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    report=['# Большая партия NPC — 0.15.85','','`██████░░░░░░░░░░░░░░░░░░░` 24,54% точных ключей публичной базы (полоса округлена).','', '| Учёт | До | После |','| --- | ---: | ---: |','| Точные ключи переводов | 3 298 · 20,30% | 3 987 · 24,54% |','| Без подтверждённого точного ключа | 12 946 | 12 257 · 75,46% |','| Всего уникальных строк базы | 16 244 | 16 244 |','','Это не процент полностью проверенного перевода всей игры. Отсутствие точного ключа не исключает динамический перевод. База содержит исторические и редакторские строки; закрытые серверные скрипты недоступны.','', '| Говорящий | Новых реплик |','| --- | ---: |']
    for speaker,count in SPEAKERS.items():report.append(f'| {speaker} | {count} |')
    report+=['| Всего | 689 |','','Охват выбранного среза — 808 строк на 23 страницах: 689 новых переводов, 77 сохранённых старых ключей, 31 структурная строка на отдельную проверку, четыре нейтральные и семь исключённых исторических строк. В структурной группе есть как настоящие подстановки, так и требующие разбора номера слотов и редакторские пометки. Они не зарегистрированы фиктивными точными переводами. Новые варианты ответа и таблички имён над моделями этой партией не сертифицируются.','',
        '## Качество и актуальность','',
        'Диалоги Бет и Джейка сверены вместе; уточнены пицца и тарелки в разговорах Брарнаса. Названия питомцев, существ и материалов согласованы с действующим словарём. Подробности — [контекстная вычитка](NPC_0.15.85_REVIEW_NOTES.md).','',
        'Последняя API-проверка: '+revision['checked_client_date']+'. 22 ревизии совпали; в статье Cult of the Fallen Star удалена только редакторская пометка. Весь остальной текст побуквенно прежний. Точное сравнение сохранено. Архивные реплики Серафины о Данте не объявлены новым действующим событием.','',
        f'Все 141 группа проверок выполнены заново на одном окончательном JAR. Новая партия: {number(assertions)} проверок, все 689 опубликованных цветовых схем. Проверены свежие цвета, курсив, действия и всплывающие подсказки, числа, время, счётчики, фрагментация, переопределения, переключатели и защита подписанного, пользовательского и клиентского чата. Отдельно прошли 13 тестов парсера. Это не живой проход сервера.','',
        '| Те же 11 247 строк: техническое срабатывание, не оценка качества | 84 | 85 |','| --- | ---: | ---: |','| Обычный текст | 3 260 | 3 949 |','| Опубликованные цвета | 3 188 | 3 877 |','','Все 689 новых строк сработали в обоих режимах. Вне выбранных новых строк оба результата дословно прежние. Старые 72 цветных кандидата остаются открытыми.','',
        '## Нагрузка','',
        '689 строк загружаются в прежнюю точную карту при запуске. Новых кадровых проходов и сетевых вызовов в игре нет. Все остальные исполняемые классы, включая HUD/F3 и обработчик чата, побайтово совпали с 84. Старые текстовые ресурсы также не изменены. JAR вырос на '+number(jar.stat().st_size-baseline.stat().st_size)+' байт; полная выгруженная база в него не включена.','',
        '| Обработка входящего чата | Изменение времени, два порядка | Байты/сообщение до → после |','| --- | ---: | ---: |']
    labels={'unknown':'Неизвестные реплики','existing_dynamic':'Прежние динамические реплики','existing_exact':'Прежние точные реплики','existing_style':'Прежние цветные реплики','existing_tal':'Тал Кер','existing_wizard':'Волшебник','existing_tyashoi':'Алхимик Тьяшой','existing_elle':'Элль','existing_kat_elizabeth':'Кэт и Элизабет','existing_end_guide':'Одинокий искатель приключений','existing_juliette_style':'Джульетта: цветные реплики','existing_choices':'Прежние ответы','existing_large_batch84':'Партия 84: тот же перевод','existing_choices84':'Кнопки 84: тот же перевод','new_large_batch85':'Новые реплики 85: дополнительный перевод'}
    for r in benchmark['summary']:
        times=' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in r['orders']);memory=' / '.join(f'{o["before_bytes"]:.0f} → {o["after_bytes"]:.0f}' for o in r['orders']);report.append(f'| {labels[r["mode"]]} | {times} | {memory} |')
    equal=[o['wall_change_percent'] for r in benchmark['summary'] if r['equal_useful_output'] for o in r['orders']]
    extra=next(r['orders'] for r in benchmark['summary'] if r['mode']=='new_large_batch85');extra_us=[(r['after_ns']-r['before_ns'])/1000 for r in extra]
    report+=['',f'Прежние сценарии в этих запусках: от {min(equal):+.1f}% до {max(equal):+.1f}%; это смешанные микрозамеры, не доказательство ускорения или отсутствия просадок в игре. Четыре отдельные JVM: 84 → 85 → 85 → 84, все раунды сохранены. Отрицательный процент означает меньшее время, положительный — большее. Новые реплики делают дополнительную полезную работу. Это не FPS и не 0,1% lows; нулевая нагрузка не обещана.','',
        f'Новые положительные случаи в этой выборке требуют дополнительно около {min(extra_us):.2f}–{max(extra_us):.2f} мкс на входящее сообщение по сравнению с прежним пропуском. Это обработка при получении сообщения, не повторение всей базы каждый кадр. Выделение памяти тоже выросло; абсолютные значения и все раунды сохранены в BENCHMARK_RAW.json. Эти цифры нельзя пересчитать в FPS без измерения в игре.','',
        '## Оставшиеся ограничения','',
        'Вся база ещё не переведена. Помимо структурных строк, остаются 72 старых цветных кандидата, прежний широкий смысловой аудит ReviewedLoreStyles и другие непроверенные реплики. Эта сборка в игре не измерялась. Автоматически в игру не установлена; старые JAR сохранены.','',
        '[Все говорящие и остаток](NPC_ALL_SPEAKERS_0.15.85_RU.md).', '',f'SHA-256 JAR: `{fingerprint}`.']
    (SOURCE/'NPC_PROGRESS_0.15.85_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
    destination.mkdir(parents=True)
    files=[p for p in SOURCE.glob('*0.15.85*') if p.is_file()]+authoring_paths+[DATA/'npc-expansion85.json',ROOT/'src/main/resources/META-INF/licenses/NPC85-SOURCES.txt',ROOT/'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java',ROOT/'src/main/resources/fabric.mod.json',ROOT/'build.gradle']
    files += [ROOT/'tests'/name for name in ('TestNpc85.java','BenchmarkNpc85.java','AuditNpcCorpus77.java')]
    files += [ROOT/'tools'/name for name in ('prepare_npc78.py','prepare_npc85.py','review_npc85_revision_delta.py','check_npc85.ps1','dispatch_npc85.ps1','benchmark_npc85.ps1','finalize_npc85_progress.py')]
    files += [p for p in (ROOT/'tests').glob('*.java') if 'phraseCount()==3749' in p.read_text(encoding='utf-8-sig')]
    for path in sorted(set(files)):
        target=destination/path.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
    checks=destination/'checks';checks.mkdir()
    for name in tests:shutil.copy2(a.checks/(name+'-output.txt'),checks)
    shutil.copy2(a.checks/'CHECK_SCOPE.json',checks);shutil.copy2(a.benchmark/'RAW.json',destination/'BENCHMARK_RAW.json')
    for version in ('0.15.84-dev','0.15.85-dev'):
        target=destination/'dispatch'/version;target.mkdir(parents=True);shutil.copy2(a.audit/version/'DISPATCH_SUMMARY.json',target)
    (destination/'README_RU.md').write_text('# Большая партия 85\n\n689 новых реплик, 17 обозначений говорящего, 141 свежая группа проверок. 3 987 / 16 244 точных ключей (24,54%).\n\n[Отчёт и ограничения](translation-source/NPC_PROGRESS_0.15.85_RU.md)\n\n[Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.85_RU.md)\n\nЧастичный пакет исходников и отчётов, не полный проект. Тестовый JAR выдаётся отдельно и не устанавливается автоматически.\n',encoding='utf-8')
    hashes={p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()};write(destination/'HASHES.json',hashes);archive=destination/'NPC_TRANSLATION_BATCH_09.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for name in sorted(hashes):z.write(destination/name,name)
        z.write(destination/'HASHES.json','HASHES.json')
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        for name,expected in hashes.items():assert hashlib.sha256(z.read(name)).hexdigest().upper()==expected
        entries=len(z.namelist())
    delivered=a.jar_directory/jar.name
    if delivered.exists():assert sha(delivered)==fingerprint,'Never replace a different published JAR'
    else:shutil.copy2(jar,delivered)
    assert sha(delivered)==fingerprint
    write(destination/'DELIVERY.json',{'archive':str(archive),'sha256':sha(archive),'bytes':archive.stat().st_size,'entries':entries,'jar':str(delivered),'jar_sha256':fingerprint,'installed_game_modified':False})
    print(json.dumps(load(destination/'DELIVERY.json'),ensure_ascii=True,indent=2))
if __name__=='__main__':main()
