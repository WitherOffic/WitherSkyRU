param([Parameter(Mandatory=$true)][string]$PrismRoot,[Parameter(Mandatory=$true)][string]$Jar,[string]$VendorMods)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskRoot=Join-Path $taskProject ('build/skycubed-client117-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path "$taskRoot/mods","$taskRoot/config/witherskyru" | Out-Null
$taskJar=(Resolve-Path -LiteralPath $Jar).Path
if($VendorMods){
 # Only public binaries, never private configurations, accounts, logs or worlds.
 Get-ChildItem -LiteralPath $VendorMods -Filter '*.jar' | Where-Object {$_.Name -notlike '*WitherSkyRU*' -and $_.Name -notlike '*SkyblockRPGRU*' -and $_.Name -notlike '*SkyblockRPG_RU*' -and $_.Name -notlike '*smoke*'} | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination "$taskRoot/mods"}
}
Copy-Item -LiteralPath $taskJar,"$taskProject/build/test-mods/skycubed-client117-test-only.jar" -Destination "$taskRoot/mods"
$taskCp=(Get-Content "$taskProject/build/verification-classpath.txt" -Raw).Trim().Split(';')
$taskClasspath=@($taskCp | Where-Object {$_ -notmatch '[\\/]build[\\/](classes|resources)[\\/]'} | ForEach-Object {
 if($_ -match 'minecraft-merged-deobf'){Join-Path $PrismRoot 'libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar'}else{$_}
})
if(-not $VendorMods){foreach($taskDependency in $taskClasspath | Where-Object {$_ -match 'fabric-(resource-loader-v1|api-base)-[^/\\]+\.jar$'}){Copy-Item -LiteralPath $taskDependency -Destination "$taskRoot/mods"}}
[IO.File]::WriteAllText("$taskRoot/config/witherskyru/settings.properties","enabled=true`nonlyHypixel=false`nrecordUnknown=false`n",[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText("$taskRoot/options.txt","enableVsync:false`nmaxFps:60`nrenderDistance:2`nsimulationDistance:5`nfullscreen:false`npauseOnLostFocus:false`nonboardAccessibility:false`n",[Text.UTF8Encoding]::new($false))
$taskPresent=if($VendorMods){'true'}else{'false'}
$taskArgs=@('-Xms256m','-Xmx4g','--enable-native-access=ALL-UNNAMED','-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8',"-Dwitherskyru.skycubed.smoke.present=$taskPresent",'-cp',($taskClasspath -join ';'),'net.fabricmc.loader.impl.launch.knot.KnotClient','--gameDir',$taskRoot,'--assetsDir',"$PrismRoot/assets",'--assetIndex','32','--version','26.2','--username','WitherSkyRU_Test','--uuid','00000000000000000000000000000000','--accessToken','0','--width','960','--height','540')
$taskArgumentFile="$taskRoot/client-arguments.txt"
[IO.File]::WriteAllLines($taskArgumentFile,@($taskArgs | ForEach-Object {'"'+$_.Replace('\','/')+'"'}),[Text.UTF8Encoding]::new($false))
$taskProcess=Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin/java.exe') -ArgumentList ('"@'+$taskArgumentFile+'"') -WorkingDirectory $taskRoot -WindowStyle Hidden -RedirectStandardOutput "$taskRoot/client.out.txt" -RedirectStandardError "$taskRoot/client.err.txt" -PassThru
@{pid=$taskProcess.Id;root=$taskRoot;jar_sha256=(Get-FileHash $taskJar).Hash;skycubed_expected=[bool]$VendorMods;not_multiplayer=$true;installed_pack_modified=$false} | ConvertTo-Json | Set-Content "$taskRoot/LAUNCH.json" -Encoding utf8
Write-Output "SKYCUBED117_PID=$($taskProcess.Id)"
Write-Output "SKYCUBED117_ROOT=$taskRoot"
