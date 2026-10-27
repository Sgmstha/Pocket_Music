# GPL-3.0. Official YouTube IFrame API inside Microsoft's WebView2 control.
$script:playbackSource = 'local'
$script:previewBrowser = $null
$script:previewToken = ''
$script:previewPosition = 0.0
$script:previewVolumeSync = $false
$script:previewEndedHandled = $false
$script:previewReady = $false
$script:previewBlocked = $false
$script:previewLastStatus = [DateTime]::MinValue
$script:previewStartedAt = [DateTime]::MinValue
$script:previewFailed = $false
$script:previewProgressAt = [DateTime]::MinValue
$previewRow = New-Object Windows.Controls.RowDefinition
$previewRow.Height = New-Object Windows.GridLength 0
$shell.RowDefinitions.Insert(1, $previewRow)
[Windows.Controls.Grid]::SetRow($playerView, 2)
[xml]$previewXaml = @'
<Border xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" Background="#172133" BorderBrush="#303F59" BorderThickness="1" CornerRadius="16" Padding="12" Margin="12" Visibility="Collapsed">
 <Grid><Grid.ColumnDefinitions><ColumnDefinition Width="Auto"/><ColumnDefinition Width="Auto"/><ColumnDefinition Width="*"/></Grid.ColumnDefinitions>
  <Border Name="PreviewHost" Height="200" Width="200" Background="Black"/>
  <Image Name="PreviewArtwork" Grid.Column="1" Width="72" Height="72" Stretch="UniformToFill" Margin="0,0,12,0"/>
  <StackPanel Name="PreviewInfo" Grid.Column="2" Margin="12,0">
   <Border Background="#223049" CornerRadius="18" HorizontalAlignment="Left" Padding="3"><StackPanel Orientation="Horizontal">
    <Button Name="PreviewAudioMode" Content="Audio" Width="80" Padding="8,4" AutomationProperties.Name="Audio preview mode"/>
    <Button Name="PreviewVideoMode" Content="Video" Width="80" Padding="8,4" AutomationProperties.Name="Video preview mode"/>
   </StackPanel></Border>
   <TextBlock Name="PreviewTitle" FontSize="18" FontWeight="SemiBold" TextTrimming="CharacterEllipsis" Margin="0,10,0,6"/>
   <TextBlock Name="PreviewStatus" Text="Loading YouTube…" Foreground="#B7C5DA" TextWrapping="Wrap"/>
   <TextBlock Text="Use the player below. Downloads are separate." Foreground="#B7C5DA" TextWrapping="Wrap" FontSize="12" Margin="0,8,0,8"/>
   <WrapPanel><Button Name="PreviewLocal" Content="Play local copy" Padding="8,6" FontSize="11" IsEnabled="False"/><Button Name="PreviewStop" Content="Stop preview" Padding="8,6" FontSize="11"/></WrapPanel>
  </StackPanel>
 </Grid>
