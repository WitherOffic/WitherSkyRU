$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskCapture=Join-Path $taskProject 'build/item-audit64-a4de0b74b10243fba6713d30f70978f7/catalogue.json'
if((Get-FileHash -LiteralPath $taskCapture).Hash -ne '38FBA44F6D2843C3E31B4A682660553CA51C35D40B663DB1F4B4D725B7AD69A6'){throw 'Different source catalogue'}
$taskSource=Get-Content -LiteralPath $taskCapture -Raw|ConvertFrom-Json
$taskSelected=@($taskSource.records|Where-Object {$_.id -match '^TURBO_.*;[1-7]$' -or $_.id -in @('TURBO_GOURD','ENCHANTED_TURBO_GOURD')})
if($taskSelected.Count -ne 93){throw 'Expected91 books and2 upgrade items'}
$taskTarget=Join-Path $taskProject 'translation-source/TURBO_PRIMARY_0.15.65.json'
if(Test-Path -LiteralPath $taskTarget){throw 'Preserve existing primary snapshot'}
[IO.File]::WriteAllText($taskTarget,(@{source_commit=$taskSource.commit;source_catalogue_sha256=(Get-FileHash -LiteralPath $taskCapture).Hash;scope='All91 current I-VII Turbo book cards,13 families, plus both upgrade gourds; not all enchantments or live states';records=$taskSelected}|ConvertTo-Json -Depth 7),[Text.UTF8Encoding]::new($false))
Write-Output $taskTarget
