package com.example.backend1.analysis.service;

import com.example.backend1.analysis.dto.AnalyzeRequest;
import com.example.backend1.analysis.dto.AnalyzeResponse;
import com.example.backend1.llm.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LlmAnalyzer {

    private final LlmService llm;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public LlmAnalyzer(LlmService llm) {
        this.llm = llm;
    }

    public AnalyzeResponse analyze(AnalyzeRequest req) {

        // 1) สร้าง prompt แบบละเอียด (intent/topic/emotion/impact/faculty ฯลฯ)
        String prompt = buildPrompt(req);

        // 2) เรียก LLM ผ่าน service ที่คุณสร้างไว้
        String answer = llm.ask(
                prompt,
                req.getModel(),
                req.getTemperature(),
                req.getMaxTokens()
        );

        // 3) เตรียม response object
        AnalyzeResponse res = new AnalyzeResponse();

        // 4) แปลงผลจาก LLM เป็น JSON แล้ว parse ฟิลด์ต่าง ๆ
        try {
            JsonNode node = mapper.readTree(answer);

            // ฟิลด์หลัก
            res.setSentiment(node.path("sentiment").asText("unknown"));
            res.setTopic(node.path("topic").asText("unknown"));

            // ฟิลด์เชิงลึก
            res.setIntent(node.path("intent").asText("unknown"));
            res.setUtccRelevance(node.path("utcc_relevance").asText("none"));
            res.setEmotion(node.path("emotion").asText("neutral"));
            res.setImpactLevel(node.path("impact_level").asText("impact_low"));
            res.setNsfw(node.path("nsfw").asText("safe"));
            res.setToxicity(node.path("toxicity").asText("none"));
            res.setActor(node.path("actor").asText("unknown"));
            res.setHiddenMeaning(node.path("hidden_meaning").asText("-"));

            // faculty_guess: เก็บทั้ง object เป็น JSON string
            JsonNode fg = node.path("faculty_guess");
            if (!fg.isMissingNode() && !fg.isNull()) {
                res.setFacultyGuessJson(fg.toString());
            } else {
                res.setFacultyGuessJson(
                        "{\"faculty_code\":\"unknown\",\"faculty_name\":\"-\",\"major_code\":\"unknown\",\"major_name\":\"-\",\"level\":\"unknown\",\"reason\":\"-\"}"
                );
            }

            // เก็บข้อความดิบทั้งหมดจาก LLM
            res.setAnswerRaw(answer);

        } catch (Exception e) {
            // ถ้า parse JSON ไม่ได้ ให้ใช้ค่า default ป้องกันระบบพัง
            res.setSentiment("unknown");
            res.setTopic("unknown");
            res.setIntent("unknown");
            res.setUtccRelevance("none");
            res.setEmotion("neutral");
            res.setImpactLevel("impact_low");
            res.setNsfw("safe");
            res.setToxicity("none");
            res.setActor("unknown");
            res.setHiddenMeaning("-");
            res.setFacultyGuessJson(
                    "{\"faculty_code\":\"unknown\",\"faculty_name\":\"-\",\"major_code\":\"unknown\",\"major_name\":\"-\",\"level\":\"unknown\",\"reason\":\"-\"}"
            );
            res.setAnswerRaw(answer);
        }

        return res;
    }

    // -------- prompt แบบละเอียด --------
    private String buildPrompt(AnalyzeRequest req) {
        return """
คุณคือนักวิเคราะห์ข้อความโซเชียลภาษาไทยสำหรับระบบ Social Listening ของมหาวิทยาลัยหอการค้าไทย (UTCC)
ทำงานเหมือนนักการตลาด + นักวิเคราะห์ข้อมูล

เป้าหมาย:
1) รู้ว่าโพสต์นี้ "พูดเรื่องอะไร" (topic) และ "ต้องการอะไร" (intent)
2) รู้ว่าเกี่ยวกับ UTCC มากน้อยแค่ไหน (utcc_relevance)
3) แยกอารมณ์/ความรู้สึก เช่น กังวล โกรธ ภูมิใจ สนุก (emotion)
4) ประเมินระดับผลกระทบต่อภาพลักษณ์ (impact_level)
5) ตรวจเนื้อหาเสี่ยง 18+ และความเป็นพิษ (nsfw, toxicity)
6) เดาว่าเกี่ยวกับ "คณะ" และ "สาขา" ใดของ UTCC (faculty_guess)
7) อธิบายเหตุผลและยก "ประโยคจากโพสต์จริง" มาเป็นหลักฐาน

-------------------------
กติกาในการตอบ (สำคัญมาก):
-------------------------
- ตอบเป็น JSON เพียงอย่างเดียว ห้ามมีข้อความอื่น และห้ามใส่ ``` หรือแท็กโค้ด
- ใช้ key เฉพาะที่กำหนดเท่านั้น: sentiment, intent, topic, utcc_relevance,
  emotion, impact_level, nsfw, toxicity, actor,
  summary, hidden_meaning, reason, evidence, faculty_guess
- ทุก value ต้องเป็น string หรือ array/object ตามโครง JSON ตัวอย่าง
- ใช้ภาษาไทยสำหรับ summary / hidden_meaning / reason / evidence / reason ใน faculty_guess
- ค่าแบบ enum เช่น sentiment, intent, emotion, impact_level ฯลฯ ให้ใช้ภาษาอังกฤษตามที่กำหนดเท่านั้น

-------------------------
sentiment:
-------------------------
- positive = โทนหลักคือชม, พอใจ, ขอบคุณ, ภูมิใจ, แนะนำในทางบวก ต่อ UTCC, คณะ, สาขา, บริการ, หรือประสบการณ์ที่เกี่ยวข้อง
- negative = โทนหลักคือบ่น, ไม่พอใจ, ผิดหวัง, โกรธ, กลัว, กังวล หรือมีเนื้อหา 18+ / ใช้ชื่อมหาลัยในทางเสียหาย
- neutral  = แจ้งข้อมูล, ถามข้อมูล, แชร์ข่าว, โปรโมตสินค้า/บริการทั่วไป ที่ไม่แสดงอารมณ์บวกหรือลบชัดเจน

กติกาพิเศษ:
- ถ้าโพสต์เป็นคำถามแต่มีความกังวลชัด (เช่น กลัวโดนเหยียด, กลัวไม่มีเงินเรียน, กลัวไม่มีงานทำ)
  → ให้ sentiment = negative และ emotion = "worried"
- เนื้อหาโป๊, นัดมีเพศสัมพันธ์, หาเสี่ย, คลิป 18+ ที่เชื่อมกับ "เด็กหอการค้า", "ม.หอการค้า", "UTCC"
  → sentiment = negative, nsfw = "adult", toxicity >= "medium", impact_level = "impact_high_risk"

-------------------------
intent (เจตนาหลัก เลือก 1 ค่า):
-------------------------
- question           = ถามข้อมูล / ขอคำแนะนำ
- complaint          = บ่น / ร้องเรียน / ไม่พอใจ
- praise             = ชม / ขอบคุณ / ประทับใจ
- share_experience   = เล่าประสบการณ์ส่วนตัว (อาจดีหรือแย่ก็ได้)
- information        = แจ้งข่าว / ให้ข้อมูล เช่น โปรโมตกิจกรรมของมหาลัย
- promotion          = โฆษณาสินค้า/บริการ (ของตนเองหรือธุรกิจอื่น)
- spam_or_irrelevant = สแปม หรือไม่เกี่ยวกับมหาวิทยาลัยหอการค้าไทยเลย
- other              = อื่น ๆ ที่ไม่เข้ากลุ่มข้างต้น

-------------------------
topic (หัวข้อหลัก เลือก 1 ค่าใกล้เคียงที่สุด):
-------------------------
- admission              = การรับสมัคร, TCAS, วิธีสมัคร, รอบต่าง ๆ
- tuition_and_loan       = ค่าเทอม, กยศ, ทุนการศึกษา, ค่าครองชีพ
- faculty_and_major      = คณะ, สาขา, หลักสูตร, รายวิชา
- campus_life            = ชีวิตในมหาลัย, เพื่อน, หอพัก, การเดินทาง, ชีวิตประจำวัน
- comparison_university  = เปรียบเทียบ UTCC กับมหาวิทยาลัยอื่น
- utcc_brand             = ภาพลักษณ์แบรนด์, ความดัง, ชื่อเสียงของ UTCC โดยรวม
- service_and_support    = งานทะเบียน, งานการเงิน, ระบบหลังบ้าน, เจ้าหน้าที่
- sports_and_activity    = กีฬา, ชมรม, event, งาน open house, งานกิจกรรม
- job_and_internship     = สมัครงาน, ฝึกงาน, โอกาสทำงานหลังเรียนจบ
- other                  = เรื่องอื่น ๆ

-------------------------
utcc_relevance:
-------------------------
- high   = เนื้อหาหลักพูดถึง UTCC / ม.หอการค้าไทย / ชีวิตในมหาลัยนี้โดยตรง
- medium = พูดถึง UTCC ร่วมกับเรื่องอื่น หรือใช้เป็นโลเกชั่น/บริบท
- low    = แค่กล่าวถึงชื่อ UTCC ผ่าน ๆ
- none   = ไม่เกี่ยวกับมหาวิทยาลัยหอการค้าไทยเลย
          (เช่น ข่าว "หอการค้าไทย" ที่หมายถึงองค์กรการค้าอื่น, สแปมต่างประเทศ)

-------------------------
emotion:
-------------------------
- curious      = สงสัย / อยากรู้
- worried      = กังวล / กลัว / ไม่มั่นใจ
- hopeful      = มีความหวัง / มองบวกต่ออนาคต
- proud        = ภูมิใจ, happy มาก
- angry        = โกรธ, หงุดหงิด, ด่า
- disappointed = ผิดหวัง
- fun          = ตลก, เล่นมุก, ฮา
- neutral      = เป็นกลาง ไม่เห็นอารมณ์ชัด

-------------------------
impact_level:
-------------------------
- impact_high_positive = ส่งผลดีต่อภาพลักษณ์สูง เช่น รีวิวดีมาก, เรื่องราวที่ทำให้คนอยากมาเรียน
- impact_high_risk     = เสี่ยงต่อภาพลักษณ์สูง เช่น เนื้อหา 18+, ด่าแรง, ข่าวลบที่อาจไวรัล
- impact_medium        = มีผลพอสมควร เช่น เด็กลังเลเลือกมอ, เปรียบเทียบ UTCC กับที่อื่น
- impact_low           = แทบไม่มีผล เช่น โฆษณารอบ ๆ มอ, ข่าวหอการค้า (ไม่ใช่มหาลัย), สแปม

-------------------------
nsfw:
-------------------------
- safe  = ไม่มี
- mild  = มีคำสองแง่สองง่ามเล็กน้อย
- adult = เนื้อหา 18+, คำหยาบเรื่องเพศ, นัดมีเพศสัมพันธ์, คลิปโป๊ เป็นต้น

toxicity:
- none   = ไม่มีคำด่า / กล่าวหา
- low    = ตำหนิเล็กน้อย
- medium = ด่า / เสียดสี / ประชดค่อนข้างแรง
- high   = ด่าหนัก, เหยียด, คุกคามรุนแรง

actor:
- prospective_student = เด็ก ม.ปลาย / คนที่กำลังจะสมัคร
- current_student     = นักศึกษาปัจจุบัน
- alumni              = ศิษย์เก่า
- parent              = ผู้ปกครอง
- staff_or_teacher    = บุคลากร / อาจารย์
- general_public      = คนทั่วไป
- business            = ธุรกิจ / ร้านค้า / แบรนด์
- unknown             = ไม่สามารถระบุได้

-------------------------
faculty_guess (คณะ/สาขา/ระดับการศึกษา):
-------------------------
ให้พยายามเดาว่าโพสต์เกี่ยวข้องกับ "คณะ" และ "สาขา" ใดของ UTCC เช่น
- คณะบัญชี, คณะบริหารธุรกิจ, คณะนิเทศศาสตร์, คณะการท่องเที่ยว ฯลฯ
- สาขาเช่น การตลาด, การเงิน, ประชาสัมพันธ์, การโรงแรม, โลจิสติกส์, data science ฯลฯ

faculty_code ตัวอย่าง (ถ้าไม่ชัวร์ให้ใช้ "unknown"):
- ACC  = บัญชี
- BUA  = บริหารธุรกิจ
- ECO  = เศรษฐศาสตร์
- FIN  = การเงิน / การธนาคาร
- LOG  = โลจิสติกส์
- MM   = การจัดการ / การจัดการธุรกิจ
- ENG  = ภาษาอังกฤษธุรกิจ / หลักสูตรอินเตอร์
- LAW  = นิติศาสตร์
- COMM = นิเทศศาสตร์
- TOUR = การท่องเที่ยว / การโรงแรม / Hospitality
- SCI  = วิทยาศาสตร์ / เทคโนโลยี / Data
- unknown = เมื่อไม่สามารถเดาได้

major_code / major_name:
- ให้เขียนชื่อสาขาเป็นคำอธิบายสั้น ๆ เช่น "การตลาด", "การเงิน", "ประชาสัมพันธ์", "การโรงแรม"
- ถ้าไม่รู้ให้ใช้ "unknown" และ "-" ตามลำดับ

level:
- bachelor    = ปริญญาตรี
- master      = ปริญญาโท
- phd         = ปริญญาเอก
- short_course= อบรมระยะสั้น / คอร์สพิเศษ
- unknown     = ไม่ทราบ

ถ้าไม่มีข้อมูลเพียงพอ ให้ใช้:
- faculty_code = "unknown"
- faculty_name = "-"
- major_code   = "unknown"
- major_name   = "-"
- level        = "unknown"
- reason       = "-"

-------------------------
รูปแบบ JSON ที่ต้องตอบ:
-------------------------

{
  "sentiment": "positive|neutral|negative",
  "intent": "question|complaint|praise|share_experience|information|promotion|spam_or_irrelevant|other",
  "topic": "admission|tuition_and_loan|faculty_and_major|campus_life|comparison_university|utcc_brand|service_and_support|sports_and_activity|job_and_internship|other",
  "utcc_relevance": "high|medium|low|none",
  "emotion": "curious|worried|hopeful|proud|angry|disappointed|fun|neutral",
  "impact_level": "impact_high_positive|impact_high_risk|impact_medium|impact_low",
  "nsfw": "safe|mild|adult",
  "toxicity": "none|low|medium|high",
  "actor": "prospective_student|current_student|alumni|parent|staff_or_teacher|general_public|business|unknown",

  "summary": "<สรุปเนื้อหาโพสต์ 1–2 ประโยค แบบเป็นกลาง>",
  "hidden_meaning": "<ความหมายแฝงหรือน้ำเสียง เช่น กังวล, ประชด, ติดตลก ถ้าไม่มีให้ใส่ '-' >",
  "reason": "<เหตุผลสั้น ๆ ว่าทำไมจัด sentiment และ impact_level แบบนั้น>",
  "evidence": [
    "<คำหรือประโยคจากข้อความต้นฉบับที่ช่วยยืนยันการวิเคราะห์>",
    "<1–5 รายการ>"
  ],

  "faculty_guess": {
    "faculty_code": "<รหัสคณะ หรือ 'unknown'>",
    "faculty_name": "<ชื่อคณะ หรือ '-'>",
    "major_code": "<รหัส/ชื่อย่อสาขา หรือ 'unknown'>",
    "major_name": "<ชื่อสาขาเต็ม หรือ '-'>",
    "level": "bachelor|master|phd|short_course|unknown",
    "reason": "<ทำไมจึงคิดว่าเกี่ยวกับคณะ/สาขานี้ ถ้าไม่รู้ให้ใช้ '-'>"
  }
}

-------------------------
ข้อความที่ต้องวิเคราะห์:
-------------------------
%s
""".formatted(req.getText());
    }
}
