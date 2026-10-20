/*
 * 실서버 JetworkServer-0.0.10-SNAPSHOT.jar (2022-04-18 빌드) 에서 역컴파일한 소스 (CFR 0.152).
 */
package com.jetwork.bin;

import com.jetwork.bin.ConnectionPool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
// application.properties 의 ${db.url} 등을 db.properties 에서 채운다. (실서버 jar 에는 접속 정보가 직접 적혀 있었음)
@PropertySource(value={"classpath:db.properties"}, ignoreResourceNotFound=true)
public class JetworkServerApplication {
    public static String TerminalIP = "";
    public static String terID = "";
    public static boolean chk = false;
    public static boolean chk1 = false;
    public static boolean result = false;
    public static boolean chk2 = true;
    public static int printPrice = 0;
    public static String printColor = "";
    public static String printPaperSize = "";
    public static String printCount = "";

    public static void main(String[] args) {
        SpringApplication.run(JetworkServerApplication.class, (String[])args);
        ConnectionPool cp = new ConnectionPool();
        cp.connection();
        // 결제 단말기 소켓 서버(10020). 실서버 jar 처럼 기본은 시작하지 않는다.
        // 필요하면 실행 옵션 -Dsocket.server=true 로 켠다. (jar 에는 없던 옵션)
        if (Boolean.getBoolean("socket.server")) {
            Thread socketThread = new Thread(new com.stech.Network.SocketServerDriver());
            socketThread.setName("SocketServerDriver #1");
            socketThread.start();
        }
    }
}
