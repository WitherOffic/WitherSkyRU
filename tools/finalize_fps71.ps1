param([Parameter(Mandatory=$true)][string]$CheckRoot,[Parameter(Mandatory=$true)][string]$BenchmarkRoot,[Parameter(Mandatory=$true)][string]$PilotRoot)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskChecks=(Resolve-Path -LiteralPath $CheckRoot).Path;$taskBench=(Resolve-Path -LiteralPath $BenchmarkRoot).Path;$taskPilot=(Resolve-Path -LiteralPath $PilotRoot).Path
$taskScope=Get-Content "$taskChecks/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.71-dev.jar";$taskHash=(Get-FileHash $taskJar).Hash
if($taskScope.runtime -ne '0.15.71-dev' -or $taskScope.targeted_only -or $taskScope.tests_passed.Count -ne 123 -or $taskScope.runtime_jar_sha256 -ne $taskHash){throw 'Different/incomplete executable gate'}
$taskQa=Get-Content "$taskProject/QA_REPORT.json" -Raw|ConvertFrom-Json
if($taskQa.latest_verified_batch.version -ne '0.15.70-dev'){throw 'Preserve existing finalization'}
$taskProof=Get-Content "$taskProject/translation-source/JAR_SCOPE_0.15.71.json" -Raw|ConvertFrom-Json
if($taskProof.after_sha256 -ne $taskHash -or !$taskProof.all_translation_resources_byte_identical){throw 'Missing resource proof'}
$taskMain=Get-Content "$taskBench/RAW.json" -Raw|ConvertFrom-Json;$taskPreliminary=Get-Content "$taskPilot/RAW.json" -Raw|ConvertFrom-Json
foreach($taskRun in @($taskMain,$taskPreliminary)){if($taskRun.runtime_hashes.'0.15.71-dev' -ne $taskHash -or $taskRun.runtime_hashes.'0.15.70-dev' -ne $taskProof.before_sha256){throw 'Wrong benchmark executable'}}
$taskRaw=@{version='0.15.71-dev';jar_sha256=$taskHash;scope='Four main five-mode processes plus four earlier four-mode pilot processes. Every measured round retained. Same executable. These numbers do not measure game FPS, frame times,0.1%low or modpack compatibility.';main=$taskMain;pilot=$taskPreliminary}
[IO.File]::WriteAllText("$taskProject/translation-source/PERFORMANCE_0.15.71_RAW.json",($taskRaw|ConvertTo-Json -Depth 18),[Text.UTF8Encoding]::new($false))
$taskAssertions=[regex]::Match((Get-Content "$taskChecks/TestFrameNoops71-output.txt" -Raw),'(\d+) assertions').Groups[1].Value
if($taskAssertions -ne '1346201'){throw 'Missing new-path assertions'}
$taskTable=@();foreach($taskRow in $taskMain.summary){$taskA=$taskRow.orders[0];$taskB=$taskRow.orders[1];$taskFmt=[Globalization.CultureInfo]::InvariantCulture;$taskTable+='| '+$taskRow.mode+' | '+$taskA.cpu_change_percent.ToString('F2',$taskFmt)+'% / '+$taskB.cpu_change_percent.ToString('F2',$taskFmt)+'% | '+$taskA.before_bytes.ToString('F1',$taskFmt)+' / '+$taskB.before_bytes.ToString('F1',$taskFmt)+' | '+$taskA.after_bytes.ToString('F1',$taskFmt)+' / '+$taskB.after_bytes.ToString('F1',$taskFmt)+' |'}
$taskRelative=$taskChecks.Substring($taskProject.Length+1).Replace('\','/')
$taskReport=@"
# WitherSkyRU 0.15.71-dev: уменьшение затрат HUD, 2026-10-15

Статус: сборка проверена отдельно от игры. Исчезновение микрофризов,
нулевое влияние на FPS и улучшение 0,1% low **не подтверждены**.
Установленный JAR и настройки игры не менялись; в запущенной игре была 0.15.70-dev.

## Что исправлено

- Числовые последовательности HUD проходят точный прежний список символов
  до разбора стека и построения цветных фрагментов. Нет пропуска неизвестного
  английского или смешанного текста. Цвета, числа и действия исходника сохраняются.
- RenderContext, TechnicalText и Scoreboard Overhaul оставляют пустые локальные
  ячейки вместо удаления/повторного выделения. Snapshot, текст и границы между
  вызовами не сохраняются; вложенность и восстановление при ошибках проверены.
- Переводы не добавлялись и не удалялись: все 42 ресурса перевода/графики
  побайтно совпадают с 70. F3 остаётся исходным.

## Что действительно измерено в игре с 70

Три наблюдательных записи: 30, 30 и 45 секунд. Не сравнение с игрой без мода.
В последней записи счётчик раз в секунду дал 118–156 FPS, среднее 140,56.
Это **не время отдельных кадров**: такой счётчик не исключает микрофризов.
Последняя запись содержит 17 пауз GC с максимумом 5,72 мс. В первой максимум
GC был 13,32 мс; начальные ожидания ограничителя кадров доходили до 90,18 мс
в цепочке FramerateLimiter/fpsreducer/ixeris. Их нельзя объявлять ценой перевода.
Запуск/остановка JFR тоже могут приостановить JVM.

В последней записи 32 из 2145 выборочных стеков игрового потока содержали
код мода или внедрённые обработчики (1,49%). Это включающая доля выборки,
в том числе перенаправленная штатная работа, а не потеря FPS или точная цена
мода. Редкие тяжёлые кадры такой выборкой могут быть пропущены.
Общая причина всех микрофризов не установлена. Нет замера GPU/present,
распределения времени кадров, 1%/0,1% low или игрового A/B новой версии.

## Изолированное сравнение 70 и 71

Основной тест: четыре отдельных процесса в порядке 70,71,71,70.
По 20 тысяч прогревочных пакетов, четыре измерения по 10 тысяч пакетов;
каждый пакет — 10 вызовов. Ранее выполненный четырёхрежимный пилот
сохранён целиком рядом с основными результатами. Таблица — основной тест,
два порядка сравнения, наносекунды/байты на вызов. Это **не прирост FPS**.

| Набор | Изменение времени CPU | Байты до | Байты после |
| --- | --- | --- | --- |
$($taskTable -join "`n")

Числовой путь в этом тесте перестал выделять память. Смешанный набор
ускорился примерно на 35–37%; это локальная обработка текста, а не всей игры.
Текстовый контроль не заменялся числовым: на нём нет замедления в обоих порядках.
Выделения служебных объектов зависят также от решений JIT, поэтому пилот
и все отдельные измерения сохранены, а не сведены к лучшему результату.

## Проверки и границы

- 123 группы на одном JAR; новый тест — 1 346 201 проверка.
- Сравнены все 36 044 ключа словаря с 70: текст, стили/действия и возврат
  исходного объекта. Проверены кодовые точки Unicode, 20 000 разрезанных
  последовательностей и ошибки сторонних последовательностей.
- Проверены вложенность, очистка контекстов, пользовательский ввод,
  старые NPC, предметы, зачарования, HUD, поиск и подсказки.
- Fabric преобразовал 10 классов Minecraft с реальными перехватами;
  проверка не запускала игру и не доказывает совместимость со всеми модами сборки.
- Общий перевод SkyBlock не закончен. Его качество не объявляется полным.

JAR SHA256: $taskHash
Проверки: $taskRelative
Подробности: JAR_SCOPE_0.15.71.json, PERFORMANCE_0.15.71_RAW.json,
FPS_LIVE_0.15.70_2026_10_15.json. Личные JFR и полные журналы не публикуются.

## Следующий игровой тест

После ручной замены только JAR на 71 сравнить плавность с 70 в одном месте,
с одинаковыми настройками, шейдером, направлением камеры и нагрузкой.
Закрыть F3 и дать игре прогреться. Сравнивать время кадров и редкие провалы,
а не один снимок среднего FPS. Не менять одновременно другие моды/настройки.
Автоматические тесты и сборка сами создают нагрузку; их не проводить во время
игрового сравнения. До такого теста проблема плавности остаётся неподтверждённо исправленной.
"@
[IO.File]::WriteAllText("$taskProject/translation-source/FPS_0.15.71_RU.md",$taskReport,[Text.UTF8Encoding]::new($false))
$taskQa|Add-Member -NotePropertyName 'archived_verified_batch_0_15_70' -NotePropertyValue $taskQa.latest_verified_batch
$taskQa.latest_verified_batch=[pscustomobject]@{version='0.15.71-dev';date='2026-10-15';jar_sha256=$taskHash;jar_bytes=(Get-Item $taskJar).Length;packaged_groups_passed=123;packaged_gate_root=$taskRelative;new_path_assertions=1346201;report='translation-source/FPS_0.15.71_RU.md';raw_performance='translation-source/PERFORMANCE_0.15.71_RAW.json';live_observation='translation-source/FPS_LIVE_0.15.70_2026_10_15.json';jar_entry_scope_proof='translation-source/JAR_SCOPE_0.15.71.json';runtime_dictionary_entries=36044;runtime_tsv_templates=1231;new_translation_entries=0;all_42_resources_byte_identical=$true;new_frame_hooks=0;new_network_calls=0;installed_modpack_modified=$false;game_started=$false;new_version_game_fps_measured=$false;new_version_frame_time_0_1_low_measured=$false;microstutter_cause_established=$false;zero_fps_impact_proven=$false;overall_goal_complete=$false}
[IO.File]::WriteAllText("$taskProject/QA_REPORT.json",($taskQa|ConvertTo-Json -Depth 25),[Text.UTF8Encoding]::new($false))
Write-Output "FINALIZED FPS71 $taskHash;123 groups; no live new-version frame-time proof"
