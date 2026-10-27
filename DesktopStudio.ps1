# Native desktop composition. Presentation uses the existing central player.
$script:nowPlayingPanelOpen = $true
$script:studioCoverCache = @{}
$script:studioBitmapCache = @{}
foreach ($name in @('SidebarFilter','SidebarTracks','SidebarCount','SidebarEmpty','SidebarChooseFolder','HomeAlbums','ToggleNowPlaying','CloseNowPlaying','NowPlayingPanel','NowPlayingColumn','NowArtwork','NowTitle','NowSubtitle','NowSource','NowStatus','NowToggle','NowQueue','NowNextTracks','NowNextEmpty','SearchSuggestionPopup')) {
    $ui[$name] = $window.FindName($name)
}

function Get-StudioCover([string]$Title) {
    if ($script:studioCoverCache.ContainsKey($Title)) { return $script:studioCoverCache[$Title] }
    $index = [int]([math]::Abs([long]$Title.GetHashCode()) % 5)
    $path = Join-Path $PSScriptRoot ('assets\studio-cover-' + $index + '.png')
    if ($script:studioCoverCache.Count -lt 256) { $script:studioCoverCache[$Title] = $path }
    return $path
}
function Update-StudioLibrary {
    $query = $ui.SidebarFilter.Text.Trim()
    $tracks = @($script:libraryTracksCache | Where-Object { -not $query -or $_.Title.IndexOf($query,[StringComparison]::OrdinalIgnoreCase) -ge 0 })
    $ui.SidebarTracks.ItemsSource = $tracks
    $ui.SidebarCount.Text = if ($query) { "$($tracks.Count) matching tracks" } else { "$($tracks.Count) saved tracks" }
    $ui.SidebarEmpty.Visibility = if ($tracks.Count) { 'Collapsed' } else { 'Visible' }
    $ui.SidebarEmpty.Text = if ($query) { 'No matching saved tracks.' } else { "Your music lives here.`nChoose a folder to get started." }
    $ui.HomeAlbums.ItemsSource = @($script:libraryTracksCache | Select-Object -First 4)
}

function Set-StudioImageSource($Image) {
    $data = $Image.DataContext
    $source = if ($data -and $data.PSObject.Properties['Artwork']) { $data.Artwork } else { $null }
    if (-not $source) { $Image.Source = $null; return }
    if ($source -is [Windows.Media.ImageSource]) { $Image.Source = $source; return }
    $uri = $null
    if (-not [uri]::TryCreate([string]$source,[UriKind]::Absolute,[ref]$uri)) { $Image.Source=$null; return }
    if (-not $uri.IsFile) { $Image.Source=$uri; return }
    $key=$uri.LocalPath
    try {
        if ($script:studioBitmapCache.ContainsKey($key)) { $Image.Source=$script:studioBitmapCache[$key]; return }
        $bitmap=New-Object Windows.Media.Imaging.BitmapImage
        $bitmap.BeginInit()
        $bitmap.CacheOption=[Windows.Media.Imaging.BitmapCacheOption]::OnLoad
        $bitmap.DecodePixelWidth=256
        $bitmap.UriSource=$uri
        $bitmap.EndInit()
        $bitmap.Freeze()
        if ($script:studioBitmapCache.Count -lt 256) { $script:studioBitmapCache[$key]=$bitmap }
        $Image.Source=$bitmap
    } catch { $Image.Source=$null }
}

