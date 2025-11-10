package com.example.backend1.analysis.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping({"/api", "/analysis"}) // รองรับทั้งสองพาธ
public class SettingsController {

    private final JdbcTemplate jdbc;
    private final ObjectMapper om;

    public SettingsController(JdbcTemplate jdbc, ObjectMapper om) {
        this.jdbc = jdbc;
        this.om = om;
    }

    // ---------- GET /api/settings หรือ /analysis/settings ----------
    @GetMapping("/settings")
    public Map<String, Object> getSettings() {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT theme, notifications_enabled, negative_threshold, sources_json " +
                        "FROM user_settings WHERE id = 1"
        );

        Map<String, Object> out = new LinkedHashMap<>();
        // theme
        out.put("theme", toTheme(row.get("theme")));
        // notificationsEnabled (ทนทุกชนิด)
        out.put("notificationsEnabled", toBoolean(row.get("notifications_enabled")));
        // negativeThreshold
        out.put("negativeThreshold", toInt(row.get("negative_threshold"), 20));
        // sources (json array)
        out.put("sources", readSources(row.get("sources_json")));

        return out;
    }

    // ---------- PUT /api/settings หรือ /analysis/settings ----------
    @PutMapping("/settings")
    public Map<String, Object> updateSettings(@RequestBody Map<String, Object> body) {
        String theme = toTheme(body.get("theme"));
        boolean notify = toBoolean(body.get("notificationsEnabled"));
        int threshold = toInt(body.get("negativeThreshold"), 20);

        // รองรับทั้ง array จริง หรือสตริง JSON
        String sourcesJson = writeSources(body.get("sources"));

        jdbc.update("""
                UPDATE user_settings
                   SET theme               = ?,
                       notifications_enabled = ?,
                       negative_threshold    = ?,
                       sources_json          = ?
                 WHERE id = 1
                """,
                theme,
                notify ? 1 : 0,
                threshold,
                sourcesJson
        );

        // ส่งค่าล่าสุดกลับ
        return getSettings();
    }

    // ================= helpers =================

    private String toTheme(Object v) {
        String s = Objects.toString(v, "LIGHT").trim();
        return "DARK".equalsIgnoreCase(s) ? "DARK" : "LIGHT";
    }

    /** แปลงทุกชนิดให้เป็น boolean: Boolean/Number/String/byte[] */
    private boolean toBoolean(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        if (v instanceof byte[] bytes) {
            // บาง dialect คืนค่า BIT(1) เป็น byte[]
            return bytes.length > 0 && bytes[0] != 0;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return false;
        if ("true".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s)) return false;
        // กรณี "1"/"0"
        try { return Integer.parseInt(s) != 0; } catch (Exception ignored) { }
        return false;
    }

    private int toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s && !s.isBlank()) {
            try { return Integer.parseInt(s.trim()); } catch (Exception ignored) { }
        }
        if (v instanceof byte[] bytes) {
            // เผื่อมีกรณีเก็บเป็นสตริงใน byte[]
            String s = new String(bytes, StandardCharsets.UTF_8);
            try { return Integer.parseInt(s.trim()); } catch (Exception ignored) { }
        }
        return def;
    }

    /** อ่าน sources_json -> List<String> แบบปลอดภัย */
    private List<String> readSources(Object raw) {
        if (raw == null) return List.of();
        try {
            if (raw instanceof byte[] bytes) {
                raw = new String(bytes, StandardCharsets.UTF_8);
            }
            String json = Objects.toString(raw, "[]").trim();
            if (json.isEmpty()) return List.of();
            return om.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /** เขียน sources -> JSON string (รับได้ทั้ง List<?> หรือ สตริง JSON) */
    private String writeSources(Object v) {
        try {
            if (v instanceof String s) {
                String t = s.trim();
                // ถ้าเป็น JSON array แล้ว ส่งผ่าน
                if (t.startsWith("[")) return t;
                // ถ้าเป็นคอมม่าเซพาเรต แปลงเป็น array
                List<String> arr = new ArrayList<>();
                for (String part : t.split(",")) {
                    String p = part.trim();
                    if (!p.isBlank()) arr.add(p);
                }
                return om.writeValueAsString(arr);
            }
            if (v instanceof Collection<?> col) {
                List<String> arr = new ArrayList<>();
                for (Object o : col) arr.add(Objects.toString(o, ""));
                return om.writeValueAsString(arr);
            }
        } catch (Exception ignored) { }
        // fallback เป็น array ว่าง
        return "[]";
    }
}
