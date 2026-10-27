# Desktop presentation helpers. No playback or network polling is added here.
$script:desktopSettingsPath = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'PocketMusic\desktop-settings.json'
if ($SmokeTest -or $PlaybackTestFolder -or $PreviewImage -or $LiveSearchTest -or $PreviewLiveTest -or $WindowsMediaTest) {
    $script:desktopSettingsPath=Join-Path ([IO.Path]::GetTempPath()) ('PocketMusic-test-settings-'+[guid]::NewGuid()+'.json')
    $window.Add_Closed({Remove-Item -LiteralPath $script:desktopSettingsPath -ErrorAction SilentlyContinue})
}
$script:downloadPanelOpen = $false
$script:libraryTracksCache = @()
foreach ($name in @('DownloadPanel','DownloadColumn','ToggleDownloads','CloseDownloads','LibraryFilter','LibraryClearFilter','LibraryChooseFolder','LibraryChangeFolder','LibraryEmpty','LibraryEmptyTitle','LibraryEmptyHint','SearchEmpty','SearchEmptyTitle','SearchEmptyHint','SearchActivity','KeyboardHelp','HeroBrowse','HeroLibrary','HomeChooseFolder','HomeHeroArt','HomeLibraryCount')) {
    $ui[$name] = $window.FindName($name)
}
. "$PSScriptRoot\DesktopStudio.ps1"

$heroPath = Join-Path $PSScriptRoot 'assets\home-studio.png'
if (Test-Path -LiteralPath $heroPath) {
    $heroBitmap = New-Object Windows.Media.Imaging.BitmapImage
    $heroBitmap.BeginInit()
    $heroBitmap.CacheOption = [Windows.Media.Imaging.BitmapCacheOption]::OnLoad
    $heroBitmap.DecodePixelWidth = 1024
    $heroBitmap.UriSource = [uri]$heroPath
    $heroBitmap.EndInit()
    $heroBitmap.Freeze()
    $heroBrush = New-Object Windows.Media.ImageBrush $heroBitmap
    $heroBrush.Stretch = 'UniformToFill'
    $heroBrush.Freeze()
    $ui.HomeHeroArt.Background = $heroBrush
}
$ui.HomeView.Add_SizeChanged({
    Update-StudioDensity
})
$ui.RecentTracks.AddHandler([Windows.Controls.Button]::ClickEvent, [Windows.RoutedEventHandler]{
    param($sender,$eventArgs)
    $button = $eventArgs.OriginalSource
    while ($button -and $button -isnot [Windows.Controls.Button]) { $button = [Windows.Media.VisualTreeHelper]::GetParent($button) }
    if ($button -and $button.Name -eq 'RecentPlay') { $eventArgs.Handled = $true; Open-LocalTrack $button.DataContext @($ui.RecentTracks.ItemsSource) }
})
$ui.HeroBrowse.Add_Click({ Show-Page 'Search'; $ui.Query.Focus() | Out-Null; $ui.Query.SelectAll() })
$ui.HeroLibrary.Add_Click({ Show-Page 'Library'; Update-Library })
$ui.HomeChooseFolder.Add_Click({ Show-Page 'Library'; $ui.Browse.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent))) })

function Set-DownloadPanel([bool]$Open) {
    $script:downloadPanelOpen = $Open
    $ui.DownloadPanel.Visibility = if ($Open) { 'Visible' } else { 'Collapsed' }
    $ui.DownloadColumn.Width = New-Object Windows.GridLength $(if ($Open) { 304 } else { 0 })
    Update-DownloadAffordance
    $ui.ToggleDownloads.ToolTip = 'Show or hide download settings (Ctrl+D)'
    Update-DesktopDensity
}

function Update-DesktopDensity {
    Update-StudioDensity
}

