# GPL-3.0. Single Centralized Playback Service for Pocket Music v2.

$script:playbackSessionPath = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'PocketMusic\playback-session.json'
# Automated runs must not overwrite the listener's real last track or queue.
if ($SmokeTest -or $PlaybackTestFolder -or $PreviewImage -or $LiveSearchTest -or $PreviewLiveTest) {
    $script:playbackSessionPath = Join-Path ([IO.Path]::GetTempPath()) ('PocketMusic-test-session-' + [guid]::NewGuid() + '.json')
    $window.Add_Closed({ Remove-Item -LiteralPath $script:playbackSessionPath -ErrorAction SilentlyContinue })
}

# Central Playback State
$script:playback = @{
    currentTrack   = $null      # Track object (Title, Subtitle, Path or Id/Url, Artwork, Duration)
    source         = 'none'      # 'none' | 'local' | 'preview'
    isPlaying      = $false
    isBuffering    = $false
    position       = 0.0
    duration       = 0.0
    volume         = 0.6
    isMuted        = $false
    prevVolume     = 0.6
    queue          = [System.Collections.ArrayList]::new()
    queueIndex     = -1
    isShuffled     = $false
    shuffledOrder  = [System.Collections.ArrayList]::new()
    repeatMode     = 'off'       # 'off' | 'all' | 'one'
    lastError      = $null
    crossfade      = $false
    sleepTimerEnd  = $null      # [DateTime] or 'end_of_track' or $null
    sleepTimerType = 'off'        # 'off' | '15' | '30' | '60' | 'track'
}

$script:playerState = 'idle'
$script:playbackToken = 0       # For race cancellation / rapid skip debouncing
$script:previewToken = ''
$script:previewReady = $false
$script:previewBlocked = $false
$script:previewLastStatus = [DateTime]::MinValue
$script:previewStartedAt = [DateTime]::MinValue
$script:previewProgressAt = [DateTime]::MinValue
$script:previewFailed = $false
$script:previewEndedHandled = $false

$script:playerDecode = $null
$script:playerDecodeError = $null
$script:playerTempFile = $null
$script:playerTempDir = Join-Path ([IO.Path]::GetTempPath()) ('PocketMusic-player-' + [guid]::NewGuid().ToString('N'))
$script:playerConverted = $false
$script:playerDesired = $false
$script:playerSeeking = $false
$script:playerOpeningAt = [DateTime]::MinValue

# Formatting helper
function Get-TrackProp($Obj, [string]$PropName, $Default = $null) {
    if ($null -ne $Obj -and $Obj.PSObject.Properties[$PropName]) { return $Obj.$PropName }
    return $Default
}

function Format-TimeDisplay([double]$Seconds) {
    if ([double]::IsNaN($Seconds) -or [double]::IsInfinity($Seconds) -or $Seconds -lt 0) { $Seconds = 0 }
    $time = [TimeSpan]::FromSeconds($Seconds)
    if ($time.TotalHours -ge 1) { return $time.ToString('h\:mm\:ss') }
    return $time.ToString('m\:ss')
}

# --- SHUFFLE LOGIC ---
# Generates a fixed shuffled order of queue indices without repeating
function Rebuild-ShuffleOrder {
    $count = $script:playback.queue.Count
    $script:playback.shuffledOrder.Clear()
    if ($count -eq 0) { return }
    $indices = [System.Collections.Generic.List[int]]::new()
    for ($i = 0; $i -lt $count; $i++) { $indices.Add($i) }

    # Keep current track first in shuffle order if currently playing
    $current = $script:playback.queueIndex
    if ($current -ge 0 -and $current -lt $count) {
        $script:playback.shuffledOrder.Add($current)
        $indices.Remove($current) | Out-Null
    }

    # Fisher-Yates shuffle remainder
    $rng = New-Object System.Random
    while ($indices.Count -gt 0) {
        $pick = $rng.Next(0, $indices.Count)
        $script:playback.shuffledOrder.Add($indices[$pick])
        $indices.RemoveAt($pick)
    }
}

# --- PERSISTENCE ---
function Save-PlaybackSession {
    try {
        $localTracks = @()
        foreach ($t in $script:playback.queue) {
            $tPath = if ($t -and $t.PSObject.Properties['Path']) { $t.Path } else { $null }
            if ($tPath) {
                $tTitle = if ($t.PSObject.Properties['Title']) { $t.Title } else { [IO.Path]::GetFileNameWithoutExtension($tPath) }
                $tSub = if ($t.PSObject.Properties['Subtitle']) { $t.Subtitle } else { '' }
                $tDur = if ($t.PSObject.Properties['Duration']) { $t.Duration } else { '' }
                $localTracks += @{ Title = $tTitle; Subtitle = $tSub; Path = $tPath; Duration = $tDur }
            }
        }
        $curTrack = $script:playback.currentTrack
        $curPath = if ($curTrack -and $curTrack.PSObject.Properties['Path']) { $curTrack.Path } else { $null }
        $curTitle = if ($curTrack -and $curTrack.PSObject.Properties['Title']) { $curTrack.Title } else { '' }
        $curSub = if ($curTrack -and $curTrack.PSObject.Properties['Subtitle']) { $curTrack.Subtitle } else { '' }
        $curDur = if ($curTrack -and $curTrack.PSObject.Properties['Duration']) { $curTrack.Duration } else { '' }

        $data = @{
            volume         = $script:playback.volume
            isMuted        = $script:playback.isMuted
            repeatMode     = $script:playback.repeatMode
            isShuffled     = $script:playback.isShuffled
            crossfade      = $script:playback.crossfade
            queueIndex     = $script:playback.queueIndex
            position       = if ($script:playback.source -eq 'local') { $script:playback.position } else { 0 }
            hasLocalTrack  = ($script:playback.source -eq 'local' -and $curPath)
            lastLocalTrack = if ($script:playback.source -eq 'local' -and $curTrack -and $curPath) {
                                @{ Title = $curTitle; Subtitle = $curSub; Path = $curPath; Duration = $curDur }
                             } else { $null }
            queue          = $localTracks
        }
        $json = $data | ConvertTo-Json -Depth 4 -Compress
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($script:playbackSessionPath)) | Out-Null
        [IO.File]::WriteAllText($script:playbackSessionPath, $json)
    } catch { Add-Log ('Session save error: ' + $_.Exception.Message) }
}

