param([Parameter(Mandatory=$true)][string]$SnapshotRoot)
# Generate read-only audit documents; does not edit translations or installed files.
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskRoot=(Resolve-Path -LiteralPath $SnapshotRoot).Path
$taskBuild=(Join-Path $taskProject 'build')+[IO.Path]::DirectorySeparatorChar
if(!$taskRoot.StartsWith($taskBuild,[StringComparison]::OrdinalIgnoreCase)){throw 'Expected owned snapshot under build'}
$taskPolicyPath=Join-Path $taskProject 'translation-source/CATALOGUE_AUDIT_2026_10_14_REVIEW.json'
$taskPolicy=Get-Content -Raw -LiteralPath $taskPolicyPath|ConvertFrom-Json
if((Get-FileHash -LiteralPath (Join-Path $taskRoot 'catalogue.json')).Hash -ne $taskPolicy.catalogue_sha256 -or (Get-FileHash -LiteralPath (Join-Path $taskRoot 'final62/items.json')).Hash -ne $taskPolicy.audit_items_sha256){throw 'Different data; do not apply reviewed classifications to an unseen snapshot'}
$taskOut=Join-Path $taskRoot ('export-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskOut|Out-Null
$taskRows=Get-Content -Raw -LiteralPath (Join-Path $taskRoot 'final62/items.json')|ConvertFrom-Json
$taskFamilies=@($taskRows|Where-Object {$_.book}|Group-Object {($_.id -split ';')[0]}|Sort-Object Name)
$taskBookText=[Text.StringBuilder]::new()
[void]$taskBookText.AppendLine('# Реестр всех зачарований текущего снимка')
[void]$taskBookText.AppendLine('')
[void]$taskBookText.AppendLine('767 карточек, 156 семейств. Ниже — 195 текстовых форм эффекта. Ручной просмотр представителей не означает полной проверки каждого игрового состояния или цветового акцента. Это текущие результаты 0.15.62, НЕ исправленные переводы.')
[void]$taskBookText.AppendLine('')
[void]$taskBookText.AppendLine('| ID семейства | Название | Карточек | Явный английский в эффекте, хотя бы один путь |')
[void]$taskBookText.AppendLine('|---|---|---:|---:|')
$taskShapes=0;$taskEnglish=0;$taskBadFamilies=0
function Add-Block([Text.StringBuilder]$taskBuilder,$taskLines){[void]$taskBuilder.AppendLine('');foreach($taskLine in $taskLines){[void]$taskBuilder.AppendLine('    '+$taskLine)};[void]$taskBuilder.AppendLine('')}
function Get-EffectEnd($taskRow){$taskN=3;while($taskN -lt $taskRow.source_lore.Count -and $taskRow.source_lore[$taskN] -ne ''){$taskN++};return $taskN}
function Has-EnglishEffect($taskRow){$taskEnd=Get-EffectEnd $taskRow;return (($taskRow.component_lore[3..($taskEnd-1)] -join '|') -match '[A-Za-z]{2,}' -or ($taskRow.sequence_lore[3..($taskEnd-1)] -join '|') -match '[A-Za-z]{2,}')}
foreach($taskGroup in $taskFamilies){$taskCount=@($taskGroup.Group|Where-Object {Has-EnglishEffect $_}).Count;$taskEnglish+=$taskCount;if($taskCount){$taskBadFamilies++};$taskTitle=$taskGroup.Group[0].translated_enchantment_title.Replace('|','\|');[void]$taskBookText.AppendLine('|'+$taskGroup.Name+'|'+$taskTitle+'|'+$taskGroup.Count+'|'+$taskCount+'|')}
foreach($taskGroup in $taskFamilies){
    [void]$taskBookText.AppendLine('');[void]$taskBookText.AppendLine('## '+$taskGroup.Name)
    $taskGrammar=$taskPolicy.further_clear_grammar_examples.PSObject.Properties[$taskGroup.Name];if($taskGrammar){[void]$taskBookText.AppendLine('Замечание: '+$taskGrammar.Value)}
    $taskShapeMap=[Collections.Generic.Dictionary[string,object]]::new([StringComparer]::Ordinal)
    foreach($taskRow in $taskGroup.Group){$taskEnd=Get-EffectEnd $taskRow;$taskKey=[regex]::Replace(($taskRow.source_lore[3..($taskEnd-1)] -join '|'),'[0-9]+(?:[.,][0-9]+)*','#')+' -> '+[regex]::Replace(($taskRow.component_lore[3..($taskEnd-1)] -join '|'),'[0-9]+(?:[.,][0-9]+)*','#');if(!$taskShapeMap.ContainsKey($taskKey)){$taskShapeMap.Add($taskKey,[Collections.Generic.List[object]]::new())};$taskShapeMap[$taskKey].Add($taskRow)}
    foreach($taskShape in $taskShapeMap.Values){$taskShapes++;$taskRow=$taskShape[0];$taskEnd=Get-EffectEnd $taskRow;[void]$taskBookText.AppendLine('');[void]$taskBookText.AppendLine('### Пример '+$taskRow.id);[void]$taskBookText.AppendLine('Тот же тип текста, с отдельной проверкой чисел: '+(($taskShape|ForEach-Object id) -join ', '));[void]$taskBookText.AppendLine('');[void]$taskBookText.AppendLine('Исходный эффект:');Add-Block $taskBookText $taskRow.source_lore[3..($taskEnd-1)];[void]$taskBookText.AppendLine('Текущий Component:');Add-Block $taskBookText $taskRow.component_lore[3..($taskEnd-1)];if(($taskRow.component_lore[3..($taskEnd-1)] -join '|') -ne ($taskRow.sequence_lore[3..($taskEnd-1)] -join '|')){[void]$taskBookText.AppendLine('Отличающийся Sequence:');Add-Block $taskBookText $taskRow.sequence_lore[3..($taskEnd-1)]}}
}
if($taskShapes -ne 195 -or $taskEnglish -ne 401 -or $taskBadFamilies -ne 94){throw 'Reviewed shape scope changed'}
[IO.File]::WriteAllText((Join-Path $taskOut 'ENCHANTMENTS_REVIEW_RU.md'),$taskBookText.ToString(),[Text.UTF8Encoding]::new($false))
$taskAll=[Text.StringBuilder]::new()
[void]$taskAll.AppendLine('# Текущее отображение всех 8937 карточек')
[void]$taskAll.AppendLine('')
[void]$taskAll.AppendLine('Это сплошной автоматический реестр, НЕ сертификат ручной вычитки. Исходные цвета и диагностические диапазоны для срабатываний — в items.json. Для каждого ID приведены все исходные строки и текущий результат; игровые параметры-шаблоны не выдуманы.')
foreach($taskRow in $taskRows){[void]$taskAll.AppendLine('');[void]$taskAll.AppendLine('## '+$taskRow.id);[void]$taskAll.AppendLine('');[void]$taskAll.AppendLine('Название:');Add-Block $taskAll @($taskRow.source_title,$taskRow.translated_title);[void]$taskAll.AppendLine('Оригинал:');Add-Block $taskAll $taskRow.source_lore;[void]$taskAll.AppendLine('Текущий перевод:');Add-Block $taskAll $taskRow.component_lore;if($taskRow.component_sequence_text_difference_candidate){[void]$taskAll.AppendLine('Отличающийся альтернативный путь:');Add-Block $taskAll $taskRow.sequence_lore};if($taskRow.title_format_difference_candidate){[void]$taskAll.AppendLine('Контроль одноцветного названия:');Add-Block $taskAll @($taskRow.uniform_unformatted_title_result);if($taskRow.PSObject.Properties.Name -contains 'synthetic_lvl100_title_difference'){[void]$taskAll.AppendLine('Шаблон питомца: контроль заголовка с уровнем 100, не живой предмет:');Add-Block $taskAll @($taskRow.synthetic_lvl100_title_colored,$taskRow.synthetic_lvl100_title_uniform)}};[void]$taskAll.AppendLine('[Публичный источник]('+ $taskRow.source_url+')')}
[IO.File]::WriteAllText((Join-Path $taskOut 'ALL_ITEMS_CURRENT_DISPLAY_RU.md'),$taskAll.ToString(),[Text.UTF8Encoding]::new($false))
Copy-Item -LiteralPath (Join-Path $taskProject 'translation-source/CATALOGUE_AUDIT_2026_10_14_RU.md') -Destination (Join-Path $taskOut 'README_RU.md')
Copy-Item -LiteralPath $taskPolicyPath -Destination (Join-Path $taskOut 'REVIEW.json')
foreach($taskFile in @('summary.json','items.json','official-names.json','self-test-output.txt')){Copy-Item -LiteralPath (Join-Path $taskRoot "final62/$taskFile") -Destination $taskOut}
Copy-Item -LiteralPath (Join-Path $taskRoot 'manifest.json') -Destination $taskOut
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskZip=[IO.Compression.ZipFile]::OpenRead((Join-Path $taskRoot 'public-repo.zip'))
try{$taskLicense=@($taskZip.Entries|Where-Object {$_.FullName -match '^[^/]+/LICENSE$'});if($taskLicense.Count -ne 1){throw 'Missing public source license'};$taskReader=[IO.StreamReader]::new($taskLicense[0].Open());try{[IO.File]::WriteAllText((Join-Path $taskOut 'SOURCE_LICENSE.txt'),$taskReader.ReadToEnd(),[Text.UTF8Encoding]::new($false))}finally{$taskReader.Dispose()}}finally{$taskZip.Dispose()}
$taskManifest=[ordered]@{source_commit=$taskPolicy.source_commit;runtime_version=$taskPolicy.runtime_version;runtime_jar_sha256=$taskPolicy.runtime_jar_sha256;public_cards=8937;official_names=5680;enchantment_cards=767;families=156;effect_shapes_read=195;confirmed_core_mixed_cards=401;confirmed_core_mixed_families=94;all_descriptions_manually_proofread=$false;runtime_or_translations_changed=$false;files=@(Get-ChildItem -LiteralPath $taskOut -File|ForEach-Object {@{name=$_.Name;sha256=(Get-FileHash -LiteralPath $_.FullName).Hash;bytes=$_.Length}})}
[IO.File]::WriteAllText((Join-Path $taskOut 'EXPORT_MANIFEST.json'),($taskManifest|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Write-Output "AUDIT EXPORT: $taskOut"
Get-ChildItem -LiteralPath $taskOut -File|Select-Object Name,Length
