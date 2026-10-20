# JetworkServer 코드 구조

이 저장소의 `src/main/java` 는 **실서버에서 돌고 있는 `JetworkServer-0.0.10-SNAPSHOT.jar` (2022-04-18 빌드)를 역컴파일한 소스**입니다.
동작은 실서버 jar 와 같습니다. jar 와 다른 점은 아래 "5. jar 와 다른 점" 세 가지뿐입니다.

이전에 GitHub 에서 고쳤던 버전(`/stech` 주소, 버그 수정 포함)은 `fix/full-review` 브랜치(PR #11)에 남아 있습니다.

## 1. 전체 흐름

```
단말기·복사기 에이전트 ──HTTP(8090)──▶ PostJetworkController (/jetwork/...) ──▶ com.jetwork.sql.*SQL ──▶ MSSQL
                                                                                  (DBCPConnectionMgr 커넥션 풀)
결제 단말기 ──소켓(10020)──▶ SocketServerDriver   ※ 실서버 jar 는 시작하지 않음 (-Dsocket.server=true 일 때만)
```

## 2. HTTP API (`PostJetworkController`, 주소 앞에 `/jetwork`)

| 주소 | 요청 모델 | 하는 일 |
|---|---|---|
| `/postTerminal` | Terminal | jw_terminal 에 단말기 ID+시리얼이 있으면 agent_version·terminalVersion·update 갱신 후 OK, 없으면 FALSE |
| `/postCyberMoney` | CyberMoney | 사이버머니 잔액 조회. 처음 보는 카드는 0원으로 등록 |
| `/postCyberMoneyPayment` | PaymentKicc | 사이버머니 결제 (아래 응답 규칙) |
| `/postCraditPayment` | PaymentKicc | 카드(R11=Y/N)·티머니(R33=TM)·캐시비(EB) 결제 기록. 응답은 항상 OK |
| `/kyocera/postCyberMoneyPayment` | PaymentKyocera | 교세라 사이버머니 결제 (색상 1/2 → true/false) |
| `/kicc/postCraditPayment` | PaymentCreaditKicc | KICC 단말기 결제 기록. 응답은 항상 OK |
| `/printnuri/pricepolicy` | A4Terminal | jw_paper 의 A4 흑백·컬러 가격 조회, 단말기 버전 갱신 |
| `/printnuri/payment` | PrintNuriPay | 프린트누리 결제 기록. 응답은 항상 OK |
| `/xerox/xeroxPayment` | XeroxPay | 결제 단말기(10000번 포트)에 결제 요청 후 결과 대기. 소켓 서버가 꺼져 있으면 동작하지 않음 |
| `/postPrinter` | Printer | 프린터 상태를 printer 테이블에 등록·갱신 |

사이버머니 결제 응답 (`/postCyberMoneyPayment`, `/kyocera/postCyberMoneyPayment`)

| 상황 | result | reason |
|---|---|---|
| 차감 후 잔액이 0원보다 큼 | OK | ok |
| 차감 후 잔액이 0원보다 작음 | FAIL | no money |
| 미등록 카드 (0원으로 새로 등록), 결제 후 잔액이 정확히 0원, 같은 카드·cyberNo 로 이미 결제됨 | FAIL | no card |

`/postCyberMoneyPayment` 응답의 `balance` 는 숫자다 (잔액 부족이면 차감 후 값 = 음수).

`com.jetwork.controller` 패키지와 `com.jetwork.bin.PostAPIController` 는 Spring 이 찾지 못하는 위치이거나 `@RestController` 가 없어서 실서버에서도 쓰이지 않는다.

## 3. DB

- 커넥션 풀: `DBCPConnectionMgr` (commons-dbcp 1.4, 최대 200 / 유휴 50)
- Spring DataSource(Hikari)도 `application.properties` 의 `spring.datasource.*` 로 만들어지지만 코드에서 쓰지 않는다 (실서버 jar 와 같음)
- `DBConnectionManager` 는 쓰이지 않는 예전 커넥션 관리 클래스 (`com/jetwork/sql/db.properties` 의 `ip` 를 읽음)

| 테이블 | 쓰는 곳 |
|---|---|
| jw_terminal | 단말기 확인, 버전 갱신, 가맹점 ID |
| jw_payment | 모든 결제 기록 |
| cybermoney | 사이버머니 잔액 |
| jw_franchise | 사이버머니 결제 기록의 가맹점명 |
| jw_paper | 단말기별 용지 가격 |
| printer | 프린터 상태 |

## 4. 설정

| 파일 | 내용 |
|---|---|
| `src/main/resources/application.properties` | 포트 8090, Spring DataSource (접속 정보는 db.properties 에서) |
| `src/main/resources/db.properties` | **Git 에 올리지 않음.** `db.properties.example` 을 복사해서 `db.url` / `db.username` / `db.password` 를 채운다 |
| 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 있으면 db.properties 보다 우선 |
| 실행 옵션 `-Dsocket.server=true` | 결제 단말기 소켓 서버(10020) 시작. 기본은 꺼짐 (실서버 jar 와 같음) |

## 5. jar 와 다른 점

1. **DB 접속 정보**: jar 는 `DBCPConnectionMgr`, `DBConnectionManager`, `application.properties` 에 접속 정보가 직접 적혀 있었다. 소스에서 빼고 `DbConfig`(새 클래스)가 db.properties·환경변수에서 읽는다. `JetworkServerApplication` 에 `@PropertySource("classpath:db.properties")` 를 붙였다.
2. **소켓 서버 옵션**: `-Dsocket.server=true` 로 켤 수 있게 했다. 옵션이 없으면 jar 와 같이 시작하지 않는다.
3. **SocketServerDriver 의 try 블록**: 역컴파일 결과가 그대로는 컴파일되지 않아 원래 형태인 try-with-resources 로 고쳐 썼다. 동작은 같다.

새로 컴파일한 클래스를 다시 역컴파일해 jar 의 클래스와 비교했고, 위 세 가지 외에는 컴파일러 차이(반복문 모양, 문자열 상수 합치기)뿐이다.

## 6. 실서버 jar 에 있는 알려진 문제 (그대로 둠)

jar 와 똑같이 맞췄기 때문에 아래 문제도 그대로 있다. 고친 버전은 PR #11 참고.

1. SQL 을 문자열로 이어 붙여 만든다 (SQL 인젝션 가능, 값에 `'` 가 있으면 오류)
2. 사이버머니: 잔액을 읽고 계산해서 다시 쓰므로 같은 카드로 동시에 결제하면 한 번만 차감될 수 있다. 결제 후 잔액이 정확히 0원이면 결제가 거절된다 (no card)
3. 카드 결제 중복 확인(`PaymentCK`)이 결제마다 1초씩 기다리고, 같은 승인번호 기록을 지울 수 있다
4. 승인번호(R09)가 빈 카드 결제는 하루 첫 건만 기록된다
5. 티머니·캐시비가 아닌 교통카드는 기록되지 않는다. R11 이 Y/N 이 아니고 R33 이 없으면 서버 오류(NullPointerException)
6. 모르는 카드사 코드는 카드사명이 빈 값으로 기록된다
7. 오류가 나도 rollback 하지 않고, 커넥션 풀이 가득 차면 무한 대기한다
8. 카드번호·SQL 문이 콘솔에 그대로 출력된다
9. 소켓 서버를 켜면: 한 번에 한 단말기씩 처리하고 읽기 제한 시간이 없다. 단말기 IP·결제 결과를 static 변수 하나로 같이 써서 단말기가 여러 대면 섞인다. Xerox 결제는 결과가 안 오면 무한 대기한다
