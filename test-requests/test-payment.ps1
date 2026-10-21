# JetworkServer 테스트 결제 요청 (/jetwork)
#
# 실행: PowerShell 에서
#   powershell -ExecutionPolicy Bypass -File .\test-payment.ps1
# 서버 주소·단말기·카드를 바꾸려면:
#   powershell -ExecutionPolicy Bypass -File .\test-payment.ps1 -BaseUrl http://222.122.118.8:8090/jetwork -TerminalID 123456 -Serial erwerewrwerwer -UserCard 8E156710
#
# 주의: 실제 DB 에 결제 내역이 기록되고 사이버머니 잔액이 차감됩니다.
#       운영 DB 라면 테스트 단말기·카드로만 실행하고, 끝나면 jw_payment 의 테스트 기록을 지워 주세요.
# Xerox 결제(/xerox/xeroxPayment)는 실제 결제 단말기가 있어야 해서 넣지 않았습니다.

param(
    [string]$BaseUrl    = "http://222.122.118.8:8090/jetwork",
    [string]$TerminalID = "123456",
    [string]$Serial     = "erwerewrwerwer",
    [string]$UserCard   = "8E156710"
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Send-Test([string]$Name, [string]$Path, [hashtable]$Body) {
    $json  = $Body | ConvertTo-Json -Compress
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    Write-Host ""
    Write-Host "=== $Name  POST $Path" -ForegroundColor Cyan
    Write-Host "요청: $json"
    try {
        $res = Invoke-RestMethod -Uri ($BaseUrl + $Path) -Method Post -ContentType "application/json; charset=utf-8" -Body $bytes -TimeoutSec 30
        Write-Host ("응답: " + ($res | ConvertTo-Json -Compress)) -ForegroundColor Green
    } catch {
        Write-Host ("실패: " + $_.Exception.Message) -ForegroundColor Red
    }
}

# 매번 다른 결제번호·승인번호 (같은 값이면 중복으로 걸러진다)
function New-No { (Get-Date).ToString("yyyyMMddHHmmssfff") }

$common = @{ terminalID = $TerminalID; totalAmount = "100"; paperSize = "A4"; color = "false"; jobType = "print"; totalPages = "1" }
function With($extra) { $h = $common.Clone(); foreach ($k in $extra.Keys) { $h[$k] = $extra[$k] }; $h }

# 1. 단말기
Send-Test "단말기 등록 확인 (OK / FALSE / ERROR)" "/postTerminal" @{ terminalID = $TerminalID; serial = $Serial; key = ""; license = ""; agentVersion = "Xerox_2.11.01" }
Send-Test "가격 정책 조회" "/printnuri/pricepolicy" @{ terminalID = $TerminalID; agentVersion = "Xerox_2.11.01" }

# 2. 사이버머니
Send-Test "사이버머니 잔액 조회" "/postCyberMoney" @{ userID = $TerminalID; userCard = $UserCard }
$no = New-No
Send-Test "사이버머니 결제 100원" "/postCyberMoneyPayment" (With @{ userCard = $UserCard; cyberNo = $no; R09 = $no })
Send-Test "같은 결제번호로 연속 결제 (이것도 결제됨)" "/postCyberMoneyPayment" (With @{ userCard = $UserCard; cyberNo = $no; R09 = $no })
Send-Test "교세라 사이버머니 결제 100원" "/kyocera/postCyberMoneyPayment" (With @{ userCard = $UserCard; cyberNo = (New-No); color = "2" })

# 3. 카드·교통카드 결제 기록 (응답은 항상 OK, 결과는 jw_payment 에서 확인)
Send-Test "신용카드 (신한)" "/postCraditPayment" (With @{ R09 = (New-No); R11 = "N"; R12 = "029"; R33 = "" })
Send-Test "체크카드 (카드사 코드 없음 -> 해외카드)" "/postCraditPayment" (With @{ R09 = (New-No); R11 = "Y"; R12 = ""; R33 = "" })
Send-Test "티머니" "/postCraditPayment" (With @{ R11 = ""; R33 = "TM" })
Send-Test "캐시비" "/postCraditPayment" (With @{ R11 = ""; R33 = "EB" })
Send-Test "기타 교통카드 -> 교통카드" "/postCraditPayment" (With @{ R11 = ""; R33 = "RP" })
Send-Test "KICC 단말기 카드 결제 (국민)" "/kicc/postCraditPayment" (With @{ R09 = (New-No); R10 = "N"; R12 = "016"; R33 = "" })
Send-Test "프린트누리 결제 (삼성)" "/printnuri/payment" @{ terminalID = $TerminalID; price = "100"; payOK = (New-No); craditName = "031"; payType = "N"; payTypeTM = ""; color = "false"; count = "1"; paperSize = "A4" }

Write-Host ""
Write-Host "결제 기록 확인 SQL:" -ForegroundColor Yellow
Write-Host "  SELECT TOP 20 * FROM jw_payment WHERE terminal_id = '$TerminalID' ORDER BY reg_date DESC;"
Write-Host "테스트 기록 지우기:" -ForegroundColor Yellow
Write-Host "  DELETE FROM jw_payment WHERE terminal_id = '$TerminalID' AND reg_date >= CAST(GETDATE() AS DATE);"
