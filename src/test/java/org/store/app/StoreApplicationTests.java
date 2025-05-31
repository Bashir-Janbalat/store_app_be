package org.store.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.store.app.security.jwt.JwtTokenProvider;
import org.store.app.service.EmailService;
import org.store.app.service.PasswordResetTokenService;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class StoreApplicationTests {


    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    protected JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    protected PasswordResetTokenService passwordResetTokenService;
    @MockitoBean
    protected EmailService emailService;

    @Test
    void contextLoads() {
    }

}
