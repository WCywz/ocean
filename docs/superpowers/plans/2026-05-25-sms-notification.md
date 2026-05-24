# SMS Notification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Daily 08:00 SMS to all admin users with today's health assessment and tomorrow's forecast-based health preview.

**Architecture:** New `HealthSmsTask` scheduled at 08:00 calls `HealthService.buildDailySummary()` which reads `health_record` for today, queries `forecast_grid` for tomorrow, applies grading logic, and dispatches through `SmsService`. `SmsService` has two implementations: `AliyunSmsService` (production, uses Aliyun SDK) and `MockSmsService` (dev, logs to console).

**Tech Stack:** Spring Boot scheduling, Aliyun SMS SDK (`com.aliyun:dysmsapi20170525`), MyBatis-Plus, existing forecast_grid data

---

### Task 1: Database Migration

**Files:**
- Create: `database/export/migration-add-phone.sql`

- [ ] **Step 1: Write the migration SQL**

```sql
-- Migration: Add phone field to sys_user for SMS notifications
-- Date: 2026-05-25
ALTER TABLE sys_user ADD COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '手机号';
```

- [ ] **Step 2: Run the migration on local MySQL**

Run:
```bash
mysql -u root -pyour_password ocean_forecast < database/export/migration-add-phone.sql
```
Expected: Query OK, 0 rows affected (or column already exists warning if re-run)

- [ ] **Step 3: Verify the column exists**

Run:
```bash
mysql -u root -pyour_password ocean_forecast -e "DESCRIBE sys_user;"
```
Expected: `phone` column listed with type `varchar(20)`

- [ ] **Step 4: Set admin phone for testing (replace with real number later)**

Run:
```bash
mysql -u root -pyour_password ocean_forecast -e "UPDATE sys_user SET phone = '13800138000' WHERE username = 'admin';"
```

- [ ] **Step 5: Commit**

```bash
git add database/export/migration-add-phone.sql
git commit -m "feat: add phone field to sys_user for SMS notifications"
```

---

### Task 2: Add Aliyun SMS SDK Dependency

**Files:**
- Modify: `ocean-server/pom.xml` (add dependency)

- [ ] **Step 1: Add the Aliyun SMS SDK to pom.xml**

Add inside `<dependencies>` block, after the Knife4j dependency (line 97):

```xml
        <!-- Aliyun SMS -->
        <dependency>
            <groupId>com.aliyun</groupId>
            <artifactId>dysmsapi20170525</artifactId>
            <version>3.0.1</version>
        </dependency>
```

- [ ] **Step 2: Verify Maven resolves the dependency**

Run:
```bash
cd ocean-server && mvn dependency:resolve -q 2>&1 | tail -5
```
Expected: BUILD SUCCESS (no errors about missing artifact)

- [ ] **Step 3: Commit**

```bash
git add ocean-server/pom.xml
git commit -m "feat: add Aliyun SMS SDK dependency"
```

---

### Task 3: Create SmsService Interface

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/sms/SmsService.java`

- [ ] **Step 1: Write the interface**

```java
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
```

- [ ] **Step 2: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/sms/SmsService.java
git commit -m "feat: add SmsService interface"
```

---

### Task 4: Create MockSmsService (Dev)

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/sms/MockSmsService.java`

- [ ] **Step 1: Write MockSmsService**

```java
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
```

- [ ] **Step 2: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/sms/MockSmsService.java
git commit -m "feat: add MockSmsService for dev/test"
```

---

### Task 5: Create AliyunSmsService (Production)

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/sms/AliyunSmsService.java`

- [ ] **Step 1: Write AliyunSmsService**

```java
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
```

- [ ] **Step 2: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/sms/AliyunSmsService.java
git commit -m "feat: add AliyunSmsService for production SMS"
```

---

### Task 6: Add SMS Configuration

**Files:**
- Modify: `ocean-server/src/main/resources/application.yml.example`

- [ ] **Step 1: Add SMS config section to application.yml.example**

Add at the end of the file:

