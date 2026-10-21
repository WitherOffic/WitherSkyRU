# Remove irrelevant environment/system-property events from this task's own
# generated offline recordings. Never reads event values or game recordings.
$ErrorActionPreference='Stop'
$taskProject=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskBuild=[IO.Path]::GetFullPath((Join-Path $taskProject 'build'))+[IO.Path]::DirectorySeparatorChar
$taskJava='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin'
$taskOwned=@('render-profile-e305aabce3c94827af835f0f23c4cc52','render-profile-058f7a5538f34f0cb0cbc8dabb5a6c60','render-profile-a64fe21ed10d4aafba16946d591f21ce','render-profile-bf5598874e9344d99ad3905ce777872c')
function Get-Count($taskSummary,$taskType){$taskMatch=[regex]::Match(($taskSummary -join "`n"),'(?m)^\s+'+[regex]::Escape($taskType)+'\s+(\d+)\s+');if(!$taskMatch.Success){return 0};return [long]$taskMatch.Groups[1].Value}
foreach($taskName in $taskOwned){
    $taskInput=(Resolve-Path -LiteralPath (Join-Path $taskBuild "$taskName/offline.jfr")).Path
    if(!$taskInput.StartsWith($taskBuild,[StringComparison]::OrdinalIgnoreCase) -or [IO.Path]::GetFileName($taskInput) -ne 'offline.jfr'){throw 'Unexpected recording target'}
    $taskSafe=Join-Path ([IO.Path]::GetDirectoryName($taskInput)) 'offline-safe.jfr'
    if(Test-Path -LiteralPath $taskSafe){throw 'Sanitized recording already exists; preserve and inspect'}
    $taskBefore=@(& "$taskJava/jfr.exe" summary $taskInput);if($LASTEXITCODE -ne 0){throw 'Cannot inspect source recording'}
    & "$taskJava/jfr.exe" scrub --exclude-events 'jdk.InitialEnvironmentVariable,jdk.InitialSystemProperty' $taskInput $taskSafe | Out-Null
    if($LASTEXITCODE -ne 0){throw 'Recording scrub failed'}
    $taskAfter=@(& "$taskJava/jfr.exe" summary $taskSafe);if($LASTEXITCODE -ne 0){throw 'Cannot inspect sanitized recording'}
    foreach($taskType in @('jdk.InitialEnvironmentVariable','jdk.InitialSystemProperty')){if((Get-Count $taskAfter $taskType) -ne 0){throw 'Private metadata remains'}}
    foreach($taskType in @('jdk.ExecutionSample','jdk.ObjectAllocationSample','jdk.GarbageCollection','jdk.GCPhasePause')){if((Get-Count $taskBefore $taskType) -ne (Get-Count $taskAfter $taskType)){throw "Relevant profile events changed: $taskType"}}
    # Exact validated own-generated file only; no recursive deletion or broad path.
    Remove-Item -LiteralPath $taskInput
    Move-Item -LiteralPath $taskSafe -Destination $taskInput
    Write-Output "Sanitized $taskName; CPU/allocation/GC event counts preserved; environment/property values not read or printed."
}
