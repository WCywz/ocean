package com.ocean.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "serverchan")
public class ServerChanService implements SmsService {

    @Value("${sms.serverchan.send-key:}")
    private String sendKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public boolean send(String phone, String content) {
        if (sendKey.isEmpty()) {
            log.warn("ServerChan sendKey not configured");
            return false;
        }

        try {
            String url = "https://sctapi.ftqq.com/" + sendKey + ".send";
            String body = "title=" + java.net.URLEncoder.encode("海洋健康日报", "UTF-8")
                    + "&desp=" + java.net.URLEncoder.encode(content, "UTF-8");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("ServerChan push response: {}", response.body());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.error("ServerChan push failed: {}", e.getMessage());
            return false;
        }
    }
}