```yaml
sms:
  provider: mock  # mock | aliyun — use 'mock' for dev, 'aliyun' for production
  aliyun:
    access-key-id:
    access-key-secret:
    sign-name:
    template-code:
```

- [ ] **Step 2: Commit**

```bash
git add ocean-server/src/main/resources/application.yml.example
git commit -m "feat: add SMS config template"
```

---

### Task 7: Add phone Field to SysUser Entity

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/entity/SysUser.java`

- [ ] **Step 1: Add phone field**

Add after `private Integer status;` (line 28):

```java
    /** 手机号（用于SMS通知） */
    private String phone;
```

- [ ] **Step 2: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/entity/SysUser.java
git commit -m "feat: add phone field to SysUser entity"
```

---

### Task 8: Add phone Field to DTOs

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/dto/UserSaveDTO.java`
- Modify: `ocean-server/src/main/java/com/ocean/vo/UserVO.java`

- [ ] **Step 1: Add phone to UserSaveDTO**

Add after `private Integer status;` (line 28):

```java
    private String phone;
```

- [ ] **Step 2: Add phone to UserVO**

Add after `private String role;` (line 16):

```java
    private String phone;
```

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/dto/UserSaveDTO.java ocean-server/src/main/java/com/ocean/vo/UserVO.java
git commit -m "feat: add phone field to UserSaveDTO and UserVO"
```

---

### Task 9: Add phone Column to UserView.vue

**Files:**
- Modify: `ocean-web/src/views/user/UserView.vue`

- [ ] **Step 1: Add phone column header**

In the `<thead>` block (line 34-41), add after the "真实姓名" column header:

```html
          <td>手机号</td>
```

- [ ] **Step 2: Add phone column body**

In the `<tbody>` block (line 44-54), add after the `realName` cell:

```html
          <td>{{ row.phone || '-' }}</td>
```

- [ ] **Step 3: Add phone input field to the dialog form**

Add after the "真实姓名" input block (lines 91-93):

```html
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">手机号</label>
        <input v-model="form.phone" class="editorial-input" placeholder="请输入手机号" />
      </div>
```

- [ ] **Step 4: Add phone to form reactive**

In the `<script setup>` block, add `phone: ''` to the initial `form` reactive (line 135):

Change:
```js
const form = reactive({ username: '', password: '', realName: '', role: 'USER', status: 1 })
```
To:
```js
const form = reactive({ username: '', password: '', realName: '', role: 'USER', status: 1, phone: '' })
```

- [ ] **Step 5: Add phone reset in handleAdd**

In `handleAdd()` (line 177-185), add:
```js
  form.phone = ''
```

- [ ] **Step 6: Add phone population in handleEdit**

In `handleEdit()` (line 188-197), add:
```js
  form.phone = row.phone || ''
```

- [ ] **Step 7: Commit**

```bash
git add ocean-web/src/views/user/UserView.vue
git commit -m "feat: add phone field to user management page"
```

---

### Task 10: Add buildDailySummary to HealthService

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/service/HealthService.java`
- Modify: `ocean-server/src/main/java/com/ocean/service/impl/HealthServiceImpl.java`

- [ ] **Step 1: Add method signature to HealthService interface**

Add at the end of the interface (after `getDashboard`):

```java
    /**
     * Build daily SMS summary text for all zones.
     * @return formatted SMS message body, or null if no data available
     */
    String buildDailySummary();
```

- [ ] **Step 2: Implement buildDailySummary in HealthServiceImpl**

Add the following imports at the top (line 6-13 area):

```java
import com.ocean.mapper.ForecastGridMapper;
import org.springframework.beans.factory.annotation.Autowired;
```

Add autowired dependency after existing fields (line 22):

```java
    @Autowired private ForecastGridMapper forecastGridMapper;
