# JetworkServer 코드 구조

무인 프린트·복사기 매장의 **결제 서버**입니다. 복사기 옆 결제 단말기와 복사기 에이전트가 이 서버에 결제·단말기·프린터 정보를 보내면, 서버는 MSSQL DB에 기록하고 결과를 돌려줍니다.

- Spring Boot 2.3.1, Java 8, Maven
- DB: Microsoft SQL Server (DB 이름 `GWS`, 프린터 정보는 `SONTECH`)
- 포트: HTTP API `8090`, 결제 단말기 소켓 `10020`

## 1. 전체 흐름

```
 복사기 에이전트 / 단말기 ──HTTP(JSON)──▶ [HTTP API :8090]  ──▶ MSSQL
                                            PostJetworkController
                                                 │
                                                 │ (Xerox 결제만) 소켓으로 결제 요청
                                                 ▼
 결제 단말기(카드·티머니·사이버머니) ◀──TCP :10000── 서버
 결제 단말기 ──TCP :10020──▶ [소켓 서버] SocketServerDriver
                                  │ 결과를 JSON 으로 변환
                                  ▼
                       중계 서버 pay1.printnuri.com:8090/jetwork  (HTTP POST)
```

서버는 두 가지 일을 동시에 합니다.

1. **HTTP API** (`com.jetwork.bin.PostJetworkController`): 단말기·에이전트가 JSON으로 요청하면 DB에 기록하고 응답합니다.
2. **소켓 서버** (`com.stech.Network.SocketServerDriver`): 결제 단말기가 보내는 바이너리 전문을 받아 해석하고, 결과를 중계 서버로 다시 HTTP POST 합니다.

## 2. 패키지 구성

| 패키지 | 역할 | 주요 클래스 |
|---|---|---|
| `com.jetwork.bin` | 서버 시작, HTTP API, 단말기 전문 조립 | `JetworkServerApplication`, `PostJetworkController`, `ConnectionPool`, `ReqMessage`, `ReqCharge` |
| `com.jetwork.sql` | DB 접속과 SQL 실행 | `DbConfig`, `DBCPConnectionMgr`, `*SQL` 클래스 |
| `com.jetwork.model` | 요청·응답 JSON 에 쓰는 데이터 클래스 | `Terminal`, `PaymentKicc`, `PaymentResult` 등 |
| `com.stech.Network` | 결제 단말기 소켓 서버 | `SocketServerDriver` |
| `co.kr.json` | 단말기 결과를 JSON 으로 만들어 중계 서버로 전송 | `JSonForm`~`JSonForm3`, `JsonHttpClient` |
| `co.kr.up.util` | 바이트·16진수 변환 도구 | `Bytes`, `HexUtils` |

> Spring 은 `JetworkServerApplication` 이 있는 `com.jetwork.bin` 패키지 아래만 스캔합니다. 새 컨트롤러는 이 패키지(또는 하위 패키지)에 만들어야 동작합니다.

## 3. 서버 시작 순서 (`JetworkServerApplication.main`)

1. Spring Boot 웹 서버 시작 (포트 8090)
2. `ConnectionPool.connection()` 으로 DBCP 커넥션 풀 생성
3. `SocketServerDriver` 를 별도 스레드로 시작 (포트 10020)
4. DB 접속을 한 번 확인하고 결과를 출력

## 4. HTTP API

모든 API 는 `POST`, `Content-Type: application/json` 입니다. 컨트롤러에 `@RequestMapping("/printnuri")` 가 붙어 있어서 모든 주소 앞에 `/printnuri` 가 붙습니다.

| 주소 | 하는 일 | 요청 모델 | 응답 모델 | 사용하는 SQL 클래스 |
|---|---|---|---|---|
| `/printnuri/postTerminal` | 단말기 등록 확인, 버전 정보 갱신 | `Terminal` | `PayRresult` | `TerminalInfoSQL` |
| `/printnuri/postCyberMoney` | 사이버머니 잔액 조회 (카드가 없으면 0원으로 새로 등록) | `CyberMoney` | `CyberMoney` | `UserCyberMoneySQL` |
| `/printnuri/postCyberMoneyPayment` | 사이버머니 차감 결제 | `PaymentKicc` | `PaymentResult` | `CyberMoneyPaymentSQL` |
| `/printnuri/postCraditPayment` | 신용·체크카드, 티머니, 캐시비 결제 기록 | `PaymentKicc` | `PaymentResult` | `CreaditPaymentSQL`, `PaymentCK` |
| `/printnuri/kicc/postCraditPayment` | KICC 단말기 카드 결제 기록 | `PaymentCreaditKicc` | `PaymentResult` | `KiccCreaditPaymentSQL` |
| `/printnuri/kyocera/postCyberMoneyPayment` | 교세라 사이버머니 결제 (**현재 비활성**, 빈 응답) | `PaymentKyocera` | `PaymentResult` | (주석 처리됨) |
| `/printnuri/printnuri/pricepolicy` | 단말기별 A4 흑백·컬러 가격 조회 | `A4Terminal` | `PriceSendResult` | `PaperPriceSQL` |
| `/printnuri/printnuri/payment` | 프린트누리 결제 기록 | `PrintNuriPay` | `PriceSendResult` | `PrintNuriCreaditPaymentSQL` |
| `/printnuri/xerox/xeroxPayment` | Xerox 출력 결제: 단말기에 결제를 요청하고 결과가 올 때까지 대기 | `XeroxPay` | `PriceXeroxSendResult` | (소켓 통신) |
| `/printnuri/postPrinter` | 프린터 상태·카운터·토너 정보 등록/갱신 | `Printer` | `PrinterResult` | `PrinterSQL` |

