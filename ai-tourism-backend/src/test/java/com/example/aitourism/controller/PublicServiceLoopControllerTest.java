package com.example.aitourism.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicServiceLoopControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generatesFeasibleTripAndRejectsImpossibleEndTime() throws Exception {
        String token = guestToken();
        savePreferences(token, "09:00", "18:00", 20000);

        JsonNode route = postJson("/api/trips", token, "{\"required_arrival_times\":{}}");
        assertThat(route.path("data").path("feasible").asBoolean()).isTrue();
        assertThat(route.path("data").path("conflicts").size()).isZero();
        assertThat(route.path("data").path("stops").size()).isGreaterThanOrEqualTo(3);
        assertThat(route.path("data").path("transport_options").size()).isEqualTo(3);

        mockMvc.perform(delete("/api/trips/current").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/trips/current").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());

        savePreferences(token, "09:00", "09:30", 20000);
        mockMvc.perform(post("/api/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"required_arrival_times\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feasible").value(false))
                .andExpect(jsonPath("$.data.status").value("CONFLICT"))
                .andExpect(jsonPath("$.data.conflicts[0]").isNotEmpty());
    }

    @Test
    void completesEventRerouteRecommendationAndFeedbackLoop() throws Exception {
        postJson("/api/ops/demo/reset", null, null);
        String token = guestToken();
        savePreferences(token, "09:00", "18:00", 20000);
        JsonNode route = postJson("/api/trips", token, "{\"required_arrival_times\":{}}");
        String tripId = route.path("data").path("trip_id").asText();
        String affectedCode = route.path("data").path("stops").get(1).path("poi_code").asText();
        String secondAffectedCode = route.path("data").path("stops").get(2).path("poi_code").asText();

        Instant now = Instant.now();
        String eventBody = objectMapper.createObjectNode()
                .put("type", "ROAD_CLOSURE").put("severity", "MODERATE")
                .put("title", "自动化测试封路").put("description", "用于验证局部改线")
                .put("region", "PENINSULA").put("starts_at", now.minus(1, ChronoUnit.MINUTES).toString())
                .put("ends_at", now.plus(1, ChronoUnit.HOURS).toString()).put("simulated", true)
                .set("affected_poi_codes", objectMapper.createArrayNode().add(affectedCode).add(secondAffectedCode)).toString();
        postJson("/api/ops/events", null, eventBody);

        JsonNode rerouted = postJson("/api/trips/" + tripId + "/reroute?mode=LOCAL", token, null);
        assertThat(rerouted.path("data").path("version").asInt()).isEqualTo(2);
        assertThat(rerouted.path("data").path("status").asText()).isEqualTo("LOCAL_REROUTE");
        assertThat(rerouted.path("data").path("feasible").asBoolean()).isTrue();
        assertThat(rerouted.path("data").path("stops").toString()).doesNotContain(affectedCode);
        assertThat(rerouted.path("data").path("stops").toString()).doesNotContain(secondAffectedCode);

        postJson("/api/trips/" + tripId + "/restore", token, null);
        JsonNode globallyRerouted = postJson("/api/trips/" + tripId + "/reroute?mode=GLOBAL", token, null);
        assertThat(globallyRerouted.path("data").path("version").asInt()).isEqualTo(4);
        assertThat(globallyRerouted.path("data").path("status").asText()).isEqualTo("GLOBAL_REROUTE");
        assertThat(globallyRerouted.path("data").path("feasible").asBoolean()).isTrue();
        assertThat(globallyRerouted.path("data").path("stops").toString()).doesNotContain(affectedCode);
        assertThat(globallyRerouted.path("data").path("stops").toString()).doesNotContain(secondAffectedCode);

        JsonNode recommendations = globallyRerouted.path("data").path("recommendations");
        if (!recommendations.isEmpty()) {
            String poiCode = recommendations.get(0).path("poi_code").asText();
            JsonNode added = postJson("/api/trips/" + tripId + "/recommendations/" + poiCode, token, null);
            assertThat(added.path("data").path("feasible").asBoolean()).isTrue();
        }

        JsonNode feedback = postJson("/api/feedback", token,
                "{\"category\":\"ROUTE_ISSUE\",\"content\":\"自动化闭环测试\",\"trip_id\":\"" + tripId + "\",\"trip_version\":4}");
        String feedbackId = feedback.path("data").path("feedback_id").asText();
        mockMvc.perform(patch("/api/ops/feedback/" + feedbackId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\",\"resolution\":\"测试处置完成\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CLOSED"));
        postJson("/api/ops/demo/reset", null, null);
    }

    @Test
    void usesSessionLocationToSelectNearbyAttractionsAndCalculateFirstLeg() throws Exception {
        String token = guestToken();
        String body = """
                {"interests":["CULTURE","FOOD"],"departure_time":"09:00","latest_end_time":"18:00",
                "max_walking_meters":20000,"must_visit_poi_ids":[],"transport_preference":"MIXED",
                "language":"zh-Hans","accessibility_needs":[],"current_longitude":113.5588,
                "current_latitude":22.1590,"current_location_name":"氹仔当前位置","location_source":"GPS"}
                """;
        mockMvc.perform(put("/api/preferences").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));

        JsonNode route = postJson("/api/trips", token, "{\"required_arrival_times\":{}}");
        assertThat(route.path("data").path("feasible").asBoolean()).isTrue();
        assertThat(route.path("data").path("stops").get(0).path("region").asText()).isEqualTo("TAIPA");
        assertThat(route.path("data").path("legs").get(0).path("from_name").asText()).isEqualTo("氹仔当前位置");
        assertThat(route.path("data").path("legs").get(0).path("distance_meters").asInt()).isGreaterThan(0);
    }

    @Test
    void adaptsStopCountToPreferenceWindowAndUsesLatestLocationForGlobalReroute() throws Exception {
        postJson("/api/ops/demo/reset", null, null);
        String token = guestToken();
        savePreferences(token, "09:00", "14:00", 20000);
        JsonNode shorter = postJson("/api/trips", token, "{\"required_arrival_times\":{}}");

        savePreferences(token, "09:00", "18:00", 20000);
        JsonNode longer = postJson("/api/trips", token, "{\"required_arrival_times\":{}}");
        assertThat(longer.path("data").path("stops").size())
                .isGreaterThan(shorter.path("data").path("stops").size());

        String locationBody = """
                {"interests":["CULTURE","FOOD"],"departure_time":"09:00","latest_end_time":"18:00",
                "max_walking_meters":20000,"must_visit_poi_ids":[],"transport_preference":"MIXED",
                "language":"zh-Hans","accessibility_needs":[],"current_longitude":113.5588,
                "current_latitude":22.1590,"current_location_name":"氹仔新位置","location_source":"GPS"}
                """;
        mockMvc.perform(put("/api/preferences").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(locationBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));

        String tripId = longer.path("data").path("trip_id").asText();
        String affectedCode = longer.path("data").path("stops").get(0).path("poi_code").asText();
        Instant now = Instant.now();
        String eventBody = objectMapper.createObjectNode()
                .put("type", "ROAD_CLOSURE").put("severity", "MODERATE")
                .put("title", "位置更新测试").put("description", "验证改线使用最新位置")
                .put("region", "PENINSULA").put("starts_at", now.minus(1, ChronoUnit.MINUTES).toString())
                .put("ends_at", now.plus(1, ChronoUnit.HOURS).toString()).put("simulated", true)
                .set("affected_poi_codes", objectMapper.createArrayNode().add(affectedCode)).toString();
        postJson("/api/ops/events", null, eventBody);

        JsonNode rerouted = postJson("/api/trips/" + tripId + "/reroute?mode=GLOBAL", token, null);
        assertThat(rerouted.path("data").path("feasible").asBoolean()).isTrue();
        assertThat(rerouted.path("data").path("stops").get(0).path("region").asText()).isEqualTo("TAIPA");
        assertThat(rerouted.path("data").path("legs").get(0).path("from_name").asText()).isEqualTo("氹仔新位置");
        assertThat(rerouted.path("data").path("stops").toString()).doesNotContain(affectedCode);
        postJson("/api/ops/demo/reset", null, null);
    }

    @Test
    void temporarilySuspendsBlockedMustVisitDuringConfirmedSafetyReroute() throws Exception {
        postJson("/api/ops/demo/reset", null, null);
        String token = guestToken();
        savePreferences(token, "09:00", "18:00", 20000);
        JsonNode original = postJson("/api/trips", token, "{\"required_arrival_times\":{}}");
        JsonNode blockedStop = original.path("data").path("stops").get(0);
        long blockedId = blockedStop.path("poi_id").asLong();
        String blockedCode = blockedStop.path("poi_code").asText();

        String preferences = """
                {"interests":["CULTURE","FOOD"],"departure_time":"09:00","latest_end_time":"18:00",
                "max_walking_meters":20000,"must_visit_poi_ids":[%d],"transport_preference":"MIXED",
                "language":"zh-Hans","accessibility_needs":[]}
                """.formatted(blockedId);
        mockMvc.perform(put("/api/preferences").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(preferences))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));

        Instant now = Instant.now();
        String eventBody = objectMapper.createObjectNode()
                .put("type", "ROAD_CLOSURE").put("severity", "MODERATE")
                .put("title", "必去点封锁测试").put("description", "验证安全事件临时挂起冲突必去点")
                .put("region", blockedStop.path("region").asText())
                .put("starts_at", now.minus(1, ChronoUnit.MINUTES).toString())
                .put("ends_at", now.plus(1, ChronoUnit.HOURS).toString()).put("simulated", true)
                .set("affected_poi_codes", objectMapper.createArrayNode().add(blockedCode)).toString();
        postJson("/api/ops/events", null, eventBody);

        String tripId = original.path("data").path("trip_id").asText();
        JsonNode rerouted = postJson("/api/trips/" + tripId + "/reroute?mode=GLOBAL", token, null);
        assertThat(rerouted.path("data").path("feasible").asBoolean()).isTrue();
        assertThat(rerouted.path("data").path("conflicts").size()).isZero();
        assertThat(rerouted.path("data").path("warnings").get(0).asText()).contains("临时移除");
        assertThat(rerouted.path("data").path("stops").toString()).doesNotContain(blockedCode);
        postJson("/api/ops/demo/reset", null, null);
    }

    private void savePreferences(String token, String departure, String end, int maxWalking) throws Exception {
        String body = """
                {"interests":["CULTURE","FOOD"],"departure_time":"%s","latest_end_time":"%s",
                "max_walking_meters":%d,"must_visit_poi_ids":[],"transport_preference":"MIXED",
                "language":"zh-Hans","accessibility_needs":[]}
                """.formatted(departure, end, maxWalking);
        mockMvc.perform(put("/api/preferences").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
    }

    private JsonNode postJson(String path, String token, String body) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.content(body);
        String response = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        assertThat(root.path("code").asInt()).as(response).isZero();
        return root;
    }

    private String guestToken() throws Exception {
        String response = mockMvc.perform(post("/auth/guest")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }
}
