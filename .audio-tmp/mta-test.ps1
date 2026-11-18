try { [Threading.Thread]::CurrentThread.SetApartmentState([Threading.ApartmentState]::MTA) } catch {}
Add-Type -Path 'D:\bs_finally\.audio-tmp\audio.cs'
Write-Output ('APARTMENT=' + [Threading.Thread]::CurrentThread.ApartmentState)
Write-Output ('GetDefault(capture): ' + [AudioFix]::GetDefaultEndpointInfo(1))
Write-Output ('Enum(capture,active): ' + [AudioFix]::EnumEndpointsDebug(1, 1))
Write-Output ('GetDefault(render): ' + [AudioFix]::GetDefaultEndpointInfo(0))
Write-Output 'MTA-TEST-DONE'
