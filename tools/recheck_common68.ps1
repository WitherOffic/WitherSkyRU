$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskArgs=Get-Content "$taskProject/build/dungeons68-b8aaa2d771c84238a2e681b6ffe4625c/compile-arguments.txt"
$taskLibraries=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskClasses="$taskProject/build/render-hotpaths-5456a2cb7bca4b198a8a7219cc0ced7b"
$taskRoot=Join-Path $taskProject ('build/common-recheck68-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskModes=@('tooltip_components','sack55_components','experiment57_notices','enchant59_components','enchant59_sequences','numeric59_negative','npc60_new','numeric62_dictionary','context63_stable','context63_oversized')
foreach($taskMode in $taskModes){$taskIndex=0;foreach($taskVersion in @('0.15.67-dev','0.15.68-dev','0.15.68-dev','0.15.67-dev')){$taskLabel="$taskMode-$taskIndex-$taskVersion";$taskIndex++;$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskCp=(@($taskClasses,$taskJar)+$taskLibraries)-join ';'
    & "$taskJava/java.exe" '-Xms256m' '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dwitherskyru.benchmark.warmup=100000' '-Dwitherskyru.benchmark.measure=20000' "-Dwitherskyru.benchmark.scenario=$taskMode" '-cp' $taskCp BenchmarkRenderHotPaths "$taskRoot/$taskLabel" $taskVersion "$taskRoot/$taskLabel.json" | Tee-Object -FilePath "$taskRoot/$taskLabel-output.txt"
    if($LASTEXITCODE -ne 0){throw "Failed $taskLabel"}
}}
[IO.File]::WriteAllText("$taskRoot/SCOPE.json",(@{date='2026-10-15';order='67,68,68,67 per isolated scenario';modes=$taskModes;warmup=100000;measured=20000;all_rounds_retained=$true;scope='Ten previously flagged scenarios repeated separately; not FPS or proof of no regression';hash67=(Get-FileHash "$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.67-dev.jar").Hash;hash68=(Get-FileHash "$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.68-dev.jar").Hash}|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
Write-Output "ISOLATED68 ROOT: $taskRoot"
