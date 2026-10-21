param([Parameter(Mandatory=$true)][string]$CheckRoot,[Parameter(Mandatory=$true)][string]$MainBenchmarkRoot,[Parameter(Mandatory=$true)][string]$ShortBenchmarkRoot,[Parameter(Mandatory=$true)][string[]]$PrototypeRoots)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskChecks=(Resolve-Path -LiteralPath $CheckRoot).Path
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.72-dev.jar";$taskHash=(Get-FileHash $taskJar).Hash
$taskScope=Get-Content "$taskChecks/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
if($taskScope.runtime -ne '0.15.72-dev' -or $taskScope.targeted_only -or $taskScope.tests_passed.Count -ne 124 -or $taskScope.runtime_jar_sha256 -ne $taskHash){throw 'Wrong/incomplete executable gate'}
$taskAssertions=[regex]::Match((Get-Content "$taskChecks/ru.wither.skyru.TestPreparedDisplay72-output.txt" -Raw),'(\d+) assertions').Groups[1].Value
if($taskAssertions -ne '324313'){throw 'Missing prepared-path assertions'}
$taskQa=Get-Content "$taskProject/QA_REPORT.json" -Raw|ConvertFrom-Json;if($taskQa.latest_verified_batch.version -ne '0.15.71-dev'){throw 'Preserve finalization'}
$taskProof=Get-Content "$taskProject/translation-source/JAR_SCOPE_0.15.72.json" -Raw|ConvertFrom-Json
if($taskProof.after_sha256 -ne $taskHash -or !$taskProof.all_translation_resources_byte_identical -or $taskProof.unchanged_translation_and_asset_resources -ne 42){throw 'Missing runtime resource proof'}
$taskMain=Get-Content (Join-Path $MainBenchmarkRoot 'RAW.json') -Raw|ConvertFrom-Json;$taskShort=Get-Content (Join-Path $ShortBenchmarkRoot 'RAW.json') -Raw|ConvertFrom-Json
foreach($taskRun in @($taskMain,$taskShort)){if($taskRun.runtime_hashes.'0.15.72-dev' -ne $taskHash -or $taskRun.runtime_hashes.'0.15.71-dev' -ne $taskProof.before_sha256){throw 'Benchmark not on final executable'}}
if($PrototypeRoots.Count -ne 4){throw 'Expected all four saved prototype runs'}
$taskPrototypes=@();foreach($taskRoot in $PrototypeRoots){$taskRun=Get-Content (Join-Path $taskRoot 'RAW.json') -Raw|ConvertFrom-Json;if($taskRun.runtime_hashes.'0.15.72-dev' -eq $taskHash -or $taskRun.runtime_hashes.'0.15.71-dev' -ne $taskProof.before_sha256 -or $taskRun.records.Count -ne 4){throw 'Prototype mislabeled'};$taskPrototypes+=@{scope='Unreleased earlier prototype; not evidence for final executable. All rounds retained, including cold-path regressions.';relative_root=(Resolve-Path $taskRoot).Path.Substring($taskProject.Length+1).Replace('\','/');results=$taskRun}}
$taskRaw=@{version='0.15.72-dev';jar_sha256=$taskHash;scope='Eight processes on the final executable/baseline: four main with long counter rounds and CPU counters, four earlier short-round confirmation. Sixteen earlier prototype processes kept separately, not pooled into final results. Legacy cpu_change_percent field is wall nanoTime change; actual thread CPU is separate, meaningful here only for long counter rounds. No game FPS,0.1%low,GPU or full-frame A/B.';main=$taskMain;short_final_executable=$taskShort;unreleased_prototype_runs=$taskPrototypes}
[IO.File]::WriteAllText("$taskProject/translation-source/PERFORMANCE_0.15.72_RAW.json",($taskRaw|ConvertTo-Json -Depth 22),[Text.UTF8Encoding]::new($false))
$taskContract=@{version='0.15.72-dev';jar_sha256=$taskHash;slots_per_using_thread=128;max_input_utf16_units=256;max_input_runs=16;max_output_codepoints_per_plan=512;max_aggregate_output_codepoints=16384;stored='Input text keys, primitive layout/alias metadata and immutable glyph/index/run arrays. One pending pure key is possible during preparation; dictionary and roster owners are weak references.';not_stored=@('source sequence','Style','Component','hover or click event','font textures','frame context');current_per_draw=@('source glyph snapshot','input/player/F3 guards','current styles and actions','font measurement and fitting','Minecraft draw');plan_key_changes=@('source text','run boundaries','Style reference alias partition','compact mode','dictionary instance','player roster contents');admission='First occurrence stores only a primitive signature; a repeated matching candidate prepares a plan. Full text/layout checked before reuse; hash collisions never authorize hits.';fallback='Oversized/complex inputs and unmappable result styles use previous translation; output-over-limit results are not cached.';zero_frame_cost_proven=$false;all_contexts_event_driven=$false;game_mods_or_settings_changed=$false}
[IO.File]::WriteAllText("$taskProject/translation-source/PREPARED_CONTRACT_0.15.72.json",($taskContract|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
$taskTable=@();$taskFmt=[Globalization.CultureInfo]::InvariantCulture
foreach($taskRow in $taskMain.summary){$taskA=$taskRow.orders[0];$taskB=$taskRow.orders[1];$taskTable+='| '+$taskRow.mode+' | '+$taskA.cpu_change_percent.ToString('F2',$taskFmt)+'% / '+$taskB.cpu_change_percent.ToString('F2',$taskFmt)+'% | '+$taskA.before_bytes.ToString('F1',$taskFmt)+' / '+$taskB.before_bytes.ToString('F1',$taskFmt)+' | '+$taskA.after_bytes.ToString('F1',$taskFmt)+' / '+$taskB.after_bytes.ToString('F1',$taskFmt)+' |'}
$taskRelative=$taskChecks.Substring($taskProject.Length+1).Replace('\','/')
$taskReport=@"
# WitherSkyRU 0.15.72-dev: переиспользование готового текста

2026-10-15. Статус: проверенная отдельная сборка, не установленная автоматически.
Игровой прирост FPS, устранение микрофризов и 0,1% low новой версии не измерены.

## Ответ на идею «пусть текст уже будет готов»

Да, это полезно для повторяющихся строк. Теперь форматированные последовательности
могут переиспользовать готовые символы перевода, их исходные индексы и привязки
к фрагментам. Повторное сопоставление переводов и построение их частей пропускаются.
Minecraft всё равно должен отрисовывать текст каждый кадр.

Совсем убрать проверку содержимого нельзя: иначе замрут цифры, цели, таймеры или
сохранятся старые команды/подсказки. Снимок текущего текста и защитные проверки
остаются. Верхний путь Component, определение контекста, подгонка ширины и работа
шрифтов также не превращены в полностью событийную систему.

## Почему не должны замереть цвета, числа и действия

- Кэш хранит только ключи текста и массивы символов/индексов/привязок. Исходный
  объект, Style, Component, hover и команды в записи кэша не сохраняются.
- При выдаче плана используются текущие стили. Изменённые цвета, флаги оформления,
  шрифты и содержимое hover берутся из нового вызова. Индексы символов сохраняются.
- Изменение самого текста, границ фрагментов, связи повторных ссылок на стили,
  режима HUD, словаря или списка игроков требует другого плана/сброса.
- Имена игроков, ввод, F3, Shift-названия и защищённые строки проверяются до кэша.
- Новый кэш работает только в памяти. У него нет новых сетевых вызовов или записи
  на диск; прежний сбор неизвестных строк остаётся отдельной функцией словаря.

## Ограничения памяти и редкие строки

На использующий поток: 128 позиций, общий бюджет 16 384 символов вывода;
вход до 256 единиц UTF-16 и 16 фрагментов, план до 512 выводимых символов.
Первое наблюдение записывает только числовой отпечаток, без массива готового текста.
При повторе создаётся план. Полный текст и структура проверяются перед попаданием:
одинаковый хеш не даёт права вернуть другой перевод.

Длинные и сложные строки, а также неподдерживаемые выходные стили используют
обычную обработку. Кэш ограничен на каждый поток, а не единым глобальным объёмом.
Словарь и снимок имён привязаны слабыми ссылками. При неизменном составе игроков
больше не создаётся новый снимок их имён каждые две секунды; интервал проверки прежний.

## Что проверено

124 группы на одном JAR. Новый тест: 324 313 проверок. В их числе все 36 044 ключа
словаря с четырьмя выводами: сравнение с выпущенной 71 по тексту, индексам символов,
стилям/действиям и возврату исходного объекта. Дополнительно: новые hover/команды,
изменяемый текст hover, флаги оформления/тени, разделение и повторные ссылки на стили,
10 000 меняющихся значений, коллизии хеша, досрочная остановка вывода, вложенные вызовы,
пределы памяти/вытеснение, пользовательский словарь и смена состава игроков.

Проверены также прежние NPC, предметы, зачарования, HUD, ввод и поиск. Fabric
преобразовал 10 классов Minecraft; это не запуск игры и не доказательство
совместимости со всеми модами сборки. Все 42 ресурса перевода/графики побайтно
совпадают с 71. Новых перехватов кадров нет. Полный перевод SkyBlock не заявляется.

## Замеры: не прирост FPS

Восемь отдельных процессов на финальном JAR и 71: основной и короткий прогоны,
каждый в порядке 71,72,72,71. Сохранены все измерения. Ещё 16 процессов ранних
невыпущенных прототипов сохранены отдельно, включая ухудшения, приведшие к переделке.
Они не подмешаны в результат финальной версии.

Основной прогон: 20 тысяч прогревочных пакетов, четыре измерения; пакет — 10 вызовов.
Для меняющихся счётчиков 20 тысяч измерительных пакетов в раунде, для остальных — 4 тысячи.
Тесты consume дополнительно читают все готовые символы простым приёмником, не рисуют GPU.
В прямых HUD-тестах контекст задан заранее; outside_fresh_context включает свежий
разбор стека. Создание входных объектов учтено одинаково. Числа ниже — wall-время
операции, не эксклюзивное CPU, время кадра или FPS. Поле cpu_change_percent в сыром
отчёте сохранено как историческое имя для wall-времени; CPU-счётчики даны отдельно.

| Набор | Время, два порядка | Байты до | Байты после |
| --- | --- | --- | --- |
$($taskTable -join "`n")

В основном прогоне повторяющийся HUD с чтением символов ускорился примерно на 20%.
Это только конкретный участок подготовки/чтения текста, не обещание +20% игрового FPS.
У постоянно уникальных значений результат неоднозначный: dynamic_counter_consume
дал −1,20% / +4,72% wall-времени и −0,59% / +3,55% CPU вызывающего потока.
Поэтому «все пути всегда быстрее» и нулевые накладные расходы не утверждаются.
CPU-таймер Windows слишком груб для коротких режимов; его нулевые/ступенчатые
значения там не означают бесплатную обработку и не используются для вывода.

## Проверка в игре

Заменить только предыдущий JAR вручную, оставить место, шейдер, камеру и остальные
настройки одинаковыми. Дать игре прогреться и сравнить плавность/время кадров без F3.
Не проводить одновременно сборку и автоматические тесты: они создают свою нагрузку.
Если появится ухудшение, прежняя 71 остаётся отдельным файлом для возврата и сравнения.
До игрового теста нельзя обещать отсутствие проблем или считать микрофризы исправленными.

JAR SHA256: $taskHash
Полный набор: $taskRelative
Подробности: PERFORMANCE_0.15.72_RAW.json, PREPARED_CONTRACT_0.15.72.json,
JAR_SCOPE_0.15.72.json. Личные игровые JFR и полные журналы в выдачу не включены.
"@
[IO.File]::WriteAllText("$taskProject/translation-source/PREPARED_0.15.72_RU.md",$taskReport,[Text.UTF8Encoding]::new($false))
$taskQa|Add-Member -NotePropertyName 'archived_verified_batch_0_15_71' -NotePropertyValue $taskQa.latest_verified_batch
$taskQa.latest_verified_batch=[pscustomobject]@{version='0.15.72-dev';date='2026-10-15';jar_sha256=$taskHash;jar_bytes=(Get-Item $taskJar).Length;packaged_groups_passed=124;packaged_gate_root=$taskRelative;new_prepared_assertions=324313;prior_numeric_assertions=1346201;report='translation-source/PREPARED_0.15.72_RU.md';raw_performance='translation-source/PERFORMANCE_0.15.72_RAW.json';cache_contract='translation-source/PREPARED_CONTRACT_0.15.72.json';jar_entry_scope_proof='translation-source/JAR_SCOPE_0.15.72.json';runtime_dictionary_entries=36044;runtime_tsv_templates=1231;all_42_resources_byte_identical=$true;new_translation_entries=0;final_benchmark_processes=8;unreleased_prototype_benchmark_processes=16;new_frame_hooks=0;new_network_calls=0;new_cache_disk_writes=0;cache_slots_per_thread=128;cache_glyph_budget_per_thread=16384;all_paths_faster=$false;installed_modpack_modified=$false;new_version_live_fps_or_frame_times_measured=$false;zero_fps_impact_proven=$false;microstutters_fixed_proven=$false;overall_goal_complete=$false}
[IO.File]::WriteAllText("$taskProject/QA_REPORT.json",($taskQa|ConvertTo-Json -Depth 25),[Text.UTF8Encoding]::new($false))
Write-Output "FINALIZED PREPARED72 $taskHash;124 groups;324313 new assertions;no live FPS proof"
