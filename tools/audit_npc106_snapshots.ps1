$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/npc106-snapshots-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/npc105-checks-0a8d81b407df4fc4b4ccaa0b00ac2052/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskCandidate="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.106-dev.jar"
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp ((@($taskCandidate)+$taskLibs)-join ';') -d $taskRoot "$taskProject/tests/AuditNpc106.java"
if($LASTEXITCODE -ne 0){throw 'Snapshot harness compilation failed'}
foreach($taskVersion in @('0.15.105-dev','0.15.106-dev')){
 $taskRuntime="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar"
 & "$taskJava/java.exe" '-Xmx768m' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -cp ((@($taskRoot,$taskRuntime)+$taskLibs)-join ';') AuditNpc106 "$taskRoot/$taskVersion" "$taskProject/translation-source" > "$taskRoot/$taskVersion-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskVersion-output.txt" -Tail 10;throw 'Snapshot failed'}
 Get-Content "$taskRoot/$taskVersion-output.txt" -Tail 1
}
Write-Output "NPC106 SNAPSHOT ROOT:$taskRoot"
