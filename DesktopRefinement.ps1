# GPL-3.0. Presentation over the existing playback service; no second decoder, browser or timer.
. "$PSScriptRoot\LibraryFiles.ps1"
$script:focusSeeking=$false
$script:focusSync=$false
$script:focusView=[Windows.Markup.XamlReader]::Parse([IO.File]::ReadAllText((Join-Path $PSScriptRoot 'ListeningView.xaml')))
[Windows.Controls.Grid]::SetRowSpan($script:focusView,3)
[Windows.Controls.Panel]::SetZIndex($script:focusView,100)
$shell.Children.Add($script:focusView) | Out-Null
foreach($name in @('FocusClose','FocusDelete','FocusArt','FocusSource','FocusTitle','FocusArtist','FocusSeek','FocusElapsed','FocusDuration','FocusOrder','FocusShuffle','FocusPrevious','FocusToggle','FocusNext','FocusRepeat','FocusTimer','FocusQueue','FocusSound','FocusVolume','FocusStatus')) {$ui[$name]=$script:focusView.FindName($name)}
foreach($name in @('ExpandListening','DeleteLocalFile')) {$ui[$name]=$window.FindName($name)}
function Set-FocusedPlayer([bool]$Open) {
    if($Open -and $script:playback.source -ne 'local') {return}
    $script:focusView.Visibility=if($Open){'Visible'}else{'Collapsed'}
    $mainView.Visibility=if($Open){'Collapsed'}else{'Visible'}
    $playerView.Visibility=if($Open){'Collapsed'}else{'Visible'}
    if($Open){Sync-FocusedPlayer;$ui.FocusClose.Focus() | Out-Null}
}
function Sync-FocusedPosition {
    if($script:focusView.Visibility -ne 'Visible'){return}
    if(-not $script:focusSeeking){$ui.FocusSeek.Maximum=[math]::Max(1,$script:playback.duration);$ui.FocusSeek.Value=$script:playback.position;$ui.FocusElapsed.Text=Format-TimeDisplay $script:playback.position}
    $ui.FocusDuration.Text=Format-TimeDisplay $script:playback.duration
}
function Sync-FocusedPlayer {
    $local=$script:playback.source -eq 'local' -and $null -ne $script:playback.currentTrack
    $ui.ExpandListening.IsEnabled=$local
    if(-not $local -and $script:focusView.Visibility -eq 'Visible'){Set-FocusedPlayer $false;return}
    if($script:focusView.Visibility -ne 'Visible'){return}
    $track=$script:playback.currentTrack
    $ui.FocusTitle.Text=[string]$track.Title
    $ui.FocusArtist.Text=[string](Get-TrackProp $track 'Subtitle' 'Saved audio')
    $ui.FocusSource.Text=([IO.Path]::GetExtension([string](Get-TrackProp $track 'Path')).TrimStart('.').ToUpperInvariant())+'  ·  OFFLINE'
    $ui.FocusArt.Source=$ui.PlayerArtwork.Source
    $ui.FocusToggle.Content=if($script:playback.isPlaying){[string][char]0xE769}else{[string][char]0xE768}
    [Windows.Automation.AutomationProperties]::SetName($ui.FocusToggle,$(if($script:playback.isPlaying){'Pause'}else{'Play'}))
    foreach($pair in @(@('FocusToggle','PlayerToggle'),@('FocusPrevious','PlayerPrevious'),@('FocusNext','PlayerNext'),@('FocusSeek','PlayerSeek'))){$ui[$pair[0]].IsEnabled=$ui[$pair[1]].IsEnabled}
    $ui.FocusShuffle.Foreground=$ui.PlayerShuffle.Foreground;$ui.FocusShuffle.ToolTip=$ui.PlayerShuffle.ToolTip
    $ui.FocusRepeat.Foreground=$ui.PlayerRepeat.Foreground;$ui.FocusRepeat.ToolTip=$ui.PlayerRepeat.ToolTip
    [Windows.Automation.AutomationProperties]::SetName($ui.FocusShuffle,[string]$ui.PlayerShuffle.ToolTip)
    [Windows.Automation.AutomationProperties]::SetName($ui.FocusRepeat,[string]$ui.PlayerRepeat.ToolTip)
    $ui.FocusOrder.Text=([math]::Max(0,$script:playback.queueIndex+1)).ToString()+' / '+$script:playback.queue.Count
    $ui.FocusStatus.Text=$ui.PlayerStatus.Text
    Sync-FocusedPosition
    $script:focusSync=$true;try{$ui.FocusVolume.Value=$ui.PlayerVolume.Value}finally{$script:focusSync=$false}
}
function Remove-DeletedLibraryTrack([string]$Path) {
    $currentPath=[string](Get-TrackProp $script:playback.currentTrack 'Path')
    $wasCurrent=$currentPath.Equals($Path,[StringComparison]::OrdinalIgnoreCase)
    for($i=$script:playback.queue.Count-1;$i -ge 0;$i--){if(([string](Get-TrackProp $script:playback.queue[$i] 'Path')).Equals($Path,[StringComparison]::OrdinalIgnoreCase)){$script:playback.queue.RemoveAt($i);if($i -lt $script:playback.queueIndex){$script:playback.queueIndex--}}}
    if($wasCurrent){$script:playback.currentTrack=$null;$script:playerTrack=$null;$script:playback.source='none';$script:playback.queueIndex=-1;$script:playback.position=0;$script:playback.duration=0;$ui.PlayerTitle.Text='Choose your next track';$ui.PlayerElapsed.Text='0:00';$ui.PlayerDuration.Text='0:00';$ui.PlayerSeek.Value=0;$ui.PlayerArtwork.Source=$null}
    $script:playerQueue=@($script:playback.queue);$script:playerIndex=$script:playback.queueIndex
    if($script:playback.isShuffled){Rebuild-ShuffleOrder}
    Set-PlaybackUIState $(if($wasCurrent){'idle'}else{$script:playerState}) $(if($wasCurrent){'File removed from library'}else{$ui.PlayerStatus.Text})
    Save-PlaybackSession
}
function Request-LibraryDelete($Track) {
    try{
        $path=Resolve-LibraryFile ([string](Get-TrackProp $Track 'Path')) $ui.Folder.Text
        if($script:job -and -not $script:job.HasExited){throw 'Wait for the download to finish or cancel it before deleting files.'}
        $answer=[Windows.MessageBox]::Show($window,"Send this file to the Recycle Bin?`n`n$([IO.Path]::GetFileName($path))`n$([IO.Path]::GetDirectoryName($path))`n`nOther copies are kept. Windows handles recycling and may ask for permanent deletion on drives that cannot recycle files.",'Delete library file','YesNo','Warning','No')
        if($answer -ne 'Yes'){return}
        $path=Resolve-LibraryFile $path $ui.Folder.Text
        if(([string](Get-TrackProp $script:playback.currentTrack 'Path')).Equals($path,[StringComparison]::OrdinalIgnoreCase)){Stop-LocalCore;Set-PlaybackUIState 'paused' 'Removing file…'}
        Remove-LibraryFile $path $ui.Folder.Text
        Remove-DeletedLibraryTrack $path
        Update-Library
    }catch [OperationCanceledException]{ $ui.LibraryStatus.Text='Deletion cancelled. Your file was kept.' }
    catch{$ui.LibraryStatus.Text=$_.Exception.Message;[Windows.MessageBox]::Show($window,$_.Exception.Message,'Could not delete file','OK','Warning') | Out-Null}
}
function Open-LibraryTrackMenu($Track,$Anchor) {
    if(-not $Track){return}
    $menu=New-Object Windows.Controls.ContextMenu
    $menu.Background='#172133';$menu.Foreground='#F1F5FF'
    foreach($action in @('Play','Play next','Add to queue','Show in Explorer','Delete file…')){
        $item=New-Object Windows.Controls.MenuItem;$item.Header=$action;$item.Tag=@{Action=$action;Track=$Track}
        $item.Add_Click({$track=$this.Tag.Track;switch($this.Tag.Action){'Play'{Open-LocalTrack $track @($ui.LibraryTracks.ItemsSource)} 'Play next'{Play-NextInQueue $track} 'Add to queue'{Enqueue-Track $track} 'Show in Explorer'{$path=Resolve-LibraryFile ([string](Get-TrackProp $track 'Path')) $ui.Folder.Text;Start-Process explorer.exe -ArgumentList ('/select,"'+$path+'"')} default{Request-LibraryDelete $track}}})
        if($action -eq 'Delete file…'){$item.Foreground='#FFB8C8'}
        $menu.Items.Add($item) | Out-Null
    }
    $menu.PlacementTarget=$Anchor;$menu.IsOpen=$true
}
$ui.DeleteLocalFile.Add_Click({Request-LibraryDelete $ui.LibraryTracks.SelectedItem})
$ui.LibraryTracks.Add_SelectionChanged({$ui.DeleteLocalFile.IsEnabled=$null -ne $ui.LibraryTracks.SelectedItem})
foreach($list in @($ui.LibraryTracks,$ui.RecentTracks)){
    $list.AddHandler([Windows.Controls.Button]::ClickEvent,[Windows.RoutedEventHandler]{param($sender,$e)$button=$e.OriginalSource;while($button -and $button -isnot [Windows.Controls.Button]){$button=[Windows.Media.VisualTreeHelper]::GetParent($button)}if($button -and $button.Name -eq 'RowMore'){$e.Handled=$true;Open-LibraryTrackMenu $button.DataContext $button}})
}
$ui.FocusClose.Add_Click({Set-FocusedPlayer $false})
$ui.ExpandListening.Add_Click({Set-FocusedPlayer $true})
$ui.PlayerArtBorder.Cursor='Hand';$ui.PlayerArtBorder.ToolTip='Open focused player (saved audio)'
$ui.PlayerArtBorder.Add_MouseLeftButtonUp({Set-FocusedPlayer $true})
$ui.FocusDelete.Add_Click({Open-LibraryTrackMenu $script:playback.currentTrack $ui.FocusDelete})
$ui.FocusToggle.Add_Click({Toggle-PlayPause})
$ui.FocusPrevious.Add_Click({Step-PlaybackTrack -1})
$ui.FocusNext.Add_Click({Step-PlaybackTrack 1})
$ui.FocusShuffle.Add_Click({Toggle-Shuffle})
$ui.FocusRepeat.Add_Click({Cycle-RepeatMode})
$ui.FocusQueue.Add_Click({Set-FocusedPlayer $false;Toggle-QueueView})
$ui.FocusTimer.Add_Click({$ui.PlayerSleepBtn.ContextMenu.PlacementTarget=$ui.FocusTimer;$ui.PlayerSleepBtn.ContextMenu.IsOpen=$true})
$ui.FocusSound.Add_Click({$ui.FocusVolume.Visibility=if($ui.FocusVolume.Visibility -eq 'Visible'){'Collapsed'}else{'Visible'}})
$ui.FocusVolume.Add_ValueChanged({if(-not $script:focusSync){$ui.PlayerVolume.Value=$ui.FocusVolume.Value}})
$ui.PlayerVolume.Add_ValueChanged({if(-not $script:focusSync){$script:focusSync=$true;try{$ui.FocusVolume.Value=$ui.PlayerVolume.Value}finally{$script:focusSync=$false}}})
$ui.FocusSeek.Add_PreviewMouseLeftButtonDown({$script:focusSeeking=$true})
$ui.FocusSeek.Add_PreviewMouseLeftButtonUp({Seek-Playback $ui.FocusSeek.Value;$script:focusSeeking=$false;Sync-FocusedPosition})
$ui.FocusSeek.Add_LostMouseCapture({if($script:focusSeeking){Seek-Playback $ui.FocusSeek.Value;$script:focusSeeking=$false;Sync-FocusedPosition}})
$ui.FocusSeek.Add_PreviewKeyDown({$script:focusSeeking=$true})
$ui.FocusSeek.Add_KeyUp({Seek-Playback $ui.FocusSeek.Value;$script:focusSeeking=$false;Sync-FocusedPosition})
$window.Add_PreviewKeyDown({if($_.Key -eq 'Escape' -and $script:focusView.Visibility -eq 'Visible'){Set-FocusedPlayer $false;$_.Handled=$true}})
