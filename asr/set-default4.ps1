$ProgressPreference='SilentlyContinue'
$ErrorActionPreference = 'Stop'
$dir = 'D:\bs_finally\asr\audioswitcher'
New-Item -ItemType Directory -Force -Path $dir | Out-Null

function Get-LatestVersion($id) {
  try {
    $j = Invoke-RestMethod "https://api.nuget.org/v3-flatcontainer/$($id.ToLower())/index.json" -TimeoutSec 30
    $vers = @($j.versions) | Where-Object { $_ -notmatch '-' }
    if ($vers.Count -eq 0) { $vers = @($j.versions) }
    return $vers | Select-Object -Last 1
  } catch { return $null }
}
function Find-Dll($root, $name) {
  $d = Get-ChildItem $root -Recurse -Filter $name | Where-Object { $_.FullName -match 'net4' } | Select-Object -First 1
  if (-not $d) { $d = Get-ChildItem $root -Recurse -Filter $name | Select-Object -First 1 }
  return $d.FullName
}
function Get-Lib($pkgId, $dllName) {
  $pkgRoot = "$dir\$pkgId"
  if (Test-Path "$pkgRoot\lib") {
    return (Find-Dll $pkgRoot $dllName)
  }
  $ver = Get-LatestVersion $pkgId
  if (-not $ver) { throw "cannot get version of $pkgId" }
  $zip = "$dir\$pkgId.nupkg"
  $ok = $false
  $urls = @(
    "https://www.nuget.org/api/v2/package/$pkgId/$ver",
    "https://api.nuget.org/v3-flatcontainer/$($pkgId.ToLower())/$ver/$($pkgId.ToLower()).$ver.nupkg"
  )
  foreach ($u in $urls) {
    try {
      Invoke-WebRequest $u -OutFile $zip -UseBasicParsing -TimeoutSec 60
      if ((Test-Path $zip) -and (Get-Item $zip).Length -gt 5000) { $ok = $true; Write-Host "downloaded $pkgId $ver"; break }
    } catch { "download failed: $u" }
  }
  if (-not $ok) { throw "cannot download $pkgId" }
  Copy-Item $zip "$zip.zip" -Force
  Expand-Archive "$zip.zip" $pkgRoot -Force
  return (Find-Dll $pkgRoot $dllName)
}

# NAudio first (possible dependency of AudioSwitcher)
$naudioDir = 'D:\bs_finally\asr\naudio'
Add-Type -Path (Find-Dll "$naudioDir\NAudio.Core" 'NAudio.Core.dll') -ErrorAction SilentlyContinue
Add-Type -Path (Find-Dll "$naudioDir\NAudio.Wasapi" 'NAudio.Wasapi.dll') -ErrorAction SilentlyContinue

Add-Type -Path (Get-Lib 'AudioSwitcher.AudioApi' 'AudioSwitcher.AudioApi.dll')
Add-Type -Path (Get-Lib 'AudioSwitcher.AudioApi.CoreAudio' 'AudioSwitcher.AudioApi.CoreAudio.dll')

$ctrl = New-Object AudioSwitcher.AudioApi.CoreAudio.CoreAudioController
"devices:"
foreach ($d in $ctrl.Devices) { "  ID=[" + $d.ID + "] Name=[" + $d.Name + "]" }

# Intel speaker / mic IDs from live enumeration on this machine
$render = $ctrl.Devices | Where-Object { $_ -and $_.ID -and ($_.ID.ToString() -like '*fdfc57d0-4166-4200-9835-c8e7af5d41ed*') } | Select-Object -First 1
if (-not $render) { $render = $ctrl.Devices | Where-Object { $_ -and $_.Name -and ($_.Name -like '*SoundWire*') } | Select-Object -First 1 }
$capture = $ctrl.Devices | Where-Object { $_ -and $_.ID -and ($_.ID.ToString() -like '*ee809192-8e3d-437f-adf8-aef4e1dfea68*') } | Select-Object -First 1
if (-not $capture) {
  $capture = $ctrl.Devices | Where-Object {
    $_ -and $_.ID -and
    ($_.ID.ToString() -notlike '*5501256e-2a60-4ca8-9c00-7c9b4d423a4b*') -and
    ($_.ID.ToString() -notlike '*fdfc57d0-4166-4200-9835-c8e7af5d41ed*') -and
    ($_.ID.ToString() -notlike '*2e4a210f-ae9f-40cf-b7f3-e0d2da695707*')
  } | Select-Object -First 1
}
if (-not $render) { "ERROR: render target not found"; exit 1 }
if (-not $capture) { "ERROR: capture target not found"; exit 1 }

$ctrl.DefaultPlaybackDevice = $render
"set default playback: $($render.Name)"
$ctrl.DefaultCaptureDevice = $capture
"set default capture: $($capture.Name)"

""
"verify:"
"current default playback: $($ctrl.DefaultPlaybackDevice.Name)"
"current default capture:   $($ctrl.DefaultCaptureDevice.Name)"
