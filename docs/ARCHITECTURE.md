# JetworkServer 코드 구조

> 이 문서는 /stech 버전(API 주소 `/stech/...`) 기준입니다.

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
2. **소켓 서버** (`com.stech.Network.SocketServerDriver`): 결제 단말기가 보내는 바이너리 전문을 받아 해석하고, 결과를 중계 서버로 다시 HTTP POST 합니다. 접속 받는 스레드 1개와 처리용 작업 스레드 50개로 단말기를 동시에 처리합니다.

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

모든 API 는 `POST`, `Content-Type: application/json` 입니다. 컨트롤러에 `@RequestMapping("/stech")` 가 붙어 있어서 모든 주소 앞에 `/stech` 가 붙습니다.

| 주소 | 하는 일 | 요청 모델 | 응답 모델 | 사용하는 SQL 클래스 |
|---|---|---|---|---|
| `/stech/postTerminal` | 단말기 등록 확인, 에이전트 버전 갱신 | `Terminal` | `PayRresult` | `TerminalInfoSQL` |
| `/stech/postCyberMoney` | 사이버머니 잔액 조회 (카드가 없으면 0원으로 새로 등록) | `CyberMoney` | `CyberMoney` | `UserCyberMoneySQL` |
| `/stech/postCyberMoneyPayment` | 사이버머니 차감 결제 (같은 결제번호는 다시 차감하지 않음) | `PaymentKicc` | `PaymentResult` | `CyberMoneyPaymentSQL` |
| `/stech/postCraditPayment` | 신용·체크카드, 티머니, 캐시비 결제 기록 | `PaymentKicc` | `PaymentResult` | `CreaditPaymentSQL`, `PaymentCK` |
| `/stech/kicc/postCraditPayment` | KICC 단말기 카드 결제 기록 | `PaymentCreaditKicc` | `PaymentResult` | `KiccCreaditPaymentSQL` |
| `/stech/kyocera/postCyberMoneyPayment` | 교세라 사이버머니 차감 결제 (같은 결제번호는 다시 차감하지 않음) | `PaymentKyocera` | `PaymentResult` | `CyberMoneyKyoceraPaymentSQL` |
| `/stech/printnuri/pricepolicy` | 단말기별 A4 흑백·컬러 가격 조회 | `A4Terminal` | `PriceSendResult` | `PaperPriceSQL` |
| `/stech/printnuri/payment` | 프린트누리 결제 기록 | `PrintNuriPay` | `PriceSendResult` | `PrintNuriCreaditPaymentSQL` |
| `/stech/xerox/xeroxPayment` | Xerox 출력 결제: 단말기에 결제를 요청하고 결과가 올 때까지 대기 | `XeroxPay` | `PriceXeroxSendResult` | (소켓 통신) |

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
| `jw_terminal` | 결제 단말기 | `terminal_id`, `serial`, `fran_id`(3번째 컬럼), `agent_version`, `update`, `last_dt`(마지막 결제 시각) |
| `jw_franchise` | 가맹점(매장) | `fran_id`, `fran_name` |
| `jw_paper` | 단말기별 용지 가격 | `terminalID`, `paper_size`, `paper_colorYN`, `paper_price` |
| `jw_payment` | 결제 내역 | `id`, `terminal_id`, `payment_pay`, `payment_type`, `payment_paperType`, `payment_colorYN`, `payment_ok`(승인번호), `payment_printType`, `payment_cardName`, `payment_totalCount`, `userCard`, `cyberNo`, `reg_date` |
| `cybermoney` | 사이버머니 잔액 | `cardNumber`, `cyberMoney`, `fran_id`, `reg_date` |
| `SONTECH.dbo.printer` | 프린터 상태 | `devid`, `sitename`, `desc1`, `errmsg`, `modelname`, `total`, `mono`, `color`, `black`, `cyan`, `magenta`, `yellow`, `devicestatus`, `printerstatus`, `coverstatus`, `serialnumber`, `macaddr`, `regdate` |

## 7. 설정

| 파일 | 내용 |
|---|---|
| `src/main/resources/application.properties` | 포트(8090), 로그 레벨 |
| `src/main/resources/db.properties` | **DB 주소·아이디·비밀번호. Git 에 올리지 않음.** `db.properties.example` 을 복사해서 만든다 |
| 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 있으면 `db.properties` 보다 우선 |

