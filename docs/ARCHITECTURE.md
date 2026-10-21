# JetworkServer 코드 구조

실서버에서 돌고 있는 `JetworkServer-0.0.10-SNAPSHOT.jar` (2022-04-18 빌드)와 **API 주소·요청·응답 형식이 같고**, jar 에 있던 문제를 고친 버전입니다.
jar 와 다르게 동작하는 부분은 "5. 실서버 jar 와 다른 점"에 모두 적었습니다.

## 1. 전체 흐름

```
단말기·복사기 에이전트 ──HTTP(8090)──▶ PostJetworkController (/jetwork/...) ──▶ com.jetwork.sql.*SQL ──▶ MSSQL
                                                                                  (DBCPConnectionMgr = HikariCP 커넥션 풀)
결제 단말기 ──소켓(10020)──▶ SocketServerDriver ──▶ 중계 서버(pay1.printnuri.com:8090/jetwork)
                              ※ 실서버 jar 처럼 기본은 시작하지 않음 (-Dsocket.server=true 일 때만)
```

## 2. 패키지

| 패키지 | 내용 |
|---|---|
| `com.jetwork.bin` | 시작 클래스, 컨트롤러, 단말기 상태(TerminalState/TerminalRegistry), 결제 요청 전문(ReqCharge) |
| `com.jetwork.sql` | DB 접근. 모든 SQL 은 PreparedStatement. DbConfig(접속 정보), DBCPConnectionMgr(HikariCP 커넥션 풀), CardPaymentLedger·CyberMoneyLedger(공통 SQL) |
| `com.jetwork.model` | 요청·응답 모델 (실서버 jar 와 같은 필드) |
| `com.stech.Network` | 결제 단말기 소켓 서버 |
| `co.kr.json` | 중계 서버로 보내는 JSON (JsonHttpClient 공통) |
| `co.kr.up.util` | 바이트·16진수 변환, 카드번호 가리기(Mask) |

실서버 jar 에 있던 쓰지 않는 클래스(`com.jetwork.controller.*`, `PostAPIController`, `Blog`, `DbTest`, `DBConnectionManager`, `UserSQL`, `TerminalLicenseSQL`, `PostServer` 등)는 뺐습니다. jar 에서도 Spring 이 찾지 못하거나 아무도 부르지 않던 클래스입니다.

## 3. HTTP API (주소 앞에 `/jetwork`, 실서버 jar 와 같음)

| 주소 | 하는 일 | 응답 |
|---|---|---|
| `/postTerminal` | 단말기 ID+시리얼 확인, update 갱신. agent_version·terminalVersion 은 값이 왔을 때만 갱신 | OK / FALSE (DB 오류도 FALSE) |
| `/postCyberMoney` | 사이버머니 잔액 조회. 처음 보는 카드는 0원 등록 | result=OK, money |
| `/postCyberMoneyPayment` | 사이버머니 결제 | 아래 표 |
| `/kyocera/postCyberMoneyPayment` | 교세라 사이버머니 결제 (색상 1/2 → true/false) | 아래 표 (balance 는 0) |
| `/postCraditPayment` | 카드·티머니·캐시비·교통카드 결제 기록 | 항상 OK |
| `/kicc/postCraditPayment` | KICC 단말기 결제 기록 | 항상 OK |
| `/printnuri/pricepolicy` | A4 흑백·컬러 가격. 버전(agent_version·terminalVersion)은 값이 왔을 때만 갱신. 가격이 없으면 각각 60원 | OK, a4color, a4mono |
| `/printnuri/payment` | 프린트누리 결제 기록 | 항상 OK |
| `/xerox/xeroxPayment` | 결제 단말기에 결제 요청 후 결과 대기 (소켓 서버를 켰을 때만 동작) | OK / NO |
| `/postPrinter` | 프린터 상태 등록·갱신 | 이미 있던 프린터면 OK, 새로 등록하면 FALSE (jar 와 같음) |

