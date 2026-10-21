param([Parameter(Mandatory=$true)][string]$LogPath)
$ErrorActionPreference = 'Stop'
$taskProject = Split-Path $PSScriptRoot -Parent
$taskCatalog = Get-Content -LiteralPath (Join-Path $taskProject 'translation-source/NPC_DIALOGUES_RU.json') -Raw | ConvertFrom-Json
$taskAllowed = @{}
foreach ($taskRecord in $taskCatalog.records) {
 if ($taskRecord.source_key -like 'local-log.0.15.14.*') { $taskAllowed[$taskRecord.source] = $taskRecord.translation }
}
$taskFound = @{}
foreach ($taskLine in Get-Content -LiteralPath $LogPath) {
 if ($taskLine -notmatch '\[CHAT\] (?:\[\d{2}:\d{2}:\d{2}\] )?(\[NPC\] (?:Jeff|Bramass Beastslayer): .+)$') { continue }
 $taskRaw = $Matches[1]
 $taskPlain = $taskRaw -replace '(?i)§[0-9a-fk-or]', ''
 if ($taskAllowed.ContainsKey($taskPlain) -and !$taskFound.ContainsKey($taskPlain)) {
  $taskFound[$taskPlain] = [ordered]@{ source = $taskRaw; translation = $taskAllowed[$taskPlain] }
 }
}
if ($taskFound.Count -ne $taskAllowed.Count) { throw "Expected $($taskAllowed.Count) exact public NPC samples, got $($taskFound.Count)" }
$taskOutput = Join-Path $taskProject 'translation-source/NPC_0.15.14_STYLED_SAMPLES.json'
$taskJson = ConvertTo-Json -InputObject @($taskFound.Keys | Sort-Object | ForEach-Object { $taskFound[$_] }) -Depth 4
[IO.File]::WriteAllText($taskOutput, $taskJson + "`n", [Text.UTF8Encoding]::new($false))
Write-Output "Saved $($taskFound.Count) exact public NPC samples; no other chat exported."