### Xerox 결제 흐름 (`/xerox/xeroxPayment`)

1. 요청의 금액·컬러·매수를 `JetworkServerApplication` 의 static 변수에 저장
2. 마지막으로 접속한 단말기 IP(`TerminalIP`)의 10000번 포트로 결제 요청 전문(`ReqCharge`) 전송
3. 단말기가 결제를 마치고 10020번 소켓 서버로 결과를 보내면 `SocketServerDriver` 가 `chk`(승인 여부), `chk1`(결과 도착)을 바꿈
4. API 는 1초마다 `chk1` 을 확인하다가 `true` 가 되면 `chk` 값으로 `OK` / `NO` 응답

## 5. 결제 단말기 소켓 전문 (`SocketServerDriver`, 포트 10020)

받은 데이터의 7번째 바이트(인덱스 6)가 작업 코드입니다.

| 코드 | 의미 | 처리 |
|---|---|---|
| `02` | 가격 정책 요청 | 인덱스 44부터 10바이트를 단말기 ID 로 읽고, 처음 한 번만 `JSonForm1` → `/printnuri/pricepolicy` 요청 |
| `04` | 카드·티머니 결제 결과 | `;` 로 나뉜 필드를 읽어 `R04=0000` 이면 승인(`JSonForm` → `/printnuri/payment`), `7000~9999` 면 거절, `CANCEL` 이면 취소. 티머니는 `R32` 첫 바이트가 `FF` 면 거절, 아니면 승인(`JSonForm3`) |
| `06` | 사이버머니 카드 태그 | 인덱스 8부터 4바이트를 카드번호(16진수)로 읽어 `JSonForm2` → `/kyocera/postCyberMoneyPayment` 요청, 응답의 `result` 로 승인 여부 결정 |
| `09` | 기타 | 읽기만 하고 처리하지 않음 |

단말기로 보내는 결제 요청 전문은 `ReqMessage` 가 만듭니다: `STX(0x02) + 길이(2) + 순번 + CMD(0xFB) + GCD(0x14) + 작업코드 + 본문 + ETX(0x03) + CRC(2)`. 본문 예: `S01=A1;S02=40;S09=00;S10=금액;`

## 6. DB 테이블

코드에서 쓰는 테이블과 컬럼입니다.

| 테이블 | 용도 | 코드에 나오는 컬럼 |
|---|---|---|
| `jw_terminal` | 결제 단말기 | `terminal_id`, `serial`, `fran_id`(3번째 컬럼), `agent_version`, `terminalVersion`, `update` |
| `jw_franchise` | 가맹점(매장) | `fran_id`, `fran_name` |
| `jw_paper` | 단말기별 용지 가격 | `terminalID`, `paper_size`, `paper_colorYN`, `paper_price` |
| `jw_payment` | 결제 내역 | `id`, `terminal_id`, `payment_pay`, `payment_type`, `payment_paperType`, `payment_colorYN`, `payment_ok`(승인번호), `payment_printType`, `payment_cardName`, `payment_totalCount`, `userCard`, `cyberNo`, `reg_date` |
| `cybermoney` | 사이버머니 잔액 | `cardNumber`, `cyberMoney`, `fran_id`, `reg_date` |
| `SONTECH.dbo.printer` | 프린터 상태 | `devid`, `sitename`, `desc1`, `errmsg`, `modelname`, `total`, `mono`, `color`, `black`, `cyan`, `magenta`, `yellow`, `devicestatus`, `printerstatus`, `coverstatus`, `serialnumber`, `macaddr`, `regdate` |

## 7. 설정

