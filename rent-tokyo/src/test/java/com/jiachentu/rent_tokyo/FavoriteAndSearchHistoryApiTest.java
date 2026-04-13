package com.jiachentu.rent_tokyo;

import com.jiachentu.rent_tokyo.entity.Property;
import com.jiachentu.rent_tokyo.entity.User;
import com.jiachentu.rent_tokyo.repository.FavoriteRepository;
import com.jiachentu.rent_tokyo.repository.PropertyRepository;
import com.jiachentu.rent_tokyo.repository.SearchHistoryRepository;
import com.jiachentu.rent_tokyo.repository.UserRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class FavoriteAndSearchHistoryApiTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

        @Autowired
        private FavoriteRepository favoriteRepository;

        @Autowired
        private SearchHistoryRepository searchHistoryRepository;

    private Long userId;
    private Long propertyId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        favoriteRepository.deleteAll();
        searchHistoryRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .email("favorite.user@example.com")
                .passwordHash("noop")
                .displayName("Favorite User")
                .build());

        Property property = propertyRepository.save(Property.builder()
                .name("Sunshine Mansion")
                .address("Tokyo Shibuya")
                .ward("渋谷区")
                .layout("1K")
                .rent(120000)
                .walkMinutes(8)
                .build());

        userId = user.getId();
        propertyId = property.getId();
    }

    @Test
    void favoriteApi_shouldCreateListAndDelete() throws Exception {
        String createBody = """
                {
                  "propertyId": %d
                }
                """.formatted(propertyId);

        mockMvc.perform(post("/api/favorites")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.propertyId").value(propertyId));

        mockMvc.perform(get("/api/favorites")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].propertyId").value(propertyId));

        mockMvc.perform(delete("/api/favorites/{propertyId}", propertyId)
                        .param("userId", userId.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/favorites")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void searchHistoryApi_shouldCreateListAndDelete() throws Exception {
        String createBody = """
                {
                  "ward": "渋谷区",
                  "rentMin": 90000,
                  "rentMax": 150000,
                  "layout": "1K",
                  "walkMinutesMax": 12
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/search-histories")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.ward").value("渋谷区"))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        Long historyId = extractId(responseBody);

        mockMvc.perform(get("/api/search-histories")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(historyId))
                .andExpect(jsonPath("$[0].ward").value("渋谷区"));

        mockMvc.perform(delete("/api/search-histories/{historyId}", historyId)
                        .param("userId", userId.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/search-histories")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    private Long extractId(String responseBody) {
                Matcher matcher = Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)").matcher(responseBody);
                if (matcher.find()) {
                        return Long.parseLong(matcher.group(1));
                }
                throw new IllegalStateException("Failed to parse history id from response");
    }
}
