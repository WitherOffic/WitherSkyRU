$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/npc81-dispatch-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/npc80-checks-7e205c83ab2e4e4199efbd67bd732426/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskCandidate="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.81-dev.jar"
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskCandidate)+$taskLibs)-join ';') '-d' $taskRoot "$taskProject/tests/AuditNpcCorpus77.java"
if($LASTEXITCODE -ne 0){throw 'Compile audit failed'}
foreach($taskVersion in @('0.15.80-dev','0.15.81-dev')){
 $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar"
 & "$taskJava/java.exe" '-Xmx768m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' ((@($taskRoot,$taskJar)+$taskLibs)-join ';') AuditNpcCorpus77 "$taskRoot/$taskVersion" "$taskProject/build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1" > "$taskRoot/$taskVersion-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskVersion-output.txt" -Tail 15;throw 'Corpus dispatch failed'}
 Get-Content "$taskRoot/$taskVersion-output.txt" -Tail 1
}
Write-Output "NPC81 DISPATCH ROOT:$taskRoot"
