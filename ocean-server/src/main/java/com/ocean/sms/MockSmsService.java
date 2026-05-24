package com.ocean.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsService implements SmsService {

    @Override
    public boolean send(String phone, String content) {
        log.info("========== [MOCK SMS] ==========");
        log.info("To: {}", phone);
        log.info("Content: {}", content);
        log.info("================================");
        return true;
    }
}
