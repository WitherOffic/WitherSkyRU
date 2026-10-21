param([string]$AuditRoot='build/five-pass117-20261021')
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskAudit=(Resolve-Path (Join-Path $taskProject $AuditRoot)).Path
$taskRepo="$taskAudit/repositories/NotEnoughUpdates--NotEnoughUpdates-REPO"
$taskCommit=(Get-Content "$taskRepo/commit.json" -Raw|ConvertFrom-Json).sha
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskArgs=Get-Content "$taskProject/build/npc106-checks-18089a61d6f44d749d0337c2b08ef206/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskRoot=Join-Path $taskAudit 'native-snbt-reader'
New-Item -ItemType Directory -Path $taskRoot -Force|Out-Null
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp ($taskLibs -join ';') -d $taskRoot "$taskProject/tests/CaptureRepositoryNbt117.java"
if($LASTEXITCODE -ne 0){throw 'Native SNBT reader compile failed'}
& "$taskJava/java.exe" '-Xmx512m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -cp ((@($taskRoot)+$taskLibs)-join ';') CaptureRepositoryNbt117 "$taskRepo/$taskCommit.zip" $taskRoot > "$taskRoot/console.txt" 2>&1
if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/console.txt" -Tail 15;throw 'SNBT extraction failed'}
Get-Content "$taskRoot/SNBT_SUMMARY.json"
