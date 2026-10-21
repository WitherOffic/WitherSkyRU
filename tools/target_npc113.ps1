$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/npc113-targeted-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/opt96-checks-fc41dfc90eed4bf39a58382bb736fc55/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.113-dev.jar";$taskHash=(Get-FileHash $taskJar).Hash
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp ((@($taskJar)+$taskLibs)-join ';') -d $taskRoot "$taskProject/tests/TestNpcNames103.java" "$taskProject/tests/TestNpc113.java"
if($LASTEXITCODE -ne 0){throw 'Targeted compilation failed'}
& "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp ((@($taskRoot,$taskJar)+$taskLibs)-join ';') TestNpc113 "$taskRoot/isolated" "$taskProject/translation-source" > "$taskRoot/TestNpc113-output.txt" 2>&1
if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/TestNpc113-output.txt" -Tail 15;throw "Targeted NPC113 failed;$taskRoot"}
if((Get-FileHash $taskJar).Hash -ne $taskHash){throw 'Runtime changed'}
[IO.File]::WriteAllText("$taskRoot/SCOPE.json",(@{runtime_jar_sha256=$taskHash;targeted_only=$true;not_a_full_suite=$true;game_fps_measured=$false;installed_pack_modified=$false}|ConvertTo-Json -Depth 4),[Text.UTF8Encoding]::new($false))
Get-Content "$taskRoot/TestNpc113-output.txt" -Tail 1
Write-Output "NPC113 TARGETED ROOT:$taskRoot"
