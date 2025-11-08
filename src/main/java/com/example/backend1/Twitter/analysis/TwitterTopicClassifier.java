package com.example.backend1.Twitter.analysis;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class TwitterTopicClassifier {

    public String detectFaculty(String text) {
        String t = (text == null) ? "" : text.toLowerCase();

        if (t.contains("บัญชี") || t.contains("accounting") || t.contains("ภาษี")) return "บัญชี";
        if (t.contains("บริหาร") || t.contains("business") || t.contains("การจัดการ")) return "บริหารธุรกิจ";
        if (t.contains("นิเทศ") || t.contains("สื่อสาร") || t.contains("communication")) return "นิเทศศาสตร์";
        if (t.contains("เศรษฐศาสตร์") || t.contains("econ")) return "เศรษฐศาสตร์";
        if (t.contains("ดิจิทัล") || t.contains("it") || t.contains("คอมพิวเตอร์") || t.contains("ai")) return "ดิจิทัลเทคโนโลยี";
        if (t.contains("กฎหมาย") || t.contains("law")) return "นิติศาสตร์";
        return "UNKNOWN";
    }

    public List<String> detectTopics(String text) {
        String t = (text == null) ? "" : text.toLowerCase();
        List<String> topics = new ArrayList<>();
        if (t.contains("ทุน") || t.contains("scholarship")) topics.add("ทุนการศึกษา");
        if (t.contains("รับสมัคร") || t.contains("admission") || t.contains("สมัครเรียน")) topics.add("รับสมัคร");
        if (t.contains("open house") || t.contains("กิจกรรม") || t.contains("อีเวนต์") || t.contains("งาน")) topics.add("กิจกรรม");
        if (t.contains("รีวิว") || t.contains("ประสบการณ์")) topics.add("รีวิว/ประสบการณ์");
        if (t.contains("ข่าว") || t.contains("ประกาศ") || t.contains("แจ้ง")) topics.add("ข่าวสาร");
        if (topics.isEmpty()) topics.add("อื่นๆ");
        return topics;
    }
}
