# GPL-3.0. Native in-app playback; FFmpeg supplies PCM compatibility for Opus/FLAC.
. "$PSScriptRoot\PlaybackService.ps1"

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

foreach ($name in @('PlayerTitle','PlayerStatus','PlayerPrevious','PlayerToggle','PlayerNext','PlayerElapsed','PlayerSeek','PlayerDuration','PlayerVolume','PlayerVolumeLabel','PlayerShuffle','PlayerRepeat','PlayerMute','PlayerSleepBtn','PlayerQueueBtn','PlayerSourceBadge','PlayerSourceText','PlayerArtwork','PlayerArtBorder')) {
    $ui[$name] = $playerView.FindName($name)
}

$script:media = $null
$script:playerState = 'idle'
$script:playerTrack = $null
$script:playerQueue = @()
$script:playerIndex = -1
$script:libraryRefreshing = $false

function Format-PlayerTime([double]$Seconds) {
    return Format-TimeDisplay $Seconds
}

function Set-PlayerState([string]$State, [string]$Message) {
    Set-PlaybackUIState $State $Message
}

function Update-PlayerTimer {
    $active = $script:playback.source -eq 'local' -and ($script:playerState -in @('opening','preparing') -or ($script:playerState -eq 'playing' -and $window.WindowState -ne 'Minimized'))
    if ($active) { $playerTimer.Start() } else { $playerTimer.Stop() }
}

function Close-PlayerMedia {
    Stop-LocalCore
}

function Set-PlayerFailure([string]$Message) {
    Close-PlayerMedia
    $script:playback.lastError = $Message
    Set-PlaybackUIState 'error' $Message
}

function Open-PlayerMedia([string]$Path) {
    Open-PlayerMediaWithToken $Path $script:playbackToken
}

function Start-PlayerDecode {
    Start-PlayerDecodeWithToken $script:playbackToken
}

function Start-PlayerTrack($Track, [bool]$KeepQueue = $false) {
    Play-AudioTrack $Track 'local' $KeepQueue
}

function Toggle-Player {
    Toggle-PlayPause
}

function Seek-Player([double]$Seconds) {
    Seek-Playback $Seconds
}

function Step-Player([int]$Direction) {
    Step-PlaybackTrack $Direction $false
}

# Attach bottom bar events
$ui.PlayerToggle.Add_Click({ Toggle-PlayPause })
$ui.PlayerPrevious.Add_Click({ Step-PlaybackTrack -1 })
$ui.PlayerNext.Add_Click({ Step-PlaybackTrack 1 })
if ($ui.PlayerShuffle) { $ui.PlayerShuffle.Add_Click({ Toggle-Shuffle }) }
if ($ui.PlayerRepeat) { $ui.PlayerRepeat.Add_Click({ Cycle-RepeatMode }) }
if ($ui.PlayerMute) { $ui.PlayerMute.Add_Click({ Toggle-PlaybackMute }) }
if ($ui.PlayerSleepBtn) { $ui.PlayerSleepBtn.Add_Click({ Cycle-SleepTimer }) }
if ($ui.PlayerQueueBtn) { $ui.PlayerQueueBtn.Add_Click({ Toggle-QueueView }) }

$ui.PlayerVolume.Add_ValueChanged({
    Set-PlaybackVolume $ui.PlayerVolume.Value
})

$ui.PlayerSeek.Add_PreviewMouseLeftButtonDown({ $script:playerSeeking = $true })
$ui.PlayerSeek.Add_PreviewMouseLeftButtonUp({ Seek-Playback $ui.PlayerSeek.Value; $script:playerSeeking = $false })
$ui.PlayerSeek.Add_LostMouseCapture({ if ($script:playerSeeking) { Seek-Playback $ui.PlayerSeek.Value; $script:playerSeeking = $false } })
$ui.PlayerSeek.Add_PreviewKeyDown({ $script:playerSeeking = $true })
$ui.PlayerSeek.Add_KeyUp({ Seek-Playback $ui.PlayerSeek.Value; $script:playerSeeking = $false })

$playerTimer = New-Object Windows.Threading.DispatcherTimer
$playerTimer.Interval = [TimeSpan]::FromMilliseconds(250)
$playerTimer.Add_Tick({
    # Check decode process if active
    if ($script:playerDecode -and $script:playerDecode.HasExited -and $script:playerDecodeError.IsCompleted) {
        $exitCode = $script:playerDecode.ExitCode
        $errorText = $script:playerDecodeError.GetAwaiter().GetResult()
        $script:playerDecode.Dispose(); $script:playerDecode = $null
        if ($exitCode -ne 0) {
            Add-Log $errorText
            Set-PlayerFailure 'This audio could not be prepared. Check the file and available disk space.'
        } else {
            try { Open-PlayerMediaWithToken $script:playerTempFile $script:playbackToken } catch { Set-PlayerFailure 'The prepared audio could not be opened.' }
        }
    }
    if ($script:playerState -eq 'opening' -and ([DateTime]::UtcNow - $script:playerOpeningAt).TotalSeconds -gt 20) {
        Set-PlayerFailure 'Audio did not open. Check Windows Media features and your sound device.'
    }
    if ($script:media -and $script:playerState -in @('playing','paused') -and -not $script:playerSeeking) {
        $curSec = $script:media.Position.TotalSeconds
        $script:playback.position = $curSec
        $ui.PlayerSeek.Value = $curSec
        $ui.PlayerElapsed.Text = Format-TimeDisplay $curSec
    }
    # Check sleep timer by clock
    if ($script:playback.sleepTimerEnd -is [DateTime] -and [DateTime]::UtcNow -ge $script:playback.sleepTimerEnd) {
        $script:playback.sleepTimerType = 'off'
        $script:playback.sleepTimerEnd = $null
        if ($script:playback.isPlaying) {
            Toggle-PlayPause
            Set-PlaybackUIState 'paused' 'Sleep timer reached. Playback paused.'
        }
    }
})

$window.Add_Closing({
    if ($_.Cancel) { return }
    $playerTimer.Stop()
    Save-PlaybackSession
    Stop-AllCores
    # Delete only files in the unique session directory created above; no recursive deletion.
    if ((Test-Path -LiteralPath $script:playerTempDir) -and ([IO.Path]::GetDirectoryName($script:playerTempDir) -eq [IO.Path]::GetTempPath().TrimEnd('\'))) {
        Get-ChildItem -LiteralPath $script:playerTempDir -File | Remove-Item -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $script:playerTempDir -ErrorAction SilentlyContinue
    }
})
$window.Add_StateChanged({ Update-PlayerTimer })