function Restore-PlaybackSession {
    try {
        if (-not (Test-Path -LiteralPath $script:playbackSessionPath)) { return }
        $saved = [IO.File]::ReadAllText($script:playbackSessionPath) | ConvertFrom-Json
        if ($null -ne $saved.volume) {
            $script:playback.volume = [double]$saved.volume
            if ($ui.PlayerVolume) { $ui.PlayerVolume.Value = $script:playback.volume }
        }
        if ($null -ne $saved.isMuted) { $script:playback.isMuted = [bool]$saved.isMuted }
        if ($saved.repeatMode -in @('off','all','one')) { $script:playback.repeatMode = $saved.repeatMode }
        if ($null -ne $saved.isShuffled) { $script:playback.isShuffled = [bool]$saved.isShuffled }
        if ($null -ne $saved.crossfade) { $script:playback.crossfade = [bool]$saved.crossfade }

        # Restore queue and track only if local files exist
        if ($saved.queue -and $saved.queue.Count -gt 0) {
            $script:playback.queue.Clear()
            foreach ($item in $saved.queue) {
                if ($item.Path -and (Test-Path -LiteralPath $item.Path -PathType Leaf)) {
                    $script:playback.queue.Add([pscustomobject]@{
                        Title    = $item.Title
                        Subtitle = $item.Subtitle
                        Path     = $item.Path
                        Duration = $item.Duration
                        Artwork  = $null
                    }) | Out-Null
                }
            }
        }
        if ($script:playback.isShuffled) { Rebuild-ShuffleOrder }

        if ($saved.hasLocalTrack -and $saved.lastLocalTrack -and $saved.lastLocalTrack.Path -and (Test-Path -LiteralPath $saved.lastLocalTrack.Path -PathType Leaf)) {
            $track = [pscustomobject]@{
                Title    = $saved.lastLocalTrack.Title
                Subtitle = $saved.lastLocalTrack.Subtitle
                Path     = $saved.lastLocalTrack.Path
                Duration = $saved.lastLocalTrack.Duration
                Artwork  = $null
            }
            $script:playback.currentTrack = $track
            $script:playerTrack = $track
            $script:playback.source = 'local'
            $script:playback.isPlaying = $false
            $script:playback.position = [double]$saved.position
            $script:playback.queueIndex = [int]$saved.queueIndex
            $script:playerIndex = $script:playback.queueIndex

            # Prepare paused at position
            $script:playerDesired = $false
            if ($ui.PlayerTitle) { $ui.PlayerTitle.Text = $track.Title; $ui.PlayerTitle.ToolTip = $track.Title }
            if ($ui.PlayerElapsed) { $ui.PlayerElapsed.Text = Format-TimeDisplay $script:playback.position }
            if ($ui.PlayerSeek) { $ui.PlayerSeek.Value = $script:playback.position }

            Set-PlaybackUIState 'paused' 'Restored paused session'
        }
    } catch { Add-Log ('Session restore error: ' + $_.Exception.Message) }
}

# --- STOP CORES CLEANLY ---
function Stop-LocalCore {
    if ($script:media) {
        try { $script:media.Stop(); $script:media.Close() } catch { }
        $script:media = $null
    }
    if ($script:playerDecode) {
        try {
            if (-not $script:playerDecode.HasExited) { $script:playerDecode.Kill(); $script:playerDecode.WaitForExit(2000) | Out-Null }
            $script:playerDecode.Dispose()
        } catch { }
        $script:playerDecode = $null
    }
    if ($script:playerTempFile -and (Test-Path -LiteralPath $script:playerTempFile)) {
        Remove-Item -LiteralPath $script:playerTempFile -Force -ErrorAction SilentlyContinue
    }
    $script:playerTempFile = $null
}

function Stop-PreviewCore {
    if ((Get-Variable 'previewWatch' -Scope Script -ErrorAction SilentlyContinue) -and $script:previewWatch) { $script:previewWatch.Stop() }
    $script:previewToken = ''
    $script:previewReady = $false
    if ($script:previewBrowser) {
        try { $script:previewBrowser.Dispose() } catch { }
        $script:previewBrowser = $null
    }
    if ($ui['PreviewHost']) { $ui['PreviewHost'].Child = $null }
    if ((Get-Variable 'previewPane' -Scope Script -ErrorAction SilentlyContinue) -and $script:previewPane) { $script:previewPane.Visibility = 'Collapsed' }
    if ((Get-Variable 'previewRow' -Scope Script -ErrorAction SilentlyContinue) -and $script:previewRow) { $script:previewRow.Height = New-Object Windows.GridLength 0 }
}

function Stop-AllCores {
    Stop-LocalCore
    Stop-PreviewCore
}

# --- UI SYNC ---
function Set-PlaybackUIState([string]$State, [string]$Message) {
    $script:playerState = $State
    $script:playback.isPlaying = ($State -eq 'playing')
    $script:playback.isBuffering = ($State -in @('opening','preparing','loading','buffering'))

    if ($ui['PlayerStatus']) {
        $ui['PlayerStatus'].Text = $Message
        $ui['PlayerStatus'].ToolTip = $Message
    }
    if ($ui['PlayerToggle']) {
        $ui['PlayerToggle'].Content = if ($script:playback.isPlaying) { [char]::ConvertFromUtf32(0x23F8) } else { [char]::ConvertFromUtf32(0x25B6) }
        $ui['PlayerToggle'].IsEnabled = $State -in @('playing','paused','ended')
    }
    if ($ui['PlayerSeek']) {
        $ui['PlayerSeek'].IsEnabled = $State -in @('playing','paused','ended')
    }
    if ($ui['PlayerPrevious']) {
        $ui['PlayerPrevious'].IsEnabled = ($null -ne $script:playback.currentTrack)
    }

    # Calculate Next button enabled state
    $hasNext = $false
    $qCount = $script:playback.queue.Count
    if ($script:playback.repeatMode -in @('all','one') -and $qCount -gt 0) {
        $hasNext = $true
    } elseif ($script:playback.isShuffled) {
        $curOrderIdx = $script:playback.shuffledOrder.IndexOf($script:playback.queueIndex)
        $hasNext = ($curOrderIdx -ge 0 -and $curOrderIdx -lt ($script:playback.shuffledOrder.Count - 1))
    } else {
        $hasNext = ($script:playback.queueIndex -ge 0 -and $script:playback.queueIndex -lt ($qCount - 1))
    }
    if ($ui['PlayerNext']) { $ui['PlayerNext'].IsEnabled = $hasNext }

    # Source Badge
    if ($ui['PlayerSourceText'] -and $ui['PlayerSourceBadge']) {
        switch ($script:playback.source) {
            'local'   {
                $ui.PlayerSourceText.Text = 'Local'
                $ui.PlayerSourceBadge.Background = [Windows.Media.BrushConverter]::new().ConvertFromString('#364935')
                $ui.PlayerSourceText.Foreground = [Windows.Media.BrushConverter]::new().ConvertFromString('#A3E2A0')
            }
            'preview' {
                $ui.PlayerSourceText.Text = 'Preview'
                $ui.PlayerSourceBadge.Background = [Windows.Media.BrushConverter]::new().ConvertFromString('#542B36')
                $ui.PlayerSourceText.Foreground = [Windows.Media.BrushConverter]::new().ConvertFromString('#F6A5B5')
            }
            default   {
                $ui.PlayerSourceText.Text = 'Ready'
                $ui.PlayerSourceBadge.Background = [Windows.Media.BrushConverter]::new().ConvertFromString('#403454')
                $ui.PlayerSourceText.Foreground = [Windows.Media.BrushConverter]::new().ConvertFromString('#CEC1FF')
            }
        }
    }

    # Shuffle Button State
    if ($ui['PlayerShuffle']) {
        $ui['PlayerShuffle'].Foreground = if ($script:playback.isShuffled) { [Windows.Media.BrushConverter]::new().ConvertFromString('#CEC1FF') } else { [Windows.Media.BrushConverter]::new().ConvertFromString('#8F83A0') }
        $ui['PlayerShuffle'].ToolTip = if ($script:playback.isShuffled) { 'Shuffle (On)' } else { 'Shuffle (Off)' }
    }

    # Repeat Button State
    if ($ui['PlayerRepeat']) {
        switch ($script:playback.repeatMode) {
            'one' { $ui['PlayerRepeat'].Content = [char]::ConvertFromUtf32(0x1F502); $ui['PlayerRepeat'].Foreground = [Windows.Media.BrushConverter]::new().ConvertFromString('#CEC1FF'); $ui['PlayerRepeat'].ToolTip = 'Repeat: One' }
            'all' { $ui['PlayerRepeat'].Content = [char]::ConvertFromUtf32(0x1F501); $ui['PlayerRepeat'].Foreground = [Windows.Media.BrushConverter]::new().ConvertFromString('#CEC1FF'); $ui['PlayerRepeat'].ToolTip = 'Repeat: All' }
            default { $ui['PlayerRepeat'].Content = [char]::ConvertFromUtf32(0x1F501); $ui['PlayerRepeat'].Foreground = [Windows.Media.BrushConverter]::new().ConvertFromString('#8F83A0'); $ui['PlayerRepeat'].ToolTip = 'Repeat: Off' }
        }
    }

    # Mute / Volume Button Icon
    if ($ui['PlayerMute']) {
        $ui['PlayerMute'].Content = if ($script:playback.isMuted -or $script:playback.volume -eq 0) { [char]::ConvertFromUtf32(0x1F507) } else { [char]::ConvertFromUtf32(0x1F50A) }
    }

    # Artwork
    if ($ui['PlayerArtwork']) {
        $trackArt = if ($script:playback.currentTrack -and $script:playback.currentTrack.PSObject.Properties['Artwork']) { $script:playback.currentTrack.Artwork } else { $null }
        $trackId = if ($script:playback.currentTrack -and $script:playback.currentTrack.PSObject.Properties['Id']) { $script:playback.currentTrack.Id } else { $null }
        if ($trackArt) {
            $ui['PlayerArtwork'].Source = $trackArt
            $ui['PlayerArtwork'].Visibility = 'Visible'
        } elseif ($script:playback.source -eq 'preview' -and $trackId) {
            $ui['PlayerArtwork'].Source = [uri]("https://i.ytimg.com/vi/" + $trackId + '/hqdefault.jpg')
            $ui['PlayerArtwork'].Visibility = 'Visible'
        } else {
            $ui['PlayerArtwork'].Source = $null
            $ui['PlayerArtwork'].Visibility = 'Collapsed'
        }
    }

    # Sleep timer tooltip
    if ($ui['PlayerSleepBtn']) {
        $ui['PlayerSleepBtn'].ToolTip = "Sleep timer: " + $script:playback.sleepTimerType
        $ui['PlayerSleepBtn'].Foreground = if ($script:playback.sleepTimerType -ne 'off') { [Windows.Media.BrushConverter]::new().ConvertFromString('#CEC1FF') } else { [Windows.Media.BrushConverter]::new().ConvertFromString('#B0A4C2') }
    }

    if (Get-Command 'Update-PlayerTimer' -ErrorAction SilentlyContinue) {
        Update-PlayerTimer
    }
    Update-ListsActiveRow
    Update-QueueUI
    if (Get-Command Sync-WindowsPlayback -ErrorAction SilentlyContinue) { Sync-WindowsPlayback }
}

function Update-ListsActiveRow {
    $cur = $script:playback.currentTrack
    $activePath = if ($script:playback.source -eq 'local' -and $cur -and $cur.PSObject.Properties['Path']) { $cur.Path } else { $null }
    $activeId = if ($script:playback.source -eq 'preview' -and $cur -and $cur.PSObject.Properties['Id']) { $cur.Id } else { $null }

    if ($ui['LibraryTracks'] -and $ui['LibraryTracks'].ItemsSource) {
        foreach ($item in $ui['LibraryTracks'].ItemsSource) {
            $isActive = ($activePath -and $item.PSObject.Properties['Path'] -and $item.Path -eq $activePath)
            $vis = if ($isActive) { 'Visible' } else { 'Collapsed' }
            $item | Add-Member -NotePropertyName IsActiveRow -NotePropertyValue $vis -Force
        }
        $ui['LibraryTracks'].Items.Refresh()
    }
    if ($ui['RecentTracks'] -and $ui['RecentTracks'].ItemsSource) {
        foreach ($item in $ui['RecentTracks'].ItemsSource) {
            $isActive = ($activePath -and $item.PSObject.Properties['Path'] -and $item.Path -eq $activePath)
            $vis = if ($isActive) { 'Visible' } else { 'Collapsed' }
            $item | Add-Member -NotePropertyName IsActiveRow -NotePropertyValue $vis -Force
        }
        $ui['RecentTracks'].Items.Refresh()
    }
    if ($ui['SearchResults'] -and $ui['SearchResults'].ItemsSource) {
        foreach ($item in $ui['SearchResults'].ItemsSource) {
            $isActive = ($activeId -and $item.PSObject.Properties['Id'] -and $item.Id -eq $activeId)
            $vis = if ($isActive) { 'Visible' } else { 'Collapsed' }
            $item | Add-Member -NotePropertyName IsActiveRow -NotePropertyValue $vis -Force
        }
        $ui['SearchResults'].Items.Refresh()
    }
}

