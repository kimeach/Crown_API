package com.crown.shorts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class SfxItemDto {
    private Integer     id;
    private String      name;
    private Double      duration;    // 초
    private List<String> tags;
    private String      previewUrl;  // nullable
}
