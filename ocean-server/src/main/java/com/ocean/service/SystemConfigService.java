package com.ocean.service;

import java.time.LocalDate;

public interface SystemConfigService {

    LocalDate getSystemDate();

    void advanceDay();

    void setDate(LocalDate date);
}
