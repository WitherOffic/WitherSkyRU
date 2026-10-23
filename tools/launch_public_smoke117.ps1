param([Parameter(Mandatory=$true)][string]$PrismRoot,
      [Parameter(Mandatory=$true)][ValidateSet('translator','collector','both')][string]$Mode,
      [string]$Jar,[string]$StandaloneCollector)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskRoot=Join-Path $taskProject ('build/public-client117-'+$Mode+'-'+[Guid]::NewGuid().ToString('N'))
$taskPrimary=$Mode -ne 'collector'
$taskIndependent=$Mode -ne 'translator'
$taskCollectorId=''
if($taskPrimary -and -not $Jar){throw 'Public translator JAR required'}
if($taskIndependent){
 if(-not $StandaloneCollector){throw 'Actual independent Collector required; never use a stub'}
 $taskCollectorPath=(Resolve-Path -LiteralPath $StandaloneCollector).Path
 $taskZip=[IO.Compression.ZipFile]::OpenRead($taskCollectorPath)
 try {
  $taskReader=[IO.StreamReader]::new($taskZip.GetEntry('fabric.mod.json').Open())
  try {$taskMetadata=$taskReader.ReadToEnd() | ConvertFrom-Json}finally{$taskReader.Dispose()}
 } finally {$taskZip.Dispose()}
 $taskCollectorId=$taskMetadata.id
 if($taskCollectorId -ne 'skyblockrpgru_collector'){throw 'Expected the current independent skyblockrpgru_collector, not a legacy artifact'}
 if($Mode -eq 'collector' -and $taskMetadata.depends.witherskyru){throw 'Provided Collector still requires the translator'}
}
New-Item -ItemType Directory -Path "$taskRoot/mods","$taskRoot/config/witherskyru" | Out-Null
Copy-Item -LiteralPath "$taskProject/build/test-mods/public-client117-test-only.jar" -Destination "$taskRoot/mods"
if($taskPrimary){Copy-Item -LiteralPath (Resolve-Path -LiteralPath $Jar).Path -Destination "$taskRoot/mods"}
if($taskIndependent){Copy-Item -LiteralPath $taskCollectorPath -Destination "$taskRoot/mods"}
$taskCp=(Get-Content "$taskProject/build/verification-classpath.txt" -Raw).Trim().Split(';')
$taskClasspath=@($taskCp | Where-Object {$_ -notmatch '[\\/]build[\\/](classes|resources)[\\/]'} | ForEach-Object {
 if($_ -match 'minecraft-merged-deobf'){Join-Path $PrismRoot 'libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar'}else{$_}
})
foreach($taskDependency in $taskClasspath | Where-Object {$_ -match 'fabric-(resource-loader-v1|api-base)-[^/\\]+\.jar$'}){Copy-Item -LiteralPath $taskDependency -Destination "$taskRoot/mods"}
[IO.File]::WriteAllText("$taskRoot/config/witherskyru/settings.properties","enabled=true`nonlyHypixel=false`nrecordUnknown=false`n",[Text.UTF8Encoding]::new($false))
# Legacy integrated permissions deliberately present. Public translator must ignore them.
[IO.File]::WriteAllText("$taskRoot/config/witherskyru/collector-consent.properties","version=1`ndecision=GRANTED`nmods=true`nperformance=true`ndiagnostics=true`n",[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText("$taskRoot/options.txt","enableVsync:false`nmaxFps:60`nrenderDistance:2`nsimulationDistance:5`nfullscreen:false`npauseOnLostFocus:false`nonboardAccessibility:false`n",[Text.UTF8Encoding]::new($false))
$taskPresence=if($taskPrimary){'true'}else{'false'}
$taskArgs=@('-Xms256m','-Xmx3g','--enable-native-access=ALL-UNNAMED','-Dfile.encoding=UTF-8','-Dstdout.encoding=UTF-8',
 '-Djava.net.useSystemProxies=false','-Dhttp.proxyHost=127.0.0.1','-Dhttp.proxyPort=1','-Dhttps.proxyHost=127.0.0.1','-Dhttps.proxyPort=1',
 "-Dwitherskyru.public.smoke.translator=$taskPresence","-Dwitherskyru.public.smoke.collector=$taskCollectorId",'-cp',($taskClasspath -join ';'),
 'net.fabricmc.loader.impl.launch.knot.KnotClient','--gameDir',$taskRoot,'--assetsDir',"$PrismRoot/assets",'--assetIndex','32','--version','26.2',
 '--username','SkyblockRPGRU_Test','--uuid','00000000000000000000000000000000','--accessToken','0','--width','960','--height','540')
$taskArgumentFile="$taskRoot/client-arguments.txt"
[IO.File]::WriteAllLines($taskArgumentFile,@($taskArgs | ForEach-Object {'"'+$_.Replace('\','/')+'"'}),[Text.UTF8Encoding]::new($false))
$taskProcess=Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin/java.exe') -ArgumentList ('"@'+$taskArgumentFile+'"') -WorkingDirectory $taskRoot -WindowStyle Hidden -RedirectStandardOutput "$taskRoot/client.out.txt" -RedirectStandardError "$taskRoot/client.err.txt" -PassThru
@{pid=$taskProcess.Id;root=$taskRoot;mode=$Mode;collector_id=$taskCollectorId;not_multiplayer=$true;installed_pack_modified=$false;http_proxy_loopback=$true} | ConvertTo-Json | Set-Content "$taskRoot/LAUNCH.json" -Encoding utf8
Write-Output "PUBLIC117_PID=$($taskProcess.Id)"
Write-Output "PUBLIC117_ROOT=$taskRoot"