# WPF's default converter does not unwrap artwork values from PSObject.
# Populate realized images after container generation and follow recycling.
function Set-StudioVisualArtwork($Visual) {
    if ($Visual -is [Windows.Controls.Image] -and $Visual.Tag -eq 'TrackArtwork') {
        Set-StudioImageSource $Visual
        if (-not $Visual.Resources.Contains('StudioArtworkHook')) {
            $Visual.Resources.Add('StudioArtworkHook',$true)
            $Visual.Add_DataContextChanged({ Set-StudioImageSource $this })
        }
    }
    for ($child=0; $child -lt [Windows.Media.VisualTreeHelper]::GetChildrenCount($Visual); $child++) {
        Set-StudioVisualArtwork ([Windows.Media.VisualTreeHelper]::GetChild($Visual,$child))
    }
}
function Refresh-StudioImages {
    $script:studioImagesPending=$false
    foreach ($list in @($ui.SidebarTracks,$ui.HomeAlbums,$ui.RecentTracks,$ui.LibraryTracks,$ui.SearchResults)) {
        $list.UpdateLayout()
        Set-StudioVisualArtwork $list
    }
}
$script:studioImagesPending=$false
foreach ($list in @($ui.SidebarTracks,$ui.HomeAlbums,$ui.RecentTracks,$ui.LibraryTracks,$ui.SearchResults)) {
    $list.ItemContainerGenerator.Add_StatusChanged({
        if ($this.Status -eq [Windows.Controls.Primitives.GeneratorStatus]::ContainersGenerated -and -not $script:studioImagesPending) {
            $script:studioImagesPending=$true
            $window.Dispatcher.BeginInvoke([Action]{ Refresh-StudioImages },[Windows.Threading.DispatcherPriority]::Loaded) | Out-Null
        }
    })
}
$window.Add_ContentRendered({ Refresh-StudioImages })
function Set-NowPlayingPanel([bool]$Open) {
    $script:nowPlayingPanelOpen = $Open
    if ($Open -and $script:downloadPanelOpen) { Set-DownloadPanel $false }
    Update-StudioDensity
    Sync-StudioPlayback
}

function Update-StudioDensity {
    $width = if ($window.ActualWidth -gt 0) { $window.ActualWidth } else { $window.Width }
    $compact = $script:downloadPanelOpen -and $width -lt 1200
    $window.FindName('NavigationColumn').Width = New-Object Windows.GridLength $(if ($compact) { 88 } elseif ($width -lt 1150) { 208 } else { 248 })
    $window.FindName('SidebarBrand').Visibility = if ($compact) { 'Collapsed' } else { 'Visible' }
    foreach ($name in @('SidebarNote','SidebarTools','SidebarTracks','SidebarChooseFolder')) { $window.FindName($name).Visibility = if ($compact) { 'Collapsed' } else { 'Visible' } }
    if ($compact) { $ui.SidebarEmpty.Visibility = 'Collapsed' }
    elseif ($ui.SidebarTracks.Items.Count -eq 0) { $ui.SidebarEmpty.Visibility = 'Visible' }
    foreach ($name in @('Home','Search','Library')) { $ui["${name}Nav"].Content.Children[1].Visibility = if ($compact) { 'Collapsed' } else { 'Visible' } }
    $showNow = $script:nowPlayingPanelOpen -and -not $script:downloadPanelOpen -and $width -ge 1280
    $ui.NowPlayingColumn.Width = New-Object Windows.GridLength $(if ($showNow) { 304 } else { 0 })
    $ui.NowPlayingPanel.Visibility = if ($showNow) { 'Visible' } else { 'Collapsed' }
    $ui.ToggleNowPlaying.Content = if ($showNow) { 'Hide player' } else { 'Now playing' }
    # On a narrow display the deliberate toggle replaces the sidebar instead of squeezing content.
    if ($script:nowPlayingPanelOpen -and -not $script:downloadPanelOpen -and $width -lt 1280 -and $script:studioForcePanel) {
        $ui.NowPlayingColumn.Width = New-Object Windows.GridLength 280
        $ui.NowPlayingPanel.Visibility = 'Visible'
        $window.FindName('NavigationColumn').Width = New-Object Windows.GridLength 88
        $window.FindName('SidebarBrand').Visibility = 'Collapsed'
        foreach ($name in @('SidebarNote','SidebarTools','SidebarTracks','SidebarEmpty','SidebarChooseFolder')) { $window.FindName($name).Visibility = 'Collapsed' }
        foreach ($name in @('Home','Search','Library')) { $ui["${name}Nav"].Content.Children[1].Visibility = 'Collapsed' }
        $ui.ToggleNowPlaying.Content = 'Hide player'
    }
    if ($ui.HomeView.ActualWidth -gt 0) {
        $ui.RecentTracks.Tag = if ($ui.HomeView.ActualWidth -ge 850) { 3 } elseif ($ui.HomeView.ActualWidth -ge 530) { 2 } else { 1 }
        $ui.HomeAlbums.Tag = [math]::Max(2,[math]::Min(4,[int][math]::Floor(($ui.HomeView.ActualWidth - 40) / 168)))
    }
}
$script:studioForcePanel = $false

