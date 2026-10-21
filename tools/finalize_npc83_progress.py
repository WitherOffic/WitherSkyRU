"""Validate one executable, audit delta, preserve full ledger, publish dev JAR without installing."""
import argparse
from collections import defaultdict
import hashlib
import html
import json
from pathlib import Path
import re
import shutil
import zipfile
from prepare_npc78 import rows, sha, write

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'translation-source'
DATA=ROOT/'src/main/resources/data/witherskyru'
BASE='8BC98E6DB4211407E645B580852F15A4ECCFFD5C1E021E4E73DB90E43980BE47'
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))

def main():
    parser=argparse.ArgumentParser()
    for name in ('checks','audit','benchmark','corpus','destination','jar-directory'):parser.add_argument('--'+name,type=Path,required=True)
    args=parser.parse_args();destination=args.destination.resolve();jar_directory=args.jar_directory.resolve()
    assert not destination.exists(),'Refusing to replace a prior source delivery'
    assert jar_directory.is_dir()
    jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.83-dev.jar';fingerprint=sha(jar)
    baseline=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.82-dev.jar';assert sha(baseline)==BASE
    scope=load(args.checks/'CHECK_SCOPE.json');tests=scope['tests_passed']
    assert scope['runtime_jar_sha256']==fingerprint and not scope['targeted_only']
    assert len(tests)==len(set(tests))==138
    assert {'TestNpc83','TestNpcStyles83','TestNpc82','TestNpcLookup61','TestReviewedLoreIndex','TestFrameNoops71','TestHudMixins'}<=set(tests)
    for name in tests:assert (args.checks/(name+'-output.txt')).is_file()
    result=(args.checks/'TestNpc83-output.txt').read_text(encoding='utf-8-sig')
    assertions=int(re.search(r'PASS: NPC83 (\d+) assertions',result)[1])
    assert '82 new authored literals/82 published layouts' in result
    style_result=(args.checks/'TestNpcStyles83-output.txt').read_text(encoding='utf-8-sig')
    style_assertions=int(re.search(r'PASS: NPCStyles83 (\d+) assertions',style_result)[1])
    assert '15 independently authored published layouts' in style_result
    benchmark=load(args.benchmark/'RAW.json')
    assert [r['version'] for r in benchmark['records']]==['0.15.82-dev','0.15.83-dev','0.15.83-dev','0.15.82-dev']
    for r in benchmark['records']:assert r['jar_sha256']==(fingerprint if r['version']=='0.15.83-dev' else BASE)
    batch=load(DATA/'npc-expansion83.json')['records'];new={r['source']:r['translation'] for r in batch}
    authored={}
    for line in (SOURCE/'NPC_0.15.83_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if line and not line.startswith('#'):
            f=line.split('|');authored[f'[NPC] {f[0]}: {f[1]}']=f'[NPC] {f[0]}: {f[2]}'
    assert len(new)==len(batch)==82 and new==authored
    evidence=load(SOURCE/'NPC_SOURCES_0.15.83.json')
    assert len(evidence['records'])==87 and len(evidence['pending_variables'])==5 and len(evidence['retained_originals'])==0
    style_batch=load(DATA/'npc-styles83.json')['records'];fixed={r['source']:r['translation'] for r in style_batch}
    assert len(style_batch)==len(fixed)==len(evidence['style_records'])==15
    old_gaps=load(SOURCE/'NPC_STYLE_REMAINING_0.15.82.json')['records'];authored_styles={}
    for line in (SOURCE/'NPC_STYLES_0.15.83_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if line and not line.startswith('#'):
            index,definitions=line.split('|',1);original=old_gaps[int(index)];authored_styles[original['source']]=original['flat_result']
    assert fixed==authored_styles
    for r in evidence['style_records']:assert all(v['translation']==fixed[r['source']] for v in r['original_resources'])
    drafts={}
    for line in (SOURCE/'NPC_0.15.83_VARIABLE_DRAFTS.tsv').read_text(encoding='utf-8-sig').splitlines():
        if line and not line.startswith('#'):
            source,target,status=line.split('|');assert status in ('UNCONFIRMED_TEMPLATE_COVERAGE','UNCONFIRMED_EDITORIAL_CHOICE') and source not in new
            if status=='UNCONFIRMED_TEMPLATE_COVERAGE':assert re.findall(r'\[[^]]+\]|<[^>]+>',source)==re.findall(r'\[[^]]+\]|<[^>]+>',target)
            else:assert 'Select an option:' in source and target.count('[')==source.count('[')==3
            assert source not in drafts;drafts[source]=target
    assert set(drafts)=={r['source'] for r in evidence['pending_variables']}
    previous=load(args.audit/'0.15.82-dev/DISPATCH_SUMMARY.json');current=load(args.audit/'0.15.83-dev/DISPATCH_SUMMARY.json')
    assert previous['runtime_jar_sha256']==BASE and current['runtime_jar_sha256']==fingerprint
    assert previous['tested_literal_rows']==current['tested_literal_rows']==11247
    assert previous['queue_sha256']==current['queue_sha256']==sha(args.corpus/'translation_queue.jsonl')==evidence['queue_sha256']
    old_dispatch={r['source']:r for r in rows(args.audit/'0.15.82-dev/DISPATCH_ROWS.jsonl')}
    now_dispatch={r['source']:r for r in rows(args.audit/'0.15.83-dev/DISPATCH_ROWS.jsonl')}
    assert len(old_dispatch)==len(now_dispatch)==11247
    tested=0
    for source,row in now_dispatch.items():
        if source in new:assert row['flat_result']==row['published_style_result']==new[source],source;tested+=1
        elif source in fixed:
            assert row['flat_result']==row['published_style_result']==old_dispatch[source]['flat_result']==fixed[source],source
            assert old_dispatch[source]['published_style_result']==source
        else:assert all(row[k]==old_dispatch[source][k] for k in ('flat_result','published_style_result')),source
    assert tested==82 and set(fixed)<=now_dispatch.keys()
    remaining=[r for r in now_dispatch.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']]
    assert len(remaining)==72
    with zipfile.ZipFile(baseline) as before,zipfile.ZipFile(jar) as after:
        assert after.testzip() is None
        old_names,new_names=set(before.namelist()),set(after.namelist());assert not old_names-new_names
        changed=sorted(n for n in old_names&new_names if before.read(n)!=after.read(n));added=sorted(new_names-old_names)
        assert changed==['fabric.mod.json','ru/wither/skyru/NpcDialogueTranslator$StyleCorrections.class','ru/wither/skyru/NpcDialogueTranslator.class'],changed
        assert added==['META-INF/licenses/NPC83-SOURCES.txt','data/witherskyru/npc-expansion83.json','data/witherskyru/npc-styles83.json'],added
        assert after.read('data/witherskyru/npc-expansion83.json')==(DATA/'npc-expansion83.json').read_bytes()
        assert after.read('data/witherskyru/npc-styles83.json')==(DATA/'npc-styles83.json').read_bytes()
    stats=load(SOURCE/'NPC_PROGRESS_0.15.83.json');assert stats['exact_resource_after']==2674 and stats['new_resource_sha256']==sha(DATA/'npc-expansion83.json')
    stats.update({'runtime_validation':'138_fresh_groups_passed','fresh_test_groups':138,'npc83_assertions':assertions,'style_assertions':style_assertions,
        'runtime_jar_sha256':fingerprint,'runtime_changed_files':changed,'runtime_added_files':added,
        'old_resources_and_render_classes_byte_identical':True,'outside_selected_dispatch_text_unchanged':True,
        'new_rows_in_conservative_bulk_dispatch':tested,'compatible_published_layouts_checked':82,
        'remaining_original_style_gap':72,'dispatch_before':previous,'dispatch_after':current,'benchmark_summary':benchmark['summary']})
    write(SOURCE/'NPC_PROGRESS_0.15.83.json',stats)
    write(SOURCE/'NPC_STYLE_REMAINING_0.15.83.json',{'scope':'72 remaining older candidates;15 exact published-color repairs checked,not all possible live layouts','records':remaining})
    exact=defaultdict(set)
    for path in sorted(DATA.glob('*.json')):
        for r in load(path).get('records',[]):
            if r.get('source') and r.get('translation'):exact[r['source']].add(r['translation'])
    worklist=rows(SOURCE/'NPC_WORKLIST_0.15.82.jsonl');queue=rows(args.corpus/'translation_queue.jsonl')
    assert len(worklist)==len(queue)==16244 and [r['source'] for r in worklist]==[r['source'] for r in queue]
    per_speaker=defaultdict(list)
    for r in worklist:
        source=r['source'];r['exact_registration']=bool(exact.get(source));r['registered_targets_not_automatically_certified']=sorted(exact.get(source,set()))
        if source in new:r['review_status']='authored_and_fixture_checked83';r['published_layout_fixture_checked83']=True
        if source in fixed:r['alternate_published_layout_fixture_checked83']=True
        if source in drafts:r['structural_draft83']=True;r['template_coverage_certified83']=False
        if source in now_dispatch:
            r['runtime_flat_snapshot_not_quality_proof']=now_dispatch[source]['flat_result'];r['runtime_colored_snapshot_not_quality_proof']=now_dispatch[source]['published_style_result']
        per_speaker[r['speaker'] or '(без указанного говорящего)'].append(r)
    assert sum(r['exact_registration'] for r in worklist)==2674
    (SOURCE/'NPC_WORKLIST_0.15.83.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in worklist),encoding='utf-8')
    table=['# Вся очередь NPC — 0.15.83','','Публичный корпус, не реестр действующих NPC. Точные ключи не означают полной проверки качества; часть остальных строк может переводиться динамически.','',
        '| Обозначение | Всего | Точных ключей | Доля | Новые тексты83 | Цветных кандидатов | Переменных по исходной разметке |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    gaps={r['source'] for r in remaining}
    for speaker,values in sorted(per_speaker.items()):
        count=sum(r['exact_registration'] for r in values);safe=html.escape(speaker).replace('|','\\|')
        table.append(f'| {safe} | {len(values)} | {count} | {100*count/len(values):.2f}% | {sum(r["source"] in new for r in values)} | {sum(r["source"] in gaps for r in values)} | {sum(bool(r["variable_definition"]) for r in values)} |')
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.83_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    report=['# Перевод NPC — 0.15.83','','`████░░░░░░░░░░░░░░░░░░░░░` 16,46% точных ключей (полоса округлена).','',
        '| Учёт публичной базы | Строк | Доля |','| --- | ---: | ---: |','| Зарегистрировано | 2 674 | 16,46% |','| Без подтверждённого точного ключа | 13 570 | 83,54% |','| Всего | 16 244 | 100% |','',
        'Это не процент сертифицированной всей игры. Исторические, нейтральные, редакторские и динамические строки учитываются отдельно. Партия включает NPC-реплики, а не полный перевод всех связанных вариантов ответов и системных уведомлений без NPC-префикса.','',
        '| Изменение | Строк |','| --- | ---: |','| Новые реплики Одинокого искателя приключений | 82 |','| Исправлены опубликованные цветные варианты Джульетты | 15 |','| Имена игроков остаются структурными черновиками | 4 |','| NPC-префиксная строка выбора не подтверждена как реальный пакет | 1 |','',
        f'Все 138 групп прошли заново на одном JAR; новая ветка: {assertions:,} проверок, цветные исправления: {style_assertions:,}. Все 82 новые опубликованные схемы и 15 прежних выделений проверены. Старые русские цели и планы сохранены. Черновики не подтверждают работу всех динамических вариантов.','',
        'Массовый проход тех же 11 247 строк: обычный текст 2555→2637, цветной 2468→2565. Все 82 новые строки и 15 цветных исправлений сработали. Вне этой партии оба результата дословно совпали. Прежних цветных кандидатов осталось 72 из 87.','',
        'Все старые ресурсы, классы отрисовки/HUD/F3 и прочие классы побайтово совпали с 82. Изменены только версия, список NPC-ресурсов и загрузка двух таблиц цветных исправлений один раз при запуске. Нового покадрового прохода или сетевых вызовов в игре нет. Это не обещание нулевой нагрузки. FPS и 0,1% lows в игре не измерены.','',
        '| Обработка входящего чата | Изменение времени, два порядка | Байты/сообщение до→после |','| --- | ---: | ---: |']
    for r in benchmark['summary']:
        times=' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in r['orders']);memory=' / '.join(f'{o["before_bytes"]:.0f}→{o["after_bytes"]:.0f}' for o in r['orders'])
        report.append(f'| {r["mode"]} | {times} | {memory} |')
    report+=['','Четыре отдельные JVM 82→83→83→82; все раунды сохранены. Новые положительные сценарии делают дополнительный перевод, поэтому работа не равна. Это не замер FPS.','',
        '## Ограничения тестового JAR','',
        'Полный перевод не завершён. Незавершённый аудит ReviewedLoreStyles (включая подозрительное исходное значение «9sф», формулировки Wither Skeleton, Magma Cube, Dedication и Golem) остаётся открытым. Зелёные проверки не заменяют смысловую вычитку. Игра не изменялась, JAR не установлен автоматически. Старые сборки сохранены.','',
        f'SHA-256 JAR: `{fingerprint}`.','',
        'Источники: [Одинокий искатель приключений](https://hypixelskyblock.minecraft.wiki/w/Lone_Adventurer?oldid=823127), [Джульетта](https://hypixelskyblock.minecraft.wiki/w/Juliette?oldid=789285). API 16 октября подтвердил совпадение двух последних опубликованных ревизий с базой. Это не закрытые серверные скрипты.']
    (SOURCE/'NPC_PROGRESS_0.15.83_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
    destination.mkdir(parents=True)
    files=[p for p in SOURCE.glob('*0.15.83*') if p.is_file()]
    files += [DATA/'npc-expansion83.json',DATA/'npc-styles83.json',ROOT/'src/main/resources/META-INF/licenses/NPC83-SOURCES.txt',ROOT/'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java',ROOT/'src/main/resources/fabric.mod.json',ROOT/'build.gradle']
    files += [ROOT/'tests'/n for n in ('TestNpc83.java','TestNpcStyles83.java','BenchmarkNpc83.java','AuditNpcCorpus77.java','TestHubNpcs69.java','TestNpc70.java','TestNpc74.java','TestNpc75.java','TestNpc76.java','TestNpc77.java','TestNpc78.java','TestNpc79.java','TestNpc80.java','TestNpc81.java','TestNpc82.java','TestStoryDialogues.java')]
    files += [ROOT/'tools'/n for n in ('prepare_npc78.py','prepare_npc83.py','check_npc83.ps1','dispatch_npc83.ps1','benchmark_npc83.ps1','finalize_npc83_progress.py')]
    for p in files:
        target=destination/p.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,target)
    (destination/'checks').mkdir()
    for name in tests:shutil.copy2(args.checks/(name+'-output.txt'),destination/'checks')
    shutil.copy2(args.checks/'CHECK_SCOPE.json',destination/'checks');shutil.copy2(args.benchmark/'RAW.json',destination/'BENCHMARK_RAW.json')
    for version in ('0.15.82-dev','0.15.83-dev'):
        target=destination/'dispatch'/version;target.mkdir(parents=True);shutil.copy2(args.audit/version/'DISPATCH_SUMMARY.json',target)
    (destination/'README_RU.md').write_text('# Партия 83\n\n82 новые реплики, 15 цветных исправлений, 138 свежих групп проверок. 2 674/16 244 точных ключей (16,46%).\n\n[Прогресс, проверки, ограничения](translation-source/NPC_PROGRESS_0.15.83_RU.md)\n\n[Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.83_RU.md)\n\nЭто частичный пакет исходников и отчётов, не полный проект. Тестовый JAR выдаётся отдельно, не устанавливается автоматически.\n',encoding='utf-8')
    hashes={p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()};write(destination/'HASHES.json',hashes)
    archive=destination/'NPC_TRANSLATION_BATCH_07.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for name in sorted(hashes):z.write(destination/name,name)
        z.write(destination/'HASHES.json','HASHES.json')
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        for name,expected in hashes.items():assert hashlib.sha256(z.read(name)).hexdigest().upper()==expected
        entries=len(z.namelist())
    delivered_jar=jar_directory/jar.name
    if delivered_jar.exists():assert sha(delivered_jar)==fingerprint,'Different existing JAR;not replacing'
    else:shutil.copy2(jar,delivered_jar)
    assert sha(delivered_jar)==fingerprint
    write(destination/'DELIVERY.json',{'archive':str(archive),'sha256':sha(archive),'bytes':archive.stat().st_size,'entries':entries,
        'jar':str(delivered_jar),'jar_sha256':fingerprint,'installed_game_modified':False})
    print(json.dumps(load(destination/'DELIVERY.json'),ensure_ascii=True,indent=2))

if __name__=='__main__':main()
