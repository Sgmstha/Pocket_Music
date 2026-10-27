$ErrorActionPreference='Stop'
$NoAccountSession=$true
. "$PSScriptRoot\Core.ps1"
. "$PSScriptRoot\Search.ps1"
$script:sessionCheckCount=0
function Check-Session($Value,[string]$Message) { if (-not $Value) { throw $Message }; $script:sessionCheckCount++ }
function Expect-SessionRejection([scriptblock]$Action,[string]$Message) {
    $rejected=$false
    try { & $Action | Out-Null } catch { $rejected=$true }
    Check-Session $rejected $Message
}
$tempBase=[IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$fixtureRoot=Join-Path $tempBase ('PocketMusic-sessioncheck-'+[guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($fixtureRoot) | Out-Null
$script:youtubeAuthRoot=Join-Path $fixtureRoot 'private'
$script:youtubeTestRun=$false # Only the new synthetic fixture directory is enabled.
$expires=[DateTimeOffset]::UtcNow.AddDays(1).ToUnixTimeSeconds()
$synthetic="# Netscape HTTP Cookie File`n.youtube.com`tTRUE`t/`tTRUE`t$expires`tSID`tsynthetic-session-for-test`n.google.com`tTRUE`t/`tTRUE`t$expires`tSID`tnot-youtube-test`n.youtube.com.evil.test`tTRUE`t/`tTRUE`t$expires`tSID`tevil-domain-test`n"
$fixture=Join-Path $fixtureRoot 'youtube.txt'
[IO.File]::WriteAllText($fixture,$synthetic)
try {
    Check-Session (-not (Test-YouTubeSession)) 'A missing session was treated as connected'
    Check-Session (@(Get-YouTubeAuthArguments 'download').Count -eq 0) 'Public mode added authentication'
    Expect-SessionRejection { Get-YouTubeRecommendationArguments } 'Account recommendations accepted a missing session'
    $clean=ConvertTo-YouTubeCookieFile $synthetic
    Check-Session ($clean -match 'synthetic-session-for-test') 'YouTube login cookie was lost'
    Check-Session ($clean -notmatch 'google.com|not-youtube-test|evil-domain-test') 'Other-domain cookies were retained'
    Check-Session ($clean -notmatch "`r") 'Cookie output has inconsistent newlines'
    $httpOnly=$synthetic.Replace('.youtube.com'+"`t",'#HttpOnly_.youtube.com'+"`t")
    Check-Session ((ConvertTo-YouTubeCookieFile $httpOnly) -match '#HttpOnly_') 'HTTP-only cookie marker was lost'
    Expect-SessionRejection { ConvertTo-YouTubeCookieFile 'invalid file' } 'Invalid cookie header was accepted'
    Expect-SessionRejection { ConvertTo-YouTubeCookieFile "# Netscape HTTP Cookie File`ninvalid row" } 'Malformed cookie row was accepted'
    Expect-SessionRejection { ConvertTo-YouTubeCookieFile ($synthetic.Replace([string]$expires,'1')) } 'Expired login was accepted'
    Expect-SessionRejection { ConvertTo-YouTubeCookieFile ($synthetic.Replace("`tSID`t","`tPREF`t")) } 'Non-authentication cookies were treated as sign-in'
    Expect-SessionRejection { ConvertTo-YouTubeCookieFile ('x'*524289) } 'Oversized import was accepted'
    Import-YouTubeCookieFile $fixture
    Check-Session (Test-YouTubeSession) 'Encrypted import was not saved'
    $blob=[IO.File]::ReadAllBytes((Join-Path $script:youtubeAuthRoot 'session.bin'))
    Check-Session ([Text.Encoding]::UTF8.GetString($blob) -notmatch 'synthetic-session-for-test') 'Session was stored in plaintext'
    Check-Session (([IO.File]::ReadAllText($fixture)) -eq $synthetic) 'User-selected export was modified'
    Check-Session ((Get-Acl -LiteralPath $script:youtubeAuthRoot).AreAccessRulesProtected) 'Private session folder inherits broad permissions'
    $download=@(Get-YouTubeAuthArguments 'download')
    $search=@(Get-YouTubeAuthArguments 'search')
    Check-Session ($download[0] -eq '--cookies' -and $search[0] -eq '--cookies') 'Session was not passed with yt-dlp cookie arguments'
    Check-Session ($download -notcontains '--cookies-from-browser') 'Browser database access was introduced'
    Check-Session ($download[1] -ne $search[1]) 'Concurrent request leases share one cookie file'
    Check-Session ([IO.File]::ReadAllText($download[1]) -eq $clean) 'DPAPI round trip changed cookie contents'
    Clear-YouTubeLease 'download'
    Check-Session (-not [IO.File]::Exists($download[1]) -and [IO.File]::Exists($search[1])) 'Download cleanup deleted another request lease'
    Clear-YouTubeLease 'search'
    Check-Session (-not [IO.File]::Exists($search[1])) 'Search cleanup left a plaintext file'
    $recommendations=@(Get-YouTubeRecommendationArguments)
    Check-Session ($recommendations[-1] -eq ':ytrec' -and $recommendations -contains '--skip-download') 'Recommendations do not use the bounded metadata-only feed'
    Check-Session ($recommendations[$recommendations.IndexOf('--playlist-end')+1] -eq '12') 'Recommendation feed is unbounded'
    Clear-YouTubeLease 'search'
    $args=@(Get-DownloadArguments 'https://youtu.be/_70Q-Xj3rEo' 'mp3' $fixtureRoot)
    Check-Session ($args -contains '--cookies') 'Actual download builder omitted the imported session'
    Clear-YouTubeLease 'download'
    $args=@(Get-SearchArguments 'confident')
    Check-Session ($args -contains '--cookies' -and $args[-1] -eq 'ytsearch12:confident') 'Search session wiring changed the query'
    Clear-YouTubeLease 'search'
    Check-Session ((Protect-YouTubeLogLine 'Cookie: SID=synthetic-session-for-test') -eq '[Session data redacted]') 'Cookie header was logged'
    Check-Session ((Protect-YouTubeLogLine 'Authorization: Bearer synthetic') -eq '[Session data redacted]') 'Authorization header was logged'
    Check-Session ((Protect-YouTubeLogLine ($clean.Split("`n")[1])) -eq '[Session data redacted]') 'Netscape cookie row was logged'
    Check-Session ((Protect-YouTubeLogLine 'PROGRESS: 20%') -eq 'PROGRESS: 20%') 'Log filter broke progress'
    # Let the real yt-dlp consume a synthetic lease on a generated local file.
    $audio=Join-Path $fixtureRoot 'silent.wav'
    & (Find-Tool 'ffmpeg') -hide_banner -loglevel error -f lavfi -i 'anullsrc=r=8000:cl=mono' -t 0.1 $audio
    Check-Session ($LASTEXITCODE -eq 0) 'Local session-consumption fixture could not be generated'
    $localArgs=@('--ignore-config','--no-colors','--enable-file-urls','--simulate')+@(Get-YouTubeAuthArguments 'download')+@('--',([uri]$audio).AbsoluteUri)
    $info=New-Object Diagnostics.ProcessStartInfo
    $info.FileName=Find-Tool 'yt-dlp'
    $info.Arguments=($localArgs | ForEach-Object { Quote-Argument $_ }) -join ' '
    $info.UseShellExecute=$false; $info.CreateNoWindow=$true
    $info.RedirectStandardOutput=$true; $info.RedirectStandardError=$true
    $process=[Diagnostics.Process]::Start($info)
    try {
        $stdout=$process.StandardOutput.ReadToEndAsync(); $stderr=$process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit(15000)) { $process.Kill(); $process.WaitForExit(); throw 'Local yt-dlp session check timed out' }
        Check-Session ($process.ExitCode -eq 0) 'Real yt-dlp rejected the validated synthetic cookie file'
        Check-Session (($stdout.GetAwaiter().GetResult()+$stderr.GetAwaiter().GetResult()) -notmatch 'synthetic-session-for-test') 'Real yt-dlp exposed cookie values in normal output'
    } finally { $process.Dispose(); Clear-YouTubeLease 'download' }
    Check-Session (@(Get-ChildItem -LiteralPath $script:youtubeAuthRoot -Filter 'request-*.txt').Count -eq 0) 'Real process completion left a plaintext lease'
    [IO.File]::WriteAllBytes((Join-Path $script:youtubeAuthRoot 'session.bin'),[byte[]](1,2,3))
    Expect-SessionRejection { Get-YouTubeAuthArguments 'download' } 'Corrupt encrypted session was accepted'
    Check-Session (@(Get-ChildItem -LiteralPath $script:youtubeAuthRoot -Filter 'request-*.txt').Count -eq 0) 'Failure leaked a request file'
    Import-YouTubeCookieFile $fixture
    Import-YouTubeCookieFile $fixture # Replacing an existing import must also be atomic.
    Check-Session (Test-YouTubeSession) 'Replacing an encrypted session failed'
    Disconnect-YouTube
    Check-Session (-not (Test-YouTubeSession)) 'Disconnect retained the session'
    Check-Session (@(Get-YouTubeAuthArguments 'search').Count -eq 0) 'Disconnect did not restore public mode'
    Check-Session (@(Get-ChildItem -LiteralPath $script:youtubeAuthRoot -File).Count -eq 0) 'Session artifacts remained after disconnect'
    Write-Host "PASS: $script:sessionCheckCount synthetic YouTube session checks. No real credentials, browser databases or network accessed."
} finally {
    Clear-YouTubeLease 'download'
    Clear-YouTubeLease 'search'
    $resolved=[IO.Path]::GetFullPath($fixtureRoot)
    if ($resolved.StartsWith($tempBase,[StringComparison]::OrdinalIgnoreCase) -and (Split-Path $resolved -Leaf) -match '^PocketMusic-sessioncheck-[a-f0-9]{32}$') {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
