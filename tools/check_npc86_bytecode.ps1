param([Parameter(Mandatory=$true)][string]$OutputDirectory)
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskOutput=Join-Path $taskProject $OutputDirectory
New-Item -ItemType Directory -Path $taskOutput | Out-Null
$taskRecords=@()
foreach($taskClass in @('ru.wither.skyru.ChatNotificationTranslator$Edit','ru.wither.skyru.ChatNotificationTranslator$Part','ru.wither.skyru.ChatNotificationTranslator$QuizLine66')){
 $taskOld=(& "$taskJava/javap.exe" '-c' '-p' '-classpath' "$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.85-dev.jar" $taskClass)-join "`n"
 if($LASTEXITCODE -ne 0){throw 'Old record disassembly failed'}
 $taskNew=(& "$taskJava/javap.exe" '-c' '-p' '-classpath' "$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.86-dev.jar" $taskClass)-join "`n"
 if($LASTEXITCODE -ne 0){throw 'New record disassembly failed'}
 if($taskOld -cne $taskNew){throw "Record executable bytecode changed:$taskClass"}
 $taskRecords+=@{class=$taskClass;executable_disassembly_identical=$true}
}
[IO.File]::WriteAllText("$taskOutput/RESULT.json",(@{scope='Three unchanged executable record classes;JAR differences are line/debug metadata after added table entries';records=$taskRecords;runtime_jar_sha256=(Get-FileHash "$taskProject/build/libs/WitherSkyRU-26.1.2-fabric-0.15.86-dev.jar").Hash}|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Write-Output "PASS:Three record executable bytecodes identical;$taskOutput"
