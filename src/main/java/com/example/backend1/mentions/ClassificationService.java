package com.example.backend1.mentions;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class ClassificationService {

    // ===== 1) หมวดเนื้อหาเสี่ยง/18+ (ตัวอย่างที่คุณยกมา) =====
    // หมายเหตุ: ใช้เพื่อ "แท็กคอนเทนต์" ไม่ใช่ตีตราคน
    private static final List<Pattern> ADULT_PATTERNS = List.of(
            // ตัวอย่างคำที่พบบ่อย: "ชายเดี่ยว", "หญิงเดี่ยว", "นัด", "แลก", "ไซด์ไลน์" ฯลฯ
            // ปรับ/เพิ่มได้ตามที่ทีมกำหนดนโยบาย
            Pattern.compile("ชายเดี่ยว|หญิงเดี่ยว|ชายแท้|หญิงแท้", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("นัด(เจอ|กัน)?|แลก(เปลี่ยน)?|คู่นอน|เสียว|ปล่อยคลิป|ลงคลิป|กลุ่มลับ", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
            Pattern.compile("18\\+|onlyfans|of\\b|คอลเสียว|หาคน(คุย|เทส)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
    );

    // ===== 2) กลุ่มคำเชิงลบ/เชิงบวกแบบกฎง่าย ๆ (ภาษาไทย) =====
    private static final Set<String> NEG_WORDS = Set.of(
            "แย่","ห่วย","เกลียด","โกง","แพง","ช้า","พัง","เสีย","ล้มเหลว","ไร้มาตรฐาน","ไม่ดี","หลอก","ดราม่า","เถื่อน"
    );
    private static final Set<String> POS_WORDS = Set.of(
            "ดี","เยี่ยม","ที่สุด","ประทับใจ","ชอบ","รัก","คุ้มค่า","แนะนำ","สุดยอด","ปลื้ม","ถูก","ไว","โปร"
    );
    private static final Set<String> NEGATION = Set.of("ไม่","มิ","ไม่มี","ไม่เคย");

    // ===== 3) หมวด UTCC / คณะ =====
    private static final Map<String,String> FACULTY_KEYWORDS = Map.ofEntries(
            Map.entry("คณะบริหารธุรกิจ","คณะบริหารธุรกิจ"),
            Map.entry("คณะบัญชี","คณะบัญชี"),
            Map.entry("คณะวิทยาศาสตร์","คณะวิทยาศาสตร์"),
            Map.entry("คณะนิเทศศาสตร์","คณะนิเทศศาสตร์"),
            Map.entry("คณะเศรษฐศาสตร์","คณะเศรษฐศาสตร์"),
            Map.entry("คณะศิลปศาสตร์","คณะศิลปศาสตร์"),
            Map.entry("คณะนิติศาสตร์","คณะนิติศาสตร์"),
            Map.entry("คณะการท่องเที่ยว","คณะการท่องเที่ยว")
            // เติมได้อีกตามที่ต้องการ
    );
    private static final List<Pattern> UTCC_PATTERNS = List.of(
            Pattern.compile("\\bUTCC\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ม\\.หอการค้า|มหาวิทยาลัยหอการค้า|หอการค้าไทย")
    );

    // วิเคราะห์ sentiment เบื้องต้นแบบกฎ
    public String detectSentiment(String text) {
        String[] toks = text.replaceAll("[^\\p{L}\\p{N}\\s]", " ").split("\\s+");
        int score = 0;
        for (int i = 0; i < toks.length; i++) {
            String t = toks[i].toLowerCase(Locale.ROOT);
            boolean negated = (i > 0 && NEGATION.contains(toks[i-1]));
            if (POS_WORDS.contains(t)) score += negated ? -1 : 1;
            if (NEG_WORDS.contains(t)) score += negated ? 1 : -1;
        }
        if (score > 0) return "positive";
        if (score < 0) return "negative";
        return "neutral";
    }

    // จัดหมวด (หลายแท็กต่อชิ้น)
    public List<String> detectCategories(String text) {
        List<String> tags = new ArrayList<>();

        // 18+
        if (ADULT_PATTERNS.stream().anyMatch(p -> p.matcher(text).find())) {
            tags.add("18+");
        }

        // UTCC (รวม general)
        boolean isUTCC = UTCC_PATTERNS.stream().anyMatch(p -> p.matcher(text).find());
        if (isUTCC) tags.add("UTCC");

        // คณะ
        FACULTY_KEYWORDS.keySet().forEach(k -> {
            if (text.toLowerCase(Locale.ROOT).contains(k.toLowerCase(Locale.ROOT))) {
                tags.add(FACULTY_KEYWORDS.get(k));
            }
        });

        // อื่น ๆ เช่น “โปรโมชัน/ขายของ/สแปม” (ตัวอย่างกฎ)
        if (text.matches("(?i).*(โปร|ลดราคา|โค้ด|ส่งฟรี|sale|แจก).*")) {
            tags.add("โปรโมชัน");
        }
        if (text.matches("(?i).*(หลอก|โกง|สแกม|scam).*")) {
            tags.add("เตือนภัย/Scam");
        }

        // ล้างซ้ำ
        return tags.stream().distinct().toList();
    }

    public String detectFacultyTagOrNull(String text) {
        return FACULTY_KEYWORDS.keySet().stream()
                .filter(k -> text.toLowerCase(Locale.ROOT).contains(k.toLowerCase(Locale.ROOT)))
                .map(FACULTY_KEYWORDS::get).findFirst().orElse(null);
    }
}
