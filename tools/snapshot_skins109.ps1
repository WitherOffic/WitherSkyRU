param([string[]]$Versions=@('0.15.108-dev'))
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/skins109-snapshots-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/npc106-checks-18089a61d6f44d749d0337c2b08ef206/compile-arguments.txt"
$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskCompileJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$($Versions[0]).jar"
& "$taskJava/javac.exe" --release 25 -encoding UTF-8 -proc:none -cp ((@($taskCompileJar)+$taskLibs)-join ';') -d $taskRoot "$taskProject/tests/AuditSkins109.java"
if($LASTEXITCODE -ne 0){throw 'Snapshot compilation failed'}
foreach($taskVersion in $Versions){
 $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar"
 & "$taskJava/java.exe" '-Xmx768m' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -cp ((@($taskRoot,$taskJar)+$taskLibs)-join ';') AuditSkins109 "$taskRoot/$taskVersion" "$taskProject/translation-source" > "$taskRoot/$taskVersion-output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskVersion-output.txt" -Tail 15;throw 'Snapshot failed'}
 Get-Content "$taskRoot/$taskVersion-output.txt" -Tail 1
}
Write-Output "SKINS109 SNAPSHOTS:$taskRoot"
