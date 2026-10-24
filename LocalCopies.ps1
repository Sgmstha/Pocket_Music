# Match the stable [video-id] suffix written by the downloader, never just the title.
function Find-LocalCopy([string]$Id, $Files = $null) {
    if ($Id -notmatch '^[A-Za-z0-9_-]{11}$' -or -not [IO.Directory]::Exists($ui.Folder.Text)) { return $null }
    $suffix = '[' + $Id + ']'
    if ($null -eq $Files) { $Files = @(Get-ChildItem -LiteralPath $ui.Folder.Text -File -ErrorAction SilentlyContinue) }
    $file = $Files |
        Where-Object { $_.Extension.ToLowerInvariant() -in @('.mp3','.m4a','.opus','.flac','.wav') -and $_.BaseName.EndsWith($suffix,[StringComparison]::Ordinal) } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($file) { return [pscustomobject]@{Title=$file.BaseName;Path=$file.FullName} }
    return $null
}
function Get-SelectedVideoId {
    try { $url = ConvertTo-VideoUrl $ui.Url.Text; return $url.Substring($url.Length-11) } catch { return '' }
}
function Update-LocalCopyActions {
    $files = @()
    if ([IO.Directory]::Exists($ui.Folder.Text)) { $files = @(Get-ChildItem -LiteralPath $ui.Folder.Text -File -ErrorAction SilentlyContinue) }
    $ui.SelectedLocal.IsEnabled = $null -ne (Find-LocalCopy (Get-SelectedVideoId) $files)
    foreach ($track in $ui.SearchResults.ItemsSource) {
        $track | Add-Member -NotePropertyName LocalAvailable -NotePropertyValue ($null -ne (Find-LocalCopy $track.Id $files)) -Force
    }
    $ui.SearchResults.Items.Refresh()
    $ui.PreviewLocal.IsEnabled = $script:playbackSource -eq 'youtube' -and $null -ne $script:playerTrack -and $null -ne (Find-LocalCopy $script:playerTrack.Id $files)
}
function Play-LocalCopy([string]$Id) {
    $local = Find-LocalCopy $Id
    if (-not $local) { Update-LocalCopyActions; $ui.Status.Text = 'Local audio copy not found in the selected download folder. Refresh the library or choose its folder.'; return }
    Start-PlayerTrack $local
}
$ui['SelectedLocal'] = $window.FindName('SelectedLocal')
$ui.SelectedLocal.Add_Click({ Play-LocalCopy (Get-SelectedVideoId) })
$ui.PreviewLocal.Add_Click({ if ($script:playbackSource -eq 'youtube' -and $script:playerTrack) { Play-LocalCopy $script:playerTrack.Id } })
$ui.Url.Add_TextChanged({ Update-LocalCopyActions })
$ui.Folder.Add_TextChanged({ Update-LocalCopyActions })
