package com.example.backend1.util;


import java.util.*;
import java.util.regex.Pattern;


public class CategoryClassifier {
    private static final Set<String> POSITIVE_WORDS = Set.of(
            "ดี", "เยี่ยม", "ชอบ", "รัก", "ประทับใจ", "สุดยอด", "ปัง", "เลิศ", "น่ารัก", "โคตรดี", "แจ่ม"
    );
    private static final Set<String> NEGATIVE_WORDS = Set.of(
            "แย่", "โกง", "ห่วย", "เกลียด", "ผิดหวัง", "ช้า", "แพง", "โง่", "หลอก", "ปั่น", "แฉ"
    );


    // Simple 18+ / sexual / sensitive lexicon (extend as needed)
    private static final Pattern ADULT_PATTERN = Pattern.compile(
            String.join("|", List.of(
                    "18+", "กาม", "หื่น", "เซ็กซ์", "sex", "เสียตัว", "สยิว", "NSFW",
                    "ชายแท้", "หญิงแท้", "ชายเดี่ยว", "หญิงเดี่ยว", "หาs*เสียว", "นัดs*w+",
                    "แตกใน", "โม้ค", "อม", "เสร็จ", "นม", "ก้น", "ควย", "หี", "น้ำเงี่ยน",
                    "หาคู่นอน", "เสียบ", "ดูด", "ขึ้นครู"
            )), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);


    // Faculty dictionary for UTCC style grouping (extend freely)
    private static final Map<String, String> FACULTY_KEYWORDS = Map.ofEntries(
            Map.entry("บัญชี", "คณะบัญชี"),
            Map.entry("accounting", "คณะบัญชี"),
            Map.entry("บริหารธุรกิจ", "คณะบริหารธุรกิจ"),
            Map.entry("business", "คณะบริหารธุรกิจ"),
            Map.entry("นิเทศ", "คณะนิเทศศาสตร์"),
            Map.entry("นิเทศศาสตร์", "คณะนิเทศศาสตร์"),
            Map.entry("เศรษฐศาสตร์", "คณะเศรษฐศาสตร์"),
            Map.entry("วิทยาศาสตร์การจัดการ", "คณะวิทยาศาสตร์การจัดการ"),
            Map.entry("วิทยาศาสตร์และเทคโนโลยี", "คณะวิทยาศาสตร์และเทคโนโลยี"),
            Map.entry("คอมพิวเตอร์", "คณะวิทยาศาสตร์และเทคโนโลยี"),
            Map.entry("cs", "คณะวิทยาศาสตร์และเทคโนโลยี"),
            Map.entry("โลจิสติกส์", "คณะโลจิสติกส์"),
            Map.entry("โรงแรม", "การท่องเที่ยวและการโรงแรม"),
            Map.entry("ท่องเที่ยว", "การท่องเที่ยวและการโรงแรม")
    );


    public static String sentiment(String text) {
        int pos = 0, neg = 0;
        String t = text.toLowerCase(Locale.ROOT);
        for (String w : POSITIVE_WORDS) if (t.contains(w)) pos++;
        for (String w : NEGATIVE_WORDS) if (t.contains(w)) neg++;
        if (pos == 0 && neg == 0) return "neutral";
        return pos >= neg ? "positive" : "negative";
    }


    public static boolean isAdult(String text) {
        return ADULT_PATTERN.matcher(text).find();
    }


    public static Set<String> faculties(String text) {
        Set<String> out = new LinkedHashSet<>();
        String t = text.toLowerCase(Locale.ROOT);
        FACULTY_KEYWORDS.forEach((k, v) -> {
            if (t.contains(k.toLowerCase(Locale.ROOT))) out.add(v);
        });
        return out;
    }


    public static Set<String> categories(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (isAdult(text)) out.add("18+");
        Set<String> facs = faculties(text);
        if (!facs.isEmpty()) out.add("คณะ/หน่วยงาน");
        String s = sentiment(text);
        out.add("sent:" + s);
        if (text.toLowerCase(Locale.ROOT).contains("utcc") || text.contains("หอการค้า")) {
            out.add("UTCC");
        }
        return out;
    }
}