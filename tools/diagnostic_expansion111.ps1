$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/expansion111-diagnostic-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/opt96-checks-fc41dfc90eed4bf39a58382bb736fc55/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskAfter="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.111-dev.jar";$taskHash=(Get-FileHash $taskAfter).Hash
# Explicitly post-hoc: retain original15-mode and predeclared3-mode results.
# This adds the noisy boss mode to the three unchanged long controls.
$taskSource=[IO.File]::ReadAllText("$taskProject/tests/BenchmarkExpansion111Long.java").Replace('BenchmarkExpansion111Long','BenchmarkExpansion111Diagnostic')
$taskOld='Set.of("existing_dynamic","unknown_system_notifications","retained_cosmetic_notifications")'
$taskNew='Set.of("existing_dynamic","unknown_system_notifications","retained_cosmetic_notifications","retained_boss_dialogues")'
if(-not $taskSource.Contains($taskOld)){throw 'Diagnostic source selection missing'}
$taskSource=$taskSource.Replace($taskOld,$taskNew).Replace('Predeclared3 equal-work modes','Post-hoc4 equal-work diagnostic modes')
[IO.File]::WriteAllText("$taskRoot/BenchmarkExpansion111Diagnostic.java",$taskSource,[Text.UTF8Encoding]::new($false))
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp ((@($taskAfter)+$taskLibs)-join ';') -d $taskRoot "$taskRoot/BenchmarkExpansion111Diagnostic.java"
if($LASTEXITCODE -ne 0){throw 'Diagnostic harness compilation failed'}
$taskRuns=@();$taskIndex=0
foreach($taskItem in @(
 @{version='0.15.110-dev';escape_analysis=$true},@{version='0.15.111-dev';escape_analysis=$true},
 @{version='0.15.111-dev';escape_analysis=$true},@{version='0.15.110-dev';escape_analysis=$true},
 @{version='0.15.110-dev';escape_analysis=$false},@{version='0.15.111-dev';escape_analysis=$false}
)){
 $taskVersion=$taskItem.version;$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskFile="$taskRoot/$taskIndex-$taskVersion.json"
 $taskFlags=@('-Xms256m','-Xmx256m','-Djava.awt.headless=true','-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8')
 if(-not $taskItem.escape_analysis){$taskFlags+= '-XX:-DoEscapeAnalysis'}
 & "$taskJava/java.exe" @taskFlags '-cp' ((@($taskRoot,$taskJar)+$taskLibs)-join ';') BenchmarkExpansion111Diagnostic $taskFile "$taskRoot/isolated-$taskIndex" focused > "$taskRoot/$taskIndex-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskIndex-output.txt" -Tail 10;throw 'Diagnostic failed'}
 $taskRuns+=@{version=$taskVersion;escape_analysis=$taskItem.escape_analysis;jar_sha256=(Get-FileHash $taskJar).Hash;results=Get-Content $taskFile -Raw|ConvertFrom-Json};Write-Output "PASS DIAGNOSTIC111 $taskIndex $taskVersion EA=$($taskItem.escape_analysis)";$taskIndex++
}
if((Get-FileHash $taskAfter).Hash -ne $taskHash){throw 'Candidate changed'}
[IO.File]::WriteAllText("$taskRoot/RAW.json",(@{scope='Post-hoc diagnostic after short boss outlier and24-byte allocation difference;four normal JVMs ABBA plus two without escape analysis;four modes with1M warmup/four1M-call rounds;not game FPS';records=$taskRuns;runtime_jar_sha256=$taskHash;replaces_previous_results=$false}|ConvertTo-Json -Depth 18),[Text.UTF8Encoding]::new($false))
Write-Output "EXPANSION111 DIAGNOSTIC ROOT:$taskRoot"
