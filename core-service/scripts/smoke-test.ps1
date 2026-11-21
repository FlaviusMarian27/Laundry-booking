# Smoke test pentru core-service (aplicatia trebuie sa ruleze pe $BaseUrl).
# Rulare, din folderul scriptului:
#   powershell -ExecutionPolicy Bypass -File .\smoke-test.ps1
# Date de seed folosite: users 1 (ADMIN) si 2 (STUDENT), masinile 1..4 (V1/V2).
# Masina 4 e folosita pentru fluxul de mentenanta (se redeschide la final).

param(
    [string]$BaseUrl = "http://localhost:8081",
    [int]$DelayMs = 1000
)

$script:Pass = 0
$script:Fail = 0

function Call {
    param([string]$Method, [string]$Path, $Body = $null, [string[]]$ExtraHeaders = @())
    $out = [IO.Path]::GetTempFileName()
    $curlArgs = @('-s', '-o', $out, '-w', '%{http_code}', '-X', $Method, "$BaseUrl$Path")
    $in = $null
    if ($null -ne $Body) {
        $in = [IO.Path]::GetTempFileName()
        Set-Content -Path $in -Value ($Body | ConvertTo-Json -Compress) -Encoding ascii
        $curlArgs += @('-H', 'Content-Type: application/json', '--data-binary', "@$in")
    }
    foreach ($h in $ExtraHeaders) { $curlArgs += @('-H', $h) }
    $code = & curl.exe @curlArgs
    $text = Get-Content -Path $out -Raw
    Remove-Item $out -ErrorAction SilentlyContinue
    if ($in) { Remove-Item $in -ErrorAction SilentlyContinue }
    $json = $null
    if ($text) { try { $json = $text | ConvertFrom-Json } catch { } }
    return [pscustomobject]@{ Code = [int]$code; Text = $text; Json = $json }
}

function Check {
    param([string]$Name, [int]$Expected, $Res)
    if ($Res.Code -eq $Expected) {
        Write-Host ("[PASS] {0} -> {1}" -f $Name, $Res.Code) -ForegroundColor Green
        $script:Pass++
    } else {
        Write-Host ("[FAIL] {0} -> asteptat {1}, primit {2}" -f $Name, $Expected, $Res.Code) -ForegroundColor Red
        if ($Res.Text) { Write-Host ("       " + $Res.Text) -ForegroundColor DarkGray }
        $script:Fail++
    }
    Start-Sleep -Milliseconds $DelayMs
}

function Iso([datetime]$d) { return $d.ToString("yyyy-MM-ddTHH:mm:ssZ") }

# zi viitoare aleatoare, ca rularile repetate sa nu se ciocneasca intre ele
$base = (Get-Date).ToUniversalTime().Date.AddDays((Get-Random -Minimum 30 -Maximum 3000)).AddHours(18)
function Slot([int]$fromMin, [int]$toMin) {
    return @{ startTime = (Iso $base.AddMinutes($fromMin)); endTime = (Iso $base.AddMinutes($toMin)) }
}
function BookingBody([int]$user, [int]$machine, [int]$fromMin, [int]$toMin) {
    $s = Slot $fromMin $toMin
    return @{ userId = $user; machineId = $machine; startTime = $s.startTime; endTime = $s.endTime }
}

Write-Host "=== Machines ===" -ForegroundColor Cyan
Check "GET /machines" 200 (Call GET "/machines")

Write-Host "=== Bookings ===" -ForegroundColor Cyan
$b1 = Call POST "/bookings" (BookingBody 2 1 0 90)
Check "create booking m1 18:00-19:30" 201 $b1
Check "acelasi interval, aceeasi masina (suprapunere)" 409 (Call POST "/bookings" (BookingBody 2 1 0 90))
Check "acelasi interval, alta masina" 201 (Call POST "/bookings" (BookingBody 2 2 0 90))
Check "18:30-19:00 pe m1 (suprapunere partiala)" 409 (Call POST "/bookings" (BookingBody 2 1 30 60))
Check "19:30-20:30 pe m1 (slot adiacent)" 201 (Call POST "/bookings" (BookingBody 2 1 90 150))
Check "user inexistent" 404 (Call POST "/bookings" (BookingBody 999 1 300 360))
Check "masina inexistenta" 404 (Call POST "/bookings" (BookingBody 2 999 300 360))
Check "endTime inaintea startTime" 400 (Call POST "/bookings" (BookingBody 2 1 360 300))
Check "startTime in trecut" 400 (Call POST "/bookings" @{ userId = 2; machineId = 3; startTime = "2020-01-01T10:00:00Z"; endTime = "2020-01-01T11:00:00Z" })
Check "camp lipsa (machineId)" 400 (Call POST "/bookings" @{ userId = 2; startTime = (Iso $base); endTime = (Iso $base.AddMinutes(60)) })
if ($b1.Json) {
    $id = $b1.Json.id
    Check "GET /bookings/{id}" 200 (Call GET "/bookings/$id")
    Check "start inainte de slot" 409 (Call POST "/bookings/$id/start")
    Check "cancel booking" 200 (Call POST "/bookings/$id/cancel")
    Check "cancel a doua oara" 409 (Call POST "/bookings/$id/cancel")
    Check "slot eliberat dupa cancel" 201 (Call POST "/bookings" (BookingBody 2 1 0 90))
}
Check "GET /bookings/999999" 404 (Call GET "/bookings/999999")
Check "GET /bookings?userId=2" 200 (Call GET "/bookings?userId=2")
Check "GET /machines/1/bookings" 200 (Call GET "/machines/1/bookings")
Check "GET /machines/999/bookings" 404 (Call GET "/machines/999/bookings")