```

Then add the implementation method. The grading thresholds mirror `HealthAssessmentTask`:

```java
    private static final double SST_ANOMALY_BAD = 2.5;
    private static final double SST_ANOMALY_WARN = 1.5;
    private static final double SST_ANOMALY_FINE = 0.5;
    private static final double CHL_BAD = 5.0;
    private static final double CHL_WARN = 3.0;
    private static final double CHL_FINE = 2.0;

    @Override
    public String buildDailySummary() {
        List<HealthZone> zones = getZones();
        if (zones.isEmpty()) return null;

        LocalDate today = systemConfigService.getSystemDate();
        LocalDate tomorrow = today.plusDays(1);
        String todayStr = today.toString();
        String tomorrowStr = tomorrow.toString();

        List<String> problems = new ArrayList<>();
        int totalZones = zones.size();
        int goodCount = 0;

        for (HealthZone zone : zones) {
            // Today: read from health_record
            HealthRecord todayRecord = healthRecordMapper.selectOne(
                    new LambdaQueryWrapper<HealthRecord>()
                            .eq(HealthRecord::getZoneId, zone.getId())
                            .eq(HealthRecord::getAssessDate, today));
            String todayGrade = todayRecord != null ? todayRecord.getOverallGrade() : null;

            // Tomorrow: estimate from forecast_grid
            String tomorrowGrade = estimateTomorrowGrade(zone, tomorrowStr);

            // Use the worse of today and tomorrow for alert decision
            String effectiveGrade = worstOf(todayGrade, tomorrowGrade);

            if (effectiveGrade == null) {
                continue; // no data at all, skip
            }

            if ("good".equals(effectiveGrade) || "fine".equals(effectiveGrade)) {
                goodCount++;
            } else {
                // Build problem description
                StringBuilder sb = new StringBuilder();
                sb.append(zone.getZoneName()).append("：");
                sb.append(gradeLabel(effectiveGrade));

                // Add reason hints
                if (todayRecord != null) {
                    List<String> reasons = new ArrayList<>();
                    if ("bad".equals(todayRecord.getSstGrade()) || "warn".equals(todayRecord.getSstGrade())) {
                        reasons.add("SST异常偏高" + String.format("%.1f", todayRecord.getSstAnomaly()) + "℃");
                    }
                    if ("bad".equals(todayRecord.getChlGrade()) || "warn".equals(todayRecord.getChlGrade())) {
                        reasons.add("chl偏高" + String.format("%.1f", todayRecord.getChlAvg()));
                    }
                    if (todayRecord.getHeatwaveActive() != null && todayRecord.getHeatwaveActive() == 1) {
                        reasons.add("热浪持续" + todayRecord.getHeatwaveDays() + "天");
                    }
                    if (!reasons.isEmpty()) {
                        sb.append("（").append(String.join("，", reasons)).append("）");
                    }
                }

                if (tomorrowGrade != null && !tomorrowGrade.equals(todayGrade)) {
                    sb.append(" 明日预计").append(gradeLabel(tomorrowGrade));
                }

                problems.add(sb.toString());
            }
        }

        if (problems.isEmpty() || goodCount == totalZones) {
            return "今日各海域健康状态良好，无需关注。";
        }

        return String.join(" ", problems);
    }

    private String estimateTomorrowGrade(HealthZone zone, String dateStr) {
        try {
            Map<String, Object> sstStats = forecastGridMapper.selectZoneStats(
                    "sst", dateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());
            Map<String, Object> chlStats = forecastGridMapper.selectZoneStats(
                    "chl", dateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());

            if (sstStats == null || sstStats.get("avg_val") == null) return null;

            double sstAvg = ((Number) sstStats.get("avg_val")).doubleValue();
            double chlAvg = chlStats != null && chlStats.get("avg_val") != null
                    ? ((Number) chlStats.get("avg_val")).doubleValue() : 0;

            Double baseline = forecastGridMapper.selectZoneSstBaseline(
                    zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());
            double anomaly = baseline != null ? Math.abs(sstAvg - baseline) : 0;

            String sstGrade = gradeSstValue(anomaly);
            String chlGrade = gradeChlValue(chlAvg);
            return worstOf(sstGrade, chlGrade);
        } catch (Exception e) {
            return null;
        }
    }

    private String gradeSstValue(double absAnomaly) {
        if (absAnomaly > SST_ANOMALY_BAD) return "bad";
        if (absAnomaly > SST_ANOMALY_WARN) return "warn";
        if (absAnomaly > SST_ANOMALY_FINE) return "fine";
        return "good";
    }

    private String gradeChlValue(double avg) {
        if (avg >= CHL_BAD) return "bad";
        if (avg >= CHL_WARN) return "warn";
        if (avg >= CHL_FINE) return "fine";
        return "good";
    }

    private String worstOf(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        List<String> order = List.of("good", "fine", "warn", "bad");
        return order.indexOf(a) > order.indexOf(b) ? a : b;
    }

    private String gradeLabel(String grade) {
        return switch (grade) {
            case "good" -> "优";
            case "fine" -> "良";
            case "warn" -> "中";
            case "bad" -> "差";
            default -> grade;
        };
    }
