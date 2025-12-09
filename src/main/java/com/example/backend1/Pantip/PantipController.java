package com.example.backend1.Pantip;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pantip")
public class PantipController {

    // ⭐ ใช้ทั้ง service ใหม่ (temp) และตัว scraper เดิม
    private final PantipTempService tempService;
    private final PantipScraperService scraperService;

    public PantipController(PantipTempService tempService,
                            PantipScraperService scraperService) {
        this.tempService = tempService;
        this.scraperService = scraperService;
    }

    /*
     * 1) โหมด TEMP — ใช้กับหน้า Keywords.jsx
     *    - GET  /pantip/temp-fetch?keyword=ไก่ชน
     *    - POST /pantip/save-temp
     *    - POST /pantip/clear-temp
     */

    // ดึงโพสต์จาก Pantip แบบ preview (ยังไม่บันทึก DB จริง)
    // /pantip/temp-fetch?keyword=ไก่ชน
    @GetMapping("/temp-fetch")
    public ResponseEntity<?> tempFetch(@RequestParam("keyword") String keyword) {
        try {
            List<PantipPost> posts = tempService.fetchTemp(keyword);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            e.printStackTrace();   // ดู error ใน log
            Map<String, Object> err = new HashMap<>();
            err.put("message", "ดึงข้อมูล Pantip ล้มเหลว");
            err.put("detail", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    // บันทึกจาก tempPosts -> social_analysis + pantip_post/comment
    @PostMapping("/save-temp")
    public Map<String, Object> saveTemp() {
        int saved = tempService.saveTempToSocialAnalysis();
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        resp.put("saved", saved);
        return resp;
    }

    // ยกเลิกการดึงแบบ temp (ล้างรายการใน memory)
    @PostMapping("/clear-temp")
    public Map<String, Object> clearTemp() {
        tempService.clearTemp();
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        return resp;
    }

    /*
     * 2) endpoint เดิม (ไว้ใช้กับ Postman / ทดสอบ)
     */

    // ดึงโพสต์จาก Pantip แล้ว "บันทึกลง DB ทันที"
    @GetMapping("/fetchtemp")
    public String fetchTempPlaceholder(@RequestParam("keyword") String keyword) {
        return "โหมด fetch ตรงนี้ยังไม่ได้ใช้งาน (ใช้ /pantip/temp-fetch แทน)";
    }

    //////////////////////////////////////////////// ของกูห้ามลบ
    // ห้ามลบ ห้ามลบ
    // ตอนใช้ดึงใน Postman ไม่ต้องสนใจช่องอินพุตของกูอยู่ด้านล่าง
    @GetMapping("/fetch")
    public String fetch(@RequestParam String keyword) {
        scraperService.scrapePantip(keyword);
        return "ดึงโพสต์และคอมเมนต์ทั้งหมดของ \"" + keyword + "\" สำเร็จ!";
    }

    // รีเซ็ตข้อมูลทั้งหมด (เอาไว้เทส DB)
    @PostMapping("/reset")
    public String resetData() {
        scraperService.resetPantipData();
        return "รีเซ็ตข้อมูลทั้งหมดของ Pantip และตั้งค่า ID ให้เริ่มที่ 1 แล้ว!";
    }

    // http://localhost:8082/pantip/search-and-save?keyword=หอการค้า
    @GetMapping("/search-and-save")
    public String searchAndSave(@RequestParam String keyword) {
        scraperService.scrapePantip(keyword);
        return "ดึงข้อมูลและบันทึกลงฐานข้อมูลด้วยคำว่า: " + keyword;
    }
}
