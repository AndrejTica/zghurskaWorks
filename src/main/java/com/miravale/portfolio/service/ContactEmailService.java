package com.miravale.portfolio.service;

import com.miravale.portfolio.model.ContactForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContactEmailService {
    private final JavaMailSender mailSender;
    private final String sender;
    private final String password;
    private final String recipient;

    public ContactEmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String sender,
            @Value("${spring.mail.password}") String password,
            @Value("${app.contact.recipient}") String recipient) {
        this.mailSender = mailSender;
        this.sender = sender;
        this.password = password;
        this.recipient = recipient;
    }

    public boolean isEnabled() {
        return StringUtils.hasText(sender) && StringUtils.hasText(password);
    }

    public void send(ContactForm contactForm) {
        if (!isEnabled()) {
            throw new IllegalStateException("Contact email delivery is not configured.");
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(sender);
        email.setTo(recipient);
        email.setReplyTo(contactForm.getEmail().trim());
        email.setSubject("New portfolio contact message");
        email.setText("""
                Name: %s
                Email: %s

                Message:
                %s
                """.formatted(
                contactForm.getName().trim(),
                contactForm.getEmail().trim(),
                contactForm.getMessage().trim()));
        mailSender.send(email);
    }
}
