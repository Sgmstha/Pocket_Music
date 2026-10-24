$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
try {
    $tools = Join-Path $PSScriptRoot 'tools'
    New-Item -ItemType Directory -Force $tools | Out-Null
    Write-Host 'Downloading the latest official yt-dlp release...'
    $temp = Join-Path $tools 'yt-dlp.new.exe'
    Invoke-WebRequest -UseBasicParsing 'https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe' -OutFile $temp
    & $temp --version
    if ($LASTEXITCODE -ne 0) { throw 'The downloaded tool did not start.' }
    Move-Item -LiteralPath $temp -Destination (Join-Path $tools 'yt-dlp.exe') -Force
    foreach ($dependency in @(@('ffmpeg','Gyan.FFmpeg'), @('node','OpenJS.NodeJS.LTS'))) {
        if (-not (Get-Command "$($dependency[0]).exe" -ErrorAction SilentlyContinue)) {
            Write-Host "Installing $($dependency[0]) using Windows Package Manager..."
            & winget install --id $dependency[1] --exact --accept-package-agreements --accept-source-agreements
            if ($LASTEXITCODE -ne 0) { throw "Installation failed for $($dependency[0]). See README.md for manual setup." }
        }
    }
    $runtimeRoot = Join-Path ${env:ProgramFiles(x86)} 'Microsoft\EdgeWebView\Application'
    $userRuntimeRoot = Join-Path $env:LOCALAPPDATA 'Microsoft\EdgeWebView\Application'
    if (-not (Test-Path -LiteralPath $runtimeRoot) -and -not (Test-Path -LiteralPath $userRuntimeRoot)) {
        Write-Host 'Installing Microsoft Edge WebView2 Runtime for YouTube previews...'
        & winget install --id Microsoft.EdgeWebView2Runtime --exact --accept-package-agreements --accept-source-agreements
        if ($LASTEXITCODE -ne 0) { throw 'WebView2 installation failed. Install Microsoft Edge WebView2 Evergreen Runtime, then reopen Pocket Music.' }
    }
    Write-Host 'Setup complete. Close and reopen the app with Launch.cmd.' -ForegroundColor Green
} catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }
