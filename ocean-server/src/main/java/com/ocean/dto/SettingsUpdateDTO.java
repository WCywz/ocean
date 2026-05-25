package com.ocean.dto;

import lombok.Data;
import java.util.Map;

@Data
public class SettingsUpdateDTO {
    private Map<String, String> settings;
}
