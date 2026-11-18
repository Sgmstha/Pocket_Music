# Pocket Music lyrics service powered by LRCLIB (GPL-3.0; see LICENSE).
if (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'MetadataCleaner.ps1')) {
    . (Join-Path $PSScriptRoot 'MetadataCleaner.ps1')
}

$script:lyricsCache = @{}

function Parse-LrcLines([string]$LrcText) {
    $lines = @()
    if ([string]::IsNullOrWhiteSpace($LrcText)) { return $lines }
    
    foreach ($rawLine in ($LrcText -split "`r?`n")) {
        $trimmed = $rawLine.Trim()
        if ($trimmed -match '^\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]\s*(.*)$') {
            $mins = [int]$matches[1]
            $secs = [int]$matches[2]
            $fraction = if ($matches[3]) { [double]("0." + $matches[3]) } else { 0.0 }
            $totalSeconds = ($mins * 60) + $secs + $fraction
            $text = $matches[4].Trim()
            $lines += [pscustomobject]@{
                Seconds = $totalSeconds
                Time    = [TimeSpan]::FromSeconds($totalSeconds)
                Text    = $text
            }
        }
    }
    return @($lines | Sort-Object Seconds)
}

function Get-TrackLyrics {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Title,
        [string]$Artist = '',
        [double]$Duration = 0
    )

    $meta = if (Get-Command 'Get-CleanedMetadata' -ErrorAction SilentlyContinue) {
        Get-CleanedMetadata $Title $Artist
    } else {
        [pscustomobject]@{ CleanTitle = $Title; PrimaryArtist = $Artist }
    }

    $cleanTitle = $meta.CleanTitle
    $cleanArtist = $meta.PrimaryArtist
    if ([string]::IsNullOrWhiteSpace($cleanTitle)) { return $null }

    $cacheKey = "$cleanTitle::$cleanArtist".ToLowerInvariant()
    if ($script:lyricsCache.ContainsKey($cacheKey)) {
        return $script:lyricsCache[$cacheKey]
    }

    $result = [pscustomobject]@{
        Found        = $false
        TrackName    = $cleanTitle
        ArtistName   = $cleanArtist
        SyncedLyrics = @()
        PlainLyrics  = ''
    }

    try {
        $encodedTitle = [Uri]::EscapeDataString($cleanTitle)
        $encodedArtist = if ($cleanArtist -and $cleanArtist -ne 'YouTube') { [Uri]::EscapeDataString($cleanArtist) } else { '' }
        $url = "https://lrclib.net/api/get?track_name=$encodedTitle"
        if ($encodedArtist) { $url += "&artist_name=$encodedArtist" }
        if ($Duration -gt 5) { $url += "&duration=$([int]$Duration)" }

        $response = $null
        try {
            $response = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 4 -UserAgent 'PocketMusic/1.0' -ErrorAction Stop
        } catch {
            # Fallback to search endpoint if exact match misses
            $searchQuery = [Uri]::EscapeDataString(("$cleanTitle $cleanArtist").Trim())
            $searchUrl = "https://lrclib.net/api/search?q=$searchQuery"
            $searchList = Invoke-RestMethod -Uri $searchUrl -Method Get -TimeoutSec 4 -UserAgent 'PocketMusic/1.0' -ErrorAction Stop
            if ($searchList -and @($searchList).Count -gt 0) {
                $response = $searchList[0]
            }
        }

        if ($response) {
            $parsedSynced = Parse-LrcLines ([string]$response.syncedLyrics)
            $plain = [string]$response.plainLyrics
            if (-not $plain -and $parsedSynced.Count -gt 0) {
                $plain = ($parsedSynced | ForEach-Object { $_.Text }) -join "`r`n"
            }

            if ($parsedSynced.Count -gt 0 -or -not [string]::IsNullOrWhiteSpace($plain)) {
                $result.Found = $true
                $result.TrackName = [string]$response.trackName
                $result.ArtistName = [string]$response.artistName
                $result.SyncedLyrics = $parsedSynced
                $result.PlainLyrics = $plain
            }
        }
    } catch {
        # Offline or service unavailable; return clean not-found state without crashing
    }

    $script:lyricsCache[$cacheKey] = $result
    return $result
}
