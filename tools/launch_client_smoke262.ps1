param([Parameter(Mandatory=$true)][string]$PrismRoot,[switch]$FullPack)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskRoot=Join-Path $taskProject ('build/client262-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path "$taskRoot/mods","$taskRoot/config/witherskyru" | Out-Null
$taskJar="$taskProject/build/libs/WitherSkyRU-26.2-fabric-0.16.0-dev.jar"
Copy-Item -LiteralPath $taskJar,"$taskProject/build/test-mods/client-smoke262-test-only.jar" -Destination "$taskRoot/mods"
$taskCp=(Get-Content "$taskProject/build/verification-classpath.txt" -Raw).Trim().Split(';')
$taskClasspath=@($taskCp | Where-Object {$_ -notmatch '[\\/]build[\\/](classes|resources)[\\/]'} | ForEach-Object {
 if($_ -match 'minecraft-merged-deobf'){Join-Path $PrismRoot 'libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar'}else{$_}
})
if($FullPack){
 # Copy only public mod binaries. No accounts, user settings, logs, worlds or keys.
 Copy-Item -Path "$PrismRoot/instances/26.2/minecraft/mods/*.jar" -Destination "$taskRoot/mods"
}else{
 foreach($taskDependency in $taskClasspath | Where-Object {$_ -match 'fabric-(resource-loader-v1|api-base)-[^/\\]+\.jar$'}){
  Copy-Item -LiteralPath $taskDependency -Destination "$taskRoot/mods"
 }
}
[IO.File]::WriteAllText("$taskRoot/config/witherskyru/settings.properties","enabled=true`nonlyHypixel=false`nrecordUnknown=false`n",[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText("$taskRoot/options.txt","enableVsync:false`nmaxFps:60`nrenderDistance:2`nsimulationDistance:5`nfullscreen:false`npauseOnLostFocus:false`nonboardAccessibility:false`n",[Text.UTF8Encoding]::new($false))
# Count diagnostics are useful for our isolated exact hooks, but force optional
# `expect` assertions in unrelated mods. Full-pack launch uses production rules;
# our own `require=1` checks remain mandatory in both modes.
$taskDiagnostics=if($FullPack){@()}else{@('-Dmixin.debug.countInjections=true')}
$taskHeap=if($FullPack){'-Xmx4g'}else{'-Xmx2g'}
$taskArgs=@('-Xms256m',$taskHeap,'--enable-native-access=ALL-UNNAMED','-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8')+$taskDiagnostics+@('-cp',($taskClasspath -join ';'),'net.fabricmc.loader.impl.launch.knot.KnotClient','--gameDir',$taskRoot,'--assetsDir',"$PrismRoot/assets",'--assetIndex','32','--version','26.2','--username','WitherSkyRU_Test','--uuid','00000000000000000000000000000000','--accessToken','0','--width','960','--height','540')
$taskArgumentFile="$taskRoot/client-arguments.txt"
[IO.File]::WriteAllLines($taskArgumentFile,@($taskArgs | ForEach-Object {'"'+$_.Replace('\','/')+'"'}),[Text.UTF8Encoding]::new($false))
$taskProcess=Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin/java.exe') -ArgumentList ('"@'+$taskArgumentFile+'"') -WorkingDirectory $taskRoot -WindowStyle Hidden -RedirectStandardOutput "$taskRoot/client.out.txt" -RedirectStandardError "$taskRoot/client.err.txt" -PassThru
@{pid=$taskProcess.Id;root=$taskRoot;jar_sha256=(Get-FileHash $taskJar).Hash;mode=$(if($FullPack){'copied 26.2 mod binaries, fresh settings, offline test account'}else{'minimal Fabric modules, offline test account'});not_multiplayer=$true;assets_read_from="$PrismRoot/assets";installed_pack_modified=$false} | ConvertTo-Json | Set-Content "$taskRoot/LAUNCH.json" -Encoding utf8
Write-Output "CLIENT262_PID=$($taskProcess.Id)"
Write-Output "CLIENT262_ROOT=$taskRoot"
