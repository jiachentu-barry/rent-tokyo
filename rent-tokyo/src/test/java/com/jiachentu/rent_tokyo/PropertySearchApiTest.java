package com.jiachentu.rent_tokyo;

import com.jiachentu.rent_tokyo.entity.Property;
import com.jiachentu.rent_tokyo.entity.PropertyChangeLog;
import com.jiachentu.rent_tokyo.repository.PropertyRepository;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PropertySearchApiTest {

    private MockMvc mockMvc;
    private Long propertyId;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PropertyRepository propertyRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        propertyRepository.deleteAll();

        Property property = Property.builder()
                .name("Price Track Mansion")
                .address("Tokyo Minato")
                .ward("港区")
                .layout("1LDK")
                .rent(105000)
                .walkMinutes(6)
                .build();

        PropertyChangeLog firstChange = PropertyChangeLog.builder()
                .property(property)
                .oldRent(120000)
                .newRent(110000)
                .build();

        PropertyChangeLog secondChange = PropertyChangeLog.builder()
                .property(property)
                .oldRent(110000)
                .newRent(105000)
                .build();

        LinkedHashSet<PropertyChangeLog> changeLogs = new LinkedHashSet<>();
        changeLogs.add(firstChange);
        changeLogs.add(secondChange);
        property.setChangeLogs(changeLogs);

        propertyId = propertyRepository.save(property).getId();
    }

    @Test
    void manual_search_preview() throws Exception {
        mockMvc.perform(
                        get("/api/properties/search")
                                .param("ward", "渋谷区")
                                .param("rentMin", "80000")
                                .param("rentMax", "130000")
                                .param("layout", "1K")
                                .param("walkMinutesMax", "10")
                                .param("page", "0")
                                .param("size", "5")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void priceHistoryApi_shouldReturnPriceChangesForProperty() throws Exception {
        mockMvc.perform(get("/api/properties/{propertyId}/price-history", propertyId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].oldRent").value(110000))
                .andExpect(jsonPath("$[0].newRent").value(105000))
                .andExpect(jsonPath("$[1].oldRent").value(120000))
                .andExpect(jsonPath("$[1].newRent").value(110000));
    }

    @Test
    void propertyDetailApi_shouldReturnPropertyData() throws Exception {
        mockMvc.perform(get("/api/properties/{propertyId}", propertyId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(propertyId))
                .andExpect(jsonPath("$.name").value("Price Track Mansion"))
                .andExpect(jsonPath("$.ward").value("港区"));
    }

    @Test
    void homePage_shouldRenderSearchListUi() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }
}
