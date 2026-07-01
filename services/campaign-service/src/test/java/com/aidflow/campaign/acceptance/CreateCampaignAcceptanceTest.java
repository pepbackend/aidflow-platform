package com.aidflow.campaign.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aidflow.campaign.support.PostgresTestContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "campaign.outbox.publisher.enabled=false"
})
@AutoConfigureMockMvc
class CreateCampaignAcceptanceTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void coordinatorCanCreateCampaignAndRecordCampaignCreatedOutboxEvent() throws Exception {
        UUID userId = UUID.randomUUID();

        String responseBody = mockMvc.perform(post("/campaigns")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", "coordinator@example.com")
                        .header("X-User-Roles", "COORDINATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Flood response in Granollers",
                                "description", "Volunteer coordination after heavy rain flooding",
                                "location", "Granollers",
                                "priority", "HIGH"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        UUID campaignId = UUID.fromString(response.get("id").asText());
        assertThat(response.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(response.get("createdBy").asText()).isEqualTo(userId.toString());

        Integer campaignCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM campaigns WHERE id = ?",
                Integer.class,
                campaignId
        );
        assertThat(campaignCount).isEqualTo(1);

        Map<String, Object> outboxEvent = jdbcTemplate.queryForMap(
                """
                SELECT aggregate_type, aggregate_id, event_type, topic, payload, status
                FROM outbox_events
                WHERE aggregate_id = ?
                """,
                campaignId
        );

        assertThat(outboxEvent.get("aggregate_type")).isEqualTo("Campaign");
        assertThat(outboxEvent.get("aggregate_id")).isEqualTo(campaignId);
        assertThat(outboxEvent.get("event_type")).isEqualTo("CampaignCreated");
        assertThat(outboxEvent.get("topic")).isEqualTo("aidflow.events");
        assertThat(outboxEvent.get("status")).isEqualTo("PENDING");

        JsonNode payload = objectMapper.readTree(outboxEvent.get("payload").toString());
        assertThat(payload.get("eventType").asText()).isEqualTo("CampaignCreated");
        assertThat(payload.get("aggregateId").asText()).isEqualTo(campaignId.toString());
        assertThat(payload.get("aggregateType").asText()).isEqualTo("Campaign");
        assertThat(payload.get("version").asInt()).isEqualTo(1);
        assertThat(payload.get("payload").get("campaignId").asText()).isEqualTo(campaignId.toString());
        assertThat(payload.get("payload").get("name").asText()).isEqualTo("Flood response in Granollers");
        assertThat(payload.get("payload").get("priority").asText()).isEqualTo("HIGH");
        assertThat(payload.get("payload").get("status").asText()).isEqualTo("ACTIVE");
        assertThat(payload.get("payload").get("createdBy").asText()).isEqualTo(userId.toString());
    }

    @Test
    void volunteerCannotCreateCampaignOrRecordOutboxEvent() throws Exception {
        UUID userId = UUID.randomUUID();
        Integer outboxCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Campaign' AND event_type = 'CampaignCreated'",
                Integer.class
        );

        mockMvc.perform(post("/campaigns")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", "volunteer@example.com")
                        .header("X-User-Roles", "VOLUNTEER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Flood response in Granollers",
                                "description", "Volunteer coordination after heavy rain flooding",
                                "location", "Granollers",
                                "priority", "HIGH"
                        ))))
                .andExpect(status().isForbidden());

        Integer outboxCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Campaign' AND event_type = 'CampaignCreated'",
                Integer.class
        );
        assertThat(outboxCountAfter).isEqualTo(outboxCountBefore);
    }

    @Test
    void canGetCreatedCampaignById() throws Exception {
        UUID userId = UUID.randomUUID();
        JsonNode createdCampaign = createCampaign(
                userId,
                "coordinator@example.com",
                "Get by id campaign " + UUID.randomUUID()
        );
        UUID campaignId = UUID.fromString(createdCampaign.get("id").asText());

        String responseBody = mockMvc.perform(get("/campaigns/{id}", campaignId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        assertThat(response.get("id").asText()).isEqualTo(campaignId.toString());
        assertThat(response.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(response.get("createdBy").asText()).isEqualTo(userId.toString());
    }

    @Test
    void canGetAllCampaignsIncludingCreatedCampaign() throws Exception {
        UUID userId = UUID.randomUUID();
        String campaignName = "List campaign " + UUID.randomUUID();
        JsonNode createdCampaign = createCampaign(userId, "coordinator@example.com", campaignName);
        String campaignId = createdCampaign.get("id").asText();

        String responseBody = mockMvc.perform(get("/campaigns"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        assertThat(response).anySatisfy(campaign ->
                assertThat(campaign.get("id").asText()).isEqualTo(campaignId)
        );
        assertThat(response).anySatisfy(campaign ->
                assertThat(campaign.get("name").asText()).isEqualTo(campaignName)
        );
    }

    private JsonNode createCampaign(UUID userId, String userEmail, String name) throws Exception {
        String responseBody = mockMvc.perform(post("/campaigns")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", userEmail)
                        .header("X-User-Roles", "COORDINATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "description", "Volunteer coordination after heavy rain flooding",
                                "location", "Granollers",
                                "priority", "HIGH"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody);
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestContainer.registerProperties(registry);
    }
}
