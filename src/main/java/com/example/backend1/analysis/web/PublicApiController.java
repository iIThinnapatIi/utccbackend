// src/main/java/com/example/backend1/analysis/web/PublicApiController.java
package com.example.backend1.analysis.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping({"/api", "/analysis"})
public class PublicApiController {

    private final JdbcTemplate jdbc;
    public PublicApiController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 1) รายการ mentions + filter (+exclude ใหม่)
    @GetMapping("/mentions")
    public List<Map<String,Object>> mentions(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "ทั้งหมด") String faculty,
            @RequestParam(defaultValue = "ทั้งหมด") String sent,
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to,
            @RequestParam(defaultValue = "") String exclude,      // NEW: คำที่ต้องการยกเว้น (คั่นด้วย ,)
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, " +
                        "       COALESCE(summary, '') AS title, " +
                        "       COALESCE(faculty_code, 'unknown') AS faculty, " +
                        "       COALESCE(sentiment, 'unknown') AS sentiment, " +
                        "       DATE(analyzed_at) AS created_at, " +
                        "       COALESCE(source_table, '-') AS source " +
                        "FROM typhoon_analysis WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (!q.isEmpty()) {
            sql.append("AND summary LIKE ? ");
            params.add("%" + q + "%");
        }
        if (!"ทั้งหมด".equals(faculty)) {
            sql.append("AND faculty_code = ? ");
            params.add(faculty);
        }
        if (!"ทั้งหมด".equals(sent)) {
            sql.append("AND sentiment = ? ");
            params.add(sent);
        }
        if (!from.isEmpty() && !to.isEmpty()) {
            sql.append("AND DATE(analyzed_at) BETWEEN ? AND ? ");
            params.add(from);
            params.add(to);
        }

        // NEW: exclude keyword(s) — แยกด้วย comma
        if (!exclude.isBlank()) {
            for (String kw : exclude.split(",")) {
                String k = kw.trim();
                if (!k.isEmpty()) {
                    sql.append("AND summary NOT LIKE ? ");
                    params.add("%" + k + "%");
                }
            }
        }

        sql.append("ORDER BY analyzed_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(Math.max(0, (page - 1) * size));
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // 2) latest
    @GetMapping("/latest")
    public List<Map<String,Object>> latest(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String sql =
                "SELECT id, " +
                        "       COALESCE(summary, '') AS title, " +
                        "       COALESCE(faculty_code, 'unknown') AS faculty, " +
                        "       COALESCE(sentiment, 'unknown') AS sentiment, " +
                        "       DATE(analyzed_at) AS created_at, " +
                        "       COALESCE(source_table, '-') AS source " +
                        "FROM typhoon_analysis " +
                        "ORDER BY analyzed_at DESC " +
                        "LIMIT ? OFFSET ?";
        return jdbc.queryForList(sql, size, Math.max(0, page * size));
    }

    // 3) Mentions Trend (daily) — ใช้ view v_trend_daily
    @GetMapping({"/trend/daily", "/mentions/trend", "/mentions/trend/daily"})
    public List<Map<String, Object>> trendDaily(
            @RequestParam(defaultValue = "") String from,   // YYYY-MM-DD (optional)
            @RequestParam(defaultValue = "") String to      // YYYY-MM-DD (optional)
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT ymd, total AS count FROM v_trend_daily WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (!from.isEmpty() && !to.isEmpty()) {
            sql.append("AND ymd BETWEEN ? AND ? ");
            params.add(from);
            params.add(to);
        }

        sql.append("ORDER BY ymd ASC");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // 4) Sentiment summary (รวม)
    @GetMapping({"/sentiment/summary","/summary"})
    public List<Map<String,Object>> sentimentSummary(
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to,
            @RequestParam(defaultValue = "") String faculty
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(sentiment, 'unknown') AS sentiment, COUNT(*) AS total " +
                        "FROM typhoon_analysis WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (!from.isEmpty() && !to.isEmpty()) {
            sql.append("AND DATE(analyzed_at) BETWEEN ? AND ? ");
            params.add(from);
            params.add(to);
        }
        if (!faculty.isEmpty() && !"ทั้งหมด".equals(faculty)) {
            sql.append("AND faculty_code = ? ");
            params.add(faculty);
        }

        sql.append("GROUP BY COALESCE(sentiment, 'unknown')");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // 5) Faculties summary / Top faculties
    @GetMapping({"/faculties/summary", "/top-faculties"})
    public List<Map<String,Object>> facultiesSummary(
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(faculty_code, 'unknown') AS faculty_code, COUNT(*) AS total " +
                        "FROM typhoon_analysis WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (!from.isEmpty() && !to.isEmpty()) {
            sql.append("AND DATE(analyzed_at) BETWEEN ? AND ? ");
            params.add(from);
            params.add(to);
        }

        sql.append("GROUP BY COALESCE(faculty_code, 'unknown') ORDER BY total DESC");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // 6) Topics summary
    @GetMapping("/topics/summary")
    public List<Map<String,Object>> topicsSummary(
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(topic, 'unknown') AS topic, COUNT(*) AS total " +
                        "FROM typhoon_analysis WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (!from.isEmpty() && !to.isEmpty()) {
            sql.append("AND DATE(analyzed_at) BETWEEN ? AND ? ");
            params.add(from);
            params.add(to);
        }

        sql.append("GROUP BY COALESCE(topic, 'unknown') ORDER BY total DESC");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // 7) Summary ต่อแอป (source_table) — NEW
    @GetMapping("/summary/by-app")
    public List<Map<String,Object>> summaryByApp(
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(source_table, '-') AS app, " +
                        "       COUNT(*) AS total, " +
                        "       SUM(CASE WHEN sentiment='positive' THEN 1 ELSE 0 END) AS pos, " +
                        "       SUM(CASE WHEN sentiment='neutral'  THEN 1 ELSE 0 END) AS neu, " +
                        "       SUM(CASE WHEN sentiment='negative' THEN 1 ELSE 0 END) AS neg " +
                        "FROM typhoon_analysis WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (!from.isEmpty() && !to.isEmpty()) {
            sql.append("AND DATE(analyzed_at) BETWEEN ? AND ? ");
            params.add(from);
            params.add(to);
        }

        sql.append("GROUP BY COALESCE(source_table, '-') ORDER BY total DESC");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // 8) Compare apps (ใช้ข้อมูลเดียวกับ summaryByApp) — NEW
    @GetMapping("/compare/apps")
    public List<Map<String,Object>> compareApps(
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to
    ) {
        return summaryByApp(from, to);
    }

    // 9) Trend รายเดือน — NEW
    @GetMapping("/trend/monthly")
    public List<Map<String,Object>> trendMonthly(
            @RequestParam(required = false) String from,  // รูปแบบ YYYY-MM
            @RequestParam(required = false) String to     // รูปแบบ YYYY-MM
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT DATE_FORMAT(analyzed_at, '%Y-%m') AS month, " +
                        "       COUNT(*) AS total, " +
                        "       SUM(CASE WHEN sentiment='positive' THEN 1 ELSE 0 END) AS pos, " +
                        "       SUM(CASE WHEN sentiment='neutral'  THEN 1 ELSE 0 END) AS neu, " +
                        "       SUM(CASE WHEN sentiment='negative' THEN 1 ELSE 0 END) AS neg " +
                        "FROM typhoon_analysis WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        // ใช้ boundary แบบ half-open interval กัน LAST_DAY param ป่วง
        // from: >= CONCAT(from,'-01')
        if (from != null && !from.isBlank()) {
            sql.append("AND analyzed_at >= CONCAT(?, '-01') ");
            params.add(from);
        }
        // to:  < DATE_ADD(CONCAT(to,'-01'), INTERVAL 1 MONTH)
        if (to != null && !to.isBlank()) {
            sql.append("AND analyzed_at < DATE_ADD(CONCAT(?, '-01'), INTERVAL 1 MONTH) ");
            params.add(to);
        }

        sql.append("GROUP BY DATE_FORMAT(analyzed_at, '%Y-%m') ORDER BY month");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // 10) Top 5 Topics by sentiment — NEW
    @GetMapping("/topics/top5")
    public List<Map<String,Object>> top5Topics(
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(topic, 'unknown') AS topic, " +
                        "       SUM(CASE WHEN sentiment='positive' THEN 1 ELSE 0 END) AS pos, " +
                        "       SUM(CASE WHEN sentiment='neutral'  THEN 1 ELSE 0 END) AS neu, " +
                        "       SUM(CASE WHEN sentiment='negative' THEN 1 ELSE 0 END) AS neg, " +
                        "       COUNT(*) AS total " +
                        "FROM typhoon_analysis WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (!from.isEmpty() && !to.isEmpty()) {
            sql.append("AND DATE(analyzed_at) BETWEEN ? AND ? ");
            params.add(from);
            params.add(to);
        }

        sql.append("GROUP BY COALESCE(topic, 'unknown') ");
        sql.append("ORDER BY total DESC LIMIT 5");

        return jdbc.queryForList(sql.toString(), params.toArray());
    }
}
