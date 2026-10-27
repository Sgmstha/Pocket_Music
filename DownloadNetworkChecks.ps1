$ErrorActionPreference='Stop'
$NoAccountSession=$true
. "$PSScriptRoot\Core.ps1"
$checks=0
function Check($Value,[string]$Message) { if (-not $Value) { throw $Message }; $script:checks++ }

# Regression: an HTTP 429 warning followed by a sign-in error was called a private video.
$reported=@'
WARNING: [youtube] _70Q-Xj3rEo: Unable to download webpage: HTTP Error 429: Too Many Requests
WARNING: [youtube] Unable to fetch GVS PO Token for web client: Missing required Visitor Data.
ERROR: [youtube] _70Q-Xj3rEo: Sign in to confirm you're not a bot.
'@
$friendly=Get-FriendlyError $reported
Check ($friendly -match 'HTTP 429' -and $friendly -match 'rate-limited') 'The reported failure was not classified as a rate limit'
Check ($friendly -notmatch 'only downloads publicly') 'Rate limiting still claims the video is private'
Check ((Get-FriendlyError "Sign in to confirm you're not a bot") -match 'verification') 'Bot verification is not actionable'
Check ((Get-FriendlyError 'This is a private video. Please sign in.') -match 'restricts access') 'Private-video handling regressed'
Check ((Get-FriendlyError 'HTTP Error 403: Forbidden') -match '403') 'Forbidden media response lost its status'
Check ((Get-FriendlyError 'Connection timed out') -match 'connection failed') 'Network timeout mapping regressed'

$now=[DateTime]::UtcNow
Check ((Get-YouTubeRetryDelaySeconds ($now.AddSeconds(60)) $now) -eq 60) 'Rate-limit cooldown does not block an immediate retry'
Check ((Get-YouTubeRetryDelaySeconds ($now.AddMilliseconds(100)) $now) -eq 1) 'Cooldown must round up partial seconds'
Check ((Get-YouTubeRetryDelaySeconds ($now.AddSeconds(-1)) $now) -eq 0) 'Cooldown does not expire'
Check ((Get-YouTubeRetryDelaySeconds ([DateTime]::MinValue) $now) -eq 0) 'Default retry state blocks downloads'

$folder=Join-Path ([IO.Path]::GetTempPath()) ('PocketMusic-networkcheck-'+[guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($folder) | Out-Null
try {
    $arguments=@(Get-DownloadArguments 'https://youtu.be/_70Q-Xj3rEo' 'mp3' $folder)
    Check ($arguments[$arguments.IndexOf('--retries')+1] -eq '1') 'Download retry count is too aggressive'
    Check ($arguments[$arguments.IndexOf('--extractor-retries')+1] -eq '0') 'Extractor retries should stop on verification failure'
    Check ($arguments[$arguments.IndexOf('--sleep-requests')+1] -eq '1') 'Request pacing is missing'
    Check ($arguments[$arguments.IndexOf('--encoding')+1] -eq 'utf-8') 'Downloader output encoding is not explicit'
    Check ($arguments[-1] -eq 'https://www.youtube.com/watch?v=_70Q-Xj3rEo') 'The reported video URL was changed'
} finally { [IO.Directory]::Delete($folder) }
Write-Host "PASS: $checks download network regression checks. No cookies or network accessed."
