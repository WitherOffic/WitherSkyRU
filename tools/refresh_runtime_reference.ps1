# Mechanical reference refresh only: do not recalculate frozen corpus coverage.
$ErrorActionPreference = 'Stop'
$taskProject = Split-Path -Parent $PSScriptRoot
$taskData = Join-Path $taskProject 'src/main/resources/data/witherskyru'
$taskPairs = [System.Collections.Generic.SortedDictionary[string,string]]::new([StringComparer]::Ordinal)
$taskResources = @('phrases.tsv','vanilla-names.tsv','lexicon-names.tsv','curated.tsv','lore.tsv','continuation.tsv','enchantments.tsv','names.tsv','pet-lore.tsv','catalogue.tsv','corrections.tsv','quality-fixes.tsv','proofread.tsv','quest-interface.tsv')
foreach ($taskResource in $taskResources) {
    foreach ($taskLine in [IO.File]::ReadAllLines((Join-Path $taskData $taskResource))) {
        if ([string]::IsNullOrWhiteSpace($taskLine) -or $taskLine.StartsWith('#')) { continue }
        $taskFields = $taskLine.Split([char]9, 2)
        if ($taskFields.Count -ne 2) { throw "Malformed pair in $taskResource" }
        $taskPairs[$taskFields[0].Trim()] = $taskFields[1].Trim()
    }
}
$taskTemplateCount = 0
foreach ($taskName in @('proofread-patterns.tsv','editorial-patterns.tsv','pet-lore-patterns.tsv','continuation-patterns.tsv','expanded-patterns.tsv','canonical-patterns.tsv')) {
    $taskTemplateCount += @([IO.File]::ReadAllLines((Join-Path $taskData $taskName)) | Where-Object { ![string]::IsNullOrWhiteSpace($_) -and !$_.StartsWith('#') }).Count
}
$taskCoverageFile = Join-Path $taskProject 'translation-source/coverage.json'
$taskReferenceFile = Join-Path $taskProject 'translation-source/translations.json'
$taskCoverage = Get-Content -LiteralPath $taskCoverageFile -Raw | ConvertFrom-Json -AsHashtable
$taskCoverage.runtime_dictionary_entries = $taskPairs.Count
$taskCoverage.runtime_templates = $taskTemplateCount
$taskCoverage.runtime_identity_entries = @($taskPairs.Keys | Where-Object { $_ -ceq $taskPairs[$_] }).Count
$taskVersion = (Get-Content -LiteralPath (Join-Path $taskProject 'src/main/resources/fabric.mod.json') -Raw | ConvertFrom-Json).version
$taskCoverage['runtime_reference_version'] = $taskVersion
$taskCoverage['snapshot_coverage_note'] = "Frozen snapshot statistics above have not been recalculated for $taskVersion; runtime entry counts are current. Full server denominator remains unknown."
$taskReference = Get-Content -LiteralPath $taskReferenceFile -Raw | ConvertFrom-Json -AsHashtable
$taskReference.coverage = $taskCoverage
$taskReference.translations = $taskPairs
$taskReforgeForms = [ordered]@{}
foreach ($taskLine in [IO.File]::ReadAllLines((Join-Path $taskData 'reforges.tsv'))) {
    if ([string]::IsNullOrWhiteSpace($taskLine) -or $taskLine.StartsWith('#')) { continue }
    $taskFields = $taskLine.Split([char]9)
    $taskReforgeForms[$taskFields[0]] = @($taskFields[1..4])
}
$taskReference.reforge_forms = $taskReforgeForms
$taskGenders = [System.Collections.Generic.SortedDictionary[string,string]]::new([StringComparer]::Ordinal)
foreach ($taskName in @('item-genders.tsv','item-gender-overrides.tsv')) {
    foreach ($taskLine in [IO.File]::ReadAllLines((Join-Path $taskData $taskName))) {
        if ([string]::IsNullOrWhiteSpace($taskLine) -or $taskLine.StartsWith('#')) { continue }
        $taskFields = $taskLine.Split([char]9)
        if ($taskFields.Count -ne 2) { throw "Malformed item gender in $taskName" }
        $taskGenders[$taskFields[0]] = $taskFields[1]
    }
}
$taskReference.item_genders = $taskGenders
[IO.File]::WriteAllText($taskCoverageFile, ($taskCoverage | ConvertTo-Json -Depth 30) + "`n", [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText($taskReferenceFile, ($taskReference | ConvertTo-Json -Depth 30) + "`n", [Text.UTF8Encoding]::new($false))
Write-Output "Reference: $($taskPairs.Count) runtime keys, $taskTemplateCount templates; frozen coverage unchanged."
