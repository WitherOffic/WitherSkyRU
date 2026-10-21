"""Save source-only delivery, evidence and compact progress. Never compiles/starts Java."""
import argparse,hashlib,json,shutil
from pathlib import Path
P=Path(__file__).resolve().parents[1];S=P/'translation-source';V='0.14.0-dev'
def read(n):return json.loads((S/n).read_text(encoding='utf-8'))
def write(p,o):p.write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def count(p):return sum(bool(l.strip()) and not l.startswith('#') for l in p.read_text(encoding='utf-8').splitlines())
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--output-dir',type=Path,required=True);a=parser.parse_args()
    out=a.output_dir.resolve();assert out==P.parents[2]/'outputs';out.mkdir(parents=True,exist_ok=True)
    c,g,d,t,r=map(read,['coverage.json','progress-coverage.json','BATCH_0.14_DERIVATION.json','BATCH_0.14_DATA_CHECKS.json','BATCH_0.14_REMAINING_REVIEW.json'])
    assert g['build']==V and g['known_union_changed_or_prepared_delta']==t['new_snapshot_sources']==d['imported_rows']
    assert not t['numeric_roman_icon_placeholder_id_mismatches'] and not t['malformed_rows'] and not t['regressed_previously_processed_sources']
    assert r['unchanged_sources']==g['known_union_remaining_strings']
    assert not (P/f'build/libs/WitherSkyRU-26.1.2-fabric-{V}.jar').exists()
    oldjar=out/'WitherSkyRU-26.1.2-fabric-0.12.0-dev.jar';oldhash=sha(oldjar)
    installed=Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/mods/WitherSkyRU-26.1.2-fabric-0.3.0-dev.jar');ihash=sha(installed) if installed.exists() else None
    assert oldhash=='5E5210E64E1E13220438877EC2D8F3E045AEFB406E571C40266FDEE81645E498'
    prior_installed_hash='1789BA5CB33B2FB965057DCDEDD6D9900862391230494B386DC37474728FCC1A'
    authored={p.name:count(p) for p in sorted(S.glob('BATCH_0.14_*.tsv'))}
    n=count(P/'src/main/resources/data/witherskyru/continuation.tsv');assert n==12487+d['imported_rows']
    qa=read('QA_0.13.0_HISTORY.json')
    qa.update(build=V,delivery_status='SOURCE_ONLY - no current JAR, installation or runtime tests',
        jar_built_this_batch=False,compilation='NOT PERFORMED',development_jar_sha256=None,
        prepared_dictionary_entries=c['runtime_dictionary_entries'],identity_entries=c['runtime_identity_entries'],
        numbered_templates=c['runtime_templates'],snapshot_unique_strings=c['snapshot_strings'],
        snapshot_translated_or_partially_translated_strings=c['translated_or_partially_translated_snapshot_strings'],
        snapshot_remaining_strings=c['remaining_snapshot_strings'],new_snapshot_sources=t['new_snapshot_sources'],
        previous_targets_updated_not_counted_as_new_sources=t['previous_targets_updated'],authored_rows=authored,
        continuation_resource_rows_added=d['imported_rows'],derivation_categories=d['categories'],
        static_data_checks='BATCH_0.14_DATA_CHECKS.json - passed; not universal quality verification',
        roman_word_exceptions='Exact reviewed pronoun I, French elision C, LVL and master-mode MM; genuine levels unchanged',
        remaining_classification=r['categories'],remaining_labels_verified=False,full_remaining_translation_completed=False,
        previous_build_qa='translation-source/QA_0.13.0_HISTORY.json',
        last_compiled_development_version='0.12.0-dev',last_compiled_development_jar_sha256=oldhash,
        last_compiled_development_jar_contains_this_batch=False,active_installed_jar_sha256=ihash,
        active_installed_hash_unchanged=(ihash==prior_installed_hash) if ihash else None,
        active_installed_version='0.3.0-dev' if ihash else None,
        active_installed_jar_present=installed.exists(),
        installed_state_note='Previously installed path absent at delivery; no WitherSkyRU JAR found by read-only instance scan. No game directory writes performed.' if not ihash else 'Previous path found; hash compared.',
        java_mixin_code_changed_this_batch=False,
        runtime_verified=False,quality_verified=False,new_npc_chat_replicas=0,
        unknown_strings='603 retained originals; triage does not count them as translated or alter denominator',
        context_limit='Separate text fragments. Selected full descriptions and all new skin-prefix continuations examined. Not all 8940 item descriptions quality-verified.')
    write(P/'QA_REPORT.json',qa)
    manifest={'revision':V,'previous_revision':'0.13.0-dev','date':'2026-10-10','delivery':'SOURCE_ONLY',
        'requested_scope':'Finish remaining translation in one request; unresolved original labels explicitly retained',
        'new_changed_snapshot_sources':t['new_snapshot_sources'],'previous_targets_updated_not_in_delta':t['previous_targets_updated'],
        'authored_rows':authored,'previous_continuation_rows':12487,'current_continuation_rows':n,
        'continuation_resource_rows_added':d['imported_rows'],'derivation_categories':d['categories'],
        'runtime_templates_added':0,'known_union_unique_strings':g['known_union_unique_strings'],
        'known_union_processed_or_prepared':g['known_union_changed_or_prepared_strings'],
        'known_union_prepared_percent':g['known_union_changed_or_prepared_percent_rounded'],
        'known_union_remaining':g['known_union_remaining_strings'],'remaining_categories':r['categories'],
        'remaining_candidates_not_verified':True,'all_remaining_text_translated':False,
        'partial_translation_included_in_progress':True,'source_snapshot':'corpus.jsonl','item_context_source':'item-context.json',
        'static_data_checks':'BATCH_0.14_DATA_CHECKS.json','derivation_report':'BATCH_0.14_DERIVATION.json',
        'npc_sample_count_unchanged':21,'new_npc_chat_replicas':0,'jar_built':False,'jar_sha256':None,
        'runtime_tests_run':False,'game_launched':False,'server_connected':False,'installed_mod_changed':False,
        'full_server_coverage':False,'quality_verified':False}
    write(S/'BATCH_0.14.0_MANIFEST.json',manifest)
    report='''# WitherSkyRU 0.14.0-dev — большой завершающий проход

За запрос добавлено **5195 новых строк**. 4 прежних перевода уточнены,
без повторного включения в прирост. Импорт: 4929 прямых архивных соответствий
и 266 закрытых производных, только для существующих строк снимка.

🟩🟩🟩🟩🟩🟩🟩🟩🟩🟨 **98,18%**

| Известная база | Строк |
|---|---:|
| Подготовлено, включая частичный перевод | 32 544 |
| Оставлено исходным | 603 |
| Всего | 33 147 |

Без латиницы: 30 670 (92,53%). С латинскими терминами: 1874 (5,65%).
Прирост: +15,67 процентного пункта. Полоса приблизительная.

Это **не 100% перевода всего SkyBlock** и не подтверждение качества всех
описаний. Показатель включает частичные переводы локального снимка и
21 прежнюю полную NPC-реплику; новых NPC-диалогов не добавлено.

Остаток: 164 технических шаблона/идентификатора, 286 кандидатов на собственные
имена (требуют проверки контекста), 91 неоднозначная подпись, 5 идентификаторов
сущностей с неясным назначением, 21 зеркальная загадка, 17 строк ников,
16 названий брендов, 3 числовых значения без необходимости перевода.
Кандидаты не объявлены проверенными именами. Весь остаток сохранён;
он не вычтен из знаменателя и не зачтён как готовый перевод.

Дополнены условия и окончания описаний, эффекты перековок, задания Разлома,
существа, предметы, облики, подписи статистики и пояснения настроек.
Числа, уровни, значки, переменные, команды, ID и коды цветов статически
сверены. Новых дублей, повреждённых записей и регрессий счётчика нет.
Выборочная контекстная проверка: мокрая книга не названа мокрокрылым;
lead у Kada Lead — поводок, не свинец; знак -20% не удваивает отрицание.
Термины силы исцеления, удачи лесорубства и избыточного цветения согласованы
с прежним словарём. Это не замена пользовательской проверки в игре.

**Только исходники.** Новый JAR не собран. Последний JAR 0.12.0-dev
не содержит эту партию и сверён по SHA-256: он не изменён. Прежний
установленный JAR 0.3.0-dev по ожидаемому пути сейчас отсутствует;
в папках экземпляров найден только его кэш, не сам мод. Поэтому текущую
установку и её хеш подтвердить нельзя. Папки игры в этой партии не менялись.
Minecraft, Java/Fabric
и Hypixel не запускались. Java/Mixin и NPC-обработчик не изменены.

Архив: WitherSkyRU_0.14.0-dev_MC26.1.2_project.zip.
Копия: Desktop\\WitherSkyRU\\translation-batch-0.14.0.
Дальше: проверить оставшиеся собственные имена/подписи, согласование всех
многострочных описаний и расширить подтверждённый NPC-корпус. Полнота HUD,
Tab, правой панели, голограмм и NPC-чата отдельно не подтверждена.
'''
    (S/'BATCH_0.14.0_RU.md').write_text(report,encoding='utf-8')
    (out/'ПРОГРЕСС_WITHERSKYRU_0.14.0.md').write_text(report,encoding='utf-8')
    mapping={P/'QA_REPORT.json':'WitherSkyRU_0.14.0_QA_REPORT.json'}
    for name,suffix in [('CONTEXT_RU.md','CONTEXT_RU.md'),('ROADMAP_RU.md','ROADMAP_RU.md'),
                        ('coverage.json','COVERAGE.json'),('progress-coverage.json','PREPARED_PROGRESS.json'),
                        ('translations.json','TRANSLATIONS.json'),('untranslated-backlog.tsv','UNTRANSLATED_BACKLOG.tsv'),
                        ('translation-provenance.jsonl','PROVENANCE.jsonl'),('BATCH_0.14_DERIVATION.json','DERIVATION.json'),
                        ('BATCH_0.14_DATA_CHECKS.json','DATA_CHECKS.json'),('BATCH_0.14.0_MANIFEST.json','MANIFEST.json'),
                        ('BATCH_0.14_REMAINING_REVIEW.json','REMAINING_REVIEW.json'),('REMAINING_0.14_REVIEW.tsv','REMAINING_REVIEW.tsv')]:
        mapping[S/name]='WitherSkyRU_0.14.0_'+suffix
    for p in S.glob('BATCH_0.14_*.tsv'):mapping[p]='WitherSkyRU_0.14.0_'+p.name.replace('BATCH_0.14_','')
    for src,name in mapping.items():shutil.copy2(src,out/name)
    shutil.copy2(S/'ROADMAP_RU.md',out/'ПЛАН_ПЕРЕВОДА_WITHERSKYRU.md')
    print(json.dumps({'new_sources':t['new_snapshot_sources'],'remaining':r['unchanged_sources'],'delivery_files':len(mapping)+2,
                      'source_only':True,'game_directory_modified':False,'expected_installed_jar_present':installed.exists(),'last_compiled_jar_unchanged':True},ensure_ascii=False))
if __name__=='__main__':main()
