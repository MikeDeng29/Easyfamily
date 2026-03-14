package com.easyfamily;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:easyfamily;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "easyfamily.query.provider-key=simulated",
        "easyfamily.query.aliyun-market.base-url=https://example.aliyun-market.com",
        "easyfamily.query.aliyun-market.path=/phone/binding/query",
        "easyfamily.query.aliyun-market.app-code=test-appcode"
})
class ApiFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String accessToken;
    private String refreshToken;

    @BeforeEach
    void setup() throws Exception {
        clearBusinessTables();
        String captchaResponse = mockMvc.perform(post("/api/v1/auth/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "captchaProvider", "mock",
                                "ticket", "ticket-1"
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String captchaToken = objectMapper.readTree(captchaResponse).path("data").path("captchaToken").asText();

        mockMvc.perform(post("/api/v1/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138000",
                                "captchaToken", captchaToken
                        ))))
                .andExpect(status().isOk());

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138000",
                                "smsCode", "123456"
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();
        refreshToken = objectMapper.readTree(loginResponse).path("data").path("refreshToken").asText();
    }

    private void clearBusinessTables() {
        jdbcTemplate.update("DELETE FROM token_blacklist");
        jdbcTemplate.update("DELETE FROM auth_sms_codes");
        jdbcTemplate.update("DELETE FROM auth_captcha_tokens");
        jdbcTemplate.update("DELETE FROM auth_slide_captcha_challenges");
        jdbcTemplate.update("DELETE FROM login_audit_logs");
        jdbcTemplate.update("DELETE FROM query_records");
        jdbcTemplate.update("DELETE FROM feature_events");
        jdbcTemplate.update("DELETE FROM report_event");
        jdbcTemplate.update("DELETE FROM report_metric_daily");
        jdbcTemplate.update("DELETE FROM family_members");
        jdbcTemplate.update("DELETE FROM user_phones");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void unauthorizedQueryShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/query/phone-binding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138000",
                                "queryType", "all"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void authorizedQueryAndAdminFlowShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/query/phone-binding")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138000",
                                "queryType", "all"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        mockMvc.perform(get("/api/v1/phones/mine")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].phone").value("13800138000"));

        mockMvc.perform(patch("/api/v1/admin/quota")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("dailyQuotaPerUser", "3")
                        .param("dailyQuotaPerPhone", "5")
                        .param("dailyQuotaPerIp", "7")
                        .param("preferRedisCache", "false")
                        .param("providerKey", "simulated")
                        .param("providerTimeoutMs", "1200")
                        .param("providerRetryTimes", "2")
                        .param("providerCircuitFailureThreshold", "4")
                        .param("providerCircuitOpenSeconds", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyQuotaPerUser").value(3))
                .andExpect(jsonPath("$.data.dailyQuotaPerPhone").value(5))
                .andExpect(jsonPath("$.data.dailyQuotaPerIp").value(7))
                .andExpect(jsonPath("$.data.preferRedisCache").value(false))
                .andExpect(jsonPath("$.data.providerKey").value("simulated"))
                .andExpect(jsonPath("$.data.providerTimeoutMs").value(1200))
                .andExpect(jsonPath("$.data.providerRetryTimes").value(2))
                .andExpect(jsonPath("$.data.providerCircuitFailureThreshold").value(4))
                .andExpect(jsonPath("$.data.providerCircuitOpenSeconds").value(20));

        mockMvc.perform(get("/api/v1/admin/reports/query-overview")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQueryCount").value(1))
                .andExpect(jsonPath("$.data.loginUserCount").value(1))
                .andExpect(jsonPath("$.data.dailyQuotaPerPhone").value(5))
                .andExpect(jsonPath("$.data.providerKey").value("simulated"));

        mockMvc.perform(get("/api/v1/admin/reports/dau")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dau").value(1));

        mockMvc.perform(get("/api/v1/admin/reports/feature-hot")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].feature").value("phone-binding-query"))
                .andExpect(jsonPath("$.data[0].pv").value(1));

        mockMvc.perform(get("/api/v1/admin/reports/login-users-weekly")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(7))
                .andExpect(jsonPath("$.data[6].loginUserCount").value(1));
    }

    @Test
    void queryUnboundPhoneShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/query/phone-binding")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13900139000",
                                "queryType", "all"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PHONE_ERROR"));
    }

    @Test
    void bindSecondaryAndSetPrimaryShouldSucceed() throws Exception {
        String captchaResponse = mockMvc.perform(post("/api/v1/auth/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "captchaProvider", "mock",
                                "ticket", "ticket-2"
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String captchaToken = objectMapper.readTree(captchaResponse).path("data").path("captchaToken").asText();

        mockMvc.perform(post("/api/v1/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13900139000",
                                "captchaToken", captchaToken
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/phones/bind")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13900139000",
                                "smsCode", "123456"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        mockMvc.perform(post("/api/v1/phones/primary")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13900139000"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        mockMvc.perform(get("/api/v1/phones/mine")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].phone").value("13800138000"))
                .andExpect(jsonPath("$.data[0].isPrimary").value(false))
                .andExpect(jsonPath("$.data[1].phone").value("13900139000"))
                .andExpect(jsonPath("$.data[1].isPrimary").value(true));
    }

    @Test
    void refreshShouldRotateRefreshToken() throws Exception {
        String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", refreshToken
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn().getResponse().getContentAsString();

        String newRefresh = objectMapper.readTree(refreshResponse).path("data").path("refreshToken").asText();
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", refreshToken
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", newRefresh
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void logoutShouldInvalidateAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", refreshToken
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        mockMvc.perform(get("/api/v1/phones/mine")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void loginShouldWriteAuditLog() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM login_audit_logs WHERE login_type = ? AND principal = ? AND result = ?",
                Integer.class,
                "USER_PHONE",
                "13800138000",
                "SUCCESS"
        );
        assertTrue(count != null && count >= 1);
    }

    @Test
    void familyMemberCrudShouldSucceed() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/family/members")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Alice",
                                "phone", "13900139001",
                                "relation", "spouse"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.name").value("Alice"))
                .andExpect(jsonPath("$.data.phone").value("13900139001"))
                .andExpect(jsonPath("$.data.relation").value("spouse"))
                .andReturn().getResponse().getContentAsString();

        String memberId = objectMapper.readTree(createResponse).path("data").path("memberId").asText();

        mockMvc.perform(get("/api/v1/family/members")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memberId").value(memberId))
                .andExpect(jsonPath("$.data[0].name").value("Alice"));

        mockMvc.perform(get("/api/v1/family/members/" + memberId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(memberId))
                .andExpect(jsonPath("$.data.relation").value("spouse"));

        mockMvc.perform(put("/api/v1/family/members/" + memberId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Alice Senior",
                                "phone", "13900139002",
                                "relation", "father"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(memberId))
                .andExpect(jsonPath("$.data.name").value("Alice Senior"))
                .andExpect(jsonPath("$.data.phone").value("13900139002"))
                .andExpect(jsonPath("$.data.relation").value("father"));

        mockMvc.perform(delete("/api/v1/family/members/" + memberId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        mockMvc.perform(get("/api/v1/family/members/" + memberId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BIZ_ERROR"));
    }

    @Test
    void slideCaptchaFlowShouldIssueCaptchaToken() throws Exception {
        String initResponse = mockMvc.perform(post("/api/v1/auth/captcha/slide/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("clientId", "android-app"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn().getResponse().getContentAsString();

        String challengeId = objectMapper.readTree(initResponse).path("data").path("challengeId").asText();
        Integer targetX = jdbcTemplate.queryForObject(
                "SELECT target_x FROM auth_slide_captcha_challenges WHERE challenge_id = ?",
                Integer.class,
                challengeId
        );
        Thread.sleep(600);

        String verifyResponse = mockMvc.perform(post("/api/v1/auth/captcha/slide/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "challengeId", challengeId,
                                "offsetX", targetX,
                                "totalTimeMs", 900,
                                "tracks", List.of(
                                        Map.of("x", 0, "y", 0, "t", 0),
                                        Map.of("x", Math.max(1, targetX / 3), "y", 1, "t", 320),
                                        Map.of("x", Math.max(2, targetX * 2 / 3), "y", 1, "t", 640),
                                        Map.of("x", targetX, "y", 2, "t", 900)
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn().getResponse().getContentAsString();

        String captchaToken = objectMapper.readTree(verifyResponse).path("data").path("captchaToken").asText();
        mockMvc.perform(post("/api/v1/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138001",
                                "captchaToken", captchaToken
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
    }

}
