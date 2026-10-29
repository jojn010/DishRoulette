# Owner-only auto-connected APK. Its bundled key is extractable: never publish this APK.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('tabkha-private-' + [Guid]::NewGuid())
$apk = Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk'
$privateOutput = Join-Path $root 'work/private/Tabkha-personal.apk'
try {
    $config = @{}
    if (Test-Path backend/local.properties) {
        Get-Content backend/local.properties | ForEach-Object {
            if ($_ -match '^([^#=]+)=(.*)$') { $config[$matches[1].Trim()] = $matches[2].Trim() }
        }
    }
    $key = if ($env:GEMINI_API_KEY) { $env:GEMINI_API_KEY } else { $config['GEMINI_API_KEY'] }
    $model = if ($env:GEMINI_MODEL) { $env:GEMINI_MODEL } elseif ($config['GEMINI_MODEL']) { $config['GEMINI_MODEL'] } else { 'gemini-3.5-flash-lite' }
    if (-not $key -or $key -notmatch '^[A-Za-z0-9_.-]{20,256}$') { throw 'No valid local Gemini key configured' }
    if ($model -notmatch '^gemini-[A-Za-z0-9.-]{1,70}$') { throw 'Invalid local Gemini model' }
    New-Item -ItemType Directory -Path $temporary | Out-Null
    $setup = @{ key = $key; model = $model } | ConvertTo-Json -Compress
    [IO.File]::WriteAllText((Join-Path $temporary 'private-gemini-setup.json'), $setup)
    try {
        & .\gradlew.bat :app:assembleDebug --no-configuration-cache "-PprivateSetupAssetsDir=$temporary"
        if ($LASTEXITCODE -ne 0) { throw 'Private build failed' }
        New-Item -ItemType Directory -Path (Split-Path $privateOutput) -Force | Out-Null
        Copy-Item -LiteralPath $apk -Destination $privateOutput
    } finally {
        # Verify the absolute deletion target is the task-owned temporary directory.
        $resolved = [IO.Path]::GetFullPath($temporary)
        if ((Split-Path $resolved -Parent) -ne [IO.Path]::GetTempPath().TrimEnd('\') -or (Split-Path $resolved -Leaf) -notlike 'tabkha-private-*') { throw 'Unexpected cleanup target' }
        Remove-Item -LiteralPath $resolved -Recurse -Force
        if (Test-Path $apk) { Remove-Item -LiteralPath $apk }
        & .\gradlew.bat :app:clean :app:assembleDebug --no-configuration-cache
        if ($LASTEXITCODE -ne 0) { throw 'Public rebuild failed; do not publish build output' }
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($apk)
    try { if ($archive.GetEntry('assets/private-gemini-setup.json')) { throw 'Private setup leaked into public build output' } }
    finally { $archive.Dispose() }
    Write-Output 'Auto-connected owner APK: work/private/Tabkha-personal.apk. Keep private.'
} finally { $config = $key = $setup = $null; Pop-Location }