</Border>
'@
$previewPane = [Windows.Markup.XamlReader]::Load((New-Object System.Xml.XmlNodeReader $previewXaml))
[Windows.Controls.Grid]::SetRow($previewPane, 1)
$shell.Children.Add($previewPane) | Out-Null
foreach ($name in @('PreviewHost','PreviewTitle','PreviewStatus','PreviewStop','PreviewArtwork','PreviewAudioMode','PreviewVideoMode','PreviewInfo','PreviewLocal')) { $ui[$name] = $previewPane.FindName($name) }
$script:previewModePath = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'PocketMusic\preview-mode.json'
function Get-PreviewMode {
    try {
        $saved = [IO.File]::ReadAllText($script:previewModePath) | ConvertFrom-Json
        if ($saved.mode -in @('Audio','Video')) { return $saved.mode }
    } catch { }
    return 'Audio'
}
function Set-PreviewMode([ValidateSet('Audio','Video')][string]$Mode, [bool]$Persist = $true) {
    $script:previewMode = $Mode
    $video = $Mode -eq 'Video'
    # Resize the existing host only: never navigate, recreate, or command playback here.
    $ui.PreviewHost.Width = $(if ($video) { 560 } else { 200 })
    $ui.PreviewHost.Height = $(if ($video) { 315 } else { 200 })
    $ui.PreviewArtwork.Visibility = $(if ($video) { 'Collapsed' } else { 'Visible' })
    [Windows.Controls.Grid]::SetColumn($ui.PreviewHost, $(if ($video) { 0 } else { 2 }))
    [Windows.Controls.Grid]::SetColumn($ui.PreviewArtwork, $(if ($video) { 1 } else { 0 }))
    [Windows.Controls.Grid]::SetColumn($ui.PreviewInfo, $(if ($video) { 2 } else { 1 }))
    $previewPane.Child.ColumnDefinitions[1].Width = $(if ($video) { [Windows.GridLength]::Auto } else { New-Object Windows.GridLength 1,Star })
    $previewPane.Child.ColumnDefinitions[2].Width = $(if ($video) { New-Object Windows.GridLength 1,Star } else { [Windows.GridLength]::Auto })
    [Windows.Controls.Grid]::SetRow($previewPane, $(if ($video) { 1 } else { 0 }))
    $previewPane.Width = $(if ($video) { [double]::NaN } else { 640 })
    $previewPane.HorizontalAlignment = $(if ($video) { 'Stretch' } else { 'Right' })
    $previewPane.VerticalAlignment = 'Bottom'
    if ($previewPane.Visibility -eq 'Visible') { $previewRow.Height = New-Object Windows.GridLength $(if ($video) { 365 } else { 0 }) }
    foreach ($choice in @('Audio','Video')) {
        $button = $ui[('Preview' + $choice + 'Mode')]
        $button.Background = $(if ($choice -eq $Mode) { '#C9DFFF' } else { 'Transparent' })
        $button.Foreground = $(if ($choice -eq $Mode) { '#101A2B' } else { '#B7C5DA' })
        $button.ToolTip = $(if ($choice -eq $Mode) { "$choice mode selected" } else { "Switch to $choice without interrupting playback" })
    }
    if ($Persist) {
        try {
            [IO.Directory]::CreateDirectory((Split-Path $script:previewModePath)) | Out-Null
            [IO.File]::WriteAllText($script:previewModePath, (@{mode=$Mode} | ConvertTo-Json -Compress))
        } catch { Add-Log ('Could not remember preview mode: ' + $_.Exception.Message) }
    }
}
$ui.PreviewAudioMode.Add_Click({ Set-PreviewMode 'Audio' })
$ui.PreviewVideoMode.Add_Click({ Set-PreviewMode 'Video' })
Set-PreviewMode (Get-PreviewMode) $false

