package com.easyfamily.auth.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.easyfamily.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "easyfamily.sms.provider", havingValue = "aliyun", matchIfMissing = true)
public class AliyunSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsSender.class);

    @Value("${easyfamily.sms.aliyun.access-key-id}")
    private String accessKeyId;

    @Value("${easyfamily.sms.aliyun.access-key-secret}")
    private String accessKeySecret;

    @Value("${easyfamily.sms.aliyun.sign-name}")
    private String signName;

    @Value("${easyfamily.sms.aliyun.template-code}")
    private String templateCode;

    @Value("${easyfamily.sms.aliyun.region-id:cn-hangzhou}")
    private String regionId;

    private Client client;

    @PostConstruct
    public void init() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint("dysmsapi.aliyuncs.com")
                .setRegionId(regionId);
        this.client = new Client(config);
        log.info("Aliyun SMS client initialized, signName={}, templateCode={}", signName, templateCode);
    }

    @Override
    public void send(String phone, String code) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");
            SendSmsResponse response = client.sendSms(request);
            String respCode = response.getBody().getCode();
            String respMessage = response.getBody().getMessage();
            if (!"OK".equals(respCode)) {
                log.error("Aliyun SMS send failed, phone={}, code={}, respCode={}, respMessage={}",
                        maskPhone(phone), code, respCode, respMessage);
                throw new BusinessException("SMS_SEND_FAILED", "sms send failed: " + respCode);
            }
            log.info("Aliyun SMS sent successfully, phone={}", maskPhone(phone));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Aliyun SMS send error, phone={}", maskPhone(phone), ex);
            throw new BusinessException("SMS_SEND_ERROR", "sms send error: " + ex.getMessage());
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