function Update-QueueUI {
    if (-not $ui['QueueList']) { return }
    $items = @()
    $idx = 1
    for ($i = 0; $i -lt $script:playback.queue.Count; $i++) {
        $track = $script:playback.queue[$i]
        $isCurrent = ($i -eq $script:playback.queueIndex)
        $tSub = if ($track.PSObject.Properties['Subtitle']) { $track.Subtitle } elseif ($track.PSObject.Properties['Path'] -and $track.Path) { [IO.Path]::GetFileName($track.Path) } else { '' }
        $tTitle = if ($track.PSObject.Properties['Title']) { $track.Title } else { 'Untitled' }
        $tDur = if ($track.PSObject.Properties['Duration']) { $track.Duration } else { '' }
        $items += [pscustomobject]@{
            RealIndex    = $i
            DisplayIndex = if ($isCurrent) { [char]::ConvertFromUtf32(0x25B6) } else { $idx.ToString() }
            Title        = $tTitle
            Subtitle     = $tSub
            Duration     = $tDur
            TextColor    = if ($isCurrent) { '#CEC1FF' } else { '#E6E1EB' }
        }
        $idx++
    }
    $ui['QueueList'].ItemsSource = $items
    if ($ui['QueueCountStatus']) {
        $count = $script:playback.queue.Count
        $cur = if ($script:playback.queueIndex -ge 0) { $script:playback.queueIndex + 1 } else { 0 }
        $ui['QueueCountStatus'].Text = "$cur / $count tracks"
    }
}

function Toggle-QueueView {
    if (-not $ui['QueuePanel']) { return }
    $ui['QueuePanel'].Visibility = if ($ui['QueuePanel'].Visibility -eq 'Visible') { 'Collapsed' } else { 'Visible' }
    if ($ui['QueuePanel'].Visibility -eq 'Visible') { Update-QueueUI }
}

# --- ACTIONS: PLAY, PAUSE, SEEK, VOLUME ---

function Play-AudioTrack($Track, [string]$Source = 'local', [bool]$KeepQueue = $false) {
    if (-not $Track) { return }
    $token = [System.Threading.Interlocked]::Increment([ref]$script:playbackToken)

    # Preview and local are mutually exclusive. Stop one immediately when starting other.
    if ($Source -eq 'local') {
        Stop-PreviewCore
    } else {
        Stop-LocalCore
    }

    $script:playback.source = $Source
    $script:playback.currentTrack = $Track
    $script:playerTrack = $Track  # Backwards compatibility
    $script:playbackSource = if ($Source -eq 'preview') { 'youtube' } else { 'local' }

    if ($ui.PlayerTitle) {
        $ui.PlayerTitle.Text = $Track.Title
        $ui.PlayerTitle.ToolTip = $Track.Title
    }
    if ($ui.PlayerSeek) { $ui.PlayerSeek.Value = 0 }
    if ($ui.PlayerElapsed) { $ui.PlayerElapsed.Text = '0:00' }
    if ($ui.PlayerDuration) { $ui.PlayerDuration.Text = '0:00' }
    $script:playback.position = 0
    $script:playerDesired = $true

    # Queue tracking
    if (-not $KeepQueue) {
        $script:playback.queue.Clear()
        if ($Source -eq 'local') {
            if ($ui.LibraryTracks -and $ui.LibraryTracks.ItemsSource) {
                foreach ($item in $ui.LibraryTracks.ItemsSource) { $script:playback.queue.Add($item) | Out-Null }
            }
        } else {
            if ($ui.SearchResults -and $ui.SearchResults.ItemsSource) {
                foreach ($item in $ui.SearchResults.ItemsSource) { if ($item) { $script:playback.queue.Add($item) | Out-Null } }
            }
        }
        # Find index
        $foundIdx = -1
        $trackPath = Get-TrackProp $Track 'Path'
        $trackId = Get-TrackProp $Track 'Id'
        for ($i = 0; $i -lt $script:playback.queue.Count; $i++) {
            $qItem = $script:playback.queue[$i]
            $qPath = Get-TrackProp $qItem 'Path'
            $qId = Get-TrackProp $qItem 'Id'
            if ($Source -eq 'local' -and $trackPath -and $qPath -eq $trackPath) { $foundIdx = $i; break }
            elseif ($Source -eq 'preview' -and $trackId -and $qId -eq $trackId) { $foundIdx = $i; break }
        }
        if ($foundIdx -lt 0) {
            $script:playback.queue.Add($Track) | Out-Null
            $foundIdx = $script:playback.queue.Count - 1
        }
        $script:playback.queueIndex = $foundIdx
        $script:playerQueue = @($script:playback.queue)
        $script:playerIndex = $foundIdx
        if ($script:playback.isShuffled) { Rebuild-ShuffleOrder }
    } else {
        # Check if caller assigned an explicit $script:playerQueue that differs from our queue items
        if ($script:playerQueue -and $script:playerQueue.Count -gt 0) {
            $isSameRef = ($script:playerQueue.Count -eq $script:playback.queue.Count)
            if ($isSameRef) {
                for ($k = 0; $k -lt $script:playerQueue.Count; $k++) {
                    if ($script:playerQueue[$k] -ne $script:playback.queue[$k]) { $isSameRef = $false; break }
                }
            }
            if (-not $isSameRef) {
                $script:playback.queue.Clear()
                foreach ($qItem in $script:playerQueue) { $script:playback.queue.Add($qItem) | Out-Null }
            }
        }
        # Keep queue, just update queueIndex
        $foundIdx = -1
        $trackPath = Get-TrackProp $Track 'Path'
        $trackId = Get-TrackProp $Track 'Id'
        for ($i = 0; $i -lt $script:playback.queue.Count; $i++) {
            $qItem = $script:playback.queue[$i]
            $qPath = Get-TrackProp $qItem 'Path'
            $qId = Get-TrackProp $qItem 'Id'
            if ($Source -eq 'local' -and $trackPath -and $qPath -eq $trackPath) { $foundIdx = $i; break }
            elseif ($Source -eq 'preview' -and $trackId -and $qId -eq $trackId) { $foundIdx = $i; break }
        }
        if ($foundIdx -ge 0) { $script:playback.queueIndex = $foundIdx; $script:playerIndex = $foundIdx }
    }

    if ($Source -eq 'local') {
        Start-LocalPlaybackCore $Track $token
    } else {
        Start-PreviewPlaybackCore $Track $token
    }
    Save-PlaybackSession
}

