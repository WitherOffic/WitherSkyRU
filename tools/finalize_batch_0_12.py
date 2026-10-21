"""Create accurate delivery reports from checked data and an already-built JAR.

No compilation, game, loader test, network or installation is performed here.
"""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

PROJECT=Path(__file__).resolve().parents[1]
SOURCE=PROJECT/'translation-source'
VERSION='0.12.0-dev'

def read(name):
    return json.loads((SOURCE/name).read_text(encoding='utf-8'))

def write(path,data):
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def row_count(path):
    return sum(bool(s.strip()) and not s.startswith('#') for s in path.read_text(encoding='utf-8').splitlines())

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--output-dir',type=Path,required=True)
    args=parser.parse_args()
    output=args.output_dir.resolve()
    assert output==PROJECT.parents[2]/'outputs'
    output.mkdir(parents=True,exist_ok=True)
    coverage,progress,derivation,checks=map(read,['coverage.json','progress-coverage.json','BATCH_0.12_DERIVATION.json','BATCH_0.12_DATA_CHECKS.json'])
    assert progress['build']==VERSION and progress['known_union_changed_or_prepared_delta']==checks['new_snapshot_sources']>=2000
    assert not checks['numeric_roman_icon_placeholder_id_mismatches'] and not checks['malformed_rows'] and not checks['regressed_previously_processed_sources']
    jar=PROJECT/f'build/libs/WitherSkyRU-26.1.2-fabric-{VERSION}.jar'
    with zipfile.ZipFile(jar) as archive:
        assert archive.testzip() is None
        metadata=json.loads(archive.read('fabric.mod.json'))
        assert metadata['version']==VERSION and metadata['depends']['minecraft']=='26.1.2'
        resources=PROJECT/'src/main/resources'
        verified=[]
        for path in resources.rglob('*'):
            if path.is_file():
                key=path.relative_to(resources).as_posix()
                assert archive.read(key)==path.read_bytes(),key
                verified.append(key)
        classes=[name for name in archive.namelist() if name.endswith('.class')]
        assert classes and all(int.from_bytes(archive.read(name)[6:8],'big')==69 for name in classes)
        for name in metadata['mixins']:
            config=json.loads(archive.read(name))
            assert all((config['package']+'.'+m).replace('.','/')+'.class' in classes for m in config.get('mixins',[])+config.get('client',[]))
    installed=Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/mods/WitherSkyRU-26.1.2-fabric-0.3.0-dev.jar')
    installed_hash=hashlib.sha256(installed.read_bytes()).hexdigest().upper()
    assert installed_hash=='1789BA5CB33B2FB965057DCDEDD6D9900862391230494B386DC37474728FCC1A'
    jar_hash=hashlib.sha256(jar.read_bytes()).hexdigest().upper()
    counts={path.name:row_count(path) for path in SOURCE.glob('BATCH_0.12_*.tsv')}
    qa={
        'build':VERSION,'delivery_status':'COMPILED_JAR_AND_SOURCES - not installed or runtime tested',
        'jar_built_this_batch':True,'explicit_build_request':'И собери jar',
        'compilation':'SUCCESS - build-local.ps1, javac --release 25, real locally installed libraries',
        'target':'Minecraft 26.1.2 / Fabric Loader 0.19.5 / Java 25.0.1',
        'compiled_against_real_libraries':True,'compiler_stubs_used':False,
        'prepared_dictionary_entries':coverage['runtime_dictionary_entries'],
        'identity_entries':coverage['runtime_identity_entries'],'numbered_templates':coverage['runtime_templates'],
        'snapshot_unique_strings':coverage['snapshot_strings'],
        'snapshot_translated_or_partially_translated_strings':coverage['translated_or_partially_translated_snapshot_strings'],
        'snapshot_remaining_strings':coverage['remaining_snapshot_strings'],
        'changed_strings_with_latin_terms_excluding_roman_levels_and_placeholders':coverage['translated_strings_with_remaining_latin_terms'],
        'new_snapshot_sources':checks['new_snapshot_sources'],'previous_targets_updated_not_counted_as_new_sources':checks['previous_targets_updated'],
        'authored_rows':counts,'continuation_resource_rows_added':derivation['imported_rows'],
        'derivation_categories':derivation['categories'],'additional_runtime_templates':0,
        'static_data_checks':'BATCH_0.12_DATA_CHECKS.json - passed; not universal translation quality verification',
        'non_level_abbreviation_expanded':'MM in the exact Health (MM) heading means Master Mode, not level 2000',
        'jar_bytes':jar.stat().st_size,'development_jar_sha256':jar_hash,
        'jar_crc_check':'PASS','jar_source_resource_byte_comparison':'PASS',
        'verified_jar_resource_files':verified,'jar_class_files':len(classes),'class_major_version':69,
        'declared_mixin_class_presence':'PASS - class files present, application NOT tested',
        'npc_sample_prepared_translations':21,'npc_chat_handler_implemented':True,
        'npc_chat_runtime_verified':False,'new_npc_chat_replicas':0,
        'npc_reply_choice_translation_implemented':False,'npc_unknown_dialogue_recording_implemented':False,
        'combined_prepared_coverage':'progress-coverage.json - known snapshot union NPC sample, not runtime display coverage',
        'java_mixin_code_changed_this_batch':False,'catalogue_generator_changed_this_batch':False,
        'build_script_changed':'Fresh separate output directory for each compile; old builds preserved',
        'runtime_assertions_passed':None,'mixin_classes_applied':None,
        'fabric_knot_smoke_test':'NOT PERFORMED','full_pack_transformation_and_dictionary_test':'NOT PERFORMED',
        'full_game_launch':'NOT PERFORMED','hypixel_connection':'NOT PERFORMED',
        'installed_into_active_modpack':False,'active_installed_version':'0.3.0-dev',
        'active_installed_jar_sha256':installed_hash,'active_installed_hash_unchanged':True,
        'last_compiled_development_version':VERSION,'last_compiled_development_jar_sha256':jar_hash,
        'previous_build_qa':'translation-source/QA_0.11.0_HISTORY.json',
        'guaranteed_all_hypixel_text':False,'quality_verified':False,'runtime_verified':False,
        'context_limit':'Dictionary matches separate source lines/components. Shared portal fragments adjusted explicitly; unknown mirror riddles preserved.',
        'unknown_strings':'Preserve originals; do not fabricate NPC branches or translate commands/identifiers to inflate counts'
    }
    write(PROJECT/'QA_REPORT.json',qa)
    manifest={
        'revision':VERSION,'delivery':'COMPILED_JAR_AND_SOURCES','date':'2026-10-09',
        'previous_revision':'0.11.0-dev','requested_batch_minimum':2000,
        'new_changed_snapshot_sources':checks['new_snapshot_sources'],
        'previous_targets_updated_not_in_delta':checks['previous_targets_updated'],
        'authored_rows':counts,'direct_authored_rows_imported':derivation['categories']['authored exact caption or lore'],
        'observed_numeric_variants_generated':derivation['categories']['authored numeric lore observed in snapshot'],
        'previous_continuation_rows':8009,
        'current_continuation_rows':row_count(PROJECT/'src/main/resources/data/witherskyru/continuation.tsv'),
        'continuation_resource_rows_added':derivation['imported_rows'],
        'previous_continuation_template_rows':400,'current_continuation_template_rows':400,'runtime_templates_added':0,
        'known_union_unique_strings':progress['known_union_unique_strings'],
        'known_union_processed_or_prepared':progress['known_union_changed_or_prepared_strings'],
        'known_union_prepared_percent':progress['known_union_changed_or_prepared_percent_rounded'],
        'known_union_remaining':progress['known_union_remaining_strings'],
        'caption_derivation_report':'BATCH_0.12_DERIVATION.json','static_data_checks':'BATCH_0.12_DATA_CHECKS.json',
        'source_snapshot':'corpus.jsonl','item_context_source':'item-context.json',
        'npc_sample_count_unchanged':21,'new_npc_chat_replicas':0,
        'jar_built':True,'jar_sha256':jar_hash,'runtime_tests_run':False,
        'game_launched':False,'server_connected':False,'installed_mod_changed':False,
        'full_server_coverage':False,'quality_verified':False,
        'note':'Newly changed full/partial source strings, including observed numeric variants; not independently authored sentences or all live SkyBlock text'
    }
    write(SOURCE/'BATCH_0.12.0_MANIFEST.json',manifest)
    report='''# WitherSkyRU 0.12.0-dev

Добавлено **2156 новых строк**; 164 прежних перевода изменены без повторного
включения в прирост. Предметы и мебель, меню статистики/коллекций/перемещений,
73 названия порталов и свитков, перековки, выбранные описания питомцев,
косметические облики миньонов, условия и ограничения.

🟩🟩🟩🟩🟩🟩🟩🟩⬜⬜ **76,01%**

| Известная база | Строк | Доля |
|---|---:|---:|
| Подготовлено, включая частичный перевод | 25 196 | 76,01% |
| Осталось обработать | 7 951 | 23,99% |
| Всего | 33 147 | 100% |

Из подготовленных: 23 743 без латиницы, 1453 с латинскими терминами.
Это локальный снимок и 21 прежняя NPC-реплика, не весь живой SkyBlock.
Отсутствие латиницы не доказывает полноту или качество перевода.

JAR собран по отдельной просьбе пользователя: Minecraft 26.1.2, Fabric,
Java 25, реальные библиотеки без заглушек. Сверены ресурсы JAR, формат классов,
числа/уровни/значки/переменные/ID и отсутствие новых дублей/регрессий данных.
Это не проверка игры или применения Mixin. Minecraft и Hypixel не запускались,
в активной сборке остаётся прежняя 0.3.0-dev.

Файл: WitherSkyRU-26.1.2-fabric-0.12.0-dev.jar.
Исходники и история: WitherSkyRU_0.12.0-dev_MC26.1.2_project.zip.
Копии доставки: Desktop\\WitherSkyRU\\translation-batch-0.12.0.
Не устанавливайте два JAR WitherSkyRU одновременно. Установку и игровую
проверку этой версии пока не выполнять.
'''
    (SOURCE/'BATCH_0.12.0_RU.md').write_text(report,encoding='utf-8')
    (output/'ПРОГРЕСС_WITHERSKYRU_0.12.0.md').write_text(report,encoding='utf-8')
    print(json.dumps({'jar':jar.name,'sha256':jar_hash,'jar_bytes':jar.stat().st_size,'resource_comparisons':len(verified),'new_sources':checks['new_snapshot_sources']},ensure_ascii=False))

if __name__=='__main__':main()
