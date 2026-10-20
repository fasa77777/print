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

2. 실행합니다.
   - 이클립스: `JetworkServerApplication` 우클릭 → Run As → Java Application (또는 Spring Boot App)
   - 명령줄: `./mvnw spring-boot:run` (Windows 는 `mvnw.cmd spring-boot:run`)

3. 열리는 포트
   - `8090`: HTTP API
   - `10020`: 결제 단말기 소켓 — 실서버 jar 처럼 기본은 열지 않습니다. 필요하면 VM 옵션 `-Dsocket.server=true`

## 이클립스 설정

모든 소스 파일은 UTF-8 입니다. 한글이 깨져 보이면 Window → Preferences → General → Workspace 의 Text file encoding 을 `UTF-8` 로 바꿔 주세요.

## 테스트

서버를 띄운 뒤 `src/test/java/com/jetwork/test/PaymentTestClient.java` 를 우클릭 → **Run As → Java Application** 하면
단말기 확인, 가격 정책, 사이버머니(중복 결제번호 확인 포함), 카드·티머니·캐시비·교통카드·KICC·프린트누리 결제 API 를 차례로 호출하고 통과/실패를 보여 줍니다. 

단말기 ID 등은 **Run Configurations → Arguments** 에 넣습니다.

```
terminalID=테스트단말기ID serial=시리얼 userCard=테스트카드 socket=true
```

실제 DB 에 기록되므로 테스트용 단말기·카드로만 실행하세요. Postman 을 쓰려면 `test-requests/JetworkServer.postman_collection.json` 을 Import 하면 됩니다.

