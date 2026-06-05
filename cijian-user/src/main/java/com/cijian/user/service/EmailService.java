package com.cijian.user.service;

/**
 * 邮件发送服务接口。当前默认使用日志输出实现。
 * 待添加 spring-boot-starter-mail 依赖并配置 SMTP 后，自动切换为真实邮件发送。
 */
public interface EmailService {

    void sendPasswordResetEmail(String to, String nickname, String code);

    void sendVerificationEmail(String to, String nickname, String code);
}
