# SMS Notification Design

## Overview

Daily SMS notification to admin users with today's and tomorrow's sea area health status. Uses Aliyun SMS service, scheduled at 08:00.

## Architecture

```
HealthSmsTask (08:00 daily)
  └─> HealthService.buildDailySummary()
        ├─> Read today's health_record (6 zones)
        ├─> Read forecast_grid SST/CHL → grade tomorrow's health
        ├─> Generate SMS text (smart compact/expand)
        └─> SmsService.send(phone, content)
              ├─> AliyunSmsService (production)
              └─> MockSmsService (dev, logs to console)
```

## Database Changes

### sys_user table
```sql
ALTER TABLE sys_user ADD COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '手机号';
```

## Configuration

### application.yml (new section)
```yaml
sms:
  provider: aliyun  # aliyun | mock
  aliyun:
    access-key-id: ${SMS_ACCESS_KEY_ID:}
    access-key-secret: ${SMS_ACCESS_KEY_SECRET:}
    sign-name: ${SMS_SIGN_NAME:}         # 签名，如"海洋预报系统"
    template-code: ${SMS_TEMPLATE_CODE:}  # 模板CODE
```

## SMS Content Logic

**Decision tree:**
1. Read today's `health_record` for all 6 active zones
2. For tomorrow: read `forecast_grid` SST/CHL by zone bbox → apply same grading logic (SST anomaly, CHL level, heatwave detection) → produce provisional grade
3. If all zones are 优 or 良 → short message: "今日各海域健康状态良好，无需关注。"
4. If any zone is 中 or 差 → list only problem zones: `[zone]:[grade]([reason])`

**Tomorrow grades** are computed on-the-fly using forecast data — no new database table needed. The grading logic mirrors `HealthAssessmentTask` (SST anomaly, CHL average, heatwave detection, worst-indicator-wins).

## New/Modified Files

### New
| File | Purpose |
|------|---------|
| `ocean-server/.../sms/SmsService.java` | Interface: `send(phone, content)` |
| `ocean-server/.../sms/AliyunSmsService.java` | Aliyun SDK implementation |
| `ocean-server/.../sms/MockSmsService.java` | Dev implementation, logs to console |
| `ocean-server/.../task/HealthSmsTask.java` | Scheduled task, cron `0 0 8 * * ?` |

### Modified
| File | Change |
|------|--------|
| `SysUser.java` | Add `phone` field |
| `HealthService.java` / `HealthServiceImpl.java` | Add `buildDailySummary()` method |
| `UserSaveDTO.java` | Add `phone` field |
| `UserVO.java` | Add `phone` field |
| `SysUserServiceImpl.java` | Handle `phone` in CRUD |
| `UserView.vue` | Add phone column to user table |
| `application.yml` | Add SMS config section |
| `pom.xml` | Add Aliyun SMS SDK dependency |

## Aliyun Setup (user action required)

1. Register Aliyun SMS service at https://dysms.console.aliyun.com
2. Apply for signature: "海洋预报系统" (or similar)
3. Apply for template: `【海洋健康日报】${content}` — single variable for dynamic content
4. Create RAM user with SMS permissions → get AccessKey
5. Fill in `application.yml`: access-key-id, access-key-secret, sign-name, template-code

## Edge Cases

- **No health_record yet** (before 02:30 assessment): skip, don't send
- **No forecast data** for tomorrow: send today only, note "明日预报数据暂缺"
- **No admin with phone**: log warning, skip
- **Multiple admin phones**: iterate and send to each
- **SMS send failure**: log error, don't retry (next day's run will cover)