function Get-PreviewError([string]$Code) {
    switch ($Code) {
        '2' { return 'This video link is invalid.' }
        '5' { return 'YouTube could not play this video in the embedded player.' }
        '100' { return 'This video is unavailable, removed, or private.' }
        '101' { return 'The owner has disabled embedding. This video cannot be previewed here.' }
        '150' { return 'The owner has disabled embedding. This video cannot be previewed here.' }
        '153' { return 'YouTube rejected the player identity. Please restart Pocket Music; this is not a download error.' }
        'offline' { return 'You appear to be offline. Reconnect and press Preview again.' }
        'network' { return 'Could not reach YouTube. Check your internet connection and press Preview again.' }
        'timeout' { return 'YouTube took too long to respond. Check your connection and press Preview again.' }
        default { return 'YouTube preview could not start. Try another video or retry when online.' }
    }
}
function Send-PreviewCommand([string]$Action, [double]$Value = 0) {
    if ($script:previewBrowser -and $script:previewReady -and $script:previewBrowser.CoreWebView2) {
        $json = @{token=$script:previewToken; action=$Action; value=$Value} | ConvertTo-Json -Compress
        $script:previewBrowser.CoreWebView2.PostWebMessageAsJson($json)
    }
}
function Stop-Preview {
    $previewWatch.Stop()
    $script:previewToken = ''
    $script:previewReady = $false
    if ($script:previewBrowser) {
        # Dispose, rather than just hide, so preview audio cannot continue behind local playback.
        $script:previewBrowser.Dispose(); $script:previewBrowser = $null
    }
    $ui.PreviewHost.Child = $null
    $previewPane.Visibility = 'Collapsed'; $previewRow.Height = New-Object Windows.GridLength 0
}
function Set-PreviewError([string]$Code) {
    Send-PreviewCommand 'pause'
    $script:previewFailed = $true
    $message = Get-PreviewError $Code
    Set-PlayerState 'error' $message
    $ui.PreviewStatus.Text = $message
}
function Receive-PreviewMessage($Message) {
    if ($script:playbackSource -ne 'youtube' -or [string]$Message.token -ne $script:previewToken) { return }
    $script:previewLastStatus = [DateTime]::UtcNow
    switch ($Message.type) {
        'ready' {
            $script:previewReady = $true
            if ($ui.PreviewStatus) { $ui.PreviewStatus.Text = 'YouTube player ready' }
        }
        'blocked' {
            $script:previewBlocked = $true
            Set-PlaybackUIState 'paused' 'Click Play inside the YouTube player to begin.'
            if ($ui.PreviewStatus) { $ui.PreviewStatus.Text = $ui.PlayerStatus.Text }
        }
        'error' { Set-PreviewError ([string]$Message.code) }
        'status' {
            if ($script:previewFailed) { return }
            $duration = [double]$Message.duration; $position = [double]$Message.position
            if ([double]::IsNaN($duration) -or [double]::IsInfinity($duration) -or [double]::IsNaN($position) -or [double]::IsInfinity($position)) { return }
            if ([math]::Abs($position - $script:previewPosition) -gt 0.1) { $script:previewProgressAt = [DateTime]::UtcNow }
            $script:previewPosition = [math]::Max(0,$position)
            $script:playback.position = $script:previewPosition
            $script:playback.duration = [math]::Max(1,$duration)
            $ui.PlayerSeek.Maximum = [math]::Max(1,$duration)
            $ui.PlayerDuration.Text = Format-TimeDisplay $duration
            if (-not $script:playerSeeking) {
                $ui.PlayerSeek.Value = $script:previewPosition
                $ui.PlayerElapsed.Text = Format-TimeDisplay $position
            }
            $script:previewVolumeSync = $true
            try {
                $scaledVol = [math]::Min(1,[math]::Max(0,([double]$Message.volume / 100)))
                $ui.PlayerVolume.Value = $scaledVol
                $script:playback.volume = $scaledVol
            } finally { $script:previewVolumeSync = $false }

            switch ([int]$Message.state) {
                1 { $script:previewBlocked = $false; Set-PlaybackUIState 'playing' 'Streaming from YouTube' }
                2 { Set-PlaybackUIState 'paused' 'YouTube preview paused' }
                3 { Set-PlaybackUIState 'buffering' 'Buffering YouTube preview…'; $ui.PlayerToggle.IsEnabled = $true; if (Get-Command Sync-WindowsPlayback -ErrorAction SilentlyContinue) { Sync-WindowsPlayback } }
                5 { if (-not $script:previewBlocked) { Set-PlaybackUIState 'paused' 'Press Play to preview' } }
                -1 { if (-not $script:previewBlocked) { Set-PlaybackUIState 'loading' 'Loading YouTube preview…' } }
                0 {
                    if (-not $script:previewEndedHandled) {
                        $script:previewEndedHandled = $true
                        On-TrackEnded
                    }
                }
            }
            $ui.PlayerSeek.IsEnabled = $duration -gt 0 -and $script:playerState -in @('playing','paused','ended')
            if ($ui.PreviewStatus) { $ui.PreviewStatus.Text = $ui.PlayerStatus.Text }
        }
    }
}

function Start-PreviewTrack($Track, [bool]$KeepQueue = $false) {
    Play-AudioTrack $Track 'preview' $KeepQueue
}

$ui.PreviewStop.Add_Click({
    Stop-PreviewCore
    $script:playback.source = 'none'
    $script:playback.currentTrack = $null
    $script:playerTrack = $null
    Set-PlaybackUIState 'idle' 'Preview stopped'
})

$previewWatch = New-Object Windows.Threading.DispatcherTimer
$previewWatch.Interval = [TimeSpan]::FromSeconds(5)
$previewWatch.Add_Tick({
    if ($script:playbackSource -ne 'youtube' -or $script:previewFailed -or -not $script:previewBrowser) { return }
    Keep-PreviewActive
    if ((-not $script:previewReady -and ([DateTime]::UtcNow-$script:previewStartedAt).TotalSeconds -gt 35) -or ($script:previewReady -and ([DateTime]::UtcNow-$script:previewLastStatus).TotalSeconds -gt 20)) {
        Send-PreviewCommand 'pause'
        Set-PreviewError 'timeout'
    }
    elseif ($script:playerState -in @('loading','buffering') -and ([DateTime]::UtcNow-$script:previewProgressAt).TotalSeconds -gt 40) {
        Send-PreviewCommand 'pause'
        Set-PreviewError 'timeout'
    }
})

function Keep-PreviewActive {
    if ($script:previewBrowser -and $script:previewBrowser.CoreWebView2 -and $script:previewBrowser.CoreWebView2.IsSuspended) {
        $script:previewBrowser.CoreWebView2.Resume()
    }
}
$window.Add_StateChanged({ Keep-PreviewActive })
$window.Add_Closing({ if (-not $_.Cancel) { $previewWatch.Stop(); Stop-PreviewCore } })
