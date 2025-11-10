package com.example.backend1.external.llm;

/**
 * Request สำหรับเรียก API ของ Typhoon / LLM
 * เวอร์ชันไม่พึ่ง Lombok เพื่อให้คอมไพล์ได้แน่นอน
 */
public class TyphoonRequest {

    /** ข้อความที่ต้องการวิเคราะห์ เช่น "UTCC เป็นมหาวิทยาลัยที่ยอดเยี่ยม" */
    private String text;

    /** ภาษา (เช่น "th" สำหรับไทย, "en" สำหรับอังกฤษ) */
    private String lang = "th";

    public TyphoonRequest() {}

    public TyphoonRequest(String text, String lang) {
        this.text = text;
        this.lang = (lang == null || lang.isBlank()) ? "th" : lang;
    }

    // --- getters / setters ---
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = (lang == null || lang.isBlank()) ? "th" : lang; }
}
