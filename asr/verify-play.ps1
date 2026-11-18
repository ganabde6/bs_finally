# verify-play.ps1 - prove whether audio actually reaches a render endpoint
$ErrorActionPreference = 'Continue'
Add-Type -Path 'D:/bs_finally/asr/naudio/NAudio.Core/lib/netstandard2.0/NAudio.Core.dll'
Add-Type -Path 'D:/bs_finally/asr/naudio/NAudio.Wasapi/lib/netstandard2.0/NAudio.Wasapi.dll'

$enum = New-Object NAudio.CoreAudioApi.MMDeviceEnumerator
$render = [NAudio.CoreAudioApi.DataFlow]::Render
Write-Output '=== RENDER ENDPOINTS (active) ==='
$devices = $enum.EnumerateAudioEndPoints($render, [NAudio.CoreAudioApi.DeviceState]::Active)
foreach ($d in $devices) {
  Write-Output ('{0} | id={1}' -f $d.FriendlyName, $d.ID)
}

function Get-Peak($d) {
  try { return [math]::Round($d.AudioMeterInformation.MasterPeakValue, 4) } catch { return -1 }
}

Write-Output '=== DEFAULT ENDPOINTS ==='
foreach ($roleName in @('Console','Communications','MoviesAndGames')) {
  $role = [NAudio.CoreAudioApi.AudioRole]$roleName
  try {
    $def = $enum.GetDefaultAudioEndpoint($render, $role)
    Write-Output ("DEFAULT[{0}] = {1}" -f $roleName, $def.FriendlyName)
  } catch {
    Write-Output ("DEFAULT[{0}] = <none> ({1})" -f $roleName, $_.Exception.Message)
  }
}

# start playback via WinMM (routes to default playback endpoint)
$py = 'E:/python/app/python.exe'
$code = "import winsound; winsound.PlaySound(r'D:/bs_finally/asr/test_speech.wav', winsound.SND_FILENAME)"
$proc = Start-Process -FilePath $py -ArgumentList @('-c', $code) -PassThru -WindowStyle Hidden
Start-Sleep -Milliseconds 400

Write-Output '=== METER SAMPLING (up to 10s) ==='
$best = @{}
for ($i = 0; $i -lt 50; $i++) {
  $devs = $enum.EnumerateAudioEndPoints($render, [NAudio.CoreAudioApi.DeviceState]::Active)
  foreach ($d in $devs) {
    $p = Get-Peak $d
    if ($p -gt 0) {
      $key = $d.FriendlyName
      if ($best[$key] -eq $null -or $p -gt $best[$key]) { $best[$key] = $p }
    }
  }
  if ($proc.HasExited) { break }
  Start-Sleep -Milliseconds 200
}
$proc.WaitForExit() | Out-Null
Write-Output '=== ENDPOINTS WITH ACTIVITY (max peak) ==='
if ($best.Count -eq 0) {
  Write-Output 'NONE - no endpoint showed any audio activity'
} else {
  $best.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object {
    Write-Output ('{0} => {1}' -f $_.Key, $_.Value)
  }
}
Write-Output 'verify-play done'
