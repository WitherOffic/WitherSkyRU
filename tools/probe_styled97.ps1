$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/opt97-targeted-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.97-dev.jar";$taskHash=(Get-FileHash $taskJar).Hash
$taskArgs=Get-Content "$taskProject/build/opt96-checks-316b4448c15c41e687fe88161dcda883/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskCp=(@($taskJar)+$taskLibs)-join ';'
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp $taskCp -d $taskRoot "$taskProject/tests/TestOptimization97.java"
if($LASTEXITCODE -ne 0){throw 'Targeted compilation failed'}
& "$taskJava/java.exe" '-Xmx768m' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' ((@($taskRoot,$taskJar)+$taskLibs)-join ';') ru.wither.skyru.TestOptimization97 "$taskRoot/isolated" "$taskProject/translation-source" > "$taskRoot/TestOptimization97-output.txt" 2>&1
if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/TestOptimization97-output.txt" -Tail 15;throw 'Targeted probe failed'}
if((Get-FileHash $taskJar).Hash -ne $taskHash){throw 'Candidate changed'}
[IO.File]::WriteAllText("$taskRoot/SCOPE.json",(@{runtime_jar_sha256=$taskHash;targeted_only=$true;test='ru.wither.skyru.TestOptimization97';installed_game_modified=$false}|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
Get-Content "$taskRoot/TestOptimization97-output.txt" -Tail 1
Write-Output "OPT97 TARGETED ROOT:$taskRoot"
