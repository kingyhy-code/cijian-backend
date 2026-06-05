package com.cijian.user.service.impl;

import com.cijian.user.service.EmailService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * 原生 SMTP 发送实现，不依赖 spring-boot-starter-mail。
 * QQ邮箱需先在设置中生成授权码。
 * <p>
 * 配置示例：
 * <pre>
 * cijian:
 *   mail:
 *     enabled: true
 *     host: smtp.qq.com
 *     port: 587
 *     username: your@qq.com
 *     password: 授权码
 * </pre>
 */
@Slf4j
@Service("smtpEmailService")
@ConditionalOnProperty(name = "cijian.mail.enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {

    @Value("${cijian.mail.host:smtp.qq.com}")
    private String host;

    @Value("${cijian.mail.port:587}")
    private int port;

    @Value("${cijian.mail.username:}")
    private String username;

    @Value("${cijian.mail.password:}")
    private String password;

    @Value("${cijian.mail.skip-ssl-verify:false}")
    private boolean skipSslVerify;

    private SSLSocketFactory sslFactory;
    private boolean configured;

    @PostConstruct
    void init() {
        configured = username != null && !username.isBlank()
                && password != null && !password.isBlank();
        if (!configured) {
            log.warn("SMTP 未配置用户名/密码，邮件发送不可用。请在 application.yml 中配置 cijian.mail.*");
        }
        sslFactory = buildSslFactory();
    }

    private SSLSocketFactory buildSslFactory() {
        if (skipSslVerify) {
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, new TrustManager[]{new TrustAllManager()}, null);
                log.warn("SMTP SSL 证书校验已跳过（仅开发环境使用）");
                return ctx.getSocketFactory();
            } catch (Exception e) {
                log.error("Failed to create SSL context: {}", e.getMessage());
            }
        }
        return (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    private static class TrustAllManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] c, String a) {}
        public void checkServerTrusted(X509Certificate[] c, String a) {}
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }

    @Override
    public void sendPasswordResetEmail(String to, String nickname, String code) {
        String subject = "辞间 - 密码重置验证码";
        String body = buildResetEmailBody(nickname, code);
        send(to, subject, body);
    }

    @Override
    public void sendVerificationEmail(String to, String nickname, String code) {
        String subject = "辞间 - 邮箱验证码";
        String body = buildVerifyEmailBody(nickname, code);
        send(to, subject, body);
    }

    private String buildVerifyEmailBody(String nickname, String code) {
        return "您好 " + (nickname != null ? nickname : "用户") + "，\n\n"
                + "欢迎注册辞间！请使用以下验证码完成邮箱验证：\n\n"
                + "验证码：" + code + "（24 小时内有效）\n\n"
                + "如果这不是您本人的操作，请忽略此邮件。\n\n"
                + "—— 辞间";
    }

    private String buildResetEmailBody(String nickname, String code) {
        return "您好 " + (nickname != null ? nickname : "用户") + "，\n\n"
                + "您正在重置辞间账号的密码。\n\n"
                + "验证码：" + code + "（30 分钟内有效）\n\n"
                + "如果这不是您本人的操作，请忽略此邮件。\n\n"
                + "—— 辞间";
    }

    private void send(String to, String subject, String body) {
        if (!configured) {
            log.warn("SMTP not configured, skipping email to {}", to);
            return;
        }
        try {
            if (port == 465) {
                sendOverSsl(to, subject, body);
            } else {
                sendWithStartTls(to, subject, body);
            }
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private void sendWithStartTls(String to, String subject, String body) throws Exception {
        try (Socket socket = new Socket(host, port)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream out = socket.getOutputStream();

            readResponse(in, "220");
            sendCommand(out, "EHLO cijian");
            readMultiLineResponse(in);

            sendCommand(out, "STARTTLS");
            readResponse(in, "220");

            // Upgrade to SSL
            SSLSocketFactory factory = (SSLSocketFactory) sslFactory;
            SSLSocket sslSocket = (SSLSocket) factory.createSocket(
                    socket, host, port, true);
            sslSocket.startHandshake();
            in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            out = sslSocket.getOutputStream();

            doSendMail(in, out, to, subject, body);
            sslSocket.close();
        }
    }

    private void sendOverSsl(String to, String subject, String body) throws Exception {
        SSLSocketFactory factory = (SSLSocketFactory) sslFactory;
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            socket.startHandshake();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream out = socket.getOutputStream();

            readResponse(in, "220");
            sendCommand(out, "EHLO cijian");
            readMultiLineResponse(in);

            doSendMail(in, out, to, subject, body);
        }
    }

    private void doSendMail(BufferedReader in, OutputStream out,
                            String to, String subject, String body) throws Exception {
        sendCommand(out, "AUTH LOGIN");
        readResponse(in, "334");
        sendCommand(out, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
        readResponse(in, "334");
        sendCommand(out, Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));
        readResponse(in, "235");

        sendCommand(out, "MAIL FROM:<" + username + ">");
        readResponse(in, "250");
        sendCommand(out, "RCPT TO:<" + to + ">");
        readResponse(in, "250");

        sendCommand(out, "DATA");
        readResponse(in, "354");

        String content = buildMimeMessage(to, subject, body);
        out.write(content.getBytes(StandardCharsets.UTF_8));
        out.flush();

        sendCommand(out, "");
        sendCommand(out, ".");
        readResponse(in, "250");

        sendCommand(out, "QUIT");
    }

    private String buildMimeMessage(String to, String subject, String body) {
        return "From: 辞间 <" + username + ">\r\n"
                + "To: " + to + "\r\n"
                + "Subject: =?UTF-8?B?" + Base64.getEncoder().encodeToString(
                        subject.getBytes(StandardCharsets.UTF_8)) + "?=\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n"
                + "\r\n"
                + Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8))
                + "\r\n";
    }

    private void sendCommand(OutputStream out, String cmd) throws Exception {
        out.write((cmd + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void readResponse(BufferedReader in, String expectedCode) throws Exception {
        String line = in.readLine();
        if (line == null || !line.startsWith(expectedCode)) {
            throw new RuntimeException("SMTP error: " + line);
        }
    }

    private void readMultiLineResponse(BufferedReader in) throws Exception {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.length() < 4 || line.charAt(3) == ' ') break;
        }
    }
}