사이버머니 결제 응답

| 상황 | result | reason | balance |
|---|---|---|---|
| 결제 완료 (잔액과 금액이 같아 0원이 되는 경우 포함) | OK | ok | 결제 후 잔액 |
| 잔액 부족 | FAIL | no money | 잔액 - 금액 (음수, jar 와 같음) |
| 미등록 카드 (0원으로 새로 등록) | FAIL | no card | 0 |
| 같은 카드·결제번호(cyberNo)로 이미 처리됨 | FAIL | duplicate | 0 |
| 금액 형식 오류, DB 오류 | FAIL | error | 0 |

## 4. 결제 기록 (jw_payment)

| 결제 | payment_type | payment_cardName | payment_ok |
|---|---|---|---|
| 카드 (R11=Y/N) | 체크카드 / 신용카드 | 카드사명, 모르는 코드는 해외카드 | 승인번호(R09). 오늘 같은 승인번호가 있으면 기록하지 않음 |
| 티머니 (R33=TM) | 티머니 | 티머니 | 현재 시각(밀리초) |
| 캐시비 (R33=EB) | 캐시비 | 캐시비 | 현재 시각(밀리초) |
| 그 밖의 교통카드 | 교통카드 | 교통카드 | 현재 시각(밀리초) |
| 사이버머니 | 사이버머니 | 가맹점명 | cyberNo |

결제를 jw_payment 에 기록하면 그 단말기의 마지막 결제 시간 `jw_terminal.last_dt` 를 지금으로 갱신한다. 중복·잔액 부족·미등록 카드처럼 기록하지 않은 경우는 갱신하지 않는다. 결제 기록을 먼저 커밋한 뒤 갱신하므로, last_dt 갱신에 실패해도 결제 기록은 남는다.

## 5. 실서버 jar 와 다른 점

응답 형식(필드, 숫자/문자 타입)은 같습니다. 같은 요청 23개를 실서버 jar 클래스와 이 코드에 보내 응답과 DB 기록을 비교했고, 다른 곳은 아래뿐입니다.

| 상황 | 실서버 jar | 이 코드 |
|---|---|---|
| 사이버머니 잔액과 결제 금액이 같음 | 결제 거절 (no card) | 결제됨 (잔액 0원) |
| 사이버머니 같은 결제번호 재요청 | FAIL, no card ("등록되지 않은 카드") | FAIL, duplicate ("이미 처리된 결제") |
| 사이버머니 DB 오류 | 500 오류 | FAIL, error |
| 모르는 카드사 코드 | 카드사명 빈 값 | 해외카드 |
| 티머니·캐시비가 아닌 교통카드 | 기록 안 됨 | 교통카드로 기록 |
| R11 이 Y/N 이 아니고 R33 이 없음 | 서버 오류(NullPointerException) | 교통카드로 기록 |
| 승인번호(R09)가 빈 카드 결제 | 하루 첫 건만 기록 | 모두 기록 |
| 같은 승인번호 카드 결제 중복 확인 | 결제마다 1초 대기, 기록을 지울 수 있음 | 대기 없이 잠금으로 한 건만 기록, 지우지 않음 |
| 빈 카드번호 | cybermoney 에 빈 카드로 등록 | 등록하지 않음 (응답은 같은 no card) |
| 결제 기록 후 jw_terminal.last_dt | 갱신 안 함 | 마지막 결제 시간으로 갱신 |
| 가격 정보 없는 단말기 (`/printnuri/pricepolicy`) | a4color·a4mono 를 null 로 응답 → 매장 에이전트가 기본 5원으로 출력 | 없는 쪽을 60원으로 응답, 서버 로그에 경고 |
| 요청에 버전 값이 없을 때 | agent_version·terminalVersion 을 null(문자열 'null') 또는 기본값 1.1.1 / 1052 로 덮어씀 | 기존 값 유지 (값이 온 것만 갱신) |
| 요청에 값이 없을 때 (null) | 문자열 'null' 로 기록 | NULL 로 기록 |
| DB 접속 정보 | 소스에 직접 적혀 있음 | db.properties·환경변수 |
| 커넥션 풀 | commons-dbcp 1.4 (쓰지 않는 Spring Hikari 풀도 따로 만듦) | HikariCP 하나만 |