Write-Host "=== Faults ===" -ForegroundColor Cyan
Check "report fault" 201 (Call POST "/faults" @{ machineId = 1; reportedBy = 2; description = "Water does not drain" })
Check "fault: masina inexistenta" 404 (Call POST "/faults" @{ machineId = 999; reportedBy = 2; description = "x" })
Check "fault: user inexistent" 404 (Call POST "/faults" @{ machineId = 1; reportedBy = 999; description = "x" })
Check "fault: descriere goala" 400 (Call POST "/faults" @{ machineId = 1; reportedBy = 2; description = "   " })
Check "fault: descriere > 500 caractere" 400 (Call POST "/faults" @{ machineId = 1; reportedBy = 2; description = ("a" * 501) })
Check "GET /faults?machineId=1" 200 (Call GET "/faults?machineId=1")
Check "GET /faults?machineId=999" 404 (Call GET "/faults?machineId=999")

Write-Host "=== Feedback ===" -ForegroundColor Cyan
Check "submit feedback" 201 (Call POST "/feedback" @{ userId = 2; message = "Dryer 01 takes too long" })
Check "feedback fara mesaj" 400 (Call POST "/feedback" @{ userId = 2 })
Check "feedback user inexistent" 404 (Call POST "/feedback" @{ userId = 999; message = "x" })
Check "GET /feedback" 200 (Call GET "/feedback")

Write-Host "=== Proposals ===" -ForegroundColor Cyan
Check "GET /proposals?status=foo" 400 (Call GET "/proposals?status=foo")
Check "GET /proposals?status=PENDING" 200 (Call GET "/proposals?status=PENDING")
Check "MAINTENANCE fara severity" 400 (Call POST "/proposals" @{ kind = "MAINTENANCE"; targetId = 4; reason = "test" })
Check "MAINTENANCE tinta inexistenta" 404 (Call POST "/proposals" @{ kind = "MAINTENANCE"; targetId = 999; reason = "test"; severity = "HIGH" })

$pen = Call POST "/proposals" @{ kind = "PENALTY"; targetId = 2; reason = "3 no-shows"}
Check "PENALTY fara severity (default MEDIUM)" 201 $pen
if ($pen.Json) {
    if ($pen.Json.severity -eq "MEDIUM") { Write-Host "       severity = MEDIUM" -ForegroundColor DarkGray }
    $pid1 = $pen.Json.id
    Check "PENALTY duplicat PENDING" 409 (Call POST "/proposals" @{ kind = "PENALTY"; targetId = 2; reason = "again" })
    Check "reject de catre student" 403 (Call POST "/proposals/$pid1/reject" @{ decidedBy = 2 })
    Check "reject de catre admin" 200 (Call POST "/proposals/$pid1/reject" @{ decidedBy = 1 })
    Check "reject a doua oara" 409 (Call POST "/proposals/$pid1/reject" @{ decidedBy = 1 })
}

$mnt = Call POST "/proposals" @{ kind = "MAINTENANCE"; targetId = 4; reason = "4 faults in 24h"; severity = "HIGH" }
Check "MAINTENANCE m4" 201 $mnt
if ($mnt.Json) {
    $pid2 = $mnt.Json.id
    Check "MAINTENANCE duplicat PENDING" 409 (Call POST "/proposals" @{ kind = "MAINTENANCE"; targetId = 4; reason = "again"; severity = "LOW" })
    Check "approve de catre student" 403 (Call POST "/proposals/$pid2/approve" @{ decidedBy = 2 })
    Check "approve de catre admin" 200 (Call POST "/proposals/$pid2/approve" @{ decidedBy = 1 })
    Check "approve a doua oara" 409 (Call POST "/proposals/$pid2/approve" @{ decidedBy = 1 })
    Check "rezervare pe masina in mentenanta" 409 (Call POST "/bookings" (BookingBody 2 4 0 60))
    Check "close maintenance" 200 (Call POST "/machines/4/maintenance/close")
    Check "close maintenance a doua oara" 409 (Call POST "/machines/4/maintenance/close")
}

Write-Host "=== CORS ===" -ForegroundColor Cyan
Check "preflight de pe localhost:5173" 200 (Call OPTIONS "/machines" $null @("Origin: http://localhost:5173", "Access-Control-Request-Method: GET"))
Check "preflight de pe origin necunoscut" 403 (Call OPTIONS "/machines" $null @("Origin: http://evil.example", "Access-Control-Request-Method: GET"))

Write-Host ""
$color = if ($script:Fail -eq 0) { "Green" } else { "Red" }
Write-Host ("Rezultat: {0} PASS, {1} FAIL" -f $script:Pass, $script:Fail) -ForegroundColor $color
exit $script:Fail