function Start-LocalPlaybackCore($Track, [int]$Token) {
    if (-not (Test-Path -LiteralPath $Track.Path -PathType Leaf)) {
        Handle-MissingLocalFile $Track
        return
    }
    Close-PlayerMedia
    $script:playerConverted = $false
    $ext = [IO.Path]::GetExtension($Track.Path).ToLowerInvariant()
    if ($ext -in @('.opus','.flac')) {
        Start-PlayerDecodeWithToken $Token
    } else {
        try {
            Open-PlayerMediaWithToken $Track.Path $Token
        } catch {
            Start-PlayerDecodeWithToken $Token
        }
    }
}

function Handle-MissingLocalFile($Track) {
    $msg = "File missing: '$($Track.Title)'. Check path or re-download."
    $script:playback.lastError = $msg
    Set-PlayerFailure $msg
    if ($ui.PlayerStatus) {
        $ui.PlayerStatus.Text = $msg
        $ui.PlayerStatus.ToolTip = $msg
    }
}

function Open-PlayerMediaWithToken([string]$Path, [int]$Token) {
    if ($script:media) { $script:media.Close() }
    $script:activeToken = $Token
    $script:media = New-Object Windows.Media.MediaPlayer
    $script:media.Volume = if ($script:playback.isMuted) { 0 } else { $script:playback.volume }
    $script:media.Add_MediaOpened({
        if ($script:activeToken -ne $script:playbackToken) { return }
        if (-not $script:media.NaturalDuration.HasTimeSpan) { Set-PlayerFailure 'This file has no readable audio duration.'; return }
        $dur = [math]::Max(0.1, $script:media.NaturalDuration.TimeSpan.TotalSeconds)
        $script:playback.duration = $dur
        $ui.PlayerSeek.Maximum = $dur
        $ui.PlayerDuration.Text = Format-TimeDisplay $dur
        if ($script:playerDesired) {
            $script:media.Play()
            Set-PlaybackUIState 'playing' 'Playing from your PC'
        } else {
            Set-PlaybackUIState 'paused' 'Paused'
        }
    })
    $script:media.Add_MediaFailed({
        if ($script:activeToken -ne $script:playbackToken) { return }
        if (-not $script:playerConverted) { Start-PlayerDecodeWithToken $script:activeToken }
        else { Set-PlayerFailure 'Windows could not play this audio. Check sound device and Windows Media features.' }
    })
    $script:media.Add_MediaEnded({
        if ($script:activeToken -ne $script:playbackToken) { return }
        On-TrackEnded
    })
    $script:playerOpeningAt = [DateTime]::UtcNow
    Set-PlaybackUIState 'opening' 'Opening audio…'
    $script:media.Open([uri]$Path)
}

function Start-PlayerDecodeWithToken([int]$Token) {
    try {
        if ($script:media) { $script:media.Close(); $script:media = $null }
        $script:playerConverted = $true
        $ffmpeg = Find-Tool 'ffmpeg'
        if (-not $ffmpeg) { throw 'FFmpeg is missing. Run Setup.cmd to enable playback of this format.' }
        New-Item -ItemType Directory -Force -Path $script:playerTempDir | Out-Null
        $script:playerTempFile = Join-Path $script:playerTempDir ([guid]::NewGuid().ToString('N') + '.wav')
        $arguments = @('-nostdin','-hide_banner','-loglevel','error','-i',$script:playback.currentTrack.Path,'-map','0:a:0','-vn','-ar','48000','-ac','2','-c:a','pcm_s16le',$script:playerTempFile)
        $info = New-Object Diagnostics.ProcessStartInfo
        $info.FileName = $ffmpeg
        $info.Arguments = ($arguments | ForEach-Object { Quote-Argument $_ }) -join ' '
        $info.UseShellExecute = $false
        $info.CreateNoWindow = $true
        $info.RedirectStandardError = $true
        $script:playerDecode = [Diagnostics.Process]::Start($info)
        $script:playerDecodeError = $script:playerDecode.StandardError.ReadToEndAsync()
        Set-PlaybackUIState 'preparing' 'Preparing audio for playback…'
    } catch { Set-PlayerFailure $_.Exception.Message }
}

