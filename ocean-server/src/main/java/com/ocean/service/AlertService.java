package com.ocean.service;

import com.ocean.dto.AlertRuleSaveDTO;
import com.ocean.entity.AlertRule;
import com.ocean.vo.AlertEventVO;
import com.ocean.vo.AlertStationDetailVO;
import java.time.LocalDate;
import java.util.List;

public interface AlertService {
    List<AlertRule> getRules();
    void addRule(AlertRuleSaveDTO dto);
    void updateRule(Long id, AlertRuleSaveDTO dto);
    List<AlertEventVO> getEvents(LocalDate alertDate, Long zoneId, String status);
    AlertEventVO getEventById(Long id);
    List<AlertStationDetailVO> getEventStations(Long eventId);
    void acknowledgeEvent(Long id, Long userId);
    int generateAlerts(LocalDate alertDate);
}
