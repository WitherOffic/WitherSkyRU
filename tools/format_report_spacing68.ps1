# Mechanical typography only, never source/runtime/fixture data.
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
foreach($taskRelative in @('README_RU.md','CHANGELOG_RU.md','translation-source/DUNGEONS_0.15.68_RU.md')){
    $taskPath=Join-Path $taskProject $taskRelative
    $taskText=[IO.File]::ReadAllText($taskPath)
    $taskText=[regex]::Replace($taskText,'(?<=\p{IsCyrillic})(?=[0-9])|(?<=[0-9])(?=\p{IsCyrillic})',' ')
    [IO.File]::WriteAllText($taskPath,$taskText,[Text.UTF8Encoding]::new($false))
}
