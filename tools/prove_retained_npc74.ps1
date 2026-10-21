$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path;$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskRoot=Join-Path $taskProject ('build/retained74-proof-'+[Guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $taskRoot|Out-Null
$taskArgs=Get-Content "$taskProject/build/names73-checks-1cc05db78e3b453da3efb2d5c9b40038/compile-arguments.txt";$taskLibs=@($taskArgs[6].Trim('"').Split(';')|Where-Object {$_ -notmatch '/build/libs/WitherSkyRU'});$taskBefore="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.73-dev.jar"
if((Get-FileHash $taskBefore).Hash -ne '5805CB5C5973434E666ACF6129F12DB9B005F20EF7BEAB852541AAA6995E8FDF'){throw 'Wrong73 oracle'}
& "$taskJava/javac.exe" '--release' '25' '-encoding' 'UTF-8' '-proc:none' '-cp' ((@($taskBefore)+$taskLibs)-join ';') '-d' $taskRoot "$taskProject/tests/VerifyRetainedNpc74.java";if($LASTEXITCODE -ne 0){throw 'Proof compile failed'}
$taskRuns=@();foreach($taskVersion in @('0.15.73-dev','0.15.74-dev')){
 $taskJar="$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-$taskVersion.jar";$taskOutput=& "$taskJava/java.exe" '-Xmx256m' '-Djava.awt.headless=true' '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' ((@($taskRoot,$taskJar)+$taskLibs)-join ';') VerifyRetainedNpc74 "$taskRoot/isolated-$taskVersion" "$taskProject/translation-source"
 if($LASTEXITCODE -ne 0){throw 'Retained ownership failed'};$taskRuns+=@{version=$taskVersion;jar_sha256=(Get-FileHash $taskJar).Hash;output=$taskOutput};Write-Output $taskOutput
}
[IO.File]::WriteAllText("$taskProject/translation-source/NPC74_RETAINED_RUNTIME_PROOF.json",(@{scope='Frozen73 and final74 numeric handler both own the Lucius source;excluded from new239';runs=$taskRuns}|ConvertTo-Json -Depth 7),[Text.UTF8Encoding]::new($false))
