# verify-play2.ps1 - show defaults + prove audio reaches a render endpoint
$ErrorActionPreference = 'Continue'
Add-Type -Path 'D:/bs_finally/asr/naudio/NAudio.Core/lib/netstandard2.0/NAudio.Core.dll'
Add-Type -Path 'D:/bs_finally/asr/naudio/NAudio.Wasapi/lib/netstandard2.0/NAudio.Wasapi.dll'

$enum = New-Object NAudio.CoreAudioApi.MMDeviceEnumerator
$render = [NAudio.CoreAudioApi.DataFlow]::Render
Write-Output '=== RENDER ENDPOINTS (active) ==='
foreach ($d in $enum.EnumerateAudioEndPoints($render, [NAudio.CoreAudioApi.DeviceState]::Active)) {
  Write-Output ('{0} | id={1}' -f $d.FriendlyName, $d.ID)
}

Write-Output '=== DEFAULT ENDPOINTS ==='
foreach ($roleName in @('Console','Multimedia','Communications')) {
  $role = [NAudio.CoreAudioApi.Role]$roleName
  try {
    $def = $enum.GetDefaultAudioEndpoint($render, $role)
    Write-Output ("DEFAULT[{0}] = {1}" -f $roleName, $def.FriendlyName)
  } catch {
    Write-Output ("DEFAULT[{0}] = <none>" -f $roleName)
  }
}

function Get-Peak($d) {
  try { return [math]::Round($d.AudioMeterInformation.MasterPeakValue, 4) } catch { return -1 }
}

$py = 'E:/python/app/python.exe'
$code = 'D:/bs_finally/asr/play_wav.py'
$errFile = 'D:/bs_finally/asr/play_err.txt'
$proc = Start-Process -FilePath $py -ArgumentList @($code) -PassThru -WindowStyle Hidden -RedirectStandardError $errFile
Start-Sleep -Milliseconds 400

Write-Output '=== METER SAMPLING (up to 10s) ==='
$best = @{}
for ($i = 0; $i -lt 50; $i++) {
  foreach ($d in $enum.EnumerateAudioEndPoints($render, [NAudio.CoreAudioApi.DeviceState]::Active)) {
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
Write-Output ('playback exit code = ' + $proc.ExitCode)
if (Test-Path $errFile) {
  $e = Get-Content $errFile -Raw
  if ($e) { Write-Output ('python stderr: ' + $e) }
}
Write-Output '=== ENDPOINTS WITH ACTIVITY (max peak) ==='
if ($best.Count -eq 0) {
  Write-Output 'NONE - no endpoint showed any audio activity'
} else {
  $best.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object {
    Write-Output ('{0} => {1}' -f $_.Key, $_.Value)
  }
}
Write-Output 'verify-play2 done'
