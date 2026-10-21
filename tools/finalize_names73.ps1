param([Parameter(Mandatory=$true)][string]$CheckRoot,[Parameter(Mandatory=$true)][string]$NameBenchmarkRoot,[Parameter(Mandatory=$true)][string]$NameConfirmationRoot,[Parameter(Mandatory=$true)][string]$GeneralBenchmarkRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskChecks=(Resolve-Path -LiteralPath $CheckRoot).Path
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.73-dev.jar";$taskHash=(Get-FileHash $taskJar).Hash
$taskScope=Get-Content "$taskChecks/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
if($taskScope.runtime -ne '0.15.73-dev' -or $taskScope.targeted_only -or $taskScope.tests_passed.Count -ne 125 -or $taskScope.runtime_jar_sha256 -ne $taskHash){throw 'Wrong/incomplete executable gate'}
$taskAssertions=[regex]::Match((Get-Content "$taskChecks/ru.wither.skyru.TestNames73-output.txt" -Raw),'(\d+) assertions').Groups[1].Value
if($taskAssertions -ne '76275'){throw 'Missing nickname protection checks'}
$taskQa=Get-Content "$taskProject/QA_REPORT.json" -Raw|ConvertFrom-Json;if($taskQa.latest_verified_batch.version -ne '0.15.72-dev'){throw 'Preserve prior finalization'}
$taskProof=Get-Content "$taskProject/translation-source/JAR_SCOPE_0.15.73.json" -Raw|ConvertFrom-Json
if($taskProof.after_sha256 -ne $taskHash -or !$taskProof.all_translation_resources_byte_identical -or $taskProof.unchanged_translation_and_asset_resources -ne 42 -or $taskProof.changed.Count -ne 3){throw 'Missing exact runtime scope proof'}
$taskNames=Get-Content (Join-Path $NameBenchmarkRoot 'RAW.json') -Raw|ConvertFrom-Json;$taskGeneral=Get-Content (Join-Path $GeneralBenchmarkRoot 'RAW.json') -Raw|ConvertFrom-Json
$taskConfirmation=Get-Content (Join-Path $NameConfirmationRoot 'RAW.json') -Raw|ConvertFrom-Json
if($taskNames.prototype -or $taskConfirmation.prototype){throw 'Prototype is not final benchmark evidence'}
foreach($taskRun in @($taskNames,$taskConfirmation,$taskGeneral)){if($taskRun.records.Count -ne 4){throw 'Incomplete ABBA'};foreach($taskRecord in $taskRun.records){$taskExpected=if($taskRecord.variant -eq 'baseline'){$taskProof.before_sha256}else{$taskHash};if($taskRecord.jar_sha256 -ne $taskExpected){throw 'Benchmark on different executable'}}}
$taskPrototypes=@()
foreach($taskItem in @(
 @{root='build/context73-probe-00ed2a0627714e53bf721c9456acd3e8';variant='Direct spliterator consumer;rejected,slower and more allocation'},
 @{root='build/context73-probe-82f88fba2fe1409287bcfb3f5c2639f7';variant='StackWalker initial estimate32;rejected,more allocation'},
 @{root='build/context73-probe-3f04c63b97bd437e8cf43b5b8142b29b';variant='Direct metadata front cache;rejected,no consistent complete-walk gain'},
 @{root='build/scratch73-probe-9e83bf7c4c064a529f86fafb887e226a';variant='Reusable character scratch;rejected,mixed timings and slower cold counters'},
 @{root='build/dictionary73-probe-c7a3ac90e1d941ffa19a6960794653c5';variant='Regex-equivalent trim/numeric/URL gates;467497 correctness checks passed,rejected,slower cold counters'})){
 $taskFolder=Join-Path $taskProject $taskItem.root;$taskRecords=@();for($taskIndex=0;$taskIndex -lt 4;$taskIndex++){$taskFile=@(Get-ChildItem $taskFolder -Filter "$taskIndex-*.json");if($taskFile.Count -ne 1){throw 'Incomplete prototype results'};$taskRecords+=@{index=$taskIndex;variant=if($taskIndex -in @(1,2)){'unreleased candidate override'}else{'delivered72 baseline'};results=Get-Content $taskFile[0].FullName -Raw|ConvertFrom-Json}}
 $taskClasses=@(Get-ChildItem "$taskFolder/candidate" -Recurse -Filter '*.class'|ForEach-Object {@{path=$_.FullName.Substring($taskFolder.Length+1).Replace('\','/');sha256=(Get-FileHash $_.FullName).Hash}})
 $taskPrototypes+=@{root=$taskItem.root;variant=$taskItem.variant;not_final_jar_evidence=$true;candidate_classes=$taskClasses;records=$taskRecords}
}
$taskEarly=Get-Content "$taskProject/build/names73-benchmark-fd61dcd2a10c42c4a7e172db27d85b90/RAW.json" -Raw|ConvertFrom-Json
if(!$taskEarly.prototype){throw 'Unreleased name prototype mislabeled'}
$taskRaw=@{version='0.15.73-dev';jar_sha256=$taskHash;scope='Twelve final/baseline processes:four main roster benchmarks,four confirming roster benchmarks andfour general-sequence benchmarks,in ABBA orders. All rounds retained,including anomalously slow legacy-format run in the main set. Twenty-four earlier prototype/baseline processes kept separately,not pooled. Local method wall time/allocation,not game FPS,0.1%low,GPU or full-frame proof. Windows thread CPU clock is quantized.';final_roster=$taskNames;final_roster_confirmation=$taskConfirmation;final_general=$taskGeneral;rejected_experiments=$taskPrototypes;earlier_unreleased_unbounded_name_prototype=$taskEarly;failed_setup='One scratch benchmark launch failed before measurement because a JVM property was not quoted in PowerShell;rerun recorded separately.No data from the failed launch used.'}
[IO.File]::WriteAllText("$taskProject/translation-source/PERFORMANCE_0.15.73_RAW.json",($taskRaw|ConvertTo-Json -Depth 24),[Text.UTF8Encoding]::new($false))
$taskContract=@{version='0.15.73-dev';jar_sha256=$taskHash;stored='Additional String reference table points only to current roster names,not words from rendered text';max_additional_table_slots=1024;max_standard_names_on_table_path=512;max_table_probes_per_word=8;collision_fallback='After eight occupied nonmatches,original Set lookup with a fresh substring';oversized_fallback='More than512 standard names:original Set membership check;the shared token scan still computes a hash';unchanged=@('exact case-sensitive username semantics','ASCII username token boundaries','legacy formatting stripping','nonstandard profile names','roster refresh interval and prepared-plan invalidation','source text,current styles,click/hover actions','F3 protection','caller-context freshness','all42 translation/asset resources');new_frame_hooks=0;zero_frame_cost_proven=$false;microstutters_fixed_proven=$false;game_mods_or_settings_changed=$false}
[IO.File]::WriteAllText("$taskProject/translation-source/NAMES_CONTRACT_0.15.73.json",($taskContract|ConvertTo-Json -Depth 9),[Text.UTF8Encoding]::new($false))
$taskFmt=[Globalization.CultureInfo]::InvariantCulture
function Table73($taskRun){$taskLines=@('| Набор | Время: два порядка | Байты до: два порядка | Байты после: два порядка |','| --- | --- | --- | --- |');foreach($taskRow in $taskRun.summary){$taskA=$taskRow.orders[0];$taskB=$taskRow.orders[1];$taskLines+='| '+$taskRow.mode+' | '+$taskA.wall_change_percent.ToString('F2',$taskFmt)+'% / '+$taskB.wall_change_percent.ToString('F2',$taskFmt)+'% | '+$taskA.before_bytes.ToString('F1',$taskFmt)+' / '+$taskB.before_bytes.ToString('F1',$taskFmt)+' | '+$taskA.after_bytes.ToString('F1',$taskFmt)+' / '+$taskB.after_bytes.ToString('F1',$taskFmt)+' |'};return $taskLines -join "`n"}
$taskRelative=$taskChecks.Substring($taskProject.Length+1).Replace('\','/')
$taskReport=@"
# WitherSkyRU 0.15.73-dev: дополнительный проход оптимизации

2026-10-15. Отдельная сборка, автоматически не устанавливалась.
Устранение микрофризов, улучшение 0,1% low и нулевое влияние на FPS не доказаны.

## Итоговое изменение

Защита имён больше не создаёт отдельную строку для каждого обычного английского
слова HUD. Вычисляется хеш текущего участка и выполняется точное сравнение
с именем из списка игроков. Хеш не является разрешением на совпадение:
сравниваются длина и все символы, с прежним учётом регистра и границ имени.
Thor по-прежнему не блокирует Thorns. Нестандартные имена и legacy-коды
сохраняют прежнюю семантику.

Дополнительная таблица содержит только ссылки на имена текущего списка.
Она ограничена 1024 позициями для не более 512 стандартных имён. После восьми
занятых несовпавших позиций используется прежний Set с substring. Для больших
списков также используется Set; общий проход токена дополнительно считает хеш.
Поэтому эта оптимизация не объявляется безаллокационной для любых входов.
Обновление состава игроков, очистка готовых планов, F3, ввод, цвета и действия
не менялись. Нет новых блокировок, кадровых перехватов, сети или записи на диск.

## Отброшенные эксперименты

Не включены прямой обход StackWalker, увеличенный начальный буфер стека,
дополнительный кэш метаданных, переиспользуемый символьный буфер и эквивалентная
замена части регулярных выражений. Они давали неоднозначные результаты или
ухудшали некоторые пути, несмотря на уменьшение выделений памяти.
24 ранних процесса сохранены отдельно от 12 финальных. Это не выбор
только удачных раундов и не усреднение прототипов с выпущенной сборкой.

## Проверки

125 групп на одном JAR. Новый тест: 76 275 проверок, включая 50 000 случайных
строк, точные коллизии хеша, длинные кластеры/резервный путь, предел таблицы,
большие списки, Unicode, форматирование, смену имён и четыре потока чтения.
Сохранены прежние 324 313 проверок готовых планов и 1 346 201 числового пути,
а также проверки NPC, предметов, зачарований, ввода, поиска и HUD.
Fabric преобразовал 10 реальных классов Minecraft в изолированном тесте:
это не запуск игры или проверка всех сторонних модов.

В JAR изменены только PlayerNames, его NameIndex и номер версии. Все остальные
классы, миксины и 42 ресурса перевода/графики совпадают с выпущенной 72 побайтно.
Полнота перевода всех текстов сервера по-прежнему не заявляется.

## Финальные локальные замеры

Три набора, каждый в порядке 72,73,73,72. В таблицах wall-время
операции, не эксклюзивное CPU или время кадра. Все раунды и счётчики CPU/памяти
сохранены в PERFORMANCE_0.15.73_RAW.json. Короткие CPU-замеры Windows ступенчатые
и не используются как доказательство нулевой стоимости.

Первый набор: 82 имени, 500 000 прогревочных операций на режим, четыре раунда
по 500 000 операций (для уникальных счётчиков — по 200 000). В режимах sequence
используется настоящий API перевода с заранее заданным контекстом; это не полный
GUI-кадр. В режиме legacy остаются расходы прежнего удаления кодов цвета.

$(Table73 $taskNames)

Повторный набор с теми же 82 именами. Он проведён дополнительно из-за большого
разброса режима legacy в одном процессе первого набора. Первые результаты
не заменены повторными и не скрыты: в основном наборе index_legacy дал
−25,45% / +33,88%. Причина разброса не доказана.

$(Table73 $taskConfirmation)

Третий набор: прежний сценарий без активного списка имён, включая свежий обход
стека, меняющиеся значения, новые цвета/действия, 16 фрагментов и длинную строку.
consume дополнительно читает символы простым приёмником, но не рисует GPU.
20 000 прогревочных пакетов и четыре раунда по 4 000 пакетов (у счётчиков —
по 20 000), по 10 вызовов в пакете. Создание входов учтено одинаково.

$(Table73 $taskGeneral)

Измерения отдельных методов не позволяют пересчитать результат в игровой FPS.
Уникальные значения всё ещё проходят полный разбор; все пути не объявляются
быстрее. Разброс и ухудшения в таблицах не скрыты.

## Что требуется проверить в игре

Вручную заменить только предыдущий JAR, не оставляя две версии. Сравнивать
одно место, камеру, шейдеры и настройки после прогрева; не запускать параллельно
сборку и проверки. Важны время кадров и плавность, а не только счётчик FPS.
Без такого сравнения нельзя утверждать, что оставшиеся микрофризы устранены.

JAR SHA256: $taskHash
Проверки: $taskRelative
Личные игровые JFR, полные журналы и настройки сборки в выдачу не включены.
"@
[IO.File]::WriteAllText("$taskProject/translation-source/OPTIMIZATION_0.15.73_RU.md",$taskReport,[Text.UTF8Encoding]::new($false))
$taskQa|Add-Member -NotePropertyName 'archived_verified_batch_0_15_72' -NotePropertyValue $taskQa.latest_verified_batch
$taskQa.latest_verified_batch=[pscustomobject]@{version='0.15.73-dev';date='2026-10-15';jar_sha256=$taskHash;jar_bytes=(Get-Item $taskJar).Length;packaged_groups_passed=125;packaged_gate_root=$taskRelative;new_name_assertions=76275;prior_prepared_assertions=324313;prior_numeric_assertions=1346201;report='translation-source/OPTIMIZATION_0.15.73_RU.md';raw_performance='translation-source/PERFORMANCE_0.15.73_RAW.json';lookup_contract='translation-source/NAMES_CONTRACT_0.15.73.json';jar_entry_scope_proof='translation-source/JAR_SCOPE_0.15.73.json';runtime_dictionary_entries=36044;runtime_tsv_templates=1231;all_42_resources_byte_identical=$true;new_translation_entries=0;final_benchmark_processes=12;unreleased_prototype_benchmark_processes=24;new_frame_hooks=0;new_network_calls=0;new_disk_writes=0;max_additional_name_table_slots=1024;max_table_probes=8;all_paths_faster=$false;installed_modpack_modified=$false;new_version_live_fps_or_frame_times_measured=$false;zero_fps_impact_proven=$false;microstutters_fixed_proven=$false;overall_goal_complete=$false}
[IO.File]::WriteAllText("$taskProject/QA_REPORT.json",($taskQa|ConvertTo-Json -Depth 26),[Text.UTF8Encoding]::new($false))
Write-Output "FINALIZED NAMES73 $taskHash;125 groups;76275 new assertions;no live FPS proof"
