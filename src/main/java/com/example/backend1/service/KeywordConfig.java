package com.example.backend1.service;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class KeywordConfig {
    // ขอบเขตคำที่บ่งชี้ว่าเกี่ยวกับ UTCC
    public final Set<String> utccWhitelist = Set.of(
            "utcc", "#utcc", "มหาวิทยาลัยหอการค้าไทย", "หอการค้า", "ยูทีซีซี"
    );

    // คำกวน/สับสน
    public final Set<String> utccBlacklist = Set.of(
            "utc " // space กันชนไม่ชนกับ utcc
    );

    // Faculty mapping (keyword -> canonical)
    public final Map<String,String> facultyMap = Map.ofEntries(
            Map.entry("บัญชี","บัญชี"),
            Map.entry("คณะบัญชี","บัญชี"),
            Map.entry("บริหาร","บริหารธุรกิจ"),
            Map.entry("คณะบริหาร","บริหารธุรกิจ"),
            Map.entry("นิเทศ","นิเทศศาสตร์"),
            Map.entry("วิทยาการคอมพิวเตอร์","วิทย์คอม"),
            Map.entry("cs","วิทย์คอม"),
            Map.entry("วิศว","วิศวกรรมศาสตร์"),
            Map.entry("การท่องเที่ยว","การท่องเที่ยวฯ")
    );

    // Topic mapping (keyword -> topic)
    public final Map<String,String> topicMap = Map.ofEntries(
            Map.entry("open house","รับนศ."),
            Map.entry("เปิดบ้าน","รับนศ."),
            Map.entry("สมัครเรียน","รับนศ."),
            Map.entry("รับนศ","รับนศ."),
            Map.entry("กยศ","กยศ."),
            Map.entry("ปริญญา","รับปริญญา"),
            Map.entry("รับปริญญา","รับปริญญา"),
            Map.entry("รีวิว","รีวิว"),
            Map.entry("โฆษณา","ข่าว/PR"),
            Map.entry("ข่าว","ข่าว/PR"),
            Map.entry("กิจกรรม","งานกิจกรรม"),
            Map.entry("งาน","งานกิจกรรม"),
            Map.entry("ร้องเรียน","ปัญหา/ร้องเรียน"),
            Map.entry("ปัญหา","ปัญหา/ร้องเรียน")
    );

    // คำบวก/ลบอย่างง่าย
    public final Set<String> posWords = Set.of("ดีมาก","ประทับใจ","ชอบ","แนะนำ","น่ารัก","สะอาด","ดี","awesome","great","love");
    public final Set<String> negWords = Set.of("แย่","ช้า","ห่วย","แพง","ไม่พอใจ","ร้องเรียน","เสียเวลา","bad","worst","hate");
}