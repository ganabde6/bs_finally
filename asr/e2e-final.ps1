$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:9000'
$log = 'D:/bs_finally/asr/e2e_result.txt'
$lines = New-Object System.Collections.Generic.List[string]
function W($s) { $lines.Add([string]$s) }

Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(180)

# 1. login
$json = '{"username":"student01","password":"123456"}'
$resp = $client.PostAsync("$base/api/auth/login", (New-Object System.Net.Http.StringContent($json, [System.Text.Encoding]::UTF8, 'application/json'))).Result
$login = $resp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
$token = $login.data.token
$client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $token)
W ('STEP1 LOGIN ok: user=' + $login.data.user.username)

# 2. TTS url for question 3 (imitation reading: The Lost Dog)
$ttsResp = $client.GetAsync("$base/api/student/listening-speaking/3/tts-audio").Result
$ttsJson = $ttsResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
$ttsUrl = [string]$ttsJson.data
W ('STEP2 TTS url: ' + $ttsUrl)

# 3. download wav
$ttsFile = 'D:/bs_finally/asr/e2e_tts.wav'
$dl = $client.GetAsync("$base" + $ttsUrl).Result
$bytes = $dl.Content.ReadAsByteArrayAsync().Result
[System.IO.File]::WriteAllBytes($ttsFile, $bytes)
W ('STEP3 TTS wav downloaded: ' + $bytes.Length + ' bytes')

# 4. play it and sample endpoint meters
Add-Type -Path 'D:/bs_finally/asr/naudio/NAudio.Core/lib/netstandard2.0/NAudio.Core.dll'
Add-Type -Path 'D:/bs_finally/asr/naudio/NAudio.Wasapi/lib/netstandard2.0/NAudio.Wasapi.dll'
$enum = New-Object NAudio.CoreAudioApi.MMDeviceEnumerator
$render = [NAudio.CoreAudioApi.DataFlow]::Render

$playPy = 'D:/bs_finally/asr/play_tts_e2e.py'
"import winsound; winsound.PlaySound(r'$ttsFile', winsound.SND_FILENAME); print('played')" | Set-Content -Path $playPy -Encoding ASCII
$errFile = 'D:/bs_finally/asr/play_err2.txt'
$proc = Start-Process -FilePath 'E:/python/app/python.exe' -ArgumentList @($playPy) -PassThru -WindowStyle Hidden -RedirectStandardError $errFile
Start-Sleep -Milliseconds 400

$peak = 0.0
$peakDev = 'none'
for ($i = 0; $i -lt 60; $i++) {
  foreach ($d in $enum.EnumerateAudioEndPoints($render, [NAudio.CoreAudioApi.DeviceState]::Active)) {
    try {
      $p = $d.AudioMeterInformation.MasterPeakValue
      if ($p -gt $peak) { $peak = $p; $peakDev = $d.FriendlyName }
    } catch {}
  }
  if ($proc.HasExited) { break }
  Start-Sleep -Milliseconds 200
}
$proc.WaitForExit() | Out-Null
W ('STEP4 PLAY exit=' + $proc.ExitCode + ' meter peak=' + [math]::Round($peak, 4) + ' on [' + $peakDev + ']')
if (Test-Path $errFile) {
  $e = Get-Content $errFile -Raw
  if ($e) { W ('STEP4 python stderr: ' + $e) }
}

# 5. submit a recording (test_speech.wav as mock student recording) -> whisper ASR + DeepSeek scoring
$mr = New-Object System.Net.Http.MultipartFormDataContent
$cr = New-Object System.Net.Http.StreamContent([System.IO.File]::OpenRead('D:/bs_finally/asr/test_speech.wav'))
$mr.Add($cr, 'file', 'test_speech.wav')
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$subResp = $client.PostAsync("$base/api/student/listening-speaking/3/submit", $mr).Result
$sw.Stop()
$subRaw = $subResp.Content.ReadAsStringAsync().Result
$subJson = $subRaw | ConvertFrom-Json
W ('STEP5 SUBMIT http=' + $subResp.StatusCode + ' took ' + $sw.ElapsedMilliseconds + 'ms')
W ('STEP5 SUBMIT json: ' + ($subJson | ConvertTo-Json -Depth 10 -Compress))

# 6. my records
$recResp = $client.GetAsync("$base/api/student/listening-speaking/records").Result
$recJson = $recResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
$recCount = $recJson.data.Count
W ('STEP6 RECORDS count=' + $recCount)

[System.IO.File]::WriteAllLines($log, $lines, [System.Text.Encoding]::UTF8)
Write-Output 'e2e-final done'
