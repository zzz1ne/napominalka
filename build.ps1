param(
    [switch]$AssembleOnly,
    [switch]$DeviceTests
)

$ErrorActionPreference = 'Stop'
$ritmProject = $PSScriptRoot
$ritmDrive = $null
$ritmExit = 1

try {
    # Gradle's test worker on Windows can lose Cyrillic characters in its classpath.
    # A temporary drive aliases the same files; no sources are copied or moved.
    if ($ritmProject -match '[^\x00-\x7F]') {
        foreach ($ritmCandidate in [char[]]'RSTUVWXYZ') {
            if (-not (Test-Path ($ritmCandidate + ':\'))) {
                $ritmDrive = $ritmCandidate + ':'
                break
            }
        }
        if (-not $ritmDrive) { throw 'Нет свободной буквы диска для временного пути сборки.' }
        & subst.exe $ritmDrive $ritmProject
        if ($LASTEXITCODE -ne 0) { throw 'Не удалось создать временный путь сборки.' }
        $ritmProject = $ritmDrive + '\'
    }
    $ritmTasks = @(':app:assembleDebug')
    if (-not $AssembleOnly) { $ritmTasks += ':app:testDebugUnitTest', ':app:lintDebug' }
    if ($DeviceTests) { $ritmTasks += ':app:connectedDebugAndroidTest' }
    Push-Location -LiteralPath $ritmProject
    try {
        & (Join-Path $ritmProject 'gradlew.bat') --no-daemon @ritmTasks
        $ritmExit = $LASTEXITCODE
    } finally { Pop-Location }
} finally {
    if ($ritmDrive) { & subst.exe $ritmDrive /D }
}

if ($ritmExit -ne 0) { throw "Сборка завершилась с ошибкой ($ritmExit)." }
Write-Output (Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk')
