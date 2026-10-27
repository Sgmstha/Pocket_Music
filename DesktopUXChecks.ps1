# Runs in the actual WPF window from DesktopChecks.ps1.
$savedUXFolder = $ui.Folder.Text
$savedUXFormat = $ui.Format.SelectedIndex
$savedUXFilter = $ui.LibraryFilter.Text
$savedUXCache = $script:libraryTracksCache
$savedUXRecent = $ui.RecentTracks.ItemsSource
$savedUXSettingsPath = $script:desktopSettingsPath
$savedUXOpen = (Get-Item Function:Open-LocalTrack).ScriptBlock
$savedUXPreview = (Get-Item Function:Start-PreviewTrack).ScriptBlock
$savedUXSearch = (Get-Item Function:Start-Search).ScriptBlock
$settingsFixture = Join-Path ([IO.Path]::GetTempPath()) ('PocketMusic-ux-' + [guid]::NewGuid() + '.json')
try {
    Verify ([PocketMusic.WindowsIdentity]::ProcessId() -eq 'PocketMusic.Desktop') 'Desktop process still has the host identity'
    Verify ([PocketMusic.WindowsIdentity]::WindowProperty($script:desktopWindowHandle,5) -eq 'PocketMusic.Desktop') 'Desktop taskbar window identity missing'
    Verify ([PocketMusic.WindowsIdentity]::WindowProperty($script:desktopWindowHandle,3) -eq ((Join-Path $PSScriptRoot 'PocketMusic.ico') + ',0')) 'Taskbar relaunch icon does not point to the app icon'
    Verify ([PocketMusic.WindowsIdentity]::WindowProperty($script:desktopWindowHandle,2) -eq ('"' + (Join-Path $PSScriptRoot 'Pocket Music.exe') + '"')) 'Pinned app relaunch would launch the host instead of Pocket Music'
    Verify ([PocketMusic.WindowsIdentity]::SendMessage($script:desktopWindowHandle,0x7f,[IntPtr]1,[IntPtr]::Zero) -ne [IntPtr]::Zero) 'Native window icon is missing'
    Verify ($ui.HomeHeroArt.Background -is [Windows.Media.ImageBrush] -and $ui.HomeHeroArt.Background.ImageSource.IsFrozen) 'Home artwork did not load as a static local image'
    $ui.HeroBrowse.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($ui.SearchView.Visibility -eq 'Visible') 'Home Find music action failed'
    $ui.HeroLibrary.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($ui.LibraryView.Visibility -eq 'Visible') 'Home Open library action failed'
    Set-DownloadPanel $false
    Verify ($ui.DownloadPanel.Visibility -eq 'Collapsed' -and $ui.DownloadColumn.Width.Value -eq 0) 'Closed download panel still occupies space'
    $ui.ToggleDownloads.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($ui.DownloadPanel.Visibility -eq 'Visible' -and $ui.DownloadColumn.Width.Value -eq 304) 'Downloads toggle failed'
    $ui.CloseDownloads.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify (-not $script:downloadPanelOpen) 'Download panel close failed'

    Set-Item Function:script:Open-LocalTrack -Value { param($Track) $script:uxPlayedTrack = $Track }
    $script:uxPlayedTrack = $null
    $script:libraryTracksCache = @(
        [pscustomobject]@{Title='Piano [1]'; Subtitle='MP3'; Path='piano.mp3'; Duration=''; Artwork=$null},
        [pscustomobject]@{Title='Quiet jazz'; Subtitle='MP3'; Path='jazz.mp3'; Duration=''; Artwork=$null}
    )
    $ui.RecentTracks.ItemsSource = $script:libraryTracksCache
    Show-Page 'Home'
    $window.UpdateLayout()
    function Find-RecentPlayButton($visual) {
        if ($visual -is [Windows.Controls.Button] -and $visual.Name -eq 'RecentPlay') { return $visual }
        for ($child=0; $child -lt [Windows.Media.VisualTreeHelper]::GetChildrenCount($visual); $child++) {
            $found = Find-RecentPlayButton ([Windows.Media.VisualTreeHelper]::GetChild($visual,$child))
            if ($found) { return $found }
        }
        return $null
    }
    $recentPlayButton = Find-RecentPlayButton $ui.RecentTracks
    Verify ($null -ne $recentPlayButton) 'Recent-file play button was not rendered'
    $recentPlayButton.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($script:uxPlayedTrack.Title -eq 'Piano [1]') 'Recent-file tile did not invoke local playback'
    $script:uxPlayedTrack = $null
    $ui.LibraryFilter.Text = '[1]'
    Verify ($ui.LibraryTracks.Items.Count -eq 1 -and $ui.LibraryTracks.Items[0].Title -eq 'Piano [1]') 'Library filter must match text literally'
    $ui.LibraryTracks.SelectedIndex = 0
    Verify ($null -eq $script:uxPlayedTrack -and $ui.PlayLocal.IsEnabled) 'Selecting a library track must not start playback'
    $ui.PlayLocal.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($script:uxPlayedTrack.Title -eq 'Piano [1]') 'Play selected failed'
    $script:uxPlayedTrack = $null
    $action = [pscustomobject]@{OriginalSource=$ui.LibraryTracks; Handled=$false}
    Invoke-TrackDefaultAction $ui.LibraryTracks $action
    Verify ($action.Handled -and $script:uxPlayedTrack.Title -eq 'Piano [1]') 'Library default play action failed'
    $ui.LibraryFilter.Text = 'nothing matches'
    Verify ($ui.LibraryEmpty.Visibility -eq 'Visible' -and $ui.LibraryEmptyTitle.Text -eq 'No matching tracks' -and -not $ui.PlayAllLocal.IsEnabled) 'Filtered empty library state failed'
    $ui.LibraryClearFilter.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($ui.LibraryTracks.Items.Count -eq 2 -and $ui.LibraryEmpty.Visibility -eq 'Collapsed') 'Clear library filter failed'
    $sort = $window.FindName('LibrarySort')
    $sort.SelectedIndex = 2
    Verify ($ui.LibraryTracks.Items[0].Title -eq 'Quiet jazz') 'Descending library sort failed'
    $sort.SelectedIndex = 1
    Verify ($ui.LibraryTracks.Items[0].Title -eq 'Piano [1]') 'Ascending library sort failed'
    $sort.SelectedIndex = 0
    Show-Page 'Home'
    Verify ($window.FindName('SearchBar').Visibility -eq 'Visible') 'Global search must remain available on Home'
    Show-Page 'Search'
    Verify ($window.FindName('SearchBar').Visibility -eq 'Visible') 'Search page has no search bar'
    $ui.Query.Text = 'Piano'
    Verify ($window.FindName('SearchSuggestions').Items -contains 'Piano [1]') 'Search suggestions omitted a matching local track'
    Set-Item Function:script:Start-Search -Value { $script:uxSearchedText = $ui.Query.Text }
    $script:uxSearchedText = $null
    $window.FindName('SearchSuggestions').SelectedIndex = 0
    Verify ($script:uxSearchedText -eq 'Piano [1]' -and $window.FindName('SearchSuggestions').Visibility -eq 'Collapsed') 'Suggestion selection did not run the selected search'
    $window.FindName('BrowseFocus').RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($script:uxSearchedText -eq 'lofi focus music') 'Search browse tile did not execute its music query'
    Set-Item Function:script:Start-Search -Value $savedUXSearch
    $ui.Query.Text = 'https://youtu.be/abcdefghijk'
    Verify ($window.FindName('SearchSuggestions').Visibility -eq 'Collapsed') 'URLs must not receive query expansions'
    $ui.Query.Clear()
    Set-SleepTimer '45'
    Verify ($script:playback.sleepTimerType -eq '45' -and $script:playback.sleepTimerEnd -gt [DateTime]::UtcNow.AddMinutes(44)) 'Android-compatible sleep preset failed'
    Set-SleepTimer 'off'
    Verify ($null -eq $script:playback.sleepTimerEnd) 'Sleep timer cancellation failed'
    [Windows.Input.FocusManager]::SetFocusedElement($window,$ui.LibraryTracks)
    $key = [pscustomobject]@{Key='Down'; Handled=$false}
    Handle-PlaybackGlobalKey $key
    Verify (-not $key.Handled) 'Playback shortcuts intercepted list navigation'

    Show-SearchResults '{"entries":[]}'
    Verify ($ui.SearchEmpty.Visibility -eq 'Visible' -and $ui.SearchEmptyTitle.Text -eq 'No results this time') 'Search empty state failed'
    Update-SearchPresentation 'loading'
    Verify ($ui.SearchActivity.Visibility -eq 'Visible') 'Search loading state missing'
    Verify ($window.FindName('SearchBrowse').Visibility -eq 'Collapsed') 'Browse tiles must not compete with a loading search'
    Stop-Search
    Verify ($ui.SearchActivity.Visibility -eq 'Collapsed') 'Search cancellation left loading indicator running'

    $script:desktopSettingsPath = $settingsFixture
    $ui.Folder.Text = $PSScriptRoot
    $ui.Format.SelectedIndex = 2
    Save-DesktopSettings
    $ui.Folder.Text = $savedUXFolder
    $ui.Format.SelectedIndex = 0
    Restore-DesktopSettings
    Verify ($ui.Folder.Text -eq $PSScriptRoot -and $ui.Format.SelectedIndex -eq 2) 'Folder or format settings were not restored'
    [IO.File]::WriteAllText($settingsFixture,'invalid json')
    Restore-DesktopSettings # A corrupted preference file must not prevent startup.
    $studioArt = Get-StudioCover 'Piano [1]'
    Verify ((Test-Path -LiteralPath $studioArt) -and (Get-StudioCover 'Piano [1]') -eq $studioArt) 'Original fallback artwork must use a cached local asset'
    Update-StudioLibrary
    Verify ($ui.SidebarTracks.Items.Count -eq 2 -and $ui.HomeAlbums.Items.Count -eq 2) 'Persistent library or offline shelf did not reflect the library cache'
    $ui.SidebarFilter.Text = '[1]'
    Verify ($ui.SidebarTracks.Items.Count -eq 1 -and $ui.SidebarTracks.Items[0].Title -eq 'Piano [1]') 'Sidebar filter must match literal text'
    $ui.SidebarFilter.Clear()
    $ui.SidebarTracks.SelectedIndex = 0
    $script:uxPlayedTrack = $null
    $sidebarAction = [pscustomobject]@{OriginalSource=$ui.SidebarTracks; Handled=$false}
    Invoke-TrackDefaultAction $ui.SidebarTracks $sidebarAction
    Verify ($sidebarAction.Handled -and $script:uxPlayedTrack.Title -eq 'Piano [1]') 'Sidebar default action did not reuse local playback'
    Sync-StudioPlayback
    Verify ($ui.NowTitle.Text -eq $script:playback.currentTrack.Title) 'Now-playing title is out of sync with the existing player'
    Set-DownloadPanel $true
    Verify ($ui.NowPlayingPanel.Visibility -eq 'Collapsed') 'Downloads and now playing must not squeeze the centre simultaneously'
    Set-DownloadPanel $false
    $window.Width = $window.MinWidth
    $window.UpdateLayout()
    Update-StudioDensity
    Verify ($ui.NowPlayingPanel.Visibility -eq 'Collapsed') 'Optional player must collapse on a small display'
    $ui.ToggleNowPlaying.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($ui.NowPlayingPanel.Visibility -eq 'Visible' -and $window.FindName('NavigationColumn').Width.Value -eq 88) 'Explicit now-playing toggle must work on a small display'
    $ui.CloseNowPlaying.RaiseEvent((New-Object Windows.RoutedEventArgs ([Windows.Controls.Button]::ClickEvent)))
    Verify ($ui.NowPlayingPanel.Visibility -eq 'Collapsed') 'Now-playing close action failed'
    if ($PreviewImage) {
        function Save-UXSnapshot([string]$Suffix) {
            $window.UpdateLayout()
            $window.Dispatcher.Invoke([Action]{}, [Windows.Threading.DispatcherPriority]::ContextIdle)
            $bitmap = New-Object Windows.Media.Imaging.RenderTargetBitmap ([int]$window.Content.ActualWidth), ([int]$window.Content.ActualHeight), 96, 96, ([Windows.Media.PixelFormats]::Pbgra32)
            $bitmap.Render($window.Content)
            $encoder = New-Object Windows.Media.Imaging.PngBitmapEncoder
            $encoder.Frames.Add([Windows.Media.Imaging.BitmapFrame]::Create($bitmap))
            $target = Join-Path (Split-Path $PreviewImage) (([IO.Path]::GetFileNameWithoutExtension($PreviewImage)) + '-' + $Suffix + '.png')
            $file = [IO.File]::Create($target)
            try { $encoder.Save($file) } finally { $file.Dispose() }
        }
        # Visual fixtures only: the live app never seeds or invents library tracks.
        $script:libraryTracksCache = @('Night bus','Slow morning','Blue hour','Open water','After the rain','Sunday radio' | ForEach-Object {
            [pscustomobject]@{Title=$_;Subtitle='Local audio';Path='visual-fixture.mp3';Duration='';Artwork=(Get-StudioCover $_)}
        })
        $ui.RecentTracks.ItemsSource = $script:libraryTracksCache
        $ui.RecentEmpty.Visibility = 'Collapsed'
        $ui.HomeLibraryCount.Text = '6 saved tracks, ready whenever you are'
        Update-StudioLibrary
        function Find-StudioImage($visual) {
            if ($visual -is [Windows.Controls.Image]) { return $visual }
            for ($v=0; $v -lt [Windows.Media.VisualTreeHelper]::GetChildrenCount($visual); $v++) {
                $found = Find-StudioImage ([Windows.Media.VisualTreeHelper]::GetChild($visual,$v))
                if ($found) { return $found }
            }
            return $null
        }
        Show-Page 'Home'
        $window.Width = [math]::Min(1620, [Windows.SystemParameters]::WorkArea.Width)
        $window.Height = [math]::Min(1000, [Windows.SystemParameters]::WorkArea.Height)
        Set-NowPlayingPanel $true
        $window.UpdateLayout()
        $window.Dispatcher.Invoke([Action]{}, [Windows.Threading.DispatcherPriority]::ContextIdle)
        $probeImage=Find-StudioImage $ui.SidebarTracks
        Verify ($probeImage.Source -and $probeImage.Source.IsFrozen) 'Saved-track artwork did not render as a cached frozen bitmap'
        Save-UXSnapshot 'home-wide'
        Show-Page 'Library'
        $ui.LibraryFilter.Text = 'piano'
        Save-UXSnapshot 'library'
        Show-Page 'Search'
        Show-SearchResults '{"entries":[{"id":"abcdefghijk","title":"A quiet piano session","channel":"Example artist","duration":251}]}'
        $ui.SearchResults.SelectedIndex = 0
        Set-DownloadPanel $true
        $ui.Status.Text = 'Ready when you are'
        Save-UXSnapshot 'downloads'
        $window.Width = $window.MinWidth
        $window.Height = $window.MinHeight
        Save-UXSnapshot 'compact'
    }
} finally {
    Set-Item Function:script:Open-LocalTrack -Value $savedUXOpen
    Set-Item Function:script:Start-PreviewTrack -Value $savedUXPreview
    Set-Item Function:script:Start-Search -Value $savedUXSearch
    $script:desktopSettingsPath = $savedUXSettingsPath
    Remove-Item -LiteralPath $settingsFixture -ErrorAction SilentlyContinue
    $ui.Folder.Text = $savedUXFolder
    $ui.Format.SelectedIndex = $savedUXFormat
    $script:libraryTracksCache = $savedUXCache
    $ui.RecentTracks.ItemsSource = $savedUXRecent
    $ui.LibraryFilter.Text = $savedUXFilter
    Apply-LibraryFilter
    Update-StudioLibrary
    Set-DownloadPanel $false
    Show-Page 'Home'
}
Write-Host "PASS: $script:testCount desktop checks including the new UX flows."
