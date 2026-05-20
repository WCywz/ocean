package com.ocean.service;

import java.time.LocalDate;

public interface ObservationIngestService {

    /**
     * Ingest observation data for the next un-ingested date (systemDate + 1),
     * then advance the system date. Returns the date that was ingested.
     */
    LocalDate ingestNextDay();

    /**
     * Ingest observation data for a specific date.
     */
    void ingestDate(LocalDate date);
}
