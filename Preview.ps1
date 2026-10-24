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
<Border xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" Background="#17131D" BorderBrush="#40394F" BorderThickness="1" CornerRadius="16" Padding="12" Margin="12" Visibility="Collapsed">
 <Grid><Grid.ColumnDefinitions><ColumnDefinition Width="Auto"/><ColumnDefinition Width="Auto"/><ColumnDefinition Width="*"/></Grid.ColumnDefinitions>
  <Border Name="PreviewHost" Height="200" Width="200" Background="Black"/>
  <Image Name="PreviewArtwork" Grid.Column="1" Width="72" Height="72" Stretch="UniformToFill" Margin="0,0,12,0"/>
  <StackPanel Name="PreviewInfo" Grid.Column="2" Margin="12,0">
   <Border Background="#292330" CornerRadius="18" HorizontalAlignment="Left" Padding="3"><StackPanel Orientation="Horizontal">
    <Button Name="PreviewAudioMode" Content="Audio" Width="80" Padding="8,4" AutomationProperties.Name="Audio preview mode"/>
    <Button Name="PreviewVideoMode" Content="Video" Width="80" Padding="8,4" AutomationProperties.Name="Video preview mode"/>
   </StackPanel></Border>
   <TextBlock Name="PreviewTitle" FontSize="18" FontWeight="SemiBold" TextTrimming="CharacterEllipsis" Margin="0,10,0,6"/>
   <TextBlock Name="PreviewStatus" Text="Loading YouTube…" Foreground="#C0B3CF" TextWrapping="Wrap"/>
   <TextBlock Text="Use the player below. Downloads are separate." Foreground="#958AA4" TextWrapping="Wrap" FontSize="12" Margin="0,8,0,8"/>
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
        $button.Background = $(if ($choice -eq $Mode) { '#CEC1FF' } else { 'Transparent' })
        $button.Foreground = $(if ($choice -eq $Mode) { '#251C35' } else { '#C0B3CF' })
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
        'ready' { $script:previewReady = $true; $ui.PreviewStatus.Text = 'YouTube player ready'; }
        'blocked' {
            $script:previewBlocked = $true
            Set-PlayerState 'paused' 'Click Play inside the YouTube player to begin.'
            $ui.PreviewStatus.Text = $ui.PlayerStatus.Text
        }
        'error' { Set-PreviewError ([string]$Message.code) }
        'status' {
            if ($script:previewFailed) { return }
            $duration = [double]$Message.duration; $position = [double]$Message.position
            if ([double]::IsNaN($duration) -or [double]::IsInfinity($duration) -or [double]::IsNaN($position) -or [double]::IsInfinity($position)) { return }
            if ([math]::Abs($position - $script:previewPosition) -gt 0.1) { $script:previewProgressAt = [DateTime]::UtcNow }
            $script:previewPosition = [math]::Max(0,$position)
            $ui.PlayerSeek.Maximum = [math]::Max(1,$duration)
            $ui.PlayerDuration.Text = Format-PlayerTime $duration
            if (-not $script:playerSeeking) { $ui.PlayerSeek.Value = $script:previewPosition; $ui.PlayerElapsed.Text = Format-PlayerTime $position }
            $script:previewVolumeSync = $true
            try { $ui.PlayerVolume.Value = [math]::Min(1,[math]::Max(0,([double]$Message.volume / 100))) } finally { $script:previewVolumeSync = $false }
            switch ([int]$Message.state) {
                1 { $script:previewBlocked = $false; Set-PlayerState 'playing' 'Streaming from YouTube' }
                2 { Set-PlayerState 'paused' 'YouTube preview paused' }
                3 { Set-PlayerState 'buffering' 'Buffering YouTube preview…'; $ui.PlayerToggle.IsEnabled = $true }
                5 { if (-not $script:previewBlocked) { Set-PlayerState 'paused' 'Press Play to preview' } }
                -1 { if (-not $script:previewBlocked) { Set-PlayerState 'loading' 'Loading YouTube preview…' } }
                0 {
                    if (-not $script:previewEndedHandled) {
                        $script:previewEndedHandled = $true
                        if ($script:playerIndex -lt ($script:playerQueue.Count - 1)) { Step-Player 1; return }
                        Set-PlayerState 'ended' 'End of preview queue'
                    }
                }
            }
            $ui.PlayerSeek.IsEnabled = $duration -gt 0 -and $script:playerState -in @('playing','paused','ended')
            $ui.PreviewStatus.Text = $ui.PlayerStatus.Text
        }
    }
}
function Start-PreviewTrack($Track, [bool]$KeepQueue = $false) {
    if (-not $Track -or $Track.Id -notmatch '^[A-Za-z0-9_-]{11}$') { return }
    Stop-Preview; Close-PlayerMedia
    $previewWatch.Start()
    $script:playbackSource = 'youtube'; $script:playerTrack = $Track
    if (-not $KeepQueue) {
        $script:playerQueue = @($ui.SearchResults.ItemsSource | Where-Object { $null -ne $_ })
        $script:playerIndex = -1
        for ($i=0; $i -lt $script:playerQueue.Count; $i++) { if ($script:playerQueue[$i].Id -eq $Track.Id) { $script:playerIndex = $i; break } }
        if ($script:playerIndex -lt 0) { $script:playerQueue = @($Track); $script:playerIndex = 0 }
    }
    $script:previewPosition = 0; $script:previewFailed = $false; $script:previewEndedHandled = $false; $script:previewBlocked = $false
    $script:previewToken = [guid]::NewGuid().ToString('N')
    $script:previewStartedAt = [DateTime]::UtcNow; $script:previewLastStatus = [DateTime]::UtcNow
    $script:previewProgressAt = [DateTime]::UtcNow
    $ui.PlayerTitle.Text = $Track.Title; $ui.PlayerTitle.ToolTip = $Track.Title
    $ui.PlayerElapsed.Text = '0:00'; $ui.PlayerDuration.Text = '0:00'; $ui.PlayerSeek.Value = 0
    $ui.PreviewTitle.Text = $Track.Title; $ui.PreviewStatus.Text = 'Loading YouTube…'
    $ui.PreviewTitle.ToolTip = $Track.Title
    $ui.PreviewLocal.IsEnabled = $null -ne (Find-LocalCopy $Track.Id)
    $ui.PreviewArtwork.Source = [uri]("https://i.ytimg.com/vi/" + $Track.Id + '/hqdefault.jpg')
    $previewPane.Visibility = 'Visible'; Set-PreviewMode $script:previewMode $false
    Set-PlayerState 'loading' 'Loading YouTube preview…'
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
        $properties.UserDataFolder = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'PocketMusic\WebView2'
        $script:previewBrowser.CreationProperties = $properties
        $script:previewBrowser.Add_CoreWebView2InitializationCompleted({
            if (-not [object]::ReferenceEquals($this,$script:previewBrowser)) { return }
            if (-not $_.IsSuccess) { Set-PreviewError 'network'; $ui.PreviewStatus.Text = 'The embedded browser could not start. Install or repair Microsoft Edge WebView2 Runtime.'; return }
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
            $uri = 'https://pocketmusic.desktop/index.html?id=' + $script:playerTrack.Id + '&token=' + $script:previewToken + '&volume=' + [int]($ui.PlayerVolume.Value*100)
            $core.Navigate($uri)
            } catch { Set-PreviewError 'network'; Add-Log ('Preview initialization error: ' + $_.Exception.Message) }
        })
        $ui.PreviewHost.Child = $script:previewBrowser
        $script:previewBrowser.EnsureCoreWebView2Async() | Out-Null
    } catch {
        Set-PlayerState 'error' 'YouTube preview requires Microsoft Edge WebView2. Check the activity details.'
        $ui.PreviewStatus.Text = $ui.PlayerStatus.Text
        Add-Log $_.Exception.Message
        $script:previewFailed = $true
    }
}
$ui.PreviewStop.Add_Click({ Stop-Preview; $script:playbackSource='local'; $script:playerTrack=$null; $script:playerQueue=@(); $script:playerIndex=-1; Set-PlayerState 'idle' 'Preview stopped' })
$previewWatch = New-Object Windows.Threading.DispatcherTimer
$previewWatch.Interval = [TimeSpan]::FromSeconds(5)
$previewWatch.Add_Tick({
    if ($script:playbackSource -ne 'youtube' -or $script:previewFailed -or -not $script:previewBrowser) { return }
    Keep-PreviewActive
    if ((-not $script:previewReady -and ([DateTime]::UtcNow-$script:previewStartedAt).TotalSeconds -gt 35) -or ($script:previewReady -and ([DateTime]::UtcNow-$script:previewLastStatus).TotalSeconds -gt 20)) { Send-PreviewCommand 'pause'; Set-PreviewError 'timeout' }
    elseif ($script:playerState -in @('loading','buffering') -and ([DateTime]::UtcNow-$script:previewProgressAt).TotalSeconds -gt 40) { Send-PreviewCommand 'pause'; Set-PreviewError 'timeout' }
})
function Keep-PreviewActive {
    if ($script:previewBrowser -and $script:previewBrowser.CoreWebView2 -and $script:previewBrowser.CoreWebView2.IsSuspended) {
        # Resume the renderer only; never override an intentional user pause.
        $script:previewBrowser.CoreWebView2.Resume()
    }
}
$window.Add_StateChanged({ Keep-PreviewActive })
$window.Add_Closing({ if (-not $_.Cancel) { $previewWatch.Stop(); Stop-Preview } })
