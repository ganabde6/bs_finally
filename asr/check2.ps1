$ProgressPreference='SilentlyContinue'
$dir = 'D:\bs_finally\asr\naudio'
function Find-Dll($root, $name) {
  $d = Get-ChildItem $root -Recurse -Filter $name | Where-Object { $_.FullName -like '*net472*' } | Select-Object -First 1
  if (-not $d) { $d = Get-ChildItem $root -Recurse -Filter $name | Select-Object -First 1 }
  return $d.FullName
}
Add-Type -Path (Find-Dll "$dir\NAudio.Core" 'NAudio.Core.dll')
Add-Type -Path (Find-Dll "$dir\NAudio.Wasapi" 'NAudio.Wasapi.dll')
$enum = New-Object NAudio.CoreAudioApi.MMDeviceEnumerator
try {
  $d = $enum.GetDefaultAudioEndpoint([NAudio.CoreAudioApi.DataFlow]::Render, [NAudio.CoreAudioApi.Role]::Console)
  "NAudio default render: $($d.FriendlyName)"
} catch { "NAudio default render: NONE ($($_.Exception.Message))" }
try {
  $d = $enum.GetDefaultAudioEndpoint([NAudio.CoreAudioApi.DataFlow]::Capture, [NAudio.CoreAudioApi.Role]::Console)
  "NAudio default capture: $($d.FriendlyName)"
} catch { "NAudio default capture: NONE ($($_.Exception.Message))" }