DB 접속 정보는 `DbConfig` 한 곳에서만 읽습니다. 실제 커넥션은 `DBCPConnectionMgr`(commons-dbcp 풀, 이름 `jetwork`)가 만들고, 각 `*SQL` 클래스가 `ConnectionPool.getInst().getConnection()` 으로 빌려 씁니다. Spring Boot 의 DataSource 자동 설정은 쓰지 않아서 꺼 두었습니다.

커넥션 풀 설정 (`DBCPConnectionMgr`, 크기는 `db.properties` 에서 변경 가능)

| 설정 | 값 | 의미 |
|---|---|---|
| 최대 커넥션 | 100 (`db.pool.maxActive`) | 동시에 빌릴 수 있는 최대 개수. 요청 스레드가 최대 200개라 그 이상은 필요 없다 |
| 최대 유휴 커넥션 | 20 (`db.pool.maxIdle`) | 쉬는 커넥션을 남겨 두는 최대 개수 |
| 대기 시간 | 10초 | 풀이 가득 찼을 때 기다리는 시간. 넘으면 커넥션 없음(null) → 결제는 FAIL/error 응답 |
| 검증 쿼리 | `SELECT 1` | 빌려주기 전과 쉬는 동안 커넥션이 살아 있는지 확인 |
| 유휴 정리 | 1분마다 점검, 3분 이상 쉰 커넥션 닫기 | 방화벽이 끊은 커넥션을 미리 정리 |
| 접속·응답 타임아웃 | `loginTimeout=10`, `socketTimeout=60000` | DB URL 에 값이 없을 때 자동으로 붙임 |

중계 서버 주소는 `co.kr.json.JsonHttpClient.BASE_URL` 에 있습니다.

## 8. 고친 문제와 남은 문제

### 고친 문제

