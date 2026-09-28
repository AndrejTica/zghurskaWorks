package com.miravale.portfolio.service;

import com.miravale.portfolio.model.ContactForm;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ContactEmailServiceTest {
    @Test
    void disablesDeliveryWhenCredentialsAreMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ContactEmailService service = new ContactEmailService(
                mailSender,
                "",
                "",
                "ann.drago.2002@gmail.com");

        assertThat(service.isEnabled()).isFalse();
        assertThatThrownBy(() -> service.send(new ContactForm()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Contact email delivery is not configured.");
        verifyNoInteractions(mailSender);
    }
}
