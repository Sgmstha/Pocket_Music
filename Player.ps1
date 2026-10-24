# GPL-3.0. Native in-app playback; FFmpeg supplies PCM compatibility for Opus/FLAC.
$playerView = [Windows.Markup.XamlReader]::Parse([IO.File]::ReadAllText((Join-Path $PSScriptRoot 'Player.xaml')))
$mainView = $window.Content
$window.Content = $null
$shell = New-Object Windows.Controls.Grid
$mainRow = New-Object Windows.Controls.RowDefinition
$bottomRow = New-Object Windows.Controls.RowDefinition
$bottomRow.Height = New-Object Windows.GridLength 108
$shell.RowDefinitions.Add($mainRow); $shell.RowDefinitions.Add($bottomRow)
$shell.Children.Add($mainView) | Out-Null
[Windows.Controls.Grid]::SetRow($playerView, 1)
$shell.Children.Add($playerView) | Out-Null
$window.Content = $shell
foreach ($name in @('PlayerTitle','PlayerStatus','PlayerPrevious','PlayerToggle','PlayerNext','PlayerElapsed','PlayerSeek','PlayerDuration','PlayerVolume','PlayerVolumeLabel')) { $ui[$name] = $playerView.FindName($name) }
$script:media = $null
$script:playerState = 'idle'
$script:playerTrack = $null
$script:playerQueue = @()
$script:playerIndex = -1
$script:playerDecode = $null
$script:playerDecodeError = $null
$script:playerTempFile = $null
$script:playerTempDir = Join-Path ([IO.Path]::GetTempPath()) ('PocketMusic-player-' + [guid]::NewGuid().ToString('N'))
$script:playerConverted = $false
$script:playerDesired = $true
$script:playerOpeningAt = [DateTime]::MinValue
$script:playerSeeking = $false
$script:libraryRefreshing = $false