function Update-DownloadAffordance {
    if ($script:job -and -not $script:job.HasExited) {
        $ui.ToggleDownloads.Content = if ($ui.Progress.IsIndeterminate) { 'Downloading…' } else { 'Downloading · ' + [int]$ui.Progress.Value + '%' }
    } elseif ($ui.Status.Text -like 'Saved*') { $ui.ToggleDownloads.Content = 'Saved · Downloads' }
    elseif ($ui.Status.Text -eq 'Download did not finish') { $ui.ToggleDownloads.Content = 'Download needs attention' }
    else { $ui.ToggleDownloads.Content = 'Downloads' }
}

function Apply-LibraryFilter {
    $query = $ui.LibraryFilter.Text.Trim()
    $tracks = @($script:libraryTracksCache | Where-Object { -not $query -or ([string]$_.Title+' '+[string]$_.Subtitle).IndexOf($query, [StringComparison]::OrdinalIgnoreCase) -ge 0 })
    $formatChoice=$window.FindName('LibraryFileFormat')
    if($formatChoice.SelectedIndex -gt 0){$extension='.'+([string]$formatChoice.SelectedItem.Content).ToLowerInvariant();$tracks=@($tracks | Where-Object { [IO.Path]::GetExtension([string]$_.Path).ToLowerInvariant() -eq $extension })}
    $sort = $window.FindName('LibrarySort')
    if ($sort.SelectedIndex -eq 1) { $tracks = @($tracks | Sort-Object Title) }
    elseif ($sort.SelectedIndex -eq 2) { $tracks = @($tracks | Sort-Object Title -Descending) }
    $script:libraryRefreshing = $true
    try { $ui.LibraryTracks.ItemsSource = $tracks } finally { $script:libraryRefreshing = $false }
    $total = $script:libraryTracksCache.Count
    $bytes=0L;foreach($track in $tracks){$bytes += [long](Get-TrackProp $track 'Bytes' 0)}
    $countLabel=if($tracks.Count -eq 1){'1 track'}else{"$($tracks.Count) tracks"}
    $sizeLabel=if($bytes -ge 1GB){'{0:N1} GB' -f ($bytes/1GB)}else{'{0:N1} MB' -f ($bytes/1MB)}
    $ui.LibraryStatus.Text=if($query -or $formatChoice.SelectedIndex -gt 0){"$countLabel of $total · $sizeLabel · selected folder"}else{"$countLabel · $sizeLabel · on your PC"}
    $ui.LibraryClearFilter.Visibility = if ($query) { 'Visible' } else { 'Collapsed' }
    $ui.LibraryEmpty.Visibility = if ($tracks.Count) { 'Collapsed' } else { 'Visible' }
    $ui.LibraryEmptyTitle.Text = if ($total -and ($query -or $formatChoice.SelectedIndex -gt 0)) { 'No matching tracks' } else { 'Your library starts here' }
    $ui.LibraryEmptyHint.Text = if ($total -and ($query -or $formatChoice.SelectedIndex -gt 0)) { 'Try a different name, or clear the filter.' } else { 'Choose a folder with your audio files, or download a track from Search.' }
    $ui.PlayAllLocal.IsEnabled = $tracks.Count -gt 0
    $ui.QueueAllLocal.IsEnabled = $tracks.Count -gt 0
    $ui.PlayLocal.IsEnabled = $null -ne $ui.LibraryTracks.SelectedItem
    Update-ListsActiveRow
}

function Update-SearchPresentation([string]$State = 'ready') {
    $count = $ui.SearchResults.Items.Count
    $window.FindName('SearchBrowse').Visibility = if ($State -in @('ready','empty')) { 'Visible' } else { 'Collapsed' }
    $ui.SearchActivity.Visibility = if ($State -eq 'loading') { 'Visible' } else { 'Collapsed' }
    $ui.SearchEmpty.Visibility = if ($count) { 'Collapsed' } else { 'Visible' }
    $ui.SearchEmptyTitle.Text = switch ($State) { 'loading' { 'Finding your music…' } 'empty' { 'No results this time' } 'error' { 'Search could not finish' } default { 'What would you like to hear?' } }
    $ui.SearchEmptyHint.Text = switch ($State) { 'loading' { 'Results will appear here. You can cancel this search.' } 'empty' { 'Try another song, artist, or a direct YouTube link.' } 'error' { 'Check the message above and try again.' } default { 'Search by song or artist. Double-click a result to preview it.' } }
}

