package com.easyfamily;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
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

    private String accessToken;

    @BeforeEach
    void setup() throws Exception {
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
    }

    @Test
    void unauthorizedQueryShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/query/phone-binding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "13800138000",
                                "queryType", "all"
                        ))))
                .andExpect(status().isForbidden());
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
                .andExpect(jsonPath("$.data.dailyQuotaPerPhone").value(5))
                .andExpect(jsonPath("$.data.providerKey").value("simulated"));
    }

}