## 6. 고친 문제

| 문제 | 수정 |
|---|---|
| SQL 인젝션 | 모든 SQL 을 PreparedStatement 로 |
| 사이버머니 동시 결제 시 한 번만 차감 | 잔액 확인과 차감을 UPDATE 한 번으로 (`... WHERE CAST(cyberMoney AS INT) >= 금액`) |
| 오류 시 rollback 없음, 커넥션·Statement 누수 | try-with-resources, 오류 시 rollback |
| 커넥션 풀(commons-dbcp 1.4, 2010년·지원 종료): 풀 전체 잠금 하나로 동시 요청이 서로 기다림, 빌릴 때마다 SELECT 1 추가 왕복, 쉬는 커넥션 점검 1분에 3개뿐, 점검 시간 제한 없음 | HikariCP 로 교체 (Spring Boot 기본 풀, 이미 프로젝트에 포함). 아래 "커넥션 풀" 참고 |
| 커넥션 풀이 가득 차면 무한 대기 | 최대 10초 기다린 뒤 오류로 응답 |
| DB 응답이 없으면 요청이 무한 대기, 잠금 무한 대기 | 접속 10초, 쿼리 15초, 잠금 5초 제한 (`DBCPConnectionMgr` 가 URL 에 옵션 추가) |
| 카드번호·SQL 이 콘솔에 그대로 출력 | SLF4J 로그, 카드번호는 가려서 출력 |
| (소켓) 한 번에 한 단말기씩 처리, 읽기 제한 없음 | 작업 스레드 50개, 읽기 10초 제한, 나눠서 오는 전문도 끝까지 읽음 |
| (소켓) 단말기 정보·결과를 static 변수 하나로 같이 씀 | 단말기(IP)마다 따로 관리 (TerminalState) |
| (Xerox) 결과가 안 오면 무한 대기, 금액 오류·단말기 접속 실패 시 500 | 최대 120초 대기 후 NO, 단말기 접속 5초 제한, 오류는 NO |
| (Xerox) 결제가 끝나도 금액이 남아 이후 카드 태그 시 이전 금액으로 결제 | 결제가 끝나면 작업을 지움 |
| (소켓) 티머니 거절 판별 오류 | R32 첫 바이트가 0xFF 일 때만 거절 |
| (소켓) 사이버머니 결제번호가 1초 안에 겹침 | 같은 형식으로 겹치지 않게 (CyberNo) |
| 중계 서버가 멈추면 소켓 처리 전체가 멈춤 | 중계 서버 요청에 접속 5초·응답 15초 제한, UTF-8 전송 |
| 중계 서버가 오류(200 이 아닌 응답)를 주면 소켓이 GC 될 때까지 열린 채 남음 (오류 300번에 소켓 254개) | 오류 응답 본문도 끝까지 읽고 닫음, 예외 때는 연결을 끊음 |
| 로그가 콘솔에만 찍혀, 파일로 돌려 실행하면 한 파일이 디스크가 찰 때까지 커짐 | `logs/jetwork.log` 에 기록, 10MB 마다 새 파일, 30일·전체 1GB 까지만 보관 |
| DB 비밀번호가 jar 안에 들어감 (src/main/resources/db.properties 를 넣고 빌드) | jar 옆(실행한 폴더)의 db.properties 나 `-Ddb.config=경로` 를 먼저 읽음. jar 에 넣지 않아도 됨 |
| (소켓) 한 번 접속한 IP 의 단말기 상태가 영원히 남아, IP 가 바뀔 때마다 목록이 늘어남 | 24시간 넘게 접속이 없고 결제 중이 아닌 단말기 상태는 정리 |