| 파일 | 내용 |
|---|---|
| `src/main/resources/application.properties` | 포트(8090), 로그 레벨, Spring DataSource (값은 아래 파일·환경변수에서 읽음) |
| `src/main/resources/db.properties` | **DB 주소·아이디·비밀번호. Git 에 올리지 않음.** `db.properties.example` 을 복사해서 만든다 |
| 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 있으면 `db.properties` 보다 우선 |

DB 접속 정보는 `DbConfig` 한 곳에서만 읽습니다. 실제 커넥션은 `DBCPConnectionMgr`(commons-dbcp 풀, 이름 `ora10g2`)가 만들고, 각 `*SQL` 클래스가 `ConnectionPool.getInst().getConnection()` 으로 빌려 씁니다.

중계 서버 주소는 `co.kr.json.JsonHttpClient.BASE_URL` 에 있습니다.

## 8. 발견한 문제 (아직 고치지 않음)

결제 동작이 바뀌는 문제라서 이번 정리에서는 손대지 않았습니다. 우선순위 순서입니다.

### 긴급

1. **SQL 인젝션**: 모든 SQL 이 요청 값을 문자열로 이어 붙여 만듭니다. 카드번호나 단말기 ID 에 `'` 가 들어가면 임의의 SQL 이 실행될 수 있습니다. `PreparedStatement` 로 바꿔야 합니다.
2. **사이버머니 결제가 항상 성공으로 응답**: `/postCyberMoneyPayment` 가 잔액 부족·미등록 카드 판정을 한 뒤, 마지막에 결과를 다시 `OK` 로 덮어씁니다 (`PostJetworkController` 153~157행). 잔액이 없어도 출력이 될 수 있습니다.
3. **잔액과 결제 금액이 같으면 차감되지 않음**: `CyberMoneyPaymentSQL` 은 `잔액 - 금액 > 0` 일 때만 차감·기록합니다. 정확히 0원이 되는 결제는 기록 없이 지나갑니다.
4. **동시 결제 시 이중 사용**: 잔액을 읽고 계산한 뒤 업데이트하는 사이에 잠금이 없어, 같은 카드로 동시에 결제하면 잔액이 한 번만 차감될 수 있습니다. `UPDATE ... SET cyberMoney = cyberMoney - ? WHERE cyberMoney >= ?` 처럼 한 번에 처리해야 합니다.
5. **단말기 등록 확인이 항상 OK**: `/postTerminal` 도 결과를 마지막에 `OK` 로 덮어써서, 등록되지 않은 단말기도 통과합니다.

### 높음

6. **Xerox 결제가 무한 대기할 수 있음**: 단말기가 결과를 보내지 않으면 `while(true)` 가 끝나지 않고 HTTP 요청 스레드가 묶입니다. 단말기 소켓도 닫지 않습니다. 결제 상태가 static 변수 하나라서 단말기가 2대 이상이면 결과가 섞입니다.
7. **주소 불일치 확인 필요**: 컨트롤러 주소는 `/printnuri/printnuri/pricepolicy` 인데, `PrintPolicyPost` 는 중계 서버의 `/jetwork/printnuri/pricepolicy` 로 보냅니다. `PrintCyberPOST` 는 `/jetwork//kyocera/...` 로 보내는데 이 서버의 주소는 `/printnuri/kyocera/...` 입니다. 중계 서버가 다른 버전이면 문제없지만, 같은 서버라면 404 가 납니다.
8. **교세라 사이버머니 API 비활성**: `/kyocera/postCyberMoneyPayment` 는 처리 코드가 주석 처리되어 `result` 가 빈 응답을 돌려줍니다. 원래 처리 코드는 첫 커밋의 `com/jetwork/bin/PostAPIController.java` 에 남아 있습니다.
9. **DB 오류 시 500 에러**: DB 조회가 실패하면 잔액이 빈 문자열이 되어 `Integer.parseInt` 에서 예외가 납니다.
10. **카드번호가 로그에 그대로 출력**: SQL 문 전체와 카드번호를 `System.out` 으로 출력합니다. 개인정보 노출 위험이 있습니다.

### 보통

11. 카드 결제마다 `PaymentCK` 에서 1초씩 `sleep` 하고, 같은 날 같은 승인번호를 두 번 조회합니다.
12. 커넥션 풀 최대값이 20000으로 DB 서버가 감당할 수 있는 수를 훨씬 넘습니다.
13. `System.out.println` 과 `logger` 가 섞여 있습니다.
14. Spring Boot 2.3 과 Java 8 은 지원이 끝났습니다. 보안 업데이트를 받으려면 Spring Boot 3 + Java 17 이상으로 올리는 게 좋습니다.
