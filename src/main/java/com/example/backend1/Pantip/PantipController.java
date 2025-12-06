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
    // /pantip/temp-fetch?keyword=ไก่ชน
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
    @GetMapping("/fetchtemp")
    public String fetchTempPlaceholder(@RequestParam("keyword") String keyword) {
        return "โหมด fetch ตรงนี้ยังไม่ได้ใช้งาน (ใช้ /pantip/temp-fetch แทน)";
    }

    ////////////////////////////////////////////////ของกูห้ามลบ
    //ห้ามลบ ห้ามลบ
    //ตอนใช้ดึงในpostman ไม่ต้องสนใจช่องอินพุดของกูอยุ่ด้านล่าง
    @GetMapping("/fetch")
    public String fetch(@RequestParam String keyword) {
        scraperService.scrapePantip(keyword);
        return "ดึงโพสต์และคอมเมนต์ทั้งหมดของ \"" + keyword + "\" สำเร็จ!";
    }

    //   รีเซ็ตข้อมูลทั้งหมด อย่าใส่ใจอันนี้ กูเอาไว้เทสdb
    @PostMapping("/reset")
    public String resetData() {
        scraperService.resetPantipData();
        return "  รีเซ็ตข้อมูลทั้งหมดของ Pantip และตั้งค่า ID ให้เริ่มที่ 1 แล้ว!";
    }
    // http://localhost:8082/pantip/search-and-save?keyword=หอการค้า
    @GetMapping("/search-and-save")
    public String searchAndSave(@RequestParam String keyword) {
        scraperService.scrapePantip(keyword);
        return " ดึงข้อมูลและบันทึกลงฐานข้อมูลด้วยคำว่า: " + keyword;
    }
}