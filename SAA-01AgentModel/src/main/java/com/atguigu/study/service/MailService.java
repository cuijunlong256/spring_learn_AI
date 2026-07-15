package com.atguigu.study.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 基础邮件服务类（支持 HTML 格式）
 */
@Service
public class MailService {
    // 注入 Spring 自动配置的 JavaMailSender（无需手动创建）
    private final JavaMailSender mailSender;

    // 构造器注入（Spring 4.3+ 支持无 @Autowired 注解）
    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发送 HTML 格式邮件
     * @param to 收件人邮箱（单个收件人）
     * @param subject 邮件主题
     * @param htmlContent HTML 邮件内容
     * @throws MessagingException 邮件发送异常
     */
    public void sendHtmlMail(String to, String subject, String htmlContent) throws MessagingException {
        // 1. 创建 MimeMessage 对象（Spring 封装的邮件对象）
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        // 2. 使用 MimeMessageHelper 简化配置（关键工具类）
        // 参数说明：1. 邮件对象 2. 是否支持多部分内容（附件/图片）3. 编码格式
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom("cjldanlong256@163.com");  // 发件人（必须与配置文件中 username 一致）
        helper.setTo(to);  // 单个收件人
        // 若需群发：helper.setTo(new String[]{"a@xxx.com", "b@xxx.com"});
        helper.setSubject(subject);  // 邮件主题
        helper.setText(htmlContent, true);  // 第二个参数 true 表示内容按 HTML 解析
        // 3. 发送邮件（Spring 自动处理连接和关闭）
        mailSender.send(mimeMessage);
    }
}
