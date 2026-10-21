param([string]$AuditRoot='build/five-pass117-20261021')
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskAudit=(Resolve-Path (Join-Path $taskProject $AuditRoot)).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.115-dev.jar"
$taskArgs=Get-Content "$taskProject/build/npc106-checks-18089a61d6f44d749d0337c2b08ef206/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskProbe=Join-Path $taskAudit ('runtime-probe-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskProbe|Out-Null
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp ((@($taskJar)+$taskLibs)-join ';') -d $taskProbe "$taskProject/tests/AuditFivePass117.java"
if($LASTEXITCODE -ne 0){throw 'Probe compilation failed'}
& "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp ((@($taskProbe,$taskJar)+$taskLibs)-join ';') AuditFivePass117 $taskProbe "$taskAudit/PROBE_INPUT117.jsonl" > "$taskProbe/console.txt" 2>&1
if($LASTEXITCODE -ne 0){Get-Content "$taskProbe/console.txt" -Tail 15;throw 'Runtime source screening failed'}
Get-Content "$taskProbe/RUNTIME_SUMMARY.json"
Write-Output "FIVE PASS117 PROBE:$taskProbe"