function Sync-StudioPlayback {
    if (-not $ui.NowTitle) { return }
    $track = $script:playback.currentTrack
    $ui.NowTitle.Text = if ($track) { [string]$track.Title } else { 'Make room for music' }
    $ui.NowSubtitle.Text = if ($track -and $track.PSObject.Properties['Subtitle']) { [string]$track.Subtitle } else { 'Play a saved track or find a new favourite.' }
    $ui.NowSource.Text = switch ($script:playback.source) { 'local' { 'From your music folder' } 'preview' { 'YouTube preview' } default { 'Ready to listen' } }
    $ui.NowStatus.Text = $ui.PlayerStatus.Text
    $ui.NowToggle.Content = if ($script:playback.isPlaying) { 'Pause' } else { 'Play' }
    $ui.NowToggle.IsEnabled = $ui.PlayerToggle.IsEnabled
    $art = if ($ui.PlayerArtwork.Source) { $ui.PlayerArtwork.Source } elseif ($track) { Get-StudioCover $track.Title } else { $null }
    if ($ui.NowArtwork.Source -ne $art) { $ui.NowArtwork.Source = $art }
    $upNext = @()
    $order = @(if ($script:playback.isShuffled) { $script:playback.shuffledOrder } else { for ($i=0; $i -lt $script:playback.queue.Count; $i++) { $i } })
    $current = [array]::IndexOf($order,$script:playback.queueIndex)
    for ($i=$current+1; $i -lt $order.Count -and $upNext.Count -lt 3; $i++) { $upNext += $script:playback.queue[$order[$i]] }
    $ui.NowNextTracks.ItemsSource = $upNext
    $ui.NowNextEmpty.Visibility = if ($upNext.Count) { 'Collapsed' } else { 'Visible' }
}

$ui.SidebarFilter.Add_TextChanged({ Update-StudioLibrary })
$ui.SidebarChooseFolder.Add_Click({ $ui.Browse.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent))) })
$ui.SidebarTracks.Add_MouseDoubleClick({ Invoke-TrackDefaultAction $this $_ })
$ui.SidebarTracks.Add_KeyDown({ if ($_.Key -eq 'Return') { Invoke-TrackDefaultAction $this $_ } })
$ui.HomeAlbums.AddHandler([Windows.Controls.Button]::ClickEvent,[Windows.RoutedEventHandler]{
    param($sender,$eventArgs)
    $button=$eventArgs.OriginalSource
    while ($button -and $button -isnot [Windows.Controls.Button]) { $button=[Windows.Media.VisualTreeHelper]::GetParent($button) }
    if ($button -and $button.Name -eq 'AlbumPlay') { $eventArgs.Handled=$true; Open-LocalTrack $button.DataContext @($ui.HomeAlbums.ItemsSource) }
})
$ui.ToggleNowPlaying.Add_Click({
    $visible=$ui.NowPlayingPanel.Visibility -eq 'Visible'
    $script:studioForcePanel=-not $visible
    Set-NowPlayingPanel (-not $visible)
})
$ui.CloseNowPlaying.Add_Click({ $script:studioForcePanel=$false; Set-NowPlayingPanel $false })
$ui.NowToggle.Add_Click({ Toggle-PlayPause })
$ui.NowQueue.Add_Click({ Toggle-QueueView })
foreach ($name in @('BrowseFocus','BrowseCalm','BrowseJazz','BrowseAcoustic')) {
    $window.FindName($name).Add_Click({ $ui.Query.Text=[string]$this.Tag; Start-Search })
}
$ui.Query.Add_GotKeyboardFocus({ $ui.SearchSuggestionPopup.IsOpen=$false })
$window.Add_PreviewMouseDown({
    if (-not $ui.Query.IsMouseOver) { $ui.SearchSuggestionPopup.IsOpen=$false }
})
$window.Add_Deactivated({ $ui.SearchSuggestionPopup.IsOpen=$false })
