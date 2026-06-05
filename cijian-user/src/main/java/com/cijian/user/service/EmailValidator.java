package com.cijian.user.service;

import com.cijian.common.enums.ResultCode;
import com.cijian.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Set;

/**
 * 注册时校验邮箱真实性：MX 记录 + 一次性邮箱拦截。
 */
@Slf4j
@Component
public class EmailValidator {

    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
            "mailinator.com", "guerrillamail.com", "guerrillamail.net", "guerrillamail.org",
            "tempmail.com", "tempmail.org", "tempmail.net", "10minutemail.com", "10minutemail.org",
            "yopmail.com", "yopmail.fr", "yopmail.net", "throwaway.email",
            "sharklasers.com", "grr.la", "trashmail.com", "trashmail.net",
            "dispostable.com", "maildrop.cc", "getairmail.com", "emailondeck.com",
            "temp-mail.org", "temp-mail.ru", "tempemail.co", "tempail.com",
            "moakt.com", "moakt.co", "dropmail.me", "fakeinbox.com",
            "anonaddy.com", "anonaddy.me", "simplelogin.com", "simplelogin.co",
            "33mail.com", "spamgourmet.com", "spamgourmet.net", "spamgourmet.org",
            "mytemp.email", "tempmailaddress.com", "temporary-mail.net",
            "emailfake.com", "generator.email", "mailsac.com", "mailnesia.com",
            "mailcatch.com", "mytrashmail.com", "sharkmail.com", "deadfake.cf",
            "deadfake.ga", "deadfake.tk", "trash-can-mail.com", "trashmail.de",
            "wegwerfmail.de", "wegwerfmail.net", "wegwerfmail.org",
            "mail-temporaire.fr", "jetable.org", "jetable.net", "no-spam.ws",
            "spam4.me", "spamdecoy.net", "tempr.email", "tempinbox.com"
    );

    /**
     * 校验邮箱合法性。格式校验由 @Email 注解完成，此处校验域名真实性和是否临时邮箱。
     */
    public void validate(String email) {
        String domain = extractDomain(email);

        if (DISPOSABLE_DOMAINS.contains(domain.toLowerCase())) {
            throw new BizException(ResultCode.BIZ_ERROR.getCode(), "不允许使用一次性邮箱");
        }

        if (!hasMxRecord(domain)) {
            throw new BizException(ResultCode.BIZ_ERROR.getCode(), "邮箱域名不存在，无法接收邮件");
        }
    }

    private String extractDomain(String email) {
        int at = email.lastIndexOf('@');
        return at < 0 ? email : email.substring(at + 1).trim().toLowerCase();
    }

    private boolean hasMxRecord(String domain) {
        try {
            var env = new java.util.Hashtable<String, String>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            var dirContext = new InitialDirContext(env);
            Attributes attrs = dirContext.getAttributes(domain, new String[]{"MX"});
            return attrs != null && attrs.get("MX") != null;
        } catch (javax.naming.NameNotFoundException e) {
            return false;
        } catch (Exception e) {
            log.warn("MX lookup failed for {}: {}", domain, e.getMessage());
            return true; // DNS 查询失败时放行，避免网络问题影响注册
        }
    }
}
