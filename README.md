# JetworkServer

무인 프린트·복사기 매장의 결제 서버입니다. 결제 단말기와 복사기 에이전트의 요청을 받아 (API 주소 `/jetwork/...`) MSSQL 에 결제·단말기·프린터 정보를 기록합니다.

실서버에서 돌고 있는 `JetworkServer-0.0.10-SNAPSHOT.jar` (2022-04-18 빌드)와 API 주소·응답 형식이 같고, jar 에 있던 문제(SQL 인젝션, 사이버머니 동시 결제, 결제 1초 대기 등)를 고친 버전입니다. jar 와 다르게 동작하는 부분은 docs/ARCHITECTURE.md 5장에 있습니다.

코드 구조, API 목록, DB 테이블, 알려진 문제는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 에 정리되어 있습니다.

## 실행 준비

1. DB 접속 정보 파일을 만듭니다.
   `src/main/resources/db.properties.example` 을 같은 폴더에 `db.properties` 로 복사하고 값을 채웁니다.
   이 파일은 `.gitignore` 에 들어 있어서 GitHub 에 올라가지 않습니다.
   운영 서버에서는 jar 와 같은 폴더에 `db.properties` 를 두면 jar 안의 값보다 먼저 읽습니다 (jar 에 비밀번호를 넣지 않아도 됩니다). 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 가 있으면 그것이 가장 우선입니다.
   로그는 실행한 폴더의 `logs/jetwork.log` 에 남습니다 (10MB 마다 새 파일, 30일·1GB 까지 보관).
   처리 과정을 모두 보려면 **전체 로그**를 켭니다: `java -jar JetworkServer-0.0.10-SNAPSHOT.jar --logging.level.jetwork-all=debug`
   (또는 `application.properties` 의 `#logging.level.jetwork-all=debug` 에서 `#` 를 지우고 재시작).
   HTTP 요청·응답 본문과 걸린 시간, 모든 SQL 과 넣은 값·결과 행 수·걸린 시간, commit/rollback, 소켓·중계 상세가 남고,
   같은 요청의 줄에는 같은 요청 번호(`#12` 등)가 붙습니다. 카드번호는 가려집니다. 로그가 많아지므로 문제를 볼 때만 켭니다.
   **디버깅 페이지**: `--jetwork.debug-ui=true` 로 켜고 브라우저에서 `http://서버:8090/jetwork/debug` 를 엽니다.
   서버 상태(DB 커넥션 풀·접속한 단말기·메모리), 최근 요청 200건(본문·응답·SQL), 전체 로그 켜기/끄기, 테스트 요청 보내기가 있습니다.
   **비밀번호가 없어 주소를 아는 사람은 누구나 볼 수 있으므로** 문제를 볼 때만 켜고, 다 보면 옵션을 빼고 재시작합니다.

2. 실행합니다.
   - 이클립스: `JetworkServerApplication` 우클릭 → Run As → Java Application (또는 Spring Boot App)
   - 명령줄: `./mvnw spring-boot:run` (Windows 는 `mvnw.cmd spring-boot:run`)

3. 열리는 포트
   - `8090`: HTTP API
   - `10020`: 결제 단말기 소켓 — 실서버 jar 처럼 기본은 열지 않습니다. 필요하면 VM 옵션 `-Dsocket.server=true`

## 이클립스 설정

모든 소스 파일은 UTF-8 입니다. 한글이 깨져 보이면 Window → Preferences → General → Workspace 의 Text file encoding 을 `UTF-8` 로 바꿔 주세요.

## 테스트

### 1. DB 없이 자동 테스트 (OfflineTest) — 코드를 고친 뒤 먼저 돌려 보세요

`src/test/java/com/jetwork/test/OfflineTest.java` 우클릭 → **Run As → Java Application**

메모리 위의 가짜 DB·가짜 중계 서버·가짜 결제 단말기를 띄우고, 서버를 그 가짜들에 붙여서(HTTP 18090 포트) 아래를 자동으로 확인합니다. 실제 DB·중계 서버·단말기에는 접속하지 않습니다.

