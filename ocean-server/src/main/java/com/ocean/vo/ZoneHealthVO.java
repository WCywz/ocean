package com.ocean.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ZoneHealthVO {
    private List<Map<String, Object>> zones;
    private String overallGrade;
    private String summary;
}