```

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/service/HealthService.java ocean-server/src/main/java/com/ocean/service/impl/HealthServiceImpl.java
git commit -m "feat: add buildDailySummary for SMS health text"
```

---

### Task 11: Create HealthSmsTask

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/task/HealthSmsTask.java`

- [ ] **Step 1: Write HealthSmsTask**

```java
package com.ocean.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.entity.SysUser;
import com.ocean.mapper.SysUserMapper;
import com.ocean.service.HealthService;
import com.ocean.sms.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HealthSmsTask {

    @Autowired private HealthService healthService;
    @Autowired private SmsService smsService;
    @Autowired private SysUserMapper sysUserMapper;

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailySms() {
        log.info(">>>>>> 健康短信任务开始");

        try {
            // Build summary text
            String content = healthService.buildDailySummary();
            if (content == null) {
                log.info("无健康数据，跳过短信发送");
                return;
            }

            // Find all admin users with phone numbers
            var admins = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getRole, "ADMIN")
                            .eq(SysUser::getStatus, 1)
                            .isNotNull(SysUser::getPhone)
                            .ne(SysUser::getPhone, ""));

            if (admins.isEmpty()) {
                log.warn("无管理员手机号，跳过短信发送");
                return;
            }

            // Send to each admin
            for (SysUser admin : admins) {
                try {
                    boolean ok = smsService.send(admin.getPhone(), content);
                    if (ok) {
                        log.info("健康短信已发送至 {} ({})", admin.getUsername(), admin.getPhone());
                    } else {
                        log.error("健康短信发送失败 {} ({})", admin.getUsername(), admin.getPhone());
                    }
                } catch (Exception e) {
                    log.error("健康短信发送异常 {} ({})", admin.getUsername(), admin.getPhone(), e);
                }
            }

            log.info("<<<<<< 健康短信任务完成，发送 {} 条", admins.size());
        } catch (Exception e) {
            log.error("<<<<<< 健康短信任务失败", e);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/task/HealthSmsTask.java
git commit -m "feat: add HealthSmsTask for daily SMS notifications"
```

---

### Task 12: Verify Build

**Files:** None (verification only)

- [ ] **Step 1: Build the project**

Run:
```bash
cd ocean-server && mvn compile -q 2>&1 | tail -10
```
Expected: BUILD SUCCESS

- [ ] **Step 2: Start the application and check logs**

Run:
```bash
cd ocean-server && mvn spring-boot:run 2>&1 | head -30
```
Expected: Application starts without errors. Look for "[MOCK SMS]" in logs (no SMS will actually send since `sms.provider` defaults to `mock`).

- [ ] **Step 3: If build fails, diagnose and fix; retry up to 3 times before continuing**
```

---

## Self-Review Notes

1. Spec coverage: All requirements from design doc are covered — database migration, SMS service (interface + mock + Aliyun), config, entity/DTO/VO updates, frontend phone field, health summary logic, scheduled task.
2. No placeholders — all code is complete.
3. Type consistency: The `SmsService.send(phone, content)` signature is used consistently in `HealthSmsTask`. `HealthService.buildDailySummary()` returns `String` which is consumed by `HealthSmsTask`. Phone field is `String` everywhere (entity, DTO, VO, mapper).
4. The grading logic in `HealthServiceImpl` mirrors the thresholds in `HealthAssessmentTask` exactly.
