param([string]$TextFile, [string]$OutWav)
$ProgressPreference='SilentlyContinue'
Add-Type -AssemblyName System.Speech
$text = (Get-Content -Raw -Encoding UTF8 $TextFile).Trim()
if (-not $text) { "ERR empty text"; exit 1 }
$syn = New-Object System.Speech.Synthesis.SpeechSynthesizer
$en = $syn.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture -eq 'en-US' -and $_.Enabled } | Select-Object -First 1
if ($en) { $syn.SelectVoice($en.VoiceInfo.Name) }
$dir = Split-Path -Parent $OutWav
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
$syn.SetOutputToWaveFile($OutWav)
$syn.Speak($text)
"OK " + [System.IO.File]::ReadAllBytes($OutWav).Length
exit 0
