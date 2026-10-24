$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\Core.ps1"
$checks = 0
function Assert($Condition, $Message) { if (-not $Condition) { throw $Message }; $script:checks++ }
foreach ($url in @('https://youtu.be/abcdefghijk?t=3','https://www.youtube.com/watch?v=abcdefghijk&list=ignored','https://music.youtube.com/watch?v=abcdefghijk','https://youtube.com/shorts/abcdefghijk')) {
    Assert ((ConvertTo-VideoUrl $url) -eq 'https://www.youtube.com/watch?v=abcdefghijk') 'URL normalization failed'
}
foreach ($url in @('http://youtube.com/watch?v=abcdefghijk','https://youtube.com.evil.test/watch?v=abcdefghijk','https://youtube.com/playlist?list=123','https://youtube.com@evil.test/watch?v=abcdefghijk','--exec anything','https://youtu.be/short')) {
    $rejected = $false
    try { ConvertTo-VideoUrl $url | Out-Null } catch { $rejected = $true }
    Assert $rejected "Invalid URL accepted: $url"
}
Assert ((Get-FriendlyError 'ERROR: Sign in to confirm') -match 'requires sign-in') 'Restricted-content error failed'
Assert ((Get-FriendlyError 'HTTP Error 429') -match 'rate-limited') 'Rate-limit error failed'
$testDir = Join-Path ([IO.Path]::GetTempPath()) ('PocketMusic-test-' + [guid]::NewGuid())
New-Item -ItemType Directory $testDir | Out-Null
$ffmpeg = Find-Tool 'ffmpeg'
$ytdlp = Find-Tool 'yt-dlp'
try {
    & $ffmpeg -hide_banner -loglevel error -f lavfi -i 'sine=frequency=440:duration=1' (Join-Path $testDir 'tone.wav')
    Assert ($LASTEXITCODE -eq 0) 'Test-tone generation failed'
    foreach ($format in @('mp3','m4a','opus','flac','wav')) {
        $arguments = @(Get-DownloadArguments 'https://youtu.be/abcdefghijk' $format $testDir)
        Assert ($arguments -contains '--ignore-config') 'User config isolation missing'
        # Only this test permits file URLs. The actual app accepts YouTube links exclusively.
        $arguments[-1] = ([uri](Join-Path $testDir 'tone.wav')).AbsoluteUri
        $arguments = @('--enable-file-urls') + $arguments
        $ErrorActionPreference = 'Continue'
        & $ytdlp @arguments 2>&1 | Out-String | Write-Host
        $ErrorActionPreference = 'Stop'
        Assert ($LASTEXITCODE -eq 0) "$format extraction failed"
        # Literal brackets are wildcard syntax; inspect extensions and exclude the input instead.
        $files = @(Get-ChildItem -LiteralPath $testDir | Where-Object { $_.Extension -eq ".$format" -and $_.Name -ne 'tone.wav' })
        Assert ($files.Count -eq 1 -and $files[0].Length -gt 100) "$format output missing"
        $codec = & (Find-Tool 'ffprobe') -v error -select_streams a:0 -show_entries stream=codec_name -of default=noprint_wrappers=1 $files[0].FullName
        Assert ($LASTEXITCODE -eq 0 -and $codec -match 'codec_name=') "$format audio is unreadable"
    }
    & $ffmpeg -hide_banner -loglevel error -f lavfi -i 'color=c=blue:s=320x240:d=1' -f lavfi -i 'sine=frequency=440:duration=1' -c:v libx264 -c:a aac -shortest (Join-Path $testDir 'video.mp4')
    Assert ($LASTEXITCODE -eq 0) 'Video fixture generation failed'
    $arguments = @(Get-DownloadArguments 'https://youtu.be/abcdefghijk' 'mp4' $testDir)
    Assert ($arguments -notcontains '-x' -and $arguments -contains '--merge-output-format') 'MP4 incorrectly uses audio extraction'
    $arguments[-1] = ([uri](Join-Path $testDir 'video.mp4')).AbsoluteUri
    $arguments = @('--enable-file-urls') + $arguments
    $ErrorActionPreference = 'Continue'
    & $ytdlp @arguments 2>&1 | Out-String | Write-Host
    $ErrorActionPreference = 'Stop'
    Assert ($LASTEXITCODE -eq 0) 'MP4 download failed'
    $videoFile = Get-ChildItem -LiteralPath $testDir | Where-Object { $_.Extension -eq '.mp4' -and $_.Name -ne 'video.mp4' } | Select-Object -First 1
    Assert ($null -ne $videoFile) 'MP4 output missing'
    $streams = & (Find-Tool 'ffprobe') -v error -show_entries stream=codec_type -of csv=p=0 $videoFile.FullName
    Assert ($streams -contains 'video' -and $streams -contains 'audio') 'MP4 must contain both video and audio'
    & powershell.exe -NoProfile -STA -ExecutionPolicy Bypass -File "$PSScriptRoot\MusicDownloader.ps1" -SmokeTest
    Assert ($LASTEXITCODE -eq 0) 'GUI smoke test failed'
    Write-Host "PASS: $checks checks. No live YouTube download was attempted."
} finally {
    # Only remove the uniquely created test folder directly under the temp directory.
    $resolved = [IO.Path]::GetFullPath($testDir)
    if ($resolved.StartsWith([IO.Path]::GetFullPath([IO.Path]::GetTempPath())) -and (Split-Path $resolved -Leaf) -like 'PocketMusic-test-*') {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
