param([Parameter(Mandatory=$true)][string]$CheckRoot,[Parameter(Mandatory=$true)][string]$BenchmarkRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskChecks=(Resolve-Path $CheckRoot).Path
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.75-dev.jar";$taskHash=(Get-FileHash $taskJar).Hash
$taskGate=Get-Content "$taskChecks/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
if($taskGate.runtime -ne '0.15.75-dev' -or $taskGate.targeted_only -or $taskGate.tests_passed.Count -ne 127 -or @($taskGate.tests_passed|Sort-Object -Unique).Count -ne 127 -or $taskGate.runtime_jar_sha256 -ne $taskHash -or $taskGate.reused_same_executable_successes.Count){throw 'Require127fresh groups on final executable'}
$taskAssertions=[int][regex]::Match((Get-Content "$taskChecks/TestNpc75-output.txt" -Raw),'(\d+) assertions').Groups[1].Value
if($taskAssertions -ne 64192){throw 'Missing full new NPC75 checks'}
$taskQa=Get-Content "$taskProject/QA_REPORT.json" -Raw|ConvertFrom-Json
if($taskQa.latest_verified_batch.version -ne '0.15.74-dev' -or $taskQa.latest_verified_batch.jar_sha256 -ne 'F07822E5F0D3A8753D8E770C196B5EED471828A33B10702BBBB404211147EB18'){throw 'Preserve prior finalized batch'}
$taskProof=Get-Content "$taskProject/translation-source/JAR_SCOPE_0.15.75.json" -Raw|ConvertFrom-Json
if($taskProof.after_sha256 -ne $taskHash -or $taskProof.old_translation_and_asset_resources_byte_identical -ne 43 -or $taskProof.unchanged_other_runtime_classes -ne 107 -or !$taskProof.all_frame_world_input_item_dictionary_and_prepared_cache_classes_byte_identical -or $taskProof.new_frame_hooks -ne 0){throw 'Missing byte identity proof'}
$taskAudit=Get-Content "$taskProject/translation-source/NPC_AUDIT_0.15.75.json" -Raw|ConvertFrom-Json
if($taskAudit.new_exact_rows -ne 100 -or $taskAudit.new_exact_row_speakers -ne 11 -or $taskAudit.exact_unique_sources_before -ne 1122 -or $taskAudit.exact_unique_sources_after -ne 1222 -or $taskAudit.exact_speakers_after -ne 127 -or $taskAudit.indexed_unique_names -ne 572){throw 'Wrong audit scope'}
$taskBench=Get-Content (Join-Path $BenchmarkRoot 'RAW.json') -Raw|ConvertFrom-Json
if($taskBench.records.Count -ne 4 -or $taskBench.summary.Count -ne 6){throw 'IncompleteABBA benchmark'}
$taskExpectedVersions=@('0.15.74-dev','0.15.75-dev','0.15.75-dev','0.15.74-dev')
for($taskRun=0;$taskRun -lt 4;$taskRun++){if($taskBench.records[$taskRun].version -ne $taskExpectedVersions[$taskRun]){throw 'WrongABBA order'};$taskExpected=if($taskExpectedVersions[$taskRun] -eq '0.15.74-dev'){$taskProof.before_sha256}else{$taskHash};if($taskBench.records[$taskRun].jar_sha256 -ne $taskExpected){throw 'Different benchmark executable'}}
$taskPerformance=@{version='0.15.75-dev';date='2026-10-16';jar_sha256=$taskHash;scope='Four isolated JVMs,74/75/75/74;all wall,CPU,allocation rounds. New positives do extra translation work. Not game rendering,FPS or0.1%low.';results=$taskBench;actual_game_fps_measured=$false;zero_fps_impact_proven=$false}
[IO.File]::WriteAllText("$taskProject/translation-source/PERFORMANCE_0.15.75_RAW.json",($taskPerformance|ConvertTo-Json -Depth 20),[Text.UTF8Encoding]::new($false))
$taskFmt=[Globalization.CultureInfo]::InvariantCulture;$taskTable=@('| Сценарий | Одинаковый полезный результат | Wall: два порядка | После, мкс/сообщение |','| --- | --- | --- | --- |')
foreach($taskRow in $taskBench.summary){$taskA=$taskRow.orders[0];$taskB=$taskRow.orders[1];$taskTable+='| '+$taskRow.mode+' | '+$(if($taskRow.equal_useful_output){'да'}else{'нет: добавлен перевод'})+' | '+$taskA.wall_change_percent.ToString('F2',$taskFmt)+'% / '+$taskB.wall_change_percent.ToString('F2',$taskFmt)+'% | '+($taskA.after_ns/1000).ToString('F3',$taskFmt)+' / '+($taskB.after_ns/1000).ToString('F3',$taskFmt)+' |'}
$taskCounts=@('| NPC | Новых точных реплик |','| --- | ---: |');$taskResource=Get-Content "$taskProject/src/main/resources/data/witherskyru/npc-expansion75.json" -Raw|ConvertFrom-Json
foreach($taskGroup in ($taskResource.records|Group-Object speaker|Sort-Object Name)){$taskCounts+='| '+$taskGroup.Name+' | '+$taskGroup.Count+' |'}
$taskRelative=$taskChecks.Substring($taskProject.Length+1).Replace('\','/')
$taskReport=@"
# WitherSkyRU 0.15.75-dev — следующая партия NPC

2026-10-16. Minecraft 26.1.2 / Fabric. Все NPC игры и все их ветки ещё не переведены.

## Результат

100 новых точных реплик для 11 NPC. Впервые точные строки появились у Алды,
Аликсера, Ангуса, ученика и Булвара. Добавлены пять закрытых наборов кнопок
Алды и Серафины. Опции со стрелками и четырьмя кнопками поддержаны в
существующем ограниченном разборе; неизвестный набор не переводится.
Поддержан счётчик повтора, без изменения команд кнопок или их выполнения.

$( $taskCounts -join [Environment]::NewLine )

| Учёт | До 74 | После 75 |
| --- | ---: | ---: |
| Точные уникальные NPC-ключи ресурсов | 1122 | 1222 |
| Говорящие персонажи хотя бы с одной точной строкой | 122 | 127 |

Полностью переведённые NPC из этих чисел не вычисляются. Динамические
обработчики не развёрнуты в вымышленные тысячи реплик. Полный объём
непереведённых серверных скриптов неизвестен.

## Источники и ограничения

Ссылки на community wiki содержатся в каждой записи ресурса; независимый
опубликованный материал шести новых разделов — NPC_SOURCES_0.15.75.json.
Пять наборов прежних пропусков сверены на страницах 2026-10-16; исходные
разделы сохранены отдельно в NPC_SOURCES_0.15.74.json. Индекс 572 названий
зафиксирован 2026-10-15, включает 33 названия в исторической группе и не
объявляется полным текущим серверным реестром.

В доступных разделах покрыты все однозначные фиксированные кандидаты
Алды 16, Аликсера 16, Ангуса 9, ученика 5, Булвара 13; вместе со старыми строками —
лесоруба 20, Джакса 24, бармена 18, Серафины 27 и 23 такие реплики плотника.
Для портного 20 — именно прежний нормальный раздел, не вся свежая страница:
на ней отдельно появились три реплики с явной пометкой AHN/Alpha.
Переменные приветствия, эффекты зелий, контекстные вставки не выданы
за работающие буквальные ключи.

Неясная граница фразы плотника о мебели с непомеченным продолжением
«Check it out!» исключена, а не угадана. У бармена аналогично отдельно
отмечено «How's that sound?». На полученной странице Smithmonger не
опубликован раздел диалога; это не доказательство отсутствия живых реплик.
Джордж всё ещё имеет 43 опубликованных точных кандидата без записи.
Общий реестр NPC_REGISTRY_0.15.75_RU.md и NPC_REVIEW_NOTES_0.15.75.json
показывают ограничения; 462 названия индекса без точных ключей — не
доказанный счётчик непереведённых действующих NPC.

Семантические якоря отдельно различают повторяющиеся слова/термины,
сохраняют текущие цвета, цифры, click/hover/insertion и копируемые символы.
Стиль неизвестных выделений намеренно не угадывается: такая реплика
может остаться исходной. Синтетические тесты не заменяют все живые пакеты.
Старые ресурсы не изменены; прежние редакционные несогласованности,
включая разные формы обращения у плотника, не объявляются исправленными.

Адаптации атрибутированы участникам wiki и Hypixel; CC BY-NC-SA 3.0,
не MIT-код. Уведомления: THIRD_PARTY_NOTICES.md и META-INF/licenses/NPC75-SOURCES.txt.

## Проверки и оптимизация

127 уникальных групп прошли свежий прогон на одном итоговом JAR.
Новый тест — 64 192 проверки: 100 независимых исходников/русских целей,
полный диспетчер чата, фрагментация, временные метки, счётчики,
свежие цвета/действия, неоднозначные слова, legacy-форматирование,
переопределения, подписи, каналы и переключатели. Все пять наборов
кнопок проверены с разными пробелами, стилями, командами и повторами.
Команды не выполнялись.

Сохранены 98 024 проверки NPC74, 76 275 защиты имён, 324 313 готовых планов,
1 346 201 числового пути, старые корпуса NPC, предметов, зачарований и HUD.
В старых тестах изменён только суммарный размер story-карты 820→920.
Изолированное преобразование 10 классов Minecraft не запускает игру.

Все 43 прежних ресурса перевода/графики и 107 остальных runtime-классов
побайтно совпадают с итоговой 74. Кадры, F3, мир, предметы, ввод и готовые
планы не менялись. Новые реплики объединяются с прежней картой при
запуске. Новых обработчиков кадров, отдельных проходов сообщений,
сетевых переводов или провайдеров нет. Изменён существующий ограниченный
разбор кнопок чата; добавлен один ресурс NPC и файл атрибуции.

Четыре отдельных JVM в порядке 74,75,75,74; прогрев 200 000 вызовов/сценарий,
четыре раунда по 40 000. Все результаты, включая ухудшения, сохранены.
Измеряется вставка в чат, не GPU/шрифты/кадры или игровой FPS.

$( $taskTable -join [Environment]::NewLine )

Новые положительные сценарии теперь выполняют дополнительный перевод,
поэтому не являются сравнением одинаковой полезной работы.
Нулевое влияние на FPS, исчезновение микрофризов и улучшение 0,1% low
не доказаны. Игра и её настройки автоматически не менялись.

## Файл

WitherSkyRU-26.1.2-fabric-0.15.75-dev.jar
SHA256: $taskHash
Размер: $((Get-Item $taskJar).Length) байт
Проверки: $taskRelative

При ручной установке заменить прежний JAR, не оставлять две версии.
В архив выдачи не включены личные JFR, полные игровые журналы или unknown.tsv.
"@
[IO.File]::WriteAllText("$taskProject/translation-source/NPC_0.15.75_RU.md",$taskReport,[Text.UTF8Encoding]::new($false))
$taskQa|Add-Member -NotePropertyName 'archived_verified_batch_0_15_74' -NotePropertyValue $taskQa.latest_verified_batch
$taskQa.latest_verified_batch=[pscustomobject]@{version='0.15.75-dev';date='2026-10-16';jar_sha256=$taskHash;jar_bytes=(Get-Item $taskJar).Length;packaged_groups_passed=127;packaged_gate_root=$taskRelative;new_npc_assertions=$taskAssertions;prior_npc74_assertions=98024;prior_name_assertions=76275;prior_prepared_assertions=324313;prior_numeric_assertions=1346201;new_exact_npc_rows=100;new_row_speakers=11;newly_represented_exact_speakers=5;new_closed_option_layouts=5;retained_existing_targets=0;exact_npc_source_union_before=1122;exact_npc_source_union_after=1222;exact_npc_speakers_before=122;exact_npc_speakers_after=127;indexed_unique_names=572;indexed_names_without_exact_rows=462;global_untranslated_server_rows=$null;all_npc_branches_complete=$false;report='translation-source/NPC_0.15.75_RU.md';registry='translation-source/NPC_REGISTRY_0.15.75_RU.md';audit='translation-source/NPC_AUDIT_0.15.75.json';review_notes='translation-source/NPC_REVIEW_NOTES_0.15.75.json';raw_performance='translation-source/PERFORMANCE_0.15.75_RAW.json';jar_entry_scope_proof='translation-source/JAR_SCOPE_0.15.75.json';all_43_prior_resources_byte_identical=$true;unchanged_other_runtime_classes=107;final_benchmark_processes=4;new_frame_hooks=0;new_per_message_providers=0;installed_modpack_modified=$false;new_version_live_fps_or_frame_times_measured=$false;zero_fps_impact_proven=$false;microstutters_fixed_proven=$false;overall_goal_complete=$false}
[IO.File]::WriteAllText("$taskProject/QA_REPORT.json",($taskQa|ConvertTo-Json -Depth 30),[Text.UTF8Encoding]::new($false))
Write-Output "FINALIZED NPC75 $taskHash;100 new rows/11 speakers;127fresh groups;not all NPCs"
