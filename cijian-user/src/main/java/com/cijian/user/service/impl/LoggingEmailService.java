package com.cijian.user.service.impl;

import com.cijian.user.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 默认邮件实现：将重置令牌输出到日志。
 * 待 SMTP 配置就绪后，SmptEmailService 会自动替代此实现。
 */
@Slf4j
@Service
@ConditionalOnMissingBean(name = "smtpEmailService")
public class LoggingEmailService implements EmailService {

    @Override
    public void sendPasswordResetEmail(String to, String nickname, String code) {
        log.info("========== 密码重置验证码 ==========");
        log.info("收件人: {} ({})", nickname, to);
        log.info("验证码: {} (有效期30分钟)", code);
        log.info("(配置 SMTP 后将自动发送邮件)");
        log.info("==================================");
    }

    @Override
    public void sendVerificationEmail(String to, String nickname, String code) {
        log.info("========== 邮箱验证码 ==========");
        log.info("收件人: {} ({})", nickname, to);
        log.info("验证码: {} (有效期24小时)", code);
        log.info("(配置 SMTP 后将自动发送邮件)");
        log.info("==================================");
    }
}
