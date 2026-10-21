param([Parameter(Mandatory=$true)][string]$Checks)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskChecks=(Resolve-Path $Checks).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskOldArgs=Get-Content "$taskChecks/compile-arguments.txt"
$taskLibs=@($taskOldArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'})
$taskRoot=Join-Path $taskProject ('build/npc103-names-compare-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskRecords=@()
foreach($taskVersion in @('0.15.102-dev','0.15.103-dev')){
 $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar"
 New-Item -ItemType Directory -Path "$taskRoot/$taskVersion"|Out-Null
 & "$taskJava/java.exe" '-Xmx768m' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-cp' ((@($taskChecks,$taskJar)+$taskLibs)-join ';') TestNpcNames103 "$taskRoot/$taskVersion/config" "$taskProject/translation-source" baseline > "$taskRoot/$taskVersion/output.txt" 2>&1
 if($LASTEXITCODE -ne 0){Get-Content "$taskRoot/$taskVersion/output.txt" -Tail 10;throw 'Names snapshot failed'}
 $taskRecords+=@{version=$taskVersion;runtime_jar_sha256=(Get-FileHash $taskJar).Hash;results_file="$taskVersion/baseline-results.json";results_sha256=(Get-FileHash "$taskRoot/$taskVersion/baseline-results.json").Hash}
}
[IO.File]::WriteAllText("$taskRoot/SCOPE.json",(@{scope='Same135 real-name inputs/136 publication layouts, final fresh fixture;not live capture';records=$taskRecords;fixture_sha256=(Get-FileHash "$taskProject/tests/TestNpcNames103.java").Hash;reviewed_tsv_sha256=(Get-FileHash "$taskProject/translation-source/NPC_NAMES_0.15.103_REVIEWED.tsv").Hash}|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Write-Output "NPC103 NAMES COMPARE ROOT:$taskRoot"
