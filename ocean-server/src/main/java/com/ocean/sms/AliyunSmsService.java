package com.ocean.sms;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
import darabonba.core.client.ClientOverrideConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

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

    private AsyncClient client;

    @PostConstruct
    public void init() {
        if (accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
            log.warn("Aliyun SMS credentials not configured — SMS will not be sent");
            return;
        }
        StaticCredentialProvider provider = StaticCredentialProvider.create(
                Credential.builder()
                        .accessKeyId(accessKeyId)
                        .accessKeySecret(accessKeySecret)
                        .build());
        client = AsyncClient.builder()
                .region("cn-hangzhou")
                .credentialsProvider(provider)
                .overrideConfiguration(ClientOverrideConfiguration.create().setEndpoint("dysmsapi.aliyuncs.com"))
                .build();
        log.info("Aliyun SMS client initialized");
    }

    @PreDestroy
    public void destroy() {
        if (client != null) client.close();
    }

    @Override
    public boolean send(String phone, String content) {
        if (client == null) {
            log.warn("SMS client not available, skipping send to {}", phone);
            return false;
        }

        SendSmsRequest request = SendSmsRequest.builder()
                .phoneNumbers(phone)
                .signName(signName)
                .templateCode(templateCode)
                .templateParam("{\"content\":\"" + escapeJson(content) + "\"}")
                .build();

        try {
            CompletableFuture<SendSmsResponse> future = client.sendSms(request);
            SendSmsResponse response = future.get();
            if ("OK".equals(response.getBody().getCode())) {
                log.info("SMS sent to {}: {}", phone, response.getBody().getBizId());
                return true;
            } else {
                log.error("SMS send failed: code={}, message={}",
                        response.getBody().getCode(), response.getBody().getMessage());
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
