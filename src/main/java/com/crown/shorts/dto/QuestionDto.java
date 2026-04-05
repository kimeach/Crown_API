package com.crown.shorts.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class QuestionDto {
    private Integer questionId;
    private String  category;
    private String  groupName;
    private String  type;       // single | multi | number | text
    private String  keyName;
    private String  label;
    private String  description;
    private List<Map<String, String>> options; // [{value, label}]
    private String  defaultVal;
    private Integer minVal;
    private Integer maxVal;
    private Integer sortOrder;
    private Boolean required;
}
