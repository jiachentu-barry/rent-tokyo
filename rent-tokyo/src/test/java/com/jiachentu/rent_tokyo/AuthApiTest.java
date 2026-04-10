package com.jiachentu.rent_tokyo;

import com.jiachentu.rent_tokyo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthApiTest {

    private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.deleteAll();
    }

    @Test
    void register_shouldCreateUserAndReturnToken() throws Exception {
        String body = """
                {
                  "email": "new.user@example.com",
                  "password": "password123",
                  "displayName": "New User"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").value("new.user@example.com"))
                .andExpect(jsonPath("$.displayName").value("New User"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void register_shouldRejectDuplicateEmail() throws Exception {
        String body = """
                {
                  "email": "dup.user@example.com",
                  "password": "password123",
                  "displayName": "Dup User"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void login_shouldReturnTokenWhenCredentialsValid() throws Exception {
        String registerBody = """
                {
                  "email": "login.user@example.com",
                  "password": "password123",
                  "displayName": "Login User"
                }
                """;

        String loginBody = """
                {
                  "email": "login.user@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").value("login.user@example.com"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void login_shouldReturnUnauthorizedWhenPasswordInvalid() throws Exception {
        String registerBody = """
                {
                  "email": "wrong.pass@example.com",
                  "password": "password123",
                  "displayName": "Wrong Pass"
                }
                """;

        String badLoginBody = """
                {
                  "email": "wrong.pass@example.com",
                  "password": "password999"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(badLoginBody)
                )
                .andExpect(status().isUnauthorized());
    }
}
