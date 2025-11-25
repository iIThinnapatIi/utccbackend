package com.example.backend1.Pantip;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pantip")
public class PantipController {

    private final PantipScraperService scraperService;

    public PantipController(PantipScraperService scraperService) {
        this.scraperService = scraperService;
    }

    /*
     1) โหมด TEMP — ใช้กับหน้า Keywords.jsx
       - GET  /pantip/temp-fetch?keyword=ไก่ชน
        - POST /pantip/save-temp
        - POST /pantip/clear-temp
      */

    // ดึงโพสต์จาก Pantip แบบ preview (ยังไม่บันทึก DB จริง)
    @GetMapping("/temp-fetch")
    public List<PantipPost> tempFetch(@RequestParam("keyword") String keyword) {
        return scraperService.scrapePantipTemp(keyword);
    }

    // บันทึกจาก tempPosts  pantip_post / pantip_comment
    @PostMapping("/save-temp")
    public Map<String, Object> saveTemp() {
        int saved = scraperService.saveTempToDB();

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        resp.put("saved", saved);
        return resp;
    }

    // ยกเลิกการดึงแบบ temp (ล้างรายการใน memory)
    @PostMapping("/clear-temp")
    public Map<String, Object> clearTemp() {
        scraperService.clearTemp();

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        return resp;
    }

    /*
     * 2) endpoint เดิม (ถ้ายังอยากเก็บไว้ใช้ Postman)
     * */

    // ดึงโพสต์จาก Pantip แล้ว "บันทึกลง DB ทันที"
    @GetMapping("/fetch")
    public String fetch(@RequestParam("keyword") String keyword) {
        return "โหมด fetch ตรงนี้ยังไม่ได้ใช้งาน (ใช้ /pantip/temp-fetch แทน)";
    }

    // reset ข้อมูล Pantip ทั้งหมด
    @PostMapping("/reset")
    public String resetData() {
        //  เทสตอนดึงมั่วๆคำอื่น//
        return "ยังไม่ได้ implement reset ข้อมูล";
    }
}