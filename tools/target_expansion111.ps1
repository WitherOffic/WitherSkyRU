$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/expansion111-targeted-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/npc106-checks-18089a61d6f44d749d0337c2b08ef206/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.111-dev.jar";$taskHash=(Get-FileHash -LiteralPath $taskJar).Hash
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp ((@($taskJar)+$taskLibs)-join ';') -d $taskRoot "$taskProject/tests/TestExpansion111.java"
if($LASTEXITCODE -ne 0){throw 'Targeted harness compilation failed'}
& "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp ((@($taskRoot,$taskJar)+$taskLibs)-join ';') TestExpansion111 "$taskRoot/isolated" "$taskProject/translation-source" > "$taskRoot/TestExpansion111-output.txt" 2>&1
if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/TestExpansion111-output.txt" -Tail 12;throw "Targeted fixture failed;$taskRoot"}
if((Get-FileHash -LiteralPath $taskJar).Hash -ne $taskHash){throw 'Executable changed'}
Get-Content "$taskRoot/TestExpansion111-output.txt" -Tail 1
Write-Output "EXPANSION111 TARGETED ONLY:$taskRoot"
