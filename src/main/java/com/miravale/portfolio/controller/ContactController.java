package com.miravale.portfolio.controller;

import com.miravale.portfolio.model.ContactForm;
import com.miravale.portfolio.service.ContactRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContactController.class);

    private final JavaMailSender mailSender;
    private final ContactRateLimiter rateLimiter;
    private final String sender;
    private final String recipient;

    public ContactController(
            JavaMailSender mailSender,
            ContactRateLimiter rateLimiter,
            @Value("${spring.mail.username}") String sender,
            @Value("${app.contact.recipient}") String recipient) {
        this.mailSender = mailSender;
        this.rateLimiter = rateLimiter;
        this.sender = sender;
        this.recipient = recipient;
    }

    @PostMapping("/contact")
    String submitContactForm(
            @Valid @ModelAttribute ContactForm contactForm,
            BindingResult bindingResult,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormError(
                    redirectAttributes,
                    contactForm,
                    "Please provide a valid name, email address, and message.");
            return "redirect:/#contact";
        }

        if (!rateLimiter.tryAcquire(request.getRemoteAddr())) {
            addFormError(
                    redirectAttributes,
                    contactForm,
                    "Too many messages were submitted. Please wait 15 minutes and try again.");
            return "redirect:/#contact";
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

        try {
            mailSender.send(email);
        } catch (MailException exception) {
            LOGGER.error("Contact email delivery failed", exception);
            addFormError(
                    redirectAttributes,
                    contactForm,
                    "The message could not be sent. Please email ann.drago.2002@gmail.com directly.");
            return "redirect:/#contact";
        }

        redirectAttributes.addFlashAttribute("contactSuccess", "Thank you. Your message has been sent.");
        return "redirect:/#contact";
    }

    private void addFormError(
            RedirectAttributes redirectAttributes,
            ContactForm contactForm,
            String errorMessage) {
        redirectAttributes.addFlashAttribute("contactError", errorMessage);
        redirectAttributes.addFlashAttribute("contactName", contactForm.getName());
        redirectAttributes.addFlashAttribute("contactEmail", contactForm.getEmail());
        redirectAttributes.addFlashAttribute("contactMessage", contactForm.getMessage());
    }
}