## 7. 커넥션 풀 (DBCPConnectionMgr, HikariCP)

| 설정 | 값 | 뜻 |
|---|---|---|
| 최대 커넥션 | 200 (`db.pool.maxActive`) | 실서버 jar 와 같음 |
| 최소 유휴 커넥션 | 10 (`db.pool.minIdle`) | 한가할 때도 열어 두는 수. 요청이 몰리면 최대까지 늘어남 |
| 커넥션 대기 | 10초 | 모두 사용 중이면 10초 기다린 뒤 실패 (jar 는 무한 대기) |
| 살아 있는지 확인 | 3초 안에 | 0.5초 넘게 쉰 커넥션을 빌려줄 때만 확인 (jar 는 매번 SELECT 1) |
| 유휴 정리 | 3분 | 3분 넘게 쉰 커넥션은 닫음 (최소 유휴 수는 남김) |
| 최대 수명 | 20분 | 오래된 커넥션은 쉬는 동안 새 것으로 교체 |
| 반납 누락 경고 | 60초 | 커넥션을 60초 넘게 쥐고 있으면 로그에 경고 (동작에는 영향 없음) |
| 서버 시작 때 DB 꺼짐 | 서버는 뜸 | DB 가 살아나면 재시작 없이 접속 |
| 커밋 안 하고 반납 | 자동 rollback | 다음에 빌리는 쪽은 auto commit 기본값 |
| URL 접속 옵션 | loginTimeout=10, socketTimeout=15000, lockTimeout=5000, sendStringParametersAsUnicode=false | URL 에 없으면 자동으로 붙임 |

커넥션을 못 빌리면 로그에 `[DB] 커넥션을 가져오지 못했습니다: ... (사용 n / 쉬는 중 n / 대기 n / 최대 200)` 처럼 풀 상태가 같이 찍힌다.

## 8. 설정

| 항목 | 내용 |
|---|---|
| `src/main/resources/application.properties` | 포트 8090, 로그 파일(`logs/jetwork.log`, 10MB·30일·1GB) |
| `db.properties` (jar 와 같은 폴더) 또는 `-Ddb.config=경로` | **운영 서버 권장.** jar 안의 값보다 우선. jar 에 비밀번호가 들어가지 않는다 |
| `src/main/resources/db.properties` | **Git 에 올리지 않음.** 이클립스에서 실행할 때 쓰기 좋음. `db.properties.example` 을 복사해서 `db.url`, `db.username`, `db.password` 를 채운다. 풀 크기 `db.pool.maxActive` / `db.pool.minIdle` (기본 200 / 10) |
| 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 있으면 db.properties 보다 우선 |
| `db.driver` (db.properties) | JDBC 드라이버 클래스. 보통은 쓰지 않음 (기본 MSSQL). 테스트 프로그램 OfflineTest 가 가짜 DB 드라이버를 지정할 때 쓴다 |
| 실행 옵션 `-Dsocket.server=true` | 결제 단말기 소켓 서버(10020) 시작. 기본은 꺼짐 (jar 와 같음) |
| 실행 옵션 `-Drelay.baseUrl=...` | 소켓 서버가 결과를 보내는 중계 서버 주소 (기본 `http://pay1.printnuri.com:8090/jetwork`) |

## 9. 남은 문제

1. Spring Boot 2.3 과 Java 8 은 지원이 끝났다. 운영 서버의 Java 버전을 확인한 뒤 올리는 게 좋다.
2. 소켓 서버를 켜고 Xerox 결제를 쓸 때, 복사기 에이전트와 결제 단말기의 IP 가 다르면 결제 요청에 `terminalID` 를 넣어야 정확한 단말기로 간다.
3. `/postPrinter` 는 jar 와 같이 `[SONTECH].[dbo].[printer]` 에서 등록 여부를 보고, 현재 DB 의 `[dbo].[printer]` 에 기록한다. 두 DB 가 다르면 확인이 필요하다.