function Save-DesktopSettings {
    try {
        $bounds = $window.RestoreBounds
        $settings = @{ folder=$ui.Folder.Text; format=[string]$ui.Format.SelectedItem.Content; width=$bounds.Width; height=$bounds.Height; maximized=($window.WindowState -eq 'Maximized'); librarySort=$window.FindName('LibrarySort').SelectedIndex; searchHistory=@($script:desktopSearchHistory); nowPlayingPanel=$script:nowPlayingPanelOpen }
        [IO.Directory]::CreateDirectory((Split-Path $script:desktopSettingsPath)) | Out-Null
        [IO.File]::WriteAllText($script:desktopSettingsPath, ($settings | ConvertTo-Json))
    } catch { Add-Log ('Could not remember desktop settings: ' + $_.Exception.Message) }
}

function Restore-DesktopSettings {
    try {
        $saved = [IO.File]::ReadAllText($script:desktopSettingsPath) | ConvertFrom-Json
        if ($saved.folder -and (Test-Path -LiteralPath $saved.folder -PathType Container)) { $ui.Folder.Text = $saved.folder }
        foreach ($item in $ui.Format.Items) { if ($item.Content -eq $saved.format) { $ui.Format.SelectedItem = $item; break } }
        if ($saved.width -ge $window.MinWidth -and $saved.width -le [Windows.SystemParameters]::WorkArea.Width) { $window.Width = $saved.width }
        if ($saved.height -ge $window.MinHeight -and $saved.height -le [Windows.SystemParameters]::WorkArea.Height) { $window.Height = $saved.height }
        if ($saved.maximized) { $window.WindowState = 'Maximized' }
        if ($saved.PSObject.Properties['librarySort'] -and $saved.librarySort -in @(0,1,2)) { $window.FindName('LibrarySort').SelectedIndex = $saved.librarySort }
        if ($saved.PSObject.Properties['searchHistory']) { $script:desktopSearchHistory = @($saved.searchHistory | Where-Object { $_ -is [string] -and $_.Length -le 200 } | Select-Object -First 20) }
        if ($saved.PSObject.Properties['nowPlayingPanel'] -and $saved.nowPlayingPanel -is [bool]) { $script:nowPlayingPanelOpen = $saved.nowPlayingPanel }
    } catch { } # Missing or invalid preferences use the normal defaults.
}

function Invoke-TrackDefaultAction($List, $Event) {
    $source = $Event.OriginalSource
    while ($source -and $source -ne $List) {
        if ($source -is [Windows.Controls.Primitives.ButtonBase]) { return }
        if ($source -isnot [Windows.Media.Visual] -and $source -isnot [Windows.Media.Media3D.Visual3D]) { break }
        $source = [Windows.Media.VisualTreeHelper]::GetParent($source)
    }
    if (-not $List.SelectedItem) { return }
    if ($List -eq $ui.SearchResults) { Start-PreviewTrack $List.SelectedItem }
    elseif ($List -eq $ui.SidebarTracks -or $List -eq $ui.RecentTracks) { Open-LocalTrack $List.SelectedItem @($List.ItemsSource) }
    else { Open-LocalTrack $List.SelectedItem }
    $Event.Handled = $true
}

