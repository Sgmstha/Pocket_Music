# Pocket Music desktop adaptation. GPL-3.0; see NOTICE.md and LICENSE.
function Get-SearchArguments([string]$Query) {
    $Query = $Query.Trim()
    if (-not $Query -or $Query.Length -gt 200) { throw 'Enter a song or artist name (up to 200 characters).' }
    return @('--ignore-config','--no-colors','--flat-playlist','--skip-download','--dump-single-json',
        '--socket-timeout','15','--retries','0','--extractor-retries','0','--sleep-requests','1') + @(Get-YouTubeAuthArguments 'search') + @('--',"ytsearch12:$Query")
}
function Get-OptionalValue($Object, [string]$Name, $Fallback = '') {
    $property = $Object.PSObject.Properties[$Name]
    if ($property -and $null -ne $property.Value) { return $property.Value }
    return $Fallback
}
function ConvertFrom-SearchResults([string]$Json) {
    $data = $Json | ConvertFrom-Json
    $seen = @{}
    foreach ($entry in @(Get-OptionalValue $data 'entries' @())) {
        if ($null -eq $entry) { continue }
        $id = [string](Get-OptionalValue $entry 'id')
        if ($id -notmatch '^[A-Za-z0-9_-]{11}$' -or $seen.ContainsKey($id)) { continue }
        $seen[$id] = $true
        $title = [string](Get-OptionalValue $entry 'title' 'Untitled video')
        $artist = [string](Get-OptionalValue $entry 'channel' (Get-OptionalValue $entry 'uploader' 'YouTube'))
        $seconds = Get-OptionalValue $entry 'duration' 0
        $duration = ''
        if ($seconds -is [ValueType] -and $seconds -gt 0) {
            $span = [TimeSpan]::FromSeconds([double]$seconds)
            $duration = if ($span.TotalHours -ge 1) { $span.ToString('h\:mm\:ss') } else { $span.ToString('m\:ss') }
        }
        $artwork = $null
        foreach ($thumb in @(Get-OptionalValue $entry 'thumbnails' @())) {
            $candidate = [string](Get-OptionalValue $thumb 'url')
            $uri = $null
            if ([Uri]::TryCreate($candidate, [UriKind]::Absolute, [ref]$uri) -and $uri.Scheme -eq 'https' -and $uri.Host -in @('i.ytimg.com','i9.ytimg.com','img.youtube.com')) { $artwork = $candidate; break }
        }
        [pscustomobject]@{Id=$id; Title=$title; Subtitle=$artist; Duration=$duration; Artwork=$artwork; Url="https://www.youtube.com/watch?v=$id"}
    }
}
