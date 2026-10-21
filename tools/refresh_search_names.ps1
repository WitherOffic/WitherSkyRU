$ErrorActionPreference = 'Stop'
$taskProject = Split-Path $PSScriptRoot -Parent
$taskSnapshot = Join-Path $taskProject 'translation-source/item-context.json'
$taskOutput = Join-Path $taskProject 'src/main/resources/data/witherskyru/search-item-names.txt'
$taskItems = Get-Content -LiteralPath $taskSnapshot -Raw | ConvertFrom-Json
$taskNames = @($taskItems | ForEach-Object { $_.name -replace '(?i)§[0-9a-fk-or]', '' } |
    Where-Object { $_ -and $_.Length -le 100 -and $_ -notmatch '[\r\n\t]' } | Sort-Object -Unique)
[IO.File]::WriteAllLines($taskOutput, $taskNames, [Text.UTF8Encoding]::new($false))
Write-Output "Embedded $($taskNames.Count) original item names from the local snapshot. No prices, player data or network."