$ui.ToggleDownloads.Add_Click({ Set-DownloadPanel (-not $script:downloadPanelOpen) })
$ui.CloseDownloads.Add_Click({ Set-DownloadPanel $false })
$ui.LibraryFilter.Add_TextChanged({ Apply-LibraryFilter })
$ui.LibraryClearFilter.Add_Click({ $ui.LibraryFilter.Clear(); $ui.LibraryFilter.Focus() | Out-Null })
$ui.LibraryChooseFolder.Add_Click({ $ui.Browse.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent))) })
$ui.LibraryChangeFolder.Add_Click({ $ui.Browse.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent))) })
$ui.KeyboardHelp.Add_Click({
    [Windows.MessageBox]::Show("Ctrl+K  Search`nCtrl+D  Downloads`nEnter / double-click  Play the selected track`nSpace  Play / pause (outside controls)`nLeft / Right  Seek 5 seconds (outside controls)`nUp / Down  Volume (outside lists and controls)`nEsc  Close queue or downloads", 'Pocket Music shortcuts') | Out-Null
})
foreach ($list in @($ui.LibraryTracks,$ui.RecentTracks,$ui.SearchResults)) {
    $list.Add_MouseDoubleClick({ Invoke-TrackDefaultAction $this $_ })
    $list.Add_KeyDown({ if ($_.Key -eq 'Return') { Invoke-TrackDefaultAction $this $_ } })
}
$window.Add_PreviewKeyDown({
    if (($_.KeyboardDevice.Modifiers -band [Windows.Input.ModifierKeys]::Control) -ne 0) {
        switch ($_.Key) {
            'K' { Show-Page 'Search'; $ui.Query.Focus() | Out-Null; $ui.Query.SelectAll(); $_.Handled=$true }
            'D' { Set-DownloadPanel (-not $script:downloadPanelOpen); $_.Handled=$true }
        }
    } elseif ($_.Key -eq 'Escape') {
        if ($ui.QueuePanel.Visibility -eq 'Visible') { $ui.QueuePanel.Visibility='Collapsed'; $_.Handled=$true }
        elseif ($script:downloadPanelOpen) { Set-DownloadPanel $false; $_.Handled=$true }
    }
})
$window.Add_Closing({
    if (-not $_.Cancel -and -not ($SmokeTest -or $PlaybackTestFolder -or $PreviewLiveTest -or $PreviewImage -or $LiveSearchTest)) { Save-DesktopSettings }
})
Set-DownloadPanel $false
$window.Width = [math]::Min($window.Width, [Windows.SystemParameters]::WorkArea.Width)
$window.Height = [math]::Min($window.Height, [Windows.SystemParameters]::WorkArea.Height)
$script:desktopSearchHistory = @()
Restore-DesktopSettings
$window.Add_SizeChanged({ Update-DesktopDensity })

$window.FindName('LibrarySort').Add_SelectionChanged({ Apply-LibraryFilter })
$window.FindName('LibraryFileFormat').Add_SelectionChanged({ Apply-LibraryFilter })

# Local suggestions are event-driven: no extra requests or idle timers.
$ui.Query.Add_TextChanged({
    $text = $ui.Query.Text.Trim()
    $suggestions = $window.FindName('SearchSuggestions')
    if ($text.Length -lt 2 -or $text -match '^https?://') {
        $suggestions.Visibility = 'Collapsed'
        $ui.SearchSuggestionPopup.IsOpen = $false
        return
    }
    $candidates = @($script:desktopSearchHistory) + @($script:libraryTracksCache | ForEach-Object { $_.Title })
    $matches = @($candidates | Where-Object { $_ -and $_ -ne $text -and $_.IndexOf($text,[StringComparison]::OrdinalIgnoreCase) -ge 0 } | Select-Object -Unique -First 4)
    if ($matches.Count -lt 4) { $matches += @("$text music", "$text live", "$text acoustic") }
    $suggestions.ItemsSource = @($matches | Select-Object -Unique -First 5)
    $suggestions.Visibility = 'Visible'
    $ui.SearchSuggestionPopup.IsOpen = $ui.Query.IsKeyboardFocusWithin
})
$window.FindName('SearchSuggestions').Add_SelectionChanged({
    if ($this.SelectedItem) {
        $value = [string]$this.SelectedItem
        $ui.Query.Text = $value
        $this.Visibility = 'Collapsed'
        $ui.SearchSuggestionPopup.IsOpen = $false
        $ui.Query.Focus() | Out-Null
        Start-Search
    }
})