function Format-PlayerTime([double]$Seconds) {
    $time = [TimeSpan]::FromSeconds([math]::Max(0, $Seconds))
    if ($time.TotalHours -ge 1) { return $time.ToString('h\:mm\:ss') }
    return $time.ToString('m\:ss')
}
function Set-PlayerState([string]$State, [string]$Message) {
    $script:playerState = $State
    $ui.PlayerStatus.Text = $Message
    $ui.PlayerStatus.ToolTip = $Message
    $ui.PlayerToggle.Content = if ($State -eq 'playing') { '❚❚' } else { '▶' }
    $ui.PlayerToggle.IsEnabled = $State -in @('playing','paused','ended')
    $ui.PlayerSeek.IsEnabled = $State -in @('playing','paused','ended')
    $ui.PlayerPrevious.IsEnabled = $null -ne $script:playerTrack
    $ui.PlayerNext.IsEnabled = $script:playerIndex -ge 0 -and $script:playerIndex -lt ($script:playerQueue.Count - 1)
    Update-PlayerTimer
}
function Update-PlayerTimer {
    $active = $script:playbackSource -eq 'local' -and ($script:playerState -in @('opening','preparing') -or ($script:playerState -eq 'playing' -and $window.WindowState -ne 'Minimized'))
    if ($active) { $playerTimer.Start() } else { $playerTimer.Stop() }
}
function Close-PlayerMedia {
    if ($script:media) { $script:media.Close(); $script:media = $null }
    if ($script:playerDecode) {
        if (-not $script:playerDecode.HasExited) { $script:playerDecode.Kill(); $script:playerDecode.WaitForExit(3000) | Out-Null }
        $script:playerDecode.Dispose(); $script:playerDecode = $null
    }
    if ($script:playerTempFile -and (Test-Path -LiteralPath $script:playerTempFile)) {
        Remove-Item -LiteralPath $script:playerTempFile -Force -ErrorAction SilentlyContinue
    }
    $script:playerTempFile = $null
}
function Set-PlayerFailure([string]$Message) {
    Close-PlayerMedia
    Set-PlayerState 'error' $Message
}
function Open-PlayerMedia([string]$Path) {
    if ($script:media) { $script:media.Close() }
    $script:media = New-Object Windows.Media.MediaPlayer
    $script:media.Volume = $ui.PlayerVolume.Value
    $script:media.Add_MediaOpened({
        if (-not [object]::ReferenceEquals($this, $script:media)) { return }
        if (-not $script:media.NaturalDuration.HasTimeSpan) { Set-PlayerFailure 'This file has no readable audio duration.'; return }
        $ui.PlayerSeek.Maximum = [math]::Max(0.1, $script:media.NaturalDuration.TimeSpan.TotalSeconds)
        $ui.PlayerDuration.Text = Format-PlayerTime $ui.PlayerSeek.Maximum
        if ($script:playerDesired) { $script:media.Play(); Set-PlayerState 'playing' 'Playing from your PC' }
        else { Set-PlayerState 'paused' 'Paused' }
    })
    $script:media.Add_MediaFailed({
        if (-not [object]::ReferenceEquals($this, $script:media)) { return }
        if (-not $script:playerConverted) { Start-PlayerDecode }
        else { Set-PlayerFailure 'Windows could not play this audio. Check your sound device and Windows Media features.' }
    })
    $script:media.Add_MediaEnded({
        if (-not [object]::ReferenceEquals($this, $script:media)) { return }
        if ($script:playerIndex -lt ($script:playerQueue.Count - 1)) { Step-Player 1 }
        else { Set-PlayerState 'ended' 'End of library queue'; $ui.PlayerSeek.Value = $ui.PlayerSeek.Maximum }
    })
    $script:playerOpeningAt = [DateTime]::UtcNow
    Set-PlayerState 'opening' 'Opening audio…'
    $script:media.Open([uri]$Path)
}
function Start-PlayerDecode {
    try {
        if ($script:media) { $script:media.Close(); $script:media = $null }
        $script:playerConverted = $true
        $ffmpeg = Find-Tool 'ffmpeg'
        if (-not $ffmpeg) { throw 'FFmpeg is missing. Run Setup.cmd to enable playback of this format.' }
        New-Item -ItemType Directory -Force -Path $script:playerTempDir | Out-Null
        $script:playerTempFile = Join-Path $script:playerTempDir ([guid]::NewGuid().ToString('N') + '.wav')
        $arguments = @('-nostdin','-hide_banner','-loglevel','error','-i',$script:playerTrack.Path,'-map','0:a:0','-vn','-ar','48000','-ac','2','-c:a','pcm_s16le',$script:playerTempFile)
        $info = New-Object Diagnostics.ProcessStartInfo
        $info.FileName = $ffmpeg; $info.Arguments = ($arguments | ForEach-Object { Quote-Argument $_ }) -join ' '
        $info.UseShellExecute = $false; $info.CreateNoWindow = $true; $info.RedirectStandardError = $true
        $script:playerDecode = [Diagnostics.Process]::Start($info)
        $script:playerDecodeError = $script:playerDecode.StandardError.ReadToEndAsync()
        Set-PlayerState 'preparing' 'Preparing audio for playback…'
    } catch { Set-PlayerFailure $_.Exception.Message }
}
function Start-PlayerTrack($Track, [bool]$KeepQueue = $false) {
    if (-not $Track) { return }
    if ($script:playbackSource -eq 'youtube') { Stop-Preview }
    $script:playbackSource = 'local'
    Close-PlayerMedia
    $script:playerTrack = $Track
    $ui.PlayerTitle.Text = $Track.Title; $ui.PlayerTitle.ToolTip = $Track.Title
    $script:playerConverted = $false; $script:playerDesired = $true
    $ui.PlayerSeek.Value = 0; $ui.PlayerElapsed.Text = '0:00'; $ui.PlayerDuration.Text = '0:00'
    if (-not $KeepQueue) {
        $script:playerQueue = @($ui.LibraryTracks.ItemsSource)
        $script:playerIndex = -1
        for ($i = 0; $i -lt $script:playerQueue.Count; $i++) { if ($script:playerQueue[$i].Path -eq $Track.Path) { $script:playerIndex = $i; break } }
        if ($script:playerIndex -lt 0) { $script:playerQueue = @($Track); $script:playerIndex = 0 }
    }
    if (-not (Test-Path -LiteralPath $Track.Path -PathType Leaf)) { Set-PlayerFailure 'This file is missing. Refresh the library or choose another track.'; return }
    if ([IO.Path]::GetExtension($Track.Path).ToLowerInvariant() -in @('.opus','.flac')) { Start-PlayerDecode }
    else { try { Open-PlayerMedia $Track.Path } catch { Start-PlayerDecode } }
}
function Toggle-Player {
    if ($script:playbackSource -eq 'youtube') {
        if ($script:playerState -in @('playing','buffering')) { Send-PreviewCommand 'pause' }
        else { $script:previewEndedHandled = $false; Send-PreviewCommand 'play' }
        return
    }
    if (-not $script:media) { return }
    if ($script:playerState -eq 'playing') { $script:media.Pause(); $script:playerDesired = $false; Set-PlayerState 'paused' 'Paused' }
    elseif ($script:playerState -in @('paused','ended')) {
        if ($script:playerState -eq 'ended') { $script:media.Position = [TimeSpan]::Zero }
        $script:playerDesired = $true; $script:media.Play(); Set-PlayerState 'playing' 'Playing from your PC'
    }
}
function Seek-Player([double]$Seconds) {
    if ($script:playbackSource -eq 'youtube') { Send-PreviewCommand 'seek' $Seconds; return }
    if ($script:media -and $script:playerState -in @('playing','paused','ended')) {
        $script:media.Position = [TimeSpan]::FromSeconds([math]::Max(0, [math]::Min($Seconds, $ui.PlayerSeek.Maximum)))
        $ui.PlayerElapsed.Text = Format-PlayerTime $Seconds
        if ($script:playerState -eq 'ended') { Set-PlayerState 'paused' 'Paused' }
    }
}
function Step-Player([int]$Direction) {
    if ($script:playbackSource -eq 'youtube') {
        if ($Direction -lt 0 -and $script:previewPosition -gt 3) { Send-PreviewCommand 'seek' 0; return }
        $nextPreview = $script:playerIndex + $Direction
        if ($nextPreview -lt 0) { Send-PreviewCommand 'seek' 0; return }
        if ($nextPreview -ge $script:playerQueue.Count) { return }
        $script:playerIndex = $nextPreview
        Start-PreviewTrack $script:playerQueue[$nextPreview] $true
        return
    }
    if ($Direction -lt 0 -and $script:media -and $script:media.Position.TotalSeconds -gt 3) { Seek-Player 0; return }
    $next = $script:playerIndex + $Direction
    if ($next -lt 0) { if ($script:media) { Seek-Player 0 }; return }
    if ($next -ge $script:playerQueue.Count) { return }
    $script:playerIndex = $next
    Start-PlayerTrack $script:playerQueue[$next] $true
}
$ui.PlayerToggle.Add_Click({ Toggle-Player })
$ui.PlayerPrevious.Add_Click({ Step-Player -1 })
$ui.PlayerNext.Add_Click({ Step-Player 1 })
$ui.PlayerVolume.Add_ValueChanged({
    if ($script:playbackSource -eq 'youtube' -and -not $script:previewVolumeSync) { Send-PreviewCommand 'volume' ($ui.PlayerVolume.Value * 100) }
    if ($script:media) { $script:media.Volume = $ui.PlayerVolume.Value }
    $ui.PlayerVolumeLabel.Text = ([int]($ui.PlayerVolume.Value * 100)).ToString() + '%'
})
$ui.PlayerSeek.Add_PreviewMouseLeftButtonDown({ $script:playerSeeking = $true })
$ui.PlayerSeek.Add_PreviewMouseLeftButtonUp({ Seek-Player $ui.PlayerSeek.Value; $script:playerSeeking = $false })
$ui.PlayerSeek.Add_LostMouseCapture({ if ($script:playerSeeking) { Seek-Player $ui.PlayerSeek.Value; $script:playerSeeking = $false } })
$ui.PlayerSeek.Add_PreviewKeyDown({ $script:playerSeeking = $true })
$ui.PlayerSeek.Add_KeyUp({ Seek-Player $ui.PlayerSeek.Value; $script:playerSeeking = $false })
$playerTimer = New-Object Windows.Threading.DispatcherTimer
$playerTimer.Interval = [TimeSpan]::FromMilliseconds(500)
$playerTimer.Add_Tick({
    if ($script:playerDecode -and $script:playerDecode.HasExited -and $script:playerDecodeError.IsCompleted) {
        $exitCode = $script:playerDecode.ExitCode
        $errorText = $script:playerDecodeError.GetAwaiter().GetResult()
        $script:playerDecode.Dispose(); $script:playerDecode = $null
        if ($exitCode -ne 0) { Add-Log $errorText; Set-PlayerFailure 'This audio could not be prepared. Check the file and available disk space.' }
        else { try { Open-PlayerMedia $script:playerTempFile } catch { Set-PlayerFailure 'The prepared audio could not be opened.' } }
    }
    if ($script:playerState -eq 'opening' -and ([DateTime]::UtcNow - $script:playerOpeningAt).TotalSeconds -gt 20) { Set-PlayerFailure 'Audio did not open. Check Windows Media features and your sound device.' }
    if ($script:media -and $script:playerState -in @('playing','paused') -and -not $script:playerSeeking) {
        $ui.PlayerSeek.Value = $script:media.Position.TotalSeconds
        $ui.PlayerElapsed.Text = Format-PlayerTime $script:media.Position.TotalSeconds
    }
})
$window.Add_Closing({
    if ($_.Cancel) { return }
    $playerTimer.Stop(); Close-PlayerMedia
    # Delete only files in the unique session directory created above; no recursive deletion.
    if ((Test-Path -LiteralPath $script:playerTempDir) -and ([IO.Path]::GetDirectoryName($script:playerTempDir) -eq [IO.Path]::GetTempPath().TrimEnd('\'))) {
        Get-ChildItem -LiteralPath $script:playerTempDir -File | Remove-Item -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $script:playerTempDir -ErrorAction SilentlyContinue
    }
})
$window.Add_StateChanged({ Update-PlayerTimer })
