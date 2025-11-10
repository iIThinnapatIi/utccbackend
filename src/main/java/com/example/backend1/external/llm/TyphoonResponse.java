package com.example.backend1.external.llm;

import lombok.Data;

/**
 * Response ที่ได้จาก Typhoon API
 */
@Data
public class TyphoonResponse {

    /** sentiment ที่โมเดลประเมิน เช่น positive / neutral / negative */
    private String sentiment;

    /** ค่าความมั่นใจของโมเดล (0.0 - 1.0) */
    private Double score;

    /** ชื่อโมเดลที่ใช้ (optional) */
    private String model;
}