function Start-PreviewPlaybackCore($Track, [int]$Token) {
    Stop-PreviewCore
    $script:activeToken = $Token
    $previewWatch.Start()
    $script:previewPosition = 0; $script:previewFailed = $false; $script:previewEndedHandled = $false; $script:previewBlocked = $false
    $script:previewToken = [guid]::NewGuid().ToString('N')
    $script:previewStartedAt = [DateTime]::UtcNow; $script:previewLastStatus = [DateTime]::UtcNow
    $script:previewProgressAt = [DateTime]::UtcNow

    if ($ui.PreviewTitle) { $ui.PreviewTitle.Text = $Track.Title; $ui.PreviewTitle.ToolTip = $Track.Title }
    if ($ui.PreviewStatus) { $ui.PreviewStatus.Text = 'Loading YouTube…' }
    if ($ui.PreviewLocal) { $ui.PreviewLocal.IsEnabled = ($null -ne (Find-LocalCopy $Track.Id)) }
    if ($ui.PreviewArtwork) { $ui.PreviewArtwork.Source = [uri]("https://i.ytimg.com/vi/" + $Track.Id + '/hqdefault.jpg') }
    if ($previewPane) { $previewPane.Visibility = 'Visible'; Set-PreviewMode $script:previewMode $false }
    Set-PlaybackUIState 'loading' 'Loading YouTube preview…'

    try {
        $sdk = Join-Path $PSScriptRoot 'tools\webview2'
        if (-not ('Microsoft.Web.WebView2.Wpf.WebView2' -as [type])) {
            [Reflection.Assembly]::LoadFrom((Join-Path $sdk 'Microsoft.Web.WebView2.Core.dll')) | Out-Null
            [Reflection.Assembly]::LoadFrom((Join-Path $sdk 'Microsoft.Web.WebView2.Wpf.dll')) | Out-Null
            [Microsoft.Web.WebView2.Core.CoreWebView2Environment]::SetLoaderDllFolderPath($sdk)
        }
        $script:previewBrowser = New-Object Microsoft.Web.WebView2.Wpf.WebView2
        $properties = New-Object Microsoft.Web.WebView2.Wpf.CoreWebView2CreationProperties
        $properties.AdditionalBrowserArguments = '--disable-background-timer-throttling --disable-renderer-backgrounding --disable-backgrounding-occluded-windows'
        # The app's SMTC owns media keys; do not let the embedded browser register a competing handler.
        if ((Get-Variable windowsMediaSession -Scope Script -ErrorAction SilentlyContinue) -and $script:windowsMediaSession) {
            $properties.AdditionalBrowserArguments += ' --disable-features=HardwareMediaKeyHandling'
        }
        $properties.UserDataFolder = if ($PreviewLiveTest) { Join-Path ([IO.Path]::GetTempPath()) ('PocketMusic-WebView-test-' + [guid]::NewGuid().ToString('N')) } else { Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'PocketMusic\WebView2' }
        $script:previewBrowser.CreationProperties = $properties
        $script:previewBrowser.Add_CoreWebView2InitializationCompleted({
            if ($script:activeToken -ne $script:playbackToken) { return }
            if (-not [object]::ReferenceEquals($this,$script:previewBrowser)) { return }
            if (-not $_.IsSuccess) { Set-PreviewError 'network'; if ($ui.PreviewStatus) { $ui.PreviewStatus.Text = 'The embedded browser could not start. Install or repair Microsoft Edge WebView2 Runtime.' }; return }
            try {
                $core = $script:previewBrowser.CoreWebView2
                $core.Settings.AreDevToolsEnabled = $false
                $core.Settings.AreDefaultContextMenusEnabled = $true
                $core.SetVirtualHostNameToFolderMapping('pocketmusic.desktop',(Join-Path $PSScriptRoot 'preview'),[Microsoft.Web.WebView2.Core.CoreWebView2HostResourceAccessKind]::DenyCors)
                $core.Add_WebMessageReceived({
                    if ($_.Source -notlike 'https://pocketmusic.desktop/index.html*') { return }
                    try { Receive-PreviewMessage ($_.WebMessageAsJson | ConvertFrom-Json) } catch { Add-Log ('Preview message error: ' + $_.Exception.Message) }
                })
                $core.Add_NavigationStarting({ if ($_.Uri -notlike 'https://pocketmusic.desktop/index.html*' -and $_.Uri -ne 'about:blank') { $_.Cancel = $true } })
                $core.Add_NewWindowRequested({
                    $_.Handled = $true
                    $target = $null
                    if ($_.IsUserInitiated -and [uri]::TryCreate($_.Uri,[UriKind]::Absolute,[ref]$target) -and $target.Scheme -eq 'https' -and $target.Host -in @('www.youtube.com','youtube.com','accounts.google.com')) { Start-Process $target.AbsoluteUri }
                })
                $vol = if ($script:playback.isMuted) { 0 } else { [int]($script:playback.volume * 100) }
                $uri = 'https://pocketmusic.desktop/index.html?id=' + $script:playback.currentTrack.Id + '&token=' + $script:previewToken + '&volume=' + $vol
                $core.Navigate($uri)
            } catch { Set-PreviewError 'network'; Add-Log ('Preview initialization error: ' + $_.Exception.Message) }
        })
        $ui.PreviewHost.Child = $script:previewBrowser
        $script:previewBrowser.EnsureCoreWebView2Async() | Out-Null
    } catch {
        Set-PlaybackUIState 'error' 'YouTube preview requires Microsoft Edge WebView2. Check activity details.'
        if ($ui.PreviewStatus) { $ui.PreviewStatus.Text = $ui.PlayerStatus.Text }
        Add-Log $_.Exception.Message
        $script:previewFailed = $true
    }
}

function Toggle-PlayPause {
    if ($script:playback.source -eq 'preview') {
        if ($script:playerState -in @('playing','buffering')) {
            Send-PreviewCommand 'pause'
            Set-PlaybackUIState 'paused' 'YouTube preview paused'
        } else {
            $script:previewEndedHandled = $false
            Send-PreviewCommand 'play'
            Set-PlaybackUIState 'playing' 'Streaming from YouTube'
        }
        return
    }

    if (-not $script:media) {
        if ($script:playback.currentTrack) {
            Play-AudioTrack $script:playback.currentTrack 'local' $true
        }
        return
    }

    if ($script:playerState -eq 'playing') {
        $script:media.Pause()
        $script:playerDesired = $false
        Set-PlaybackUIState 'paused' 'Paused'
    } elseif ($script:playerState -in @('paused','ended')) {
        if ($script:playerState -eq 'ended') { $script:media.Position = [TimeSpan]::Zero }
        $script:playerDesired = $true
        $script:media.Play()
        Set-PlaybackUIState 'playing' 'Playing from your PC'
    }
}

function Seek-Playback([double]$Seconds) {
    if ($Seconds -lt 0) { $Seconds = 0 }
    if ($script:playback.source -eq 'preview') {
        Send-PreviewCommand 'seek' $Seconds
        $script:playback.position = $Seconds
        $ui.PlayerElapsed.Text = Format-TimeDisplay $Seconds
        return
    }
    if ($script:media -and $script:playerState -in @('playing','paused','ended')) {
        $max = $ui.PlayerSeek.Maximum
        if ($max -le 0) { $max = $Seconds }
        $target = [math]::Min($Seconds, $max)
        $script:media.Position = [TimeSpan]::FromSeconds($target)
        $script:playback.position = $target
        $ui.PlayerElapsed.Text = Format-TimeDisplay $target
        if ($script:playerState -eq 'ended') { Set-PlaybackUIState 'paused' 'Paused' }
    }
}

