Set-StrictMode -Version Latest

function ConvertTo-VideoUrl([string]$Text) {
    $uri = $null
    if (-not [Uri]::TryCreate($Text.Trim(), [UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -ne 'https' -or $uri.UserInfo -or ($uri.Port -ne 443)) {
        throw 'Paste a full HTTPS YouTube video link.'
    }
    $id = $null
    if ($uri.Host -eq 'youtu.be') { $id = $uri.AbsolutePath.Trim('/') }
    elseif ($uri.Host -in @('youtube.com','www.youtube.com','m.youtube.com','music.youtube.com')) {
        if ($uri.AbsolutePath -eq '/watch') {
            foreach ($part in $uri.Query.TrimStart('?').Split('&')) {
                if ($part -match '^v=([A-Za-z0-9_-]{11})$') { $id = $Matches[1] }
            }
        } elseif ($uri.AbsolutePath -match '^/(shorts|live)/([A-Za-z0-9_-]{11})/?$') { $id = $Matches[2] }
    }
    if ($id -notmatch '^[A-Za-z0-9_-]{11}$') { throw 'Use a link to one YouTube video, rather than a channel or playlist.' }
    return "https://www.youtube.com/watch?v=$id"
}

function Find-Tool([string]$Name) {
    $local = Join-Path $PSScriptRoot "tools\$Name.exe"
    if (Test-Path -LiteralPath $local) { return $local }
    $command = Get-Command "$Name.exe" -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    return $null
}

function Get-DownloadArguments([string]$Url, [string]$Format, [string]$Folder) {
    $cleanUrl = ConvertTo-VideoUrl $Url
    if ($Format -notin @('mp3','mp4','m4a','opus','flac','wav')) { throw 'Choose a supported download format.' }
    if (-not [IO.Directory]::Exists($Folder)) { throw 'Choose an existing destination folder.' }
    $probe = Join-Path $Folder ([IO.Path]::GetRandomFileName())
    try { [IO.File]::WriteAllText($probe, ''); [IO.File]::Delete($probe) }
    catch { throw 'This folder is not writable. Choose another folder.' }
    $ffmpeg = Find-Tool 'ffmpeg'
    $node = Find-Tool 'node'
    if (-not $ffmpeg -or -not (Find-Tool 'ffprobe') -or -not $node) { throw 'FFmpeg, FFprobe, and Node.js are required. Run Setup.cmd, then reopen this app.' }
    $formatArguments = @('-f','bestaudio/best','-x','--audio-format',$Format,'--audio-quality','0')
    if ($Format -eq 'mp4') {
        $formatArguments = @('-f','bv[ext=mp4]+ba[ext=m4a]/b[ext=mp4]', '--merge-output-format','mp4','--remux-video','mp4')
    }
    return @('--ignore-config','--no-playlist','--no-colors','--no-quiet','--newline','--no-overwrites',
        '--windows-filenames','--trim-filenames','170','--socket-timeout','20','--retries','3',
        '--js-runtimes',"node:$node",'--ffmpeg-location',(Split-Path $ffmpeg)) + $formatArguments + @(
        '--embed-metadata','--progress','--progress-template','download:PROGRESS:%(progress._percent_str)s',
        '--print','after_move:FILE:%(filepath)s','--no-simulate',
        '-P',$Folder,'-o','%(title)s [%(id)s].%(ext)s','--',$cleanUrl)
}

# Windows CommandLineToArgvW-compatible quoting, including trailing backslashes.
function Quote-Argument([string]$Value) {
    return '"' + [regex]::Replace([regex]::Replace($Value, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"'
}

function Get-FriendlyError([string]$Log) {
    if ($Log -match '(?i)sign in|login|private video|members.only|age.restricted|not a bot') { return 'YouTube requires sign-in or restricts this video. This app only downloads publicly accessible content; try another authorized video.' }
    if ($Log -match '(?i)copyright|not available|unavailable|removed') { return 'This video is unavailable or restricted. Try another authorized video.' }
    if ($Log -match '(?i)403|429') { return 'YouTube refused or rate-limited the request. Wait and retry, or run Setup.cmd to update yt-dlp.' }
    if ($Log -match '(?i)resolve|timed out|network|connection') { return 'The connection failed. Check your internet access and try again.' }
    if ($Log -match '(?i)permission denied|access.+denied|no space') { return 'The file could not be saved. Check folder permissions and free disk space.' }
    return 'The download could not finish. See the details below. Updating yt-dlp with Setup.cmd may help.'
}