| 문제 | 수정 내용 |
|---|---|
| SQL 인젝션 | 모든 SQL 을 `PreparedStatement` 로 바꿨다. 공통 쿼리는 `CyberMoneyLedger`, `CardPaymentLedger` 에 모았다 |
| 사이버머니 결제가 항상 OK | 결제 결과를 `CyberMoneyResult`(OK / NO_CARD / INSUFFICIENT / DUPLICATE / ERROR)로 돌려주고 그대로 응답한다 |
| 잔액 = 금액이면 차감 안 됨 | 잔액이 금액 이상이면 차감한다 (0원이 될 수 있음) |
| 동시 결제 시 이중 사용 | 잔액 확인과 차감을 `UPDATE ... WHERE cyberMoney >= 금액` 한 번으로 처리한다 |
| DB 오류·금액 형식 오류 시 500 에러 | ERROR 상태로 `FAIL` 응답하고, 트랜잭션을 롤백한다 |
| 미등록 단말기도 OK | `/postTerminal` 은 `jw_terminal` 에 단말기 ID 와 시리얼이 함께 등록된 경우만 `OK`, 아니면 `FALSE`, DB 오류면 `ERROR` 로 응답하고 경고 로그를 남긴다 |
| 서버 시작 직후 DB 요청 실패 | 웹 서버가 먼저 뜨고 커넥션 풀은 그 뒤에 만들어져, 그 사이 들어온 요청은 NullPointerException 이 났다. 이제 풀을 처음 쓸 때 만든다 |
| 끊긴 커넥션 재사용 | DB 재시작이나 네트워크 끊김 후에도 죽은 커넥션을 계속 빌려줬다. 이제 `SELECT 1` 로 확인 후 빌려준다 |
| 커넥션 고갈 시 무한 대기 | 풀이 가득 차면 요청이 끝없이 기다렸다. 이제 10초 후 실패 처리한다 |
| 커넥션 풀 최대값 20000 | 기본 100 으로 줄이고 `db.properties` 에서 바꿀 수 있게 했다 (단말기 4000대 기준) |
| 중복 확인 중 커넥션 점유 | `PaymentCK` 가 1초 기다리는 동안 커넥션을 붙잡고 있었다. 반납한 뒤 기다리도록 바꿨다 |
| 소켓 서버가 단말기를 하나씩 처리 | 접속 하나를 중계 서버 HTTP 요청까지 끝내야 다음 단말기를 받았다. 이제 작업 스레드 50개가 동시에 처리하고, 밀리면 1000개까지 줄을 세운다 |
| 스레드 간 공유 상태가 안 보이던 문제 | 소켓 스레드가 바꾼 결제 결과(`chk`, `chk1`)를 HTTP 스레드가 못 볼 수 있었다. 공유 변수를 `volatile` 로 바꿨다 |
| 카드사명 없는 결제 | 신용·체크카드 결제에서 발급사 코드가 없거나 목록에 없으면 카드사명을 빈 값 대신 `해외카드` 로 기록한다 (`CardCompanyName.getCardName`). 티머니·캐시비는 그대로 둔다 |
| 결제번호 중복 차감 | 사이버머니 결제(`/postCyberMoneyPayment`)도 같은 카드·결제번호(`cyberNo`)로 이미 처리된 결제면 차감하지 않고 FAIL(`duplicate`) 로 응답한다 |
| 단말기 마지막 결제 시각 | 결제 내역(`jw_payment`)이 정상 등록되면 그 단말기의 `jw_terminal.last_dt` 를 현재 시각으로 갱신한다. 결제 기록을 먼저 커밋한 뒤 갱신하므로, 갱신이 실패해도 결제 기록은 남는다 |
| 기타 교통카드 결제 누락 | `/postCraditPayment` 에서 카드가 아니면서 티머니(TM)·캐시비(EB)도 아닌 교통카드 결제는 기록 없이 버려졌다. 이제 결제 종류·카드사명 모두 `교통카드` 로 기록한다 |
| 티머니 판정 누락 | `JSonForm` 이 `R33=TM` 처럼 접두어가 붙은 값을 보내 티머니 결제가 신용카드로 기록됐다. 접두어를 떼고 보낸다 |
| 단말기 소켓 무한 대기 | 소켓 서버가 한 번에 한 단말기씩 처리하는데 읽기 타임아웃이 없어, 데이터를 안 보내는 단말기 하나가 전체를 막을 수 있었다. 10초 타임아웃을 넣었다 |
| 쓰지 않는 Spring 커넥션 풀 | 아무도 쓰지 않는 Spring DataSource(Hikari)가 같은 DB 에 따로 접속했다. 자동 설정을 껐다 |
| DB 설정 누락 시 원인 불명 | 풀 생성 실패와 커넥션 실패 이유를 `[DB]` 로그로 남긴다 |
| Xerox 결제 무한 대기 | 결과를 최대 120초 기다리고, 넘으면 `NO` 로 응답한다. 요청 전에 이전 결과를 초기화하고, 단말기 소켓을 닫는다 |
| 카드번호 로그 노출 | 카드번호는 `Mask.card()` 로 가려서 출력하고, SQL 문과 요청 본문은 더 이상 출력하지 않는다 |
| 커넥션·Statement 누수 | 모든 SQL 클래스가 try-with-resources 로 자원을 닫고 커넥션을 반납한다 |

교세라 사이버머니 결제(`/stech/kyocera/postCyberMoneyPayment`)도 같은 방식으로 고쳐서 켜 두었다.

프린터 상태 API(`/postPrinter`)는 /stech 버전에 없어서 뺐다. `Printer`, `PrinterResult`, `PrinterSQL` 클래스는 남아 있지만 쓰지 않는다.

### 남은 문제 (확인이 필요해서 고치지 않음)

1. **주소 불일치**: 컨트롤러 주소는 `/stech/printnuri/pricepolicy` 인데 `PrintPolicyPost` 는 중계 서버의 `/jetwork/printnuri/pricepolicy` 로 보낸다. `PrintCyberPOST` 는 `/jetwork//kyocera/...` 로 보낸다. 중계 서버가 이 서버와 같은 것인지 확인이 필요하다.
2. **Xerox 결제 상태가 static 변수 하나**: 단말기가 2대 이상이거나 결제가 동시에 들어오면 결과가 섞인다. 단말기별로 상태를 나눠야 한다.
3. 카드 결제마다 `PaymentCK` 에서 1초씩 기다린다 (중복 기록 방지용으로 보여 유지).
4. `System.out.println` 과 `logger` 가 섞여 있다.
5. Spring Boot 2.3 과 Java 8 은 지원이 끝났다. Spring Boot 3 + Java 17 이상으로 올리는 게 좋다.
