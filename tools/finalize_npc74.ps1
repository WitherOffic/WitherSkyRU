param([Parameter(Mandatory=$true)][string]$CheckRoot,[Parameter(Mandatory=$true)][string]$BenchmarkRoot,[switch]$SupersedePreliminary)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskChecks=(Resolve-Path $CheckRoot).Path
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.74-dev.jar";$taskHash=(Get-FileHash $taskJar).Hash
$taskScope=Get-Content "$taskChecks/CHECK_SCOPE.json" -Raw|ConvertFrom-Json
if($taskScope.runtime -ne '0.15.74-dev' -or $taskScope.targeted_only -or $taskScope.tests_passed.Count -ne 126 -or @($taskScope.tests_passed|Sort-Object -Unique).Count -ne 126 -or $taskScope.runtime_jar_sha256 -ne $taskHash){throw 'Incomplete or different final executable gate'}
if([regex]::Match((Get-Content "$taskChecks/TestNpc74-output.txt" -Raw),'(\d+) assertions').Groups[1].Value -ne '98024'){throw 'Missing new NPC checks'}
$taskQa=Get-Content "$taskProject/QA_REPORT.json" -Raw|ConvertFrom-Json
if($SupersedePreliminary){
 if($taskQa.latest_verified_batch.version -ne '0.15.74-dev' -or $taskQa.latest_verified_batch.jar_sha256 -ne '90848FCBD44517854BAEDA300004E054926DC17B6B0D00ECA302EA42E5AAFF07' -or $taskQa.archived_verified_batch_0_15_73.version -ne '0.15.73-dev'){throw 'Unexpected preliminary finalization'}
 $taskQa|Add-Member -NotePropertyName 'superseded_preliminary_0_15_74_before_terminology_review' -NotePropertyValue $taskQa.latest_verified_batch
}elseif($taskQa.latest_verified_batch.version -ne '0.15.73-dev'){throw 'Preserve prior finalization'}
$taskProof=Get-Content "$taskProject/translation-source/JAR_SCOPE_0.15.74.json" -Raw|ConvertFrom-Json
if($taskProof.after_sha256 -ne $taskHash -or $taskProof.old_translation_and_asset_resources_byte_identical -ne 42 -or !$taskProof.all_frame_world_input_item_dictionary_and_prepared_cache_classes_byte_identical -or $taskProof.new_frame_hooks -ne 0){throw 'Missing byte identity scope proof'}
$taskBench=Get-Content (Join-Path $BenchmarkRoot 'RAW.json') -Raw|ConvertFrom-Json
if($taskBench.records.Count -ne 4){throw 'Incomplete benchmark'};foreach($taskRun in $taskBench.records){$taskExpected=if($taskRun.version -eq '0.15.73-dev'){$taskProof.before_sha256}else{$taskHash};if($taskRun.jar_sha256 -ne $taskExpected){throw 'Benchmark executable mismatch'}}
$taskRuntime=Get-Content "$taskProject/translation-source/NPC74_RETAINED_RUNTIME_PROOF.json" -Raw|ConvertFrom-Json
if($taskRuntime.runs.Count -ne 2 -or $taskRuntime.runs[0].jar_sha256 -ne $taskProof.before_sha256 -or $taskRuntime.runs[1].jar_sha256 -ne $taskHash){throw 'Missing retained runtime source proof'}
$taskAudit=Get-Content "$taskProject/translation-source/NPC_AUDIT_0.15.74.json" -Raw|ConvertFrom-Json
if($taskAudit.new_exact_rows -ne 239 -or $taskAudit.new_exact_row_speakers -ne 31 -or $taskAudit.exact_unique_sources_before -ne 883 -or $taskAudit.exact_unique_sources_after -ne 1122 -or $taskAudit.indexed_unique_names -ne 572){throw 'Wrong audited scope'}
$taskPerformance=@{version='0.15.74-dev';jar_sha256=$taskHash;date='2026-10-15';scope='Four isolated JVMs on delivered73/final74 inABBA order. All wall-time,CPU-clock and allocation rounds retained. New positives do extra translation work;not equal-output timing. Receipt only,not game FPS,frame timing,GPU or0.1%low.';results=$taskBench;actual_game_fps_measured=$false;zero_fps_impact_proven=$false}
if($SupersedePreliminary){$taskPreliminary=Get-Content "$taskProject/build/delivery74-3ca2f194d1f14599b7a07f508f3466c6/PERFORMANCE_0.15.74_PRELIMINARY.json" -Raw|ConvertFrom-Json;if($taskPreliminary.jar_sha256 -ne '90848FCBD44517854BAEDA300004E054926DC17B6B0D00ECA302EA42E5AAFF07'){throw 'Wrong preliminary evidence'};$taskPerformance.superseded_preliminary_not_final_evidence=$taskPreliminary}
[IO.File]::WriteAllText("$taskProject/translation-source/PERFORMANCE_0.15.74_RAW.json",($taskPerformance|ConvertTo-Json -Depth 22),[Text.UTF8Encoding]::new($false))
$taskFmt=[Globalization.CultureInfo]::InvariantCulture;$taskTable=@('| Сценарий | Одинаковый полезный результат | Wall-время: два порядка | После, мкс/сообщение |','| --- | --- | --- | --- |')
foreach($taskRow in $taskBench.summary){$taskA=$taskRow.orders[0];$taskB=$taskRow.orders[1];$taskTable+='| '+$taskRow.mode+' | '+$(if($taskRow.equal_useful_output){'да'}else{'нет: добавлен перевод'})+' | '+$taskA.wall_change_percent.ToString('F2',$taskFmt)+'% / '+$taskB.wall_change_percent.ToString('F2',$taskFmt)+'% | '+($taskA.after_ns/1000).ToString('F3',$taskFmt)+' / '+($taskB.after_ns/1000).ToString('F3',$taskFmt)+' |'}
$taskCounts=@('| NPC | Новых точных реплик |','| --- | ---: |');$taskResource=Get-Content "$taskProject/src/main/resources/data/witherskyru/npc-expansion74.json" -Raw|ConvertFrom-Json
foreach($taskGroup in ($taskResource.records|Group-Object speaker|Sort-Object Name)){$taskCounts+='| '+$taskGroup.Name+' | '+$taskGroup.Count+' |'}
$taskRelative=$taskChecks.Substring($taskProject.Length+1).Replace('\','/')
$taskReport=@"
# WitherSkyRU 0.15.74-dev — продолжение перевода NPC

2026-10-15. Minecraft 26.1.2 / Fabric. Отдельная сборка, не установленная автоматически.
Ключевая задача «каждый NPC и все его ветки» ещё не завершена.

## Сделано

239 новых точных реплик для 31 NPC. У 28 из этих говорящих персонажей раньше
не было точных строк в ресурсах. Знакомства, торговля, поручения, Сад и Абифон.
Также поддержан один точный вариант ответа лесорубу: «Конечно / Нет, спасибо».
Как и раньше, эти метки вариантов зависят от переключателя игровых уведомлений;
сами диалоги NPC от него не зависят.

Семь ранее поддержанных строк оставлены с прежними целями. Одна из них —
числовой результат Люциуса: он уже обслуживался старым обработчиком, поэтому
не включён в 239 новых. Это независимо перепроверено на выпущенной 73 и новой 74.
Первоначальная предварительная сборка с 240 записями отклонена; она не выдана.
После финальной вычитки также заменена предварительная 239-строчная сборка
90848FCB: термины новых реплик согласованы с действующим словарём и старой
учебной цепочкой. Это «лесорубство», «перспективный топор», «мешок рун»,
«представитель отряда безопасности» и обращение Сьюзан на «вы».
Предварительные файлы сохранены отдельно, а не удалены; использовать JAR
с итоговым SHA256 внизу отчёта. Сведения — NPC_TERMS_0.15.74.json.

$( $taskCounts -join "`n" )

## Честный учёт охвата

| Показатель | До | После |
| --- | ---: | ---: |
| Точные уникальные строки с тегом NPC в ресурсах | 883 | 1122 |
| Говорящие персонажи с хотя бы одной точной строкой | 94 | 122 |
| Названия из индекса, сопоставленные с такими строками | 77 | 105 |

Это три разных величины. «Есть строка» не означает «весь NPC готов».
Список содержит 572 уникальных названия и 590 членств в разделах, включая
33 названия в явно архивной группе. Он взят из навигации community wiki,
а не из полного актуального реестра серверных скриптов. Разные версии одного
имени, например в Разломе и обычном мире, не склеиваются без проверки.

У 467 названий индекса нет точных строк в этой объединённой базе. Это не
доказанный счётчик непереведённых NPC: часть может обслуживаться другими
обработчиками, не иметь диалога или относиться к архиву. Полный остаток строк
сервера неизвестен, поэтому глобальный процент готовности не вычисляется.

Реестр NPC_REGISTRY_0.15.74_RU.md показывает все названия и отдельно 32
исследованные страницы. У Джорджа остаются 43 опубликованных точных кандидата
без записи, у лесоруба — 6; другие пропуски видны в таблице. Имена игрока,
названия наборов, переменные счётчики и неоднозначные границы сообщений
помечены отдельно. Шаблон с буквальным [player] не выдаётся за работающий перевод.
Для Smithmonger раздел диалога не получен — это неизвестность, не «нулевой остаток».
Сами голоса, все меню, мировые имена, весь Абифон и все живые ветки не объявлены готовыми.

## Источники и актуальность

В этой партии использована действующая community wiki:
https://hypixelskyblock.minecraft.wiki/w/NPC
Каждая новая запись содержит ссылку на свою страницу; отдельный независимый
снимок опубликованных разделов — NPC_SOURCES_0.15.74.json. Русские цели
вычитаны в NPC_0.15.74_REVIEWED.tsv. Исторические варианты не подмешаны в
современные кандидаты; публикация сама по себе не доказывает текущий живой пакет.

Старая официальная wiki закрыта; поисковая выдача ещё показывает её сохранённые
страницы. Они не использованы как подтверждение актуальности этой партии:
https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/

Новые адаптации npc-expansion74.json атрибутированы авторам community wiki
и распространяются отдельно от MIT-кода по CC BY-NC-SA 3.0. Ссылки и сведения
об изменениях включены в JAR и THIRD_PARTY_NOTICES.md. Это не официальный продукт Hypixel.

## Проверки

126 уникальных групп прошли свежий прогон на одном окончательном JAR;
старые успехи из других версий не подставлялись. Новый тест — 98 024 проверки.
Сверены независимые исходники/цели, полные выходные строки, фрагментированные
Component, два набора свежих стилей, цвета, тени, жирность, hover/click/insertion,
числа, временные метки, счётчики повторов, legacy-коды, пользовательские
переопределения, подписи, каналы и выключатели. Команды кнопок не выполняются.

Сохранены прежние проверки NPC, предметов, зачарований, ввода, поиска и HUD,
а также 76 275 проверок защиты имён, 324 313 готовых планов и 1 346 201 числового пути.
В старых тестах изменено только ожидаемое суммарное число записей общего
индекса: 581 + 239 = 820. Их старые корпуса и ожидаемые переводы не переписаны.
Fabric преобразовал 10 классов Minecraft в изолированной проверке; это не
запуск игры или доказательство совместимости со всеми сторонними модами.

Цветовые раскладки нового теста синтетические. Реальные пакеты и все варианты
выделения сервера не получены. Неизвестное разноцветное сообщение безопасно
остаётся исходным, если соответствия выделений нельзя доказать; это остаётся
потенциальным пропуском, а не замалчиваемой «100% готовностью».

## Оптимизация

Новые строки объединяются с существующей картой story при запуске. Нового
прохода сообщений, кадрового обработчика, кэша контекста или сетевого обращения нет.
Новый вариант ответа добавлен в прежнюю закрытую таблицу, без нового regex.
Оптимизации 73 сохранены: все 107 остальных классов, включая обработку кадров,
F3, мира, предметов, ввода, словаря и готовых планов, побайтно совпадают с 73.
Все 42 прежних ресурса перевода/графики также неизменны. Добавлен только новый
ресурс NPC и файл атрибуции; данные этой партии занимают $((Get-Item "$taskProject/src/main/resources/data/witherskyru/npc-expansion74.json").Length) байт несжатого JSON.

Четыре отдельных JVM, порядок 73,74,74,73; прогрев 200 000 вызовов на сценарий,
четыре раунда по 40 000 вызовов. Все wall/CPU/allocation раунды сохранены.
Это вставка сообщений в чат, не отрисовка шрифтов, мир, GPU или игровой FPS.

$( $taskTable -join "`n" )

Новые положительные сценарии выполняют дополнительную работу: раньше реплика
не переводилась. У new_intro и новых телефонных реплик могут быть дополнительные
затраты, это не сравнение одинаковой полезной работы. Обычные неизменённые
сценарии сопоставимы. Все результаты, включая ухудшения, указаны в таблице.
Нулевое влияние на FPS, исчезновение микрофризов и улучшение 0,1% low не доказаны.

## Выдача

JAR: WitherSkyRU-26.1.2-fabric-0.15.74-dev.jar
SHA256: $taskHash
Размер: $((Get-Item $taskJar).Length) байт.
Проверки: $taskRelative

Сборка игры, настройки и установленные моды не изменены. Для ручной проверки
заменить прежний JAR, не оставляя две версии. Личные JFR, полные журналы и
пользовательские сообщения в эту выдачу не добавлены.
"@
[IO.File]::WriteAllText("$taskProject/translation-source/NPC_0.15.74_RU.md",$taskReport,[Text.UTF8Encoding]::new($false))
if(!$SupersedePreliminary){$taskQa|Add-Member -NotePropertyName 'archived_verified_batch_0_15_73' -NotePropertyValue $taskQa.latest_verified_batch}
$taskQa.latest_verified_batch=[pscustomobject]@{version='0.15.74-dev';date='2026-10-15';jar_sha256=$taskHash;jar_bytes=(Get-Item $taskJar).Length;packaged_groups_passed=126;packaged_gate_root=$taskRelative;new_npc_assertions=98024;prior_name_assertions=76275;prior_prepared_assertions=324313;prior_numeric_assertions=1346201;new_exact_npc_rows=239;new_row_speakers=31;newly_represented_exact_speakers=28;retained_existing_targets=7;exact_npc_source_union_before=883;exact_npc_source_union_after=1122;exact_npc_speakers_before=94;exact_npc_speakers_after=122;indexed_unique_names=572;indexed_names_without_exact_rows=467;global_untranslated_server_rows=$null;all_npc_branches_complete=$false;runtime_dictionary_entries=36044;runtime_tsv_templates=1231;report='translation-source/NPC_0.15.74_RU.md';registry='translation-source/NPC_REGISTRY_0.15.74_RU.md';audit='translation-source/NPC_AUDIT_0.15.74.json';terminology_review='translation-source/NPC_TERMS_0.15.74.json';raw_performance='translation-source/PERFORMANCE_0.15.74_RAW.json';jar_entry_scope_proof='translation-source/JAR_SCOPE_0.15.74.json';all_42_prior_resources_byte_identical=$true;unchanged_other_runtime_classes=107;final_benchmark_processes=4;new_frame_hooks=0;new_per_message_providers=0;installed_modpack_modified=$false;new_version_live_fps_or_frame_times_measured=$false;zero_fps_impact_proven=$false;microstutters_fixed_proven=$false;overall_goal_complete=$false}
[IO.File]::WriteAllText("$taskProject/QA_REPORT.json",($taskQa|ConvertTo-Json -Depth 27),[Text.UTF8Encoding]::new($false));Write-Output "FINALIZED NPC74 $taskHash;239 new rows/31 speakers;126 groups;not all NPCs"