- 모든 HTTP API 의 응답과 DB 기록 (사이버머니 차감·같은 결제번호 연속 결제·잔액 부족, 카드사명·해외카드, 티머니·캐시비·교통카드, last_dt, 버전 갱신, 프린터)
- 단말기 소켓(10020): 가격 정책 전문, Xerox 결제의 카드 승인·거절·취소, 티머니 승인·거절, 사이버머니 카드 태그, 중계 서버 오류

끝나면 콘솔에 `통과 55  실패 0` 처럼 나옵니다. 실패가 있으면 맨 아래에 실패한 항목이 다시 나옵니다.
같은 PC 에서 실제 서버나 결제 단말기 프로그램이 10020·10000 포트를 쓰고 있으면 소켓 테스트는 건너뜁니다.

### 1-1. DB 없이 스트레스 테스트 (OfflineStressTest, 1~2분)

`src/test/java/com/jetwork/test/OfflineStressTest.java` 우클릭 → **Run As → Java Application**

가짜 DB(쿼리마다 3ms)에 서버를 붙여 동시 100~200 요청으로 읽기 10,000건·결제 5,000건, 같은 승인번호 동시 요청·같은 결제번호 연속 결제·같은 카드 동시 결제,
부하 중 DB 3초 장애와 복구, 커넥션·스레드·소켓 누수를 확인합니다. 끝나면 `통과 19  실패 0`.

### 1-2. 실제 DB 연결·부하 테스트 (DbStressTest, 읽기 전용)

`db.properties` 에 실제 DB 접속 정보를 넣고 `DbStressTest.java` 우클릭 → **Run As → Java Application**

접속 시간, SQL Server 버전, 표 6개, 테스트 단말기(123456)·카드(8E156710) 데이터, 서버가 쓰는 쿼리별 응답 시간,
동시 50 × 2,000건 부하, 커넥션 반납을 확인합니다. **SELECT 만 하고 DB 에 아무것도 쓰지 않습니다.**
쿼리 하나가 평균 100ms 를 넘으면 "인덱스 확인 필요" 로 표시합니다. 바꾸기: `threads=100 requests=5000`

### 1-3. 실제 서버 부하 테스트 (StressTest)

`StressTest.java` 우클릭 → **Run As → Java Application** — 기본 서버 222.122.118.8, 동시 50 × 2,000건씩
단말기 확인·가격 조회·잔액 조회·섞어서 의 처리량과 응답 시간(50/95/99%)을 잽니다.
`write=true` 를 넣으면 카드 결제 기록도 requests 건 넣고, 끝나면 지우는 SQL 을 출력합니다. **운영 서버에서는 사람이 적은 시간에 돌리세요.**

### 2. 실제 서버·DB 로 확인 (PaymentTestClient)

서버를 띄운 뒤 `src/test/java/com/jetwork/test/PaymentTestClient.java` 를 우클릭 → **Run As → Java Application** 하면
단말기 확인, 가격 정책, 사이버머니(같은 결제번호 연속 결제 포함), 카드·티머니·캐시비·교통카드·KICC·프린트누리 결제 API 를 차례로 호출하고 통과/실패를 보여 줍니다.

기본으로 테스트 서버 `http://222.122.118.8:8090/jetwork` (소켓 222.122.118.8:10020), 단말기 123456 / erwerewrwerwer, 카드 8E156710 으로 보냅니다.
다른 서버·단말기로 하려면 **Run Configurations → Arguments** 에 넣습니다. (내 PC 에서 띄운 서버: `baseUrl=http://127.0.0.1:8090/jetwork socketHost=127.0.0.1`)

```
terminalID=123456 serial=erwerewrwerwer userCard=8E156710 socket=true
```

"3-1. 실제 결제 요청 형식" 은 실제 결제 에이전트가 보내는 요청(`test-requests/real-card-payment.json`: subJobData 배열, 숫자·true/false, R01~R30)을 단말기 ID·시리얼·승인번호만 바꿔서 그대로 보냅니다. 파일을 고치면 다른 형식도 시험할 수 있습니다.

실제 DB 에 기록되므로 테스트용 단말기·카드로만 실행하세요. Postman 을 쓰려면 `test-requests/JetworkServer.postman_collection.json` 을 Import 하면 됩니다.
