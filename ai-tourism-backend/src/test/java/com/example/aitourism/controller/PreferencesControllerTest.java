package com.example.aitourism.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PreferencesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void savesReadsAndResetsPreferencesWithinGuestSession() throws Exception {
        String token = guestToken();
        String body = """
                {
                  "interests": ["CULTURE", "NATURE"],
                  "departure_time": "10:00",
                  "latest_end_time": "16:30",
                  "max_walking_meters": 4200,
                  "must_visit_poi_ids": [1, 3],
                  "transport_preference": "MIXED",
                  "language": "zh-Hant",
                  "accessibility_needs": ["STEP_FREE"],
                  "current_longitude": 113.54382,
                  "current_latitude": 22.19156,
                  "current_location_name": "澳门议事亭前地",
                  "location_source": "GPS"
                }
                """;

        mockMvc.perform(put("/api/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.saved").value(true))
                .andExpect(jsonPath("$.data.max_walking_meters").value(4200))
                .andExpect(jsonPath("$.data.current_location_name").value("澳门议事亭前地"))
                .andExpect(jsonPath("$.data.location_source").value("GPS"));

        mockMvc.perform(get("/api/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departure_time").value("10:00"))
                .andExpect(jsonPath("$.data.must_visit_poi_ids[1]").value(3))
                .andExpect(jsonPath("$.data.language").value("zh-Hant"))
                .andExpect(jsonPath("$.data.current_longitude").value(113.54382));

        mockMvc.perform(delete("/api/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(false))
                .andExpect(jsonPath("$.data.departure_time").value("09:00"))
                .andExpect(jsonPath("$.data.max_walking_meters").value(5000))
                .andExpect(jsonPath("$.data.current_longitude").doesNotExist());
    }

    @Test
    void rejectsInvalidTimeRange() throws Exception {
        String token = guestToken();
        String body = """
                {
                  "interests": ["CULTURE"],
                  "departure_time": "18:00",
                  "latest_end_time": "12:00",
                  "max_walking_meters": 3000,
                  "must_visit_poi_ids": [],
                  "transport_preference": "WALK",
                  "language": "zh-Hans",
                  "accessibility_needs": []
                }
                """;

        mockMvc.perform(put("/api/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.msg").value("最晚结束时间必须晚于出发时间"));
    }

    private String guestToken() throws Exception {
        String response = mockMvc.perform(post("/auth/guest"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("token").asText();
    }
}
