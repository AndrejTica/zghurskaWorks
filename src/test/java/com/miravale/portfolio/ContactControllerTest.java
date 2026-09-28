package com.miravale.portfolio;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ContactControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void sendsValidContactMessageToConfiguredRecipient() throws Exception {
        mockMvc.perform(post("/contact")
                        .with(csrf())
                        .param("name", "Portfolio Visitor")
                        .param("email", "visitor@example.com")
                        .param("message", "I would like to discuss an artwork."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/#contact"))
                .andExpect(flash().attribute("contactSuccess", "Thank you. Your message has been sent."));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly("ann.drago.2002@gmail.com");
        assertThat(message.getReplyTo()).isEqualTo("visitor@example.com");
        assertThat(message.getText()).contains("Portfolio Visitor", "I would like to discuss an artwork.");
    }

    @Test
    void rejectsInvalidContactMessageWithoutSendingEmail() throws Exception {
        mockMvc.perform(post("/contact")
                        .with(csrf())
                        .param("name", "")
                        .param("email", "not-an-email")
                        .param("message", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/#contact"))
                .andExpect(flash().attribute(
                        "contactError",
                        "Please provide a valid name, email address, and message."));

        verifyNoInteractions(mailSender);
    }
}