function Set-PlaybackVolume([double]$Value) {
    $val = [math]::Max(0.0, [math]::Min(1.0, $Value))
    $script:playback.volume = $val
    $script:playback.isMuted = ($val -eq 0)

    if ($script:playback.source -eq 'preview' -and -not $script:previewVolumeSync) {
        Send-PreviewCommand 'volume' (if ($script:playback.isMuted) { 0 } else { $val * 100 })
    }
    if ($script:media) {
        $script:media.Volume = if ($script:playback.isMuted) { 0 } else { $val }
    }
    if ($ui.PlayerVolumeLabel) { $ui.PlayerVolumeLabel.Text = ([int]($val * 100)).ToString() + '%' }
    if ($ui.PlayerMute) { $ui.PlayerMute.Content = if ($script:playback.isMuted) { '🔇' } else { '🔊' } }
    Save-PlaybackSession
}

function Toggle-PlaybackMute {
    if ($script:playback.isMuted) {
        $script:playback.isMuted = $false
        $vol = if ($script:playback.prevVolume -gt 0) { $script:playback.prevVolume } else { 0.6 }
        $script:playback.volume = $vol
        if ($ui.PlayerVolume) { $ui.PlayerVolume.Value = $vol }
    } else {
        $script:playback.prevVolume = $script:playback.volume
        $script:playback.isMuted = $true
        if ($ui.PlayerVolume) { $ui.PlayerVolume.Value = 0 }
    }
    Set-PlaybackVolume $ui.PlayerVolume.Value
}

function Toggle-Shuffle {
    $script:playback.isShuffled = -not $script:playback.isShuffled
    if ($script:playback.isShuffled) {
        Rebuild-ShuffleOrder
    }
    Set-PlaybackUIState $script:playerState $ui.PlayerStatus.Text
    Save-PlaybackSession
}

function Cycle-RepeatMode {
    switch ($script:playback.repeatMode) {
        'off' { $script:playback.repeatMode = 'all' }
        'all' { $script:playback.repeatMode = 'one' }
        'one' { $script:playback.repeatMode = 'off' }
    }
    Set-PlaybackUIState $script:playerState $ui.PlayerStatus.Text
    Save-PlaybackSession
}

# --- ADVANCE & PREVIOUS LOGIC ---

function On-TrackEnded {
    if ($script:playback.sleepTimerType -eq 'track') {
        $script:playback.sleepTimerType = 'off'
        Set-PlaybackUIState 'paused' 'Sleep timer stopped playback'
        return
    }

    if ($script:playback.repeatMode -eq 'one') {
        if ($script:playback.source -eq 'local') {
            Seek-Playback 0
            if ($script:media) { $script:media.Play() }
            Set-PlaybackUIState 'playing' 'Repeating current track'
        } else {
            Seek-Playback 0
            Send-PreviewCommand 'play'
            Set-PlaybackUIState 'playing' 'Repeating current track'
        }
        return
    }

    # For preview: when a preview ends, stop cleanly unless repeat is 'all'
    if ($script:playback.source -eq 'preview' -and $script:playback.repeatMode -ne 'all') {
        Set-PlaybackUIState 'ended' 'End of preview'
        return
    }

    Step-PlaybackTrack 1 $true
}

function Step-PlaybackTrack([int]$Direction, [bool]$IsAutoAdvance = $false) {
    $qCount = $script:playback.queue.Count
    if ($qCount -eq 0) { return }

    # Previous track threshold rule: if position > 3 seconds, restart current track
    if ($Direction -lt 0) {
        $pos = 0.0
        if ($script:playback.source -eq 'preview') { $pos = $script:previewPosition }
        elseif ($script:media) { $pos = $script:media.Position.TotalSeconds }
        if ($pos -gt 3.0) {
            Seek-Playback 0
            return
        }
    }

    $nextQueueIndex = -1
    if ($script:playback.isShuffled -and $script:playback.shuffledOrder.Count -gt 0) {
        $orderIdx = $script:playback.shuffledOrder.IndexOf($script:playback.queueIndex)
        if ($orderIdx -lt 0) { $orderIdx = 0 }
        $targetOrderIdx = $orderIdx + $Direction
        if ($targetOrderIdx -lt 0) {
            if ($script:playback.repeatMode -eq 'all') { $targetOrderIdx = $script:playback.shuffledOrder.Count - 1 }
            else { Seek-Playback 0; return }
        } elseif ($targetOrderIdx -ge $script:playback.shuffledOrder.Count) {
            if ($script:playback.repeatMode -eq 'all') { $targetOrderIdx = 0 }
            else {
                Set-PlaybackUIState 'ended' 'End of queue'
                return
            }
        }
        $nextQueueIndex = $script:playback.shuffledOrder[$targetOrderIdx]
    } else {
        # Linear queue order
        $target = $script:playback.queueIndex + $Direction
        if ($target -lt 0) {
            if ($script:playback.repeatMode -eq 'all') { $target = $qCount - 1 }
            else { Seek-Playback 0; return }
        } elseif ($target -ge $qCount) {
            if ($script:playback.repeatMode -eq 'all') { $target = 0 }
            else {
                Set-PlaybackUIState 'ended' 'End of queue'
                return
            }
        }
        $nextQueueIndex = $target
    }

    if ($nextQueueIndex -ge 0 -and $nextQueueIndex -lt $qCount) {
        $script:playback.queueIndex = $nextQueueIndex
        $script:playerIndex = $nextQueueIndex
        $nextTrack = $script:playback.queue[$nextQueueIndex]
        $source = if (Get-TrackProp $nextTrack 'Path') { 'local' } else { 'preview' }
        Play-AudioTrack $nextTrack $source $true
    }
}

# --- QUEUE MANAGEMENT ---
function Enqueue-Track($Track) {
    if (-not $Track) { return }
    $script:playback.queue.Add($Track) | Out-Null
    $script:playerQueue = @($script:playback.queue)
    if ($script:playback.isShuffled) { Rebuild-ShuffleOrder }
    Update-QueueUI
    Set-PlaybackUIState $script:playerState $ui.PlayerStatus.Text
    if ($ui.Status) { $ui.Status.Text = "Added '$($Track.Title)' to queue" }
}

function Play-NextInQueue($Track) {
    if (-not $Track) { return }
    $insertAt = if ($script:playback.queueIndex -ge 0) { $script:playback.queueIndex + 1 } else { 0 }
    if ($insertAt -gt $script:playback.queue.Count) { $insertAt = $script:playback.queue.Count }
    $script:playback.queue.Insert($insertAt, $Track)
    $script:playerQueue = @($script:playback.queue)
    if ($script:playback.isShuffled) { Rebuild-ShuffleOrder }
    Update-QueueUI
    Set-PlaybackUIState $script:playerState $ui.PlayerStatus.Text
    if ($ui.Status) { $ui.Status.Text = "Will play '$($Track.Title)' next" }
}

