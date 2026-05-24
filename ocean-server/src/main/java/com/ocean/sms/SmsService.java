package com.ocean.sms;

/**
 * SMS sending service — decouples notification from provider implementation.
 */
public interface SmsService {
    /**
     * Send an SMS message.
     * @param phone  recipient phone number
     * @param content message body (Chinese text, template variable)
     * @return true if sent successfully
     */
    boolean send(String phone, String content);
}
