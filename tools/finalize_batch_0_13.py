"""Source-only delivery reports. No compilation, runtime, server or installation."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil

PROJECT=Path(__file__).resolve().parents[1]
SOURCE=PROJECT/'translation-source'
VERSION='0.13.0-dev'

def read(name):return json.loads((SOURCE/name).read_text(encoding='utf-8'))
def write(path,data):path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest().upper()
def rows(path):return sum(bool(s.strip()) and not s.startswith('#') for s in path.read_text(encoding='utf-8').splitlines())

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--output-dir',type=Path,required=True)
    args=parser.parse_args()
    output=args.output_dir.resolve()
    assert output==PROJECT.parents[2]/'outputs'
    output.mkdir(parents=True,exist_ok=True)
    coverage,progress,derivation,checks=map(read,['coverage.json','progress-coverage.json','BATCH_0.13_DERIVATION.json','BATCH_0.13_DATA_CHECKS.json'])
    assert progress['build']==VERSION and progress['known_union_changed_or_prepared_delta']==checks['new_snapshot_sources']>=2000
    assert not checks['numeric_roman_icon_placeholder_id_mismatches'] and not checks['malformed_rows'] and not checks['regressed_previously_processed_sources']
    assert not (PROJECT/f'build/libs/WitherSkyRU-26.1.2-fabric-{VERSION}.jar').exists()
    previous_jar=output/'WitherSkyRU-26.1.2-fabric-0.12.0-dev.jar'
    previous_hash=sha(previous_jar)
    assert previous_hash=='5E5210E64E1E13220438877EC2D8F3E045AEFB406E571C40266FDEE81645E498'
    installed=Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/mods/WitherSkyRU-26.1.2-fabric-0.3.0-dev.jar')
    installed_hash=sha(installed)
    assert installed_hash=='1789BA5CB33B2FB965057DCDEDD6D9900862391230494B386DC37474728FCC1A'
    counts={p.name:rows(p) for p in SOURCE.glob('BATCH_0.13_*.tsv')}
    continuation_count=rows(PROJECT/'src/main/resources/data/witherskyru/continuation.tsv')
    assert continuation_count==10340+derivation['imported_rows']
    qa={'build':VERSION,'delivery_status':'SOURCE_ONLY - no new JAR, no installation or runtime tests',
        'jar_built_this_batch':False,'compilation':'NOT PERFORMED','development_jar_sha256':None,
        'target':'Minecraft 26.1.2 / Fabric / Java 25',
        'compiled_against_real_libraries':None,'compiler_stubs_used':False,
        'prepared_dictionary_entries':coverage['runtime_dictionary_entries'],'identity_entries':coverage['runtime_identity_entries'],
        'numbered_templates':coverage['runtime_templates'],'snapshot_unique_strings':coverage['snapshot_strings'],
        'snapshot_translated_or_partially_translated_strings':coverage['translated_or_partially_translated_snapshot_strings'],
        'snapshot_remaining_strings':coverage['remaining_snapshot_strings'],
        'new_snapshot_sources':checks['new_snapshot_sources'],
        'previous_targets_updated_not_counted_as_new_sources':checks['previous_targets_updated'],
        'authored_rows':counts,'continuation_resource_rows_added':derivation['imported_rows'],
        'derivation_categories':derivation['categories'],'additional_runtime_templates':0,
        'static_data_checks':'BATCH_0.13_DATA_CHECKS.json - passed; not universal quality verification',
        'roman_word_exceptions':'Exact reviewed English pronoun I, French C\' elision and LVL abbreviation; genuine Roman levels preserved',
        'jar_crc_check':'NOT APPLICABLE - no current JAR','jar_source_resource_byte_comparison':'NOT PERFORMED',
        'declared_mixin_class_presence':'NOT PERFORMED this batch',
        'npc_sample_prepared_translations':21,'npc_chat_handler_implemented':True,'npc_chat_runtime_verified':False,
        'new_npc_chat_replicas':0,'npc_reply_choice_translation_implemented':False,
        'npc_unknown_dialogue_recording_implemented':False,
        'combined_prepared_coverage':'progress-coverage.json - known snapshot union NPC sample, not runtime display coverage',
        'java_mixin_code_changed_this_batch':False,'catalogue_generator_changed_this_batch':False,
        'runtime_assertions_passed':None,'mixin_classes_applied':None,
        'fabric_knot_smoke_test':'NOT PERFORMED','full_pack_transformation_and_dictionary_test':'NOT PERFORMED',
        'full_game_launch':'NOT PERFORMED','hypixel_connection':'NOT PERFORMED',
        'installed_into_active_modpack':False,'active_installed_version':'0.3.0-dev',
        'active_installed_jar_sha256':installed_hash,'active_installed_hash_unchanged':True,
        'last_compiled_development_version':'0.12.0-dev','last_compiled_development_jar_sha256':previous_hash,
        'last_compiled_development_jar_contains_this_batch':False,'previous_build_qa':'translation-source/QA_0.12.0_HISTORY.json',
        'guaranteed_all_hypixel_text':False,'quality_verified':False,'runtime_verified':False,
        'context_limit':'Separate source fragments/components. Selected full item contexts checked; not all 8940 descriptions reviewed. Partial translations counted.',
        'unknown_strings':'Preserve originals, commands, opaque names and mirrored riddles; no fabricated NPC branches'}
    write(PROJECT/'QA_REPORT.json',qa)
    manifest={'revision':VERSION,'delivery':'SOURCE_ONLY','date':'2026-10-10','previous_revision':'0.12.0-dev',
        'requested_batch_minimum':2000,'new_changed_snapshot_sources':checks['new_snapshot_sources'],
        'previous_targets_updated_not_in_delta':checks['previous_targets_updated'],
        'authored_rows':counts,'direct_authored_rows_imported':derivation['categories']['authored exact source'],
        'observed_numeric_variants_generated':derivation['categories']['literal numeric lore observed in snapshot'],
        'previous_continuation_rows':10340,'current_continuation_rows':continuation_count,
        'continuation_resource_rows_added':derivation['imported_rows'],'runtime_templates_added':0,
        'known_union_unique_strings':progress['known_union_unique_strings'],
        'known_union_processed_or_prepared':progress['known_union_changed_or_prepared_strings'],
        'known_union_prepared_percent':progress['known_union_changed_or_prepared_percent_rounded'],
        'known_union_remaining':progress['known_union_remaining_strings'],
        'caption_derivation_report':'BATCH_0.13_DERIVATION.json','static_data_checks':'BATCH_0.13_DATA_CHECKS.json',
        'source_snapshot':'corpus.jsonl','item_context_source':'item-context.json',
        'npc_sample_count_unchanged':21,'new_npc_chat_replicas':0,'jar_built':False,'jar_sha256':None,
        'runtime_tests_run':False,'game_launched':False,'server_connected':False,'installed_mod_changed':False,
        'full_server_coverage':False,'quality_verified':False,
        'note':'Changed/prepared full or partial source strings; not independently authored sentences or all live SkyBlock text'}
    write(SOURCE/'BATCH_0.13.0_MANIFEST.json',manifest)
    report='''# WitherSkyRU 0.13.0-dev

Добавлено **2153 новые строки**; 6 прежних уточнены без повторного счёта.
Бонусы брони, способности, эффекты, ограничения, расходники и мешки,
косметические облики, задания Разлома, подсказки душ Энигмы,
заголовки перековок и описания самоцветов.

🟩🟩🟩🟩🟩🟩🟩🟩⬜⬜ **82,51%**

| Известная база | Строк |
|---|---:|
| Подготовлено, включая частичный перевод | 27 349 |
| Осталось | 5 798 |
| Всего | 33 147 |

Без латиницы: 25 813 (77,87%). С латинскими терминами: 1536 (4,63%).
Остаток: 17,49%. Прирост: +6,50 процентного пункта к 0.12.0.
Полоса приблизительная; точная доля указана рядом.
Это известный локальный снимок и 21 прежняя NPC-реплика, не весь живой
SkyBlock. Частичный перевод включён; отсутствие латиницы не доказывает
качество и полноту. Новых реплик NPC-чата в этой партии нет.

Сохранены ручные таблицы, каталог и происхождение переводов, оставшиеся
строки, контекст, дорожная карта и история предыдущих версий.
Числа, уровни, значки, переменные и ID статически сверены; новых дублей
и регрессий счётчика нет. Проверка отдельных соседних строк не заменяет
проверку качества всех текстов или их отображения в игре.

**Только исходники:** JAR 0.13.0 не собран. Последний собранный JAR 0.12.0
сохранён, но переводов этой партии не содержит. В установленной сборке
остаётся 0.3.0-dev; её файл сверён по SHA-256 и не менялся.
Minecraft, Java/Fabric и Hypixel не запускались, игровых тестов не было.

Архив: WitherSkyRU_0.13.0-dev_MC26.1.2_project.zip.
Копия доставки: Desktop\\WitherSkyRU\\translation-batch-0.13.0.
'''
    (SOURCE/'BATCH_0.13.0_RU.md').write_text(report,encoding='utf-8')
    (output/'ПРОГРЕСС_WITHERSKYRU_0.13.0.md').write_text(report,encoding='utf-8')
    mapping={PROJECT/'QA_REPORT.json':'WitherSkyRU_0.13.0_QA_REPORT.json',
        SOURCE/'CONTEXT_RU.md':'WitherSkyRU_0.13.0_CONTEXT_RU.md',
        SOURCE/'ROADMAP_RU.md':'WitherSkyRU_0.13.0_ROADMAP_RU.md',
        SOURCE/'coverage.json':'WitherSkyRU_0.13.0_COVERAGE.json',
        SOURCE/'progress-coverage.json':'WitherSkyRU_0.13.0_PREPARED_PROGRESS.json',
        SOURCE/'translations.json':'WitherSkyRU_0.13.0_TRANSLATIONS.json',
        SOURCE/'untranslated-backlog.tsv':'WitherSkyRU_0.13.0_UNTRANSLATED_BACKLOG.tsv',
        SOURCE/'BATCH_0.13_DERIVATION.json':'WitherSkyRU_0.13.0_DERIVATION.json',
        SOURCE/'BATCH_0.13_DATA_CHECKS.json':'WitherSkyRU_0.13.0_DATA_CHECKS.json',
        SOURCE/'BATCH_0.13.0_MANIFEST.json':'WitherSkyRU_0.13.0_MANIFEST.json'}
    for table in SOURCE.glob('BATCH_0.13_*.tsv'):
        mapping[table]='WitherSkyRU_0.13.0_'+table.name.replace('BATCH_0.13_','')
    for src,name in mapping.items():shutil.copy2(src,output/name)
    shutil.copy2(SOURCE/'ROADMAP_RU.md',output/'ПЛАН_ПЕРЕВОДА_WITHERSKYRU.md')
    print(json.dumps({'source_only':True,'imported_rows':derivation['imported_rows'],'new_sources':checks['new_snapshot_sources'],'previous_targets_updated':checks['previous_targets_updated'],'output_files':len(mapping)+2,'installed_unchanged':True,'last_compiled_jar_unchanged':True},ensure_ascii=False))

if __name__=='__main__':main()
