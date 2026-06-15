package com.easyfamily;

import com.easyfamily.ai.chat.ChatController;
import com.easyfamily.ai.memory.UserMemoryService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        "easyfamily.query.aliyun-market.app-code=test-appcode",
        "easyfamily.sms.provider=mock"
})
class ApiFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserMemoryService userMemoryService;

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

        String smsCode = jdbcTemplate.queryForObject(
                "SELECT sms_code FROM auth_sms_codes WHERE phone = ?",
                String.class,
                "13800138000"
        );

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138000",
                                "smsCode", smsCode
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();
        refreshToken = objectMapper.readTree(loginResponse).path("data").path("refreshToken").asText();
    }

    private void clearBusinessTables() {
        jdbcTemplate.update("DELETE FROM token_blacklist");
        jdbcTemplate.update("DELETE FROM auth_sms_codes");
        jdbcTemplate.update("DELETE FROM auth_sms_send_logs");
        jdbcTemplate.update("DELETE FROM auth_captcha_tokens");
        jdbcTemplate.update("DELETE FROM auth_slide_captcha_challenges");
        jdbcTemplate.update("DELETE FROM login_audit_logs");
        jdbcTemplate.update("DELETE FROM query_records");
        jdbcTemplate.update("DELETE FROM feature_events");
        jdbcTemplate.update("DELETE FROM report_event");
        jdbcTemplate.update("DELETE FROM report_metric_daily");
        jdbcTemplate.update("DELETE FROM card_monitor_snapshot");
        jdbcTemplate.update("DELETE FROM device_push_token");
        jdbcTemplate.update("DELETE FROM family_members");
        jdbcTemplate.update("DELETE FROM user_phones");
        jdbcTemplate.update("DELETE FROM user_memory");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void unauthorizedQueryShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/query/real-name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138000",
                                "name", "张三",
                                "idCardNo", "11010119900101123X"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void authorizedQueryAndAdminFlowShouldSucceed() throws Exception {
        // Regular user query
        mockMvc.perform(post("/api/v1/query/real-name")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138000",
                                "name", "张三",
                                "idCardNo", "11010119900101123X"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.verified").isBoolean());

        mockMvc.perform(get("/api/v1/phones/mine")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].phone").value("13800138000"));

        // Admin operations require admin token
        String adminToken = getAdminToken();

        mockMvc.perform(patch("/api/v1/admin/quota")
                        .header("Authorization", "Bearer " + adminToken)
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
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQueryCount").value(1))
                .andExpect(jsonPath("$.data.loginUserCount").value(1))
                .andExpect(jsonPath("$.data.dailyQuotaPerPhone").value(5))
                .andExpect(jsonPath("$.data.providerKey").value("simulated"));

        mockMvc.perform(get("/api/v1/admin/reports/dau")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dau").value(1));

        mockMvc.perform(get("/api/v1/admin/reports/feature-hot")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].feature").value("real-name-verify"))
                .andExpect(jsonPath("$.data[0].pv").value(1));

        mockMvc.perform(get("/api/v1/admin/reports/login-users-weekly")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(7))
                .andExpect(jsonPath("$.data[6].loginUserCount").value(1));
    }

    @Test
    void queryUnboundPhoneShouldFail() throws Exception {
        // 13900139000 未绑定到当前用户，QueryFacade 先做 ensurePhoneOwnedByUser → PHONE_ERROR
        mockMvc.perform(post("/api/v1/query/real-name")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13900139000",
                                "name", "张三",
                                "idCardNo", "11010119900101123X"
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

        String bindSmsCode = jdbcTemplate.queryForObject(
                "SELECT sms_code FROM auth_sms_codes WHERE phone = ?",
                String.class,
                "13900139000"
        );

        mockMvc.perform(post("/api/v1/phones/bind")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13900139000",
                                "smsCode", bindSmsCode
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

    // -------------------------------------------------------------------------
    // TC-Q01: 用户维度每日限额触发
    // -------------------------------------------------------------------------
    @Test
    void userDailyQuotaExceededShouldFail() throws Exception {
        // Given: 将 dailyQuotaPerUser 设为 1
        mockMvc.perform(patch("/api/v1/admin/quota")
                        .header("Authorization", "Bearer " + getAdminToken())
                        .param("dailyQuotaPerUser", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        // When: 第 1 次查询 — 应成功
        mockMvc.perform(post("/api/v1/query/real-name")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138000",
                                "name", "张三",
                                "idCardNo", "11010119900101123X"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        // Then: 第 2 次查询 — 超出限额
        mockMvc.perform(post("/api/v1/query/real-name")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138000",
                                "name", "张三",
                                "idCardNo", "11010119900101123X"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("QUOTA_EXCEEDED"));
    }

    // -------------------------------------------------------------------------
    // TC-P01: 解绑副号 Happy Path
    // -------------------------------------------------------------------------
    @Test
    void unbindSecondaryPhoneShouldSucceed() throws Exception {
        // Given: 绑定副号 13900139000
        String captchaResp = mockMvc.perform(post("/api/v1/auth/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "captchaProvider", "mock",
                                "ticket", "ticket-unbind"
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String captchaToken = objectMapper.readTree(captchaResp).path("data").path("captchaToken").asText();

        mockMvc.perform(post("/api/v1/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13900139000",
                                "captchaToken", captchaToken
                        ))))
                .andExpect(status().isOk());

        String smsCode = jdbcTemplate.queryForObject(
                "SELECT sms_code FROM auth_sms_codes WHERE phone = ?",
                String.class, "13900139000"
        );

        mockMvc.perform(post("/api/v1/phones/bind")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13900139000",
                                "smsCode", smsCode
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        // When: 解绑副号
        mockMvc.perform(post("/api/v1/phones/unbind")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13900139000"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        // Then: 列表中只剩主号
        mockMvc.perform(get("/api/v1/phones/mine")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].phone").value("13800138000"))
                .andExpect(jsonPath("$.data[0].isPrimary").value(true));
    }

    // -------------------------------------------------------------------------
    // TC-P02: 解绑主号应被拒绝
    // -------------------------------------------------------------------------
    @Test
    void unbindPrimaryPhoneShouldFail() throws Exception {
        // Given: 用户主号为 13800138000（由 @BeforeEach 登录时自动绑定）
        // When: 尝试解绑主号
        mockMvc.perform(post("/api/v1/phones/unbind")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138000"
                        ))))
                // Then: 拒绝，返回 PHONE_ERROR
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PHONE_ERROR"));
    }

    // -------------------------------------------------------------------------
    // TC-AD01: GET /admin/query-settings 正常返回当前配置
    // -------------------------------------------------------------------------
    @Test
    void getQuerySettingsShouldReturnCurrentConfig() throws Exception {
        // When: 获取运行时查询配置
        mockMvc.perform(get("/api/v1/admin/query-settings")
                        .header("Authorization", "Bearer " + getAdminToken()))
                // Then: 返回完整配置字段
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.dailyQuotaPerUser").isNumber())
                .andExpect(jsonPath("$.data.dailyQuotaPerPhone").isNumber())
                .andExpect(jsonPath("$.data.dailyQuotaPerIp").isNumber())
                .andExpect(jsonPath("$.data.providerKey").isString())
                .andExpect(jsonPath("$.data.preferRedisCache").isBoolean())
                .andExpect(jsonPath("$.data.providerTimeoutMs").isNumber())
                .andExpect(jsonPath("$.data.providerRetryTimes").isNumber());
    }

    // -------------------------------------------------------------------------
    // TC-AD02: 普通用户 token 访问 admin 接口应被拒绝 [TDD - 当前会失败]
    //
    // 安全缺口：SecurityConfig 目前仅检查 authenticated()，未做角色控制，
    // 普通用户 token 可访问 /api/v1/admin/** 接口。
    // 本测试预期 403，当前实际返回 200。
    // 需要 Layson 在 SecurityConfig 补充：
    //   .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    // 并在 adminLogin 签发的 JWT 中携带 ROLE_ADMIN。
    // -------------------------------------------------------------------------
    @Test
    void regularUserTokenShouldBeRejectedByAdminEndpoints() throws Exception {
        // Given: 普通手机号登录的 accessToken（非管理员）
        // When: 访问 admin 报表接口
        mockMvc.perform(get("/api/v1/admin/reports/dau")
                        .header("Authorization", "Bearer " + accessToken))
                // Then: 禁止访问（需 ADMIN 角色）
                .andExpect(status().isForbidden());
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

    @Test
    void smsSendWithoutCaptchaShouldSucceedOnFirstRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sms/send")
                        .header("X-Forwarded-For", "10.0.0.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13700137000"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        String smsCode = jdbcTemplate.queryForObject(
                "SELECT sms_code FROM auth_sms_codes WHERE phone = ?",
                String.class,
                "13700137000"
        );
        assertTrue(smsCode != null && !smsCode.isBlank());
    }

    @Test
    void smsSendShouldRequireCaptchaAfterFrequentRequests() throws Exception {
        String phone = "13700137001";

        // First request: no captcha needed, succeeds.
        mockMvc.perform(post("/api/v1/auth/sms/send")
                        .header("X-Forwarded-For", "10.0.0.11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", phone
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        // Second request within the risk window: still allowed without captcha
        // (threshold is reached only once count >= 2 *before* this request).
        mockMvc.perform(post("/api/v1/auth/sms/send")
                        .header("X-Forwarded-For", "10.0.0.11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", phone
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        // Third request: phone now has 2 prior sends within the window and no
        // captchaToken supplied -> risk control kicks in.
        mockMvc.perform(post("/api/v1/auth/sms/send")
                        .header("X-Forwarded-For", "10.0.0.11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", phone
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CAPTCHA_REQUIRED"));
    }

    @Test
    void smsSendShouldSucceedWithValidCaptchaTokenAfterRateLimitTriggered() throws Exception {
        String phone = "13700137002";

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/auth/sms/send")
                            .header("X-Forwarded-For", "10.0.0.12")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "phone", phone
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));
        }

        // Without captcha, the third request is challenged.
        mockMvc.perform(post("/api/v1/auth/sms/send")
                        .header("X-Forwarded-For", "10.0.0.12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", phone
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CAPTCHA_REQUIRED"));

        // With a valid captchaToken, the request is allowed despite the rate limit.
        String captchaResponse = mockMvc.perform(post("/api/v1/auth/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "captchaProvider", "mock",
                                "ticket", "ticket-rate-limit"
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String captchaToken = objectMapper.readTree(captchaResponse).path("data").path("captchaToken").asText();

        mockMvc.perform(post("/api/v1/auth/sms/send")
                        .header("X-Forwarded-For", "10.0.0.12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", phone,
                                "captchaToken", captchaToken
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
    }

    private String getAdminToken() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "admin",
                                "password", "Trump@666"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();
    }

    // -------------------------------------------------------------------------
    // TC-MON01: POST /api/v1/monitor/scan with valid JWT should return 202
    // -------------------------------------------------------------------------
    @Test
    void testMonitorScanReturns202() throws Exception {
        mockMvc.perform(post("/api/v1/monitor/scan")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("OK"));
    }

    // -------------------------------------------------------------------------
    // TC-MON02: GET /api/v1/monitor/snapshots with valid JWT should return 200
    // -------------------------------------------------------------------------
    @Test
    void testMonitorSnapshotsReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/monitor/snapshots")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").isArray());
    }

    // -------------------------------------------------------------------------
    // User memory (青鸟 AI chat assistant) — CRUD + service-level behaviour
    // -------------------------------------------------------------------------
    @Test
    void unauthorizedMemoryListShouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/memory"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void memoryListShouldBeEmptyInitially() throws Exception {
        mockMvc.perform(get("/api/v1/memory")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void addMemoryShouldDedupAndBoundAt30() throws Exception {
        String userId = "U13800138000";

        // Adding the same content twice should not create a duplicate row
        userMemoryService.add(userId, "用户家里有一只叫旺财的狗");
        userMemoryService.add(userId, "用户家里有一只叫旺财的狗");
        assertEquals(1, userMemoryService.list(userId).size());

        // Adding 35 distinct memories should be bounded at 30, keeping the most recent
        for (int i = 0; i < 35; i++) {
            userMemoryService.add(userId, "memory-fact-" + i);
        }
        var memories = userMemoryService.list(userId);
        assertEquals(30, memories.size());
        // most recent first
        assertEquals("memory-fact-34", memories.get(0).content());
        // the oldest distinct facts (memory-fact-0..4) should have been evicted
        boolean stillHasOldest = memories.stream()
                .anyMatch(m -> m.content().equals("memory-fact-0"));
        assertEquals(false, stillHasOldest);
    }

    @Test
    void deleteMemoryShouldRemoveOwnAndRejectOthers() throws Exception {
        String userId = "U13800138000";
        userMemoryService.add(userId, "用户常用手机号是 13800138000");
        userMemoryService.add("U13800138999", "另一个用户的记忆");

        var memories = userMemoryService.list(userId);
        assertEquals(1, memories.size());
        long ownMemoryId = memories.get(0).id();

        var othersMemories = userMemoryService.list("U13800138999");
        long otherMemoryId = othersMemories.get(0).id();

        // Deleting another user's memory should fail (mapped to client error code BIZ_ERROR)
        mockMvc.perform(delete("/api/v1/memory/" + otherMemoryId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BIZ_ERROR"));

        // Deleting own memory should succeed
        mockMvc.perform(delete("/api/v1/memory/" + ownMemoryId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        assertEquals(0, userMemoryService.list(userId).size());
    }

    @Test
    void recentForPromptShouldReturnMostRecentFirstUpToLimit() {
        String userId = "U13800138000";
        for (int i = 0; i < 25; i++) {
            userMemoryService.add(userId, "fact-" + i);
        }
        var recent = userMemoryService.recentForPrompt(userId, 20);
        assertEquals(20, recent.size());
        assertEquals("fact-24", recent.get(0));
    }

    @Test
    void stripMemoryMarkersShouldRemoveMemorySaveButKeepBillAction() {
        String reply = "已记录！\n"
                + "<!--BILL_ACTION:{\"category\":\"餐饮\",\"amount\":38.00,\"note\":\"午饭\",\"date\":\"2026-06-13\"}-->\n"
                + "<!--MEMORY_SAVE:{\"content\":\"用户喜欢吃辣\"}-->\n"
                + "<!--MEMORY_SAVE:{\"content\":\"用户家里有一只叫旺财的狗\"}-->";

        String stripped = ChatController.stripMemoryMarkers(reply);

        assertTrue(stripped.contains("已记录！"));
        assertTrue(stripped.contains("<!--BILL_ACTION:"));
        assertTrue(!stripped.contains("MEMORY_SAVE"));
    }

}
