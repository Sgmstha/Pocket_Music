# Pocket Music metadata cleaner, inspired by Echo Music's CleanerRules & MetadataCleaner pipeline. GPL-3.0; see LICENSE.

function Get-CleanedMetadata {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RawTitle,
        [string]$RawArtist = ''
    )

    if ([string]::IsNullOrWhiteSpace($RawTitle)) {
        return [pscustomobject]@{
            RawTitle      = ''
            RawArtist     = $RawArtist
            CleanTitle    = ''
            PrimaryArtist = $RawArtist
            CleanArtist   = $RawArtist
        }
    }

    $title = $RawTitle.Trim()
    $artist = if ($RawArtist) { $RawArtist.Trim() } else { '' }

    # Replace HTML entities
    $title = $title.Replace('&amp;', '&').Replace('&quot;', '"').Replace('&#39;', "'")
    $artist = $artist.Replace('&amp;', '&').Replace('&quot;', '"').Replace('&#39;', "'")

    # Scrub channel suffixes from artist (e.g. "Ed Sheeran - Topic", "AlanWalkerVEVO", "Official")
    $artist = $artist -replace '(?i)\s*-\s*Topic$', '' -replace '(?i)VEVO$', '' -replace '(?i)\s+Official$', '' -replace '(?i)\s+Records$', ''

    # Detect "Artist - Title" or "Artist - 'Title'" pattern in the raw title
    if ($title -match '^\s*([^–\-:|]+?)\s*[\-–]\s*["'']?(.+?)["'']?\s*$') {
        $candidateArtist = $matches[1].Trim()
        $candidateTitle = $matches[2].Trim()
        if ($candidateArtist.Length -gt 1 -and $candidateArtist -notmatch '^\d+$') {
            $artist = $candidateArtist
            $title = $candidateTitle
        }
    }

    # Scrub junk tags using Echo Music's junk regex patterns
    $junkRegexes = @(
        '(?i)\s*\(.*?\b(official\s+music\s+video|official\s+video|official\s+audio|music\s+video|lyric\s+video|lyrics\s+video|official\s+visualizer|visualizer|video|audio|mv|pv|hd|hq|4k|1080p|720p|60fps|uhd)\b.*?\)',
        '(?i)\s*\[.*?\b(official\s+music\s+video|official\s+video|official\s+audio|music\s+video|lyric\s+video|lyrics\s+video|official\s+visualizer|visualizer|video|audio|mv|pv|hd|hq|4k|1080p|720p|60fps|uhd)\b.*?\]',
        '(?i)\s*【.*?\b(official|mv|pv|video|audio|lyrics)\b.*?】',
        '(?i)\s*「.*?\b(official|mv|pv|video|audio|lyrics)\b.*?」',
        '\s*\|.*$',
        '\s*//.*$',
        '\s*#\w+',
        '(?i)\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$'
    )
    foreach ($pattern in $junkRegexes) {
        $title = $title -replace $pattern, ''
    }

    # Final trim and normalization
    $title = ($title -replace '\s{2,}', ' ').Trim(' ', '-', '–', '|', '/')
    $artist = ($artist -replace '\s{2,}', ' ').Trim(' ', '-', '–', '|', '/')

    if ([string]::IsNullOrWhiteSpace($artist)) { $artist = if ($RawArtist) { $RawArtist.Trim() } else { 'YouTube' } }
    if ([string]::IsNullOrWhiteSpace($title)) { $title = $RawTitle.Trim() }

    [pscustomobject]@{
        RawTitle      = $RawTitle
        RawArtist     = $RawArtist
        CleanTitle    = $title
        PrimaryArtist = $artist
        CleanArtist   = $artist
    }
}
