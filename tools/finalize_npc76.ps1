param([Parameter(Mandatory=$true)][string]$CheckRoot,[Parameter(Mandatory=$true)][string]$BenchmarkRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskChecks=(Resolve-Path $CheckRoot).Path
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.76-dev.jar";$taskHash=(Get-FileHash $taskJar).Hash
$taskGate=Get-Content "$taskChecks/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
if($taskGate.runtime -ne '0.15.76-dev' -or $taskGate.targeted_only -or $taskGate.tests_passed.Count -ne 128 -or @($taskGate.tests_passed|Sort-Object -Unique).Count -ne 128 -or $taskGate.runtime_jar_sha256 -ne $taskHash -or $taskGate.reused_same_executable_successes.Count){throw 'Require128fresh groups on final executable'}
$taskAssertions=[int][regex]::Match((Get-Content "$taskChecks/TestNpc76-output.txt" -Raw),'(\d+) assertions').Groups[1].Value
if($taskAssertions -ne 160453){throw 'Missing full new NPC76 checks'}
$taskQa=Get-Content "$taskProject/QA_REPORT.json" -Raw|ConvertFrom-Json
if($taskQa.latest_verified_batch.version -ne '0.15.75-dev' -or $taskQa.latest_verified_batch.jar_sha256 -ne '24ADA8A3A7B5757274ECAA2792730F52E75250B3E6E6DFAD5C823F967084D3D6'){throw 'Preserve prior finalized batch'}
$taskProof=Get-Content "$taskProject/translation-source/JAR_SCOPE_0.15.76.json" -Raw|ConvertFrom-Json
if($taskProof.after_sha256 -ne $taskHash -or $taskProof.old_translation_and_asset_resources_byte_identical -ne 44 -or $taskProof.unchanged_other_runtime_classes -ne 107 -or !$taskProof.all_frame_world_input_item_dictionary_and_prepared_cache_classes_byte_identical -or $taskProof.new_frame_hooks -ne 0){throw 'Missing byte identity proof'}
$taskAudit=Get-Content "$taskProject/translation-source/NPC_AUDIT_0.15.76.json" -Raw|ConvertFrom-Json
if($taskAudit.new_exact_rows -ne 229 -or $taskAudit.new_exact_row_speakers -ne 17 -or $taskAudit.new_exact_npc_rows -ne 228 -or $taskAudit.new_exact_security_rows -ne 1 -or $taskAudit.exact_unique_sources_before -ne 1222 -or $taskAudit.exact_unique_sources_after -ne 1450 -or $taskAudit.exact_speakers_after -ne 135 -or $taskAudit.exact_any_recorded_tag_sources_after -ne 1451 -or $taskAudit.indexed_unique_names -ne 572){throw 'Wrong audit scope'}
$taskBench=Get-Content (Join-Path $BenchmarkRoot 'RAW.json') -Raw|ConvertFrom-Json
if($taskBench.records.Count -ne 4 -or $taskBench.summary.Count -ne 7){throw 'IncompleteABBA benchmark'}
$taskExpectedVersions=@('0.15.75-dev','0.15.76-dev','0.15.76-dev','0.15.75-dev')
for($taskRun=0;$taskRun -lt 4;$taskRun++){if($taskBench.records[$taskRun].version -ne $taskExpectedVersions[$taskRun]){throw 'WrongABBA order'};$taskExpected=if($taskExpectedVersions[$taskRun] -eq '0.15.75-dev'){$taskProof.before_sha256}else{$taskHash};if($taskBench.records[$taskRun].jar_sha256 -ne $taskExpected){throw 'Different benchmark executable'}}
$taskPerformance=@{version='0.15.76-dev';date='2026-10-16';jar_sha256=$taskHash;scope='Four isolated JVMs,75/76/76/75;all wall,CPU,allocation rounds. New positives do extra translation work. Not game rendering,FPS or0.1%low.';results=$taskBench;actual_game_fps_measured=$false;zero_fps_impact_proven=$false}
[IO.File]::WriteAllText("$taskProject/translation-source/PERFORMANCE_0.15.76_RAW.json",($taskPerformance|ConvertTo-Json -Depth 20),[Text.UTF8Encoding]::new($false))
$taskFmt=[Globalization.CultureInfo]::InvariantCulture;$taskTable=@('| Сценарий | Одинаковый полезный результат | Wall: два порядка | После, мкс/сообщение |','| --- | --- | --- | --- |')
foreach($taskRow in $taskBench.summary){$taskA=$taskRow.orders[0];$taskB=$taskRow.orders[1];$taskTable+='| '+$taskRow.mode+' | '+$(if($taskRow.equal_useful_output){'да'}else{'нет: добавлен перевод'})+' | '+$taskA.wall_change_percent.ToString('F2',$taskFmt)+'% / '+$taskB.wall_change_percent.ToString('F2',$taskFmt)+'% | '+($taskA.after_ns/1000).ToString('F3',$taskFmt)+' / '+($taskB.after_ns/1000).ToString('F3',$taskFmt)+' |'}
$taskCounts=@('| NPC | Новых точных реплик |','| --- | ---: |');$taskResource=Get-Content "$taskProject/src/main/resources/data/witherskyru/npc-expansion76.json" -Raw|ConvertFrom-Json
foreach($taskGroup in ($taskResource.records|Group-Object speaker|Sort-Object Name)){$taskCounts+='| '+$taskGroup.Name+' | '+$taskGroup.Count+' |'}
$taskRelative=$taskChecks.Substring($taskProject.Length+1).Replace('\','/')
$taskReport=@"
# WitherSkyRU 0.15.76-dev — события, обучение и взаимодействия

2026-10-16. Minecraft 26.1.2 / Fabric. Эта партия не завершает перевод всех
событийных, обучающих и остальных персонажей игры или всех их интерфейсов.

## Добавлено

229 точных опубликованных реплик для 17 персонажей: 228 с тегом NPC и
одна с тегом SECURITY у обучающего ленивца. Впервые точные записи появились
у девяти говорящих персонажей; восемь из них используют NPC, один — SECURITY.
Прежние цели и ресурсы сохранены. Все варианты здесь опубликованы, а не
вымышлены по шаблонам.

$( $taskCounts -join [Environment]::NewLine )

Охвачены новые разговоры Карнавала, Бинго, Нового года, Жуткого фестиваля,
Хоппити, Оринго, зимних NPC, кузнеца, библиотекаря и Расти.
14 полных опубликованных наборов ответов Хоппити и карнавальных ведущих
добавлены в существующую закрытую таблицу. Она распознаёт до пяти кнопок
и обе опубликованные стрелки ➜/🡺. Неподтверждённые случайные комбинации
не угадываются. Переводятся подписи, а не команды или адреса переходов.

Новая реплика SECURITY проходит через прежний story-провайдер на вставке
серверного сообщения, без отдельного прохода или кадрового обработчика.

Святой Джерри требует повторить конкретную английскую фразу. Русский текст
объясняет это, но сама требуемая фраза сохранена дословно. Ничего в чат
автоматически не отправлялось. Тест отдельно проверяет её свежие посимвольные
цвета, подсказки и действия.

## Честный учёт

| Учёт | До 75 | После 76 |
| --- | ---: | ---: |
| Точные уникальные NPC-ключи ресурсов | 1222 | 1450 |
| Говорящие персонажи с точными NPC-строками | 127 | 135 |
| Дополнительные ключи SECURITY | 0 | 1 |
| Все перечисленные теги вместе | 1222 | 1451 |
| Говорящие персонажи с перечисленными тегами | 127 | 136 |

Это не число полностью переведённых персонажей. 572 названия индекса
зафиксированы 2026-10-15, включая 33 названия в исторической группе.
454 названия не имеют точного NPC-ключа; у одного из них есть SECURITY.
453 не имеют ни одного из перечисленных типов точных ключей.
Эти величины не являются доказанным числом непереведённых действующих NPC:
другие обработчики, переменные, персонажи без диалога и исторические записи
не исключены. Полный серверный объём неизвестен.

## Источники и оставшиеся ограничения

17 новых опубликованных разделов независимо получены 2026-10-16:
NPC_SOURCES_0.15.76.json. Каждый новый ключ содержит свой source_url.
Предыдущие 38 разделов перенесены без повторного получения, с исходными датами.
Реестр: NPC_REGISTRY_0.15.76_RU.md. Подробная матрица взаимодействий и того,
что ещё не проверено: NPC_INTERACTION_SCOPE_0.15.76.json.

Все однозначные фиксированные кандидаты этих 17 разделов теперь имеют записи.
Это не доказательство полноты серверных скриптов, событий, меню или живых цветов.
Имена игроков, месяцы/годы, случайные списки питомцев, счётчики и другие
непроверенные подстановки не включены в фиктивное число новых реплик.
Историческое обучение Telekinesis у Расти исключено. Искажённая HTML-строка
о подарке святого Джерри не превращена в вымышленный серверный ключ.
Несколько непомеченных продолжений и составной блок ссылок ленивца отмечены
в NPC_EXCLUSIONS_0.15.76.json как неизвестные границы сообщений.

Действуют прежние ограничения: не весь диалог Джорджа, не все персонажи
остальных регионов, не все имена над головами, меню, лор предметов, случайные
варианты кнопок и новые ветки проверены. Уже существовавшие редакционные
несогласованности не объявлены исправленными. Опубликованный текст не
заменяет живые пакеты. Неизвестное выделение остаётся исходным, если
семантический перенос стиля нельзя доказать.

Русские адаптации и новые подписи кнопок атрибутированы участникам wiki
и Hypixel, CC BY-NC-SA 3.0, отдельно от MIT-кода. Уведомления:
THIRD_PARTY_NOTICES.md и META-INF/licenses/NPC76-SOURCES.txt.

## Проверки

128 уникальных групп прошли свежий прогон на одном итоговом JAR.
Новый тест — 160 453 проверки: 229 независимых источников/целей, 14 наборов
кнопок, фрагментация, свежие стили/цвета/события, числа, повторяющиеся слова,
временные метки, счётчики, legacy-форматирование, переопределения, подписи,
каналы и переключатели. Неправильный тег/говорящий не подменяются.
Сохранение обязательного английского ввода проверено посимвольно.

Прежние 64 192 проверки NPC75, 98 024 NPC74, 76 275 защиты имён,
324 313 готовых планов и 1 346 201 числового пути также прошли.
В старых тестах изменён лишь ожидаемый общий размер story-карты: 920→1149;
старые корпуса и русские цели не переписаны.
Изолированное преобразование классов Minecraft не запускает игру.

Тестовая сборка выявила ошибку пути в новом генераторе ресурса. Путь
исправлен; прежний ресурс75 восстановлен из неизменённого исходного TSV
и побайтно сверен с выпущенным JAR75. Ошибочный промежуточный JAR не выдан.
Итоговая проверка целиком повторена после исправления.

## Оптимизация и ограничения замера

Все 44 прежних ресурса перевода/графики и 107 остальных runtime-классов
побайтно совпадают с выпущенной 75. Кадры, F3, мир, предметы, ввод, словарь,
шрифты и готовые планы не менялись. Ресурс 76 загружается один раз и
объединяется с прежней story-картой. Новых кадровых обработчиков, провайдеров,
сетевого перевода или разбора новых строк при каждой отрисовке нет.
Для диалогов и кнопок сохраняется обработка при вставке/обновлении чата.

Четыре отдельные JVM в порядке 75,76,76,75. Семь сценариев, прогрев 200 000
вызовов на сценарий, четыре раунда по 40 000. Сохранены все раунды wall/CPU/
allocation, включая ухудшения. Новые положительные случаи выполняют перевод,
которого раньше не было, и не являются сравнением одинаковой полезной работы.

$( $taskTable -join [Environment]::NewLine )

Это замер вставки сообщений, не отрисовки шрифтов, GPU, мира, кадров или
игрового FPS. Нулевое влияние на FPS, устранение микрофризов и улучшение
0,1% low не доказаны. Автоматической замены мода, настроек или запуска игры нет.

## Файл

WitherSkyRU-26.1.2-fabric-0.15.76-dev.jar
SHA256: $taskHash
Размер: $((Get-Item $taskJar).Length) байт
Полный прогон: $taskRelative

Для ручной установки заменить прежний JAR, не оставляя две версии.
Личные JFR, полные игровые журналы и unknown.tsv в выдачу не включены.
"@
[IO.File]::WriteAllText("$taskProject/translation-source/NPC_0.15.76_RU.md",$taskReport,[Text.UTF8Encoding]::new($false))
$taskQa|Add-Member -NotePropertyName 'archived_verified_batch_0_15_75' -NotePropertyValue $taskQa.latest_verified_batch
$taskQa.latest_verified_batch=[pscustomobject]@{version='0.15.76-dev';date='2026-10-16';jar_sha256=$taskHash;jar_bytes=(Get-Item $taskJar).Length;packaged_groups_passed=128;packaged_gate_root=$taskRelative;new_npc_assertions=$taskAssertions;prior_npc75_assertions=64192;prior_npc74_assertions=98024;prior_name_assertions=76275;prior_prepared_assertions=324313;prior_numeric_assertions=1346201;new_exact_npc_and_security_rows=229;new_exact_npc_rows=228;new_exact_security_rows=1;new_row_speakers=17;newly_represented_exact_npc_speakers=8;newly_represented_all_recorded_tag_speakers=9;new_closed_option_layouts=14;exact_npc_source_union_before=1222;exact_npc_source_union_after=1450;exact_all_recorded_tags_source_union_after=1451;exact_npc_speakers_before=127;exact_npc_speakers_after=135;exact_all_recorded_tag_speakers_after=136;indexed_unique_names=572;indexed_names_without_exact_npc_rows=454;indexed_names_without_all_recorded_tag_rows=453;global_untranslated_server_rows=$null;all_npc_branches_complete=$false;report='translation-source/NPC_0.15.76_RU.md';registry='translation-source/NPC_REGISTRY_0.15.76_RU.md';audit='translation-source/NPC_AUDIT_0.15.76.json';interaction_scope='translation-source/NPC_INTERACTION_SCOPE_0.15.76.json';raw_performance='translation-source/PERFORMANCE_0.15.76_RAW.json';jar_entry_scope_proof='translation-source/JAR_SCOPE_0.15.76.json';all_44_prior_resources_byte_identical=$true;unchanged_other_runtime_classes=107;final_benchmark_processes=4;new_frame_hooks=0;new_per_message_providers=0;installed_modpack_modified=$false;new_version_live_fps_or_frame_times_measured=$false;zero_fps_impact_proven=$false;microstutters_fixed_proven=$false;overall_goal_complete=$false}
[IO.File]::WriteAllText("$taskProject/QA_REPORT.json",($taskQa|ConvertTo-Json -Depth 30),[Text.UTF8Encoding]::new($false))
Write-Output "FINALIZED NPC76 $taskHash;229 new rows/17 speakers;128fresh groups;not all NPCs"