function Remove-FromQueue([int]$Index) {
    if ($Index -lt 0 -or $Index -ge $script:playback.queue.Count) { return }
    $isCurrent = ($Index -eq $script:playback.queueIndex)
    $script:playback.queue.RemoveAt($Index)
    $script:playerQueue = @($script:playback.queue)
    if ($isCurrent) {
        if ($script:playback.queue.Count -eq 0) {
            Clear-Queue
            return
        }
        if ($script:playback.queueIndex -ge $script:playback.queue.Count) {
            $script:playback.queueIndex = $script:playback.queue.Count - 1
        }
        $next = $script:playback.queue[$script:playback.queueIndex]
        $nextSrc = if (Get-TrackProp $next 'Path') { 'local' } else { 'preview' }
        Play-AudioTrack $next $nextSrc $true
    } elseif ($Index -lt $script:playback.queueIndex) {
        $script:playback.queueIndex--
    }
    $script:playerQueue = @($script:playback.queue)
    $script:playerIndex = $script:playback.queueIndex
    if ($script:playback.isShuffled) { Rebuild-ShuffleOrder }
    Update-QueueUI
    Set-PlaybackUIState $script:playerState $ui.PlayerStatus.Text
}

function Clear-Queue {
    $script:playback.queue.Clear()
    $script:playback.shuffledOrder.Clear()
    $script:playback.queueIndex = -1
    $script:playerQueue = @()
    $script:playerIndex = -1
    Stop-AllCores
    $script:playback.currentTrack = $null
    $script:playerTrack = $null
    $script:playback.source = 'none'
    if ($ui.PlayerTitle) { $ui.PlayerTitle.Text = 'Pocket Music' }
    if ($ui.PlayerElapsed) { $ui.PlayerElapsed.Text = '0:00' }
    if ($ui.PlayerDuration) { $ui.PlayerDuration.Text = '0:00' }
    if ($ui.PlayerSeek) { $ui.PlayerSeek.Value = 0 }
    Set-PlaybackUIState 'idle' 'Queue cleared'
    Update-QueueUI
}

function Move-QueueTrack([int]$Index, [int]$Direction) {
    $target = $Index + $Direction
    if ($Index -lt 0 -or $Index -ge $script:playback.queue.Count -or $target -lt 0 -or $target -ge $script:playback.queue.Count) { return }
    $item = $script:playback.queue[$Index]
    $script:playback.queue.RemoveAt($Index)
    $script:playback.queue.Insert($target, $item)
    if ($script:playback.queueIndex -eq $Index) {
        $script:playback.queueIndex = $target
    } elseif ($script:playback.queueIndex -eq $target) {
        $script:playback.queueIndex = $Index
    }
    $script:playerQueue = @($script:playback.queue)
    $script:playerIndex = $script:playback.queueIndex
    Update-QueueUI
}

# --- SLEEP TIMER ---
function Cycle-SleepTimer {
    switch ($script:playback.sleepTimerType) {
        'off'   { $script:playback.sleepTimerType = '15'; $script:playback.sleepTimerEnd = [DateTime]::UtcNow.AddMinutes(15) }
        '15'    { $script:playback.sleepTimerType = '30'; $script:playback.sleepTimerEnd = [DateTime]::UtcNow.AddMinutes(30) }
        '30'    { $script:playback.sleepTimerType = '60'; $script:playback.sleepTimerEnd = [DateTime]::UtcNow.AddMinutes(60) }
        '60'    { $script:playback.sleepTimerType = 'track'; $script:playback.sleepTimerEnd = 'end_of_track' }
        'track' { $script:playback.sleepTimerType = 'off'; $script:playback.sleepTimerEnd = $null }
    }
    $msg = if ($script:playback.sleepTimerType -eq 'off') { 'Sleep timer: Off' } elseif ($script:playback.sleepTimerType -eq 'track') { 'Sleep timer: Stop after current track' } else { "Sleep timer: $($script:playback.sleepTimerType) minutes" }
    if ($ui.Status) { $ui.Status.Text = $msg }
    Set-PlaybackUIState $script:playerState $ui.PlayerStatus.Text
}

# Global keyboard listener
function Handle-PlaybackGlobalKey($KeyEventArgs) {
    if ($KeyEventArgs.Handled) { return }
    if ($KeyEventArgs.Key -like 'Media*' -and (Get-Variable windowsMediaSession -Scope Script -ErrorAction SilentlyContinue) -and $script:windowsMediaSession) { $KeyEventArgs.Handled = $true; return }
    $focused = [Windows.Input.FocusManager]::GetFocusedElement($window)
    if ($KeyEventArgs.Key -notlike 'Media*') {
        $control = $focused
        while ($control) {
            if ($control -is [Windows.Controls.TextBox] -or $control -is [Windows.Controls.ListBox] -or $control -is [Windows.Controls.ComboBox] -or $control -is [Windows.Controls.Slider] -or $control -is [Windows.Controls.Primitives.ButtonBase]) { return }
            if ($control -isnot [Windows.Media.Visual]) { break }
            $control = [Windows.Media.VisualTreeHelper]::GetParent($control)
        }
    }

    switch ($KeyEventArgs.Key) {
        'Space' { $KeyEventArgs.Handled = $true; Toggle-PlayPause }
        'Left'  { $KeyEventArgs.Handled = $true; Seek-Playback ($script:playback.position - 5) }
        'Right' { $KeyEventArgs.Handled = $true; Seek-Playback ($script:playback.position + 5) }
        'Up'    { $KeyEventArgs.Handled = $true; Set-PlaybackVolume ($script:playback.volume + 0.05); if ($ui.PlayerVolume) { $ui.PlayerVolume.Value = $script:playback.volume } }
        'Down'  { $KeyEventArgs.Handled = $true; Set-PlaybackVolume ($script:playback.volume - 0.05); if ($ui.PlayerVolume) { $ui.PlayerVolume.Value = $script:playback.volume } }
        'MediaPlayPause' { $KeyEventArgs.Handled = $true; Toggle-PlayPause }
        'MediaNextTrack' { $KeyEventArgs.Handled = $true; Step-PlaybackTrack 1 }
        'MediaPreviousTrack' { $KeyEventArgs.Handled = $true; Step-PlaybackTrack -1 }
        'MediaStop'      { $KeyEventArgs.Handled = $true; Toggle-PlayPause }
    }
}
