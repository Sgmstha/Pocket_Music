# Approved scope: user-selected YouTube-only cookie imports, never browser databases.
$script:youtubeAuthRoot = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'PocketMusic\YouTube'
$script:youtubeLeases = @{ download=$null; search=$null }
$script:youtubeTestRun = $false
foreach ($name in @('SmokeTest','PreviewImage','PlaybackTestFolder','PreviewLiveTest','WindowsMediaTest','LiveSearchTest','NoAccountSession')) {
    if (Get-Variable -Name $name -ValueOnly -ErrorAction SilentlyContinue) { $script:youtubeTestRun=$true }
}

function Test-YouTubeSession {
    return (-not $script:youtubeTestRun -and [IO.File]::Exists((Join-Path $script:youtubeAuthRoot 'session.bin')))
}

function ConvertTo-YouTubeCookieFile([string]$Text) {
    if ([Text.Encoding]::UTF8.GetByteCount($Text) -gt 524288) { throw 'Choose a YouTube-only cookie file smaller than 512 KB.' }
    $lines=@($Text -split '\r?\n')
    if ($lines.Count -gt 4096 -or $lines[0].TrimStart([char]0xFEFF) -notmatch '^# (Netscape HTTP Cookie File|HTTP Cookie File)\s*$') { throw 'Choose a Netscape-format YouTube cookie file.' }
    $result=New-Object Collections.Generic.List[string]
    $result.Add('# Netscape HTTP Cookie File')
    $hasLogin=$false
    $now=[DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    foreach ($line in $lines) {
        if (-not $line -or ($line.StartsWith('#') -and -not $line.StartsWith('#HttpOnly_'))) { continue }
        $fields=$line -split "`t",7
        if ($fields.Count -ne 7) { throw 'Invalid cookie file row. Export YouTube cookies again in Netscape format.' }
        $domain=($fields[0] -replace '^#HttpOnly_','').TrimStart('.').ToLowerInvariant()
        if ($domain -ne 'youtube.com' -and -not $domain.EndsWith('.youtube.com',[StringComparison]::Ordinal)) { continue }
        if ($domain -notmatch '^(?:[a-z0-9-]+\.)*youtube\.com$' -or $fields[1] -notin @('TRUE','FALSE') -or $fields[3] -notin @('TRUE','FALSE') -or -not $fields[2].StartsWith('/') -or -not $fields[5] -or $fields[6] -match '[\x00-\x1f\x7f]') { throw 'The YouTube cookie file contains an invalid cookie.' }
        $expires=0L
        if ($fields[4] -notmatch '^\d+$' -or -not [long]::TryParse($fields[4],[ref]$expires)) { throw 'Invalid cookie expiry.' }
        if ($expires -ne 0 -and $expires -le $now) { continue }
        $result.Add($line)
        if ($fields[5] -in @('SID','SAPISID','__Secure-1PSID','__Secure-3PSID') -and $fields[6]) { $hasLogin=$true }
    }
    if (-not $hasLogin) { throw 'No unexpired YouTube sign-in cookies were found. Sign in on YouTube, then export its cookies again.' }
    return (($result -join "`n")+"`n")
}

function Initialize-YouTubePrivateFolder {
    $directory=[IO.Directory]::CreateDirectory($script:youtubeAuthRoot)
    if (($directory.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { throw 'The YouTube session folder must not be a directory link.' }
    $acl=New-Object Security.AccessControl.DirectorySecurity
    $acl.SetAccessRuleProtection($true,$false)
    $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule $sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow'))
    [IO.Directory]::SetAccessControl($script:youtubeAuthRoot,$acl)
}

function Import-YouTubeCookieFile([string]$Path) {
    $file=Get-Item -LiteralPath $Path -ErrorAction Stop
    if ($file.PSIsContainer -or $file.Length -gt 524288) { throw 'Choose a Netscape cookie file smaller than 512 KB.' }
    $clean=ConvertTo-YouTubeCookieFile ([IO.File]::ReadAllText($file.FullName))
    Add-Type -AssemblyName System.Security
    $bytes=[Text.Encoding]::UTF8.GetBytes($clean)
    $stage=$null
    try {
        $blob=[Security.Cryptography.ProtectedData]::Protect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
        Initialize-YouTubePrivateFolder
        $stage=Join-Path $script:youtubeAuthRoot ('import-'+[guid]::NewGuid().ToString('N')+'.bin')
        $saved=Join-Path $script:youtubeAuthRoot 'session.bin'
        [IO.File]::WriteAllBytes($stage,$blob)
        if ([IO.File]::Exists($saved)) { [IO.File]::Replace($stage,$saved,[NullString]::Value) } else { [IO.File]::Move($stage,$saved) }
    } finally {
        [array]::Clear($bytes,0,$bytes.Length)
        $clean=$null
        if ($stage -and [IO.File]::Exists($stage)) { [IO.File]::Delete($stage) }
    }
}

function Clear-YouTubeLease([ValidateSet('download','search')][string]$Operation) {
    $path=$script:youtubeLeases[$Operation]
    if ($path) {
        if ([IO.Path]::GetFullPath((Split-Path $path)) -eq [IO.Path]::GetFullPath($script:youtubeAuthRoot) -and (Split-Path $path -Leaf) -match '^request-\d+-[a-f0-9]{32}\.txt$') {
            if ([IO.File]::Exists($path)) { [IO.File]::Delete($path) }
        }
        $script:youtubeLeases[$Operation]=$null
    }
}

function Get-YouTubeAuthArguments([ValidateSet('download','search')][string]$Operation) {
    if (-not (Test-YouTubeSession)) { return @() }
    Clear-YouTubeLease $Operation
    Add-Type -AssemblyName System.Security
    $bytes=$null
    $clean=$null
    try {
        $savedPath=Join-Path $script:youtubeAuthRoot 'session.bin'
        if ((Get-Item -LiteralPath $savedPath).Length -gt 1048576) { throw 'Invalid encrypted session size.' }
        $blob=[IO.File]::ReadAllBytes($savedPath)
        $bytes=[Security.Cryptography.ProtectedData]::Unprotect($blob,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
        $clean=ConvertTo-YouTubeCookieFile ([Text.Encoding]::UTF8.GetString($bytes))
        Initialize-YouTubePrivateFolder
        $path=Join-Path $script:youtubeAuthRoot ('request-'+$PID+'-'+[guid]::NewGuid().ToString('N')+'.txt')
        $script:youtubeLeases[$Operation]=$path
        [IO.File]::WriteAllText($path,$clean,(New-Object Text.UTF8Encoding $false))
        return @('--cookies',$path)
    } catch {
        Clear-YouTubeLease $Operation
        throw 'The saved YouTube session cannot be used or has expired. Open YouTube connection and import fresh YouTube cookies.'
    } finally { if ($bytes) { [array]::Clear($bytes,0,$bytes.Length) }; $clean=$null }
}

function Disconnect-YouTube {
    Clear-YouTubeLease 'download'
    Clear-YouTubeLease 'search'
    $path=Join-Path $script:youtubeAuthRoot 'session.bin'
    if ([IO.File]::Exists($path)) { [IO.File]::Delete($path) }
}

function Get-YouTubeRecommendationArguments {
    if (-not (Test-YouTubeSession)) { throw 'Import YouTube cookies in YouTube connection before loading account recommendations.' }
    return @('--ignore-config','--no-colors','--encoding','utf-8','--flat-playlist','--skip-download','--dump-single-json','--playlist-end','12','--socket-timeout','15','--retries','0','--extractor-retries','0','--sleep-requests','1') + @(Get-YouTubeAuthArguments 'search') + @('--',':ytrec')
}

function Protect-YouTubeLogLine([string]$Line) {
    if ($Line -match '(?i)^\s*(cookie|set-cookie|authorization):|^#?HttpOnly_.*\t|^\.?[a-z0-9.-]*youtube\.com\t') { return '[Session data redacted]' }
    return $Line
}

function Remove-AbandonedYouTubeLeases {
    if ($script:youtubeTestRun -or -not [IO.Directory]::Exists($script:youtubeAuthRoot)) { return }
    foreach ($file in @(Get-ChildItem -LiteralPath $script:youtubeAuthRoot -Filter 'request-*.txt' -File | Select-Object -First 64)) {
        if ($file.Name -match '^request-(\d+)-[a-f0-9]{32}\.txt$') {
            $ownerPid=[int]$Matches[1]
            if (-not (Get-Process -Id $ownerPid -ErrorAction SilentlyContinue)) { [IO.File]::Delete($file.FullName) }
        }
    }
}
