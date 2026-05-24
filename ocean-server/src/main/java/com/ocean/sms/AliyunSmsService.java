package com.ocean.sms;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "aliyun")
public class AliyunSmsService implements SmsService {

    @Value("${sms.aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${sms.aliyun.access-key-secret:}")
    private String accessKeySecret;

    @Value("${sms.aliyun.sign-name:}")
    private String signName;

    @Value("${sms.aliyun.template-code:}")
    private String templateCode;

    private IAcsClient client;

    @PostConstruct
    public void init() {
        if (accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
            log.warn("Aliyun SMS credentials not configured — SMS will not be sent");
            return;
        }
        try {
            IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
            DefaultProfile.addEndpoint("cn-hangzhou", "Dysmsapi", "dysmsapi.aliyuncs.com");
            client = new DefaultAcsClient(profile);
            log.info("Aliyun SMS client initialized");
        } catch (Exception e) {
            log.error("Failed to initialize Aliyun SMS client", e);
        }
    }

    @Override
    public boolean send(String phone, String content) {
        if (client == null) {
            log.warn("SMS client not available, skipping send to {}", phone);
            return false;
        }

        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumbers(phone);
        request.setSignName(signName);
        request.setTemplateCode(templateCode);
        request.setTemplateParam("{\"content\":\"" + escapeJson(content) + "\"}");

        try {
            SendSmsResponse response = client.getAcsResponse(request);
            if ("OK".equals(response.getCode())) {
                log.info("SMS sent to {}: {}", phone, response.getBizId());
                return true;
            } else {
                log.error("SMS send failed: code={}, message={}", response.getCode(), response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("SMS send error to {}: {}", phone, e.getMessage());
            return false;
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
