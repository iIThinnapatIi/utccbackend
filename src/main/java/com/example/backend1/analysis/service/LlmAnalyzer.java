package com.example.backend1.analysis.service;

import com.example.backend1.analysis.dto.AnalyzeRequest;
import com.example.backend1.analysis.dto.AnalyzeResponse;
import com.example.backend1.llm.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LlmAnalyzer {

    private final LlmService llm;
    private final ObjectMapper mapper = new ObjectMapper();

    // สแลงที่ช่วยบอกทิศทางดี/แย่ (เอาไว้ช่วย LLM อีกชั้น)
    private static final Map<String, Integer> SLANG_SENTIMENT_BOOST = Map.ofEntries(
            // บวกแรง
            Map.entry("ชีเสิร์ฟ", +2),
            Map.entry("โฮ่งมาก", +2),
            Map.entry("เกินต้าน", +2),
            Map.entry("จึ้ง", +2),
            Map.entry("ปัง", +2),
            Map.entry("เริ่ด", +2),
            Map.entry("เทสมาก", +2),
            Map.entry("เทสฉันมาก", +2),
            Map.entry("เหยินมาก", +1),
            Map.entry("ฮ้อป", +1),

            // ลบ/บ่น/หมดไฟ
            Map.entry("อ่อมเกิน", -2),
            Map.entry("อ่อม", -2),
            Map.entry("ชั้น g", -2),
            Map.entry("ชั้น g ", -2),
            Map.entry("คุมกำเนิด", -1),
            Map.entry("วงวาร", -1),
            Map.entry("เม็ดเยอะ", -1),
            Map.entry("จบ!", -1),
            Map.entry("จบ.", -1),
            Map.entry("จบค่ะ", -1)
    );

    @Autowired
    public LlmAnalyzer(LlmService llm) {
        this.llm = llm;
    }

    // ---------------- helper ทั่วไป ----------------

    private boolean containsAny(String text, String... patterns) {
        if (text == null) return false;
        String t = text.toLowerCase();
        for (String p : patterns) {
            if (t.contains(p.toLowerCase())) return true;
        }
        return false;
    }

    // บังคับให้ sentiment เป็นบวก พร้อมยกระดับคะแนนอย่างน้อย minScore
    private void forcePositive(AnalyzeResponse res, int minScore, int maxScore) {
        res.setSentiment("positive");
        Integer cur = res.getSentimentScore();
        int score = (cur == null ? minScore : Math.max(cur, minScore));
        if (score > maxScore) score = maxScore;
        res.setSentimentScore(score);
    }

    /**
     * ปรับ sentiment ให้เป็น positive เมื่อโพสแสดง “ความสนใจ/ทัศนคติที่ดีต่อ UTCC”
     * ครอบคลุมเคส: สนใจสมัคร, เล็งไว้เป็นตัวเลือก, หาเพื่อน, ชมมอ/กิจกรรม ฯลฯ
     */
    private void upgradePositiveIfInterest(AnalyzeResponse res, String text) {
        if (text == null) return;
        String t = text.toLowerCase();

        // ถ้า LLM ตีความเป็น negative อยู่แล้ว (เช่น ด่า, กังวลแรง) เราไม่ไปแหกกลับให้เป็นบวก
        if ("negative".equalsIgnoreCase(res.getSentiment())) {
            return;
        }

        boolean hasUtccWord = containsAny(t,
                "utcc", "หอการค้า", "มหาวิทยาลัยหอการค้าไทย", "ม.หอการค้า");

        // -----------------------------
        // 1) สนใจสมัคร/ยื่น/เลือก UTCC
        // -----------------------------
        boolean interestApply = hasUtccWord && containsAny(t,
                "อยากเรียน", "อยากเข้า", "เล็งไว้", "เล็ง utcc",
                "กำลังจะสมัคร", "จะสมัคร", "สมัครเรียน", "จะยื่น", "จะยื่นพอร์ต",
                "ยื่นพอร์ต", "ปักหมุด", "ปักธง", "top choice", "ตัวเลือกแรก",
                "ลังเลระหว่าง", "เลือก utcc ดีไหม", "ดีมั้ย utcc", "ดีไหม utcc",
                "สนใจคณะ", "สนใจภาค", "มอนี้น่าเรียน", "คณะนี้น่าเรียน");

        if (interestApply) {
            // โทนนี้ถือว่า positive ต่อ UTCC แน่นอน
            forcePositive(res, 70, 85);
            return;
        }

        // -----------------------------
        // 2) ชมมหาลัย/คณะ/บรรยากาศโดยตรง
        // -----------------------------
        boolean praiseUtcc = hasUtccWord && containsAny(t,
                "ดีมาก", "ดีมากๆ", "ดีมากก", "ดีสุด", "โคตรดี", "คือดี",
                "ประทับใจ", "น่าเรียนมาก", "น่าอยู่", "บรรยากาศดี", "คณะดี",
                "คณะนี้ดี", "อาจารย์ดี", "บริการดี", "น่ารัก", "น่าเอ็นดู");

        if (praiseUtcc) {
            forcePositive(res, 75, 90);
            return;
        }

        // -----------------------------
        // 3) หาเพื่อน / สังคมในมอ / อยากมี community
        // -----------------------------
        boolean friendCommunity = hasUtccWord && containsAny(t,
                "หาเพื่อน", "เพื่อนในมอ", "เพื่อน utcc", "เพื่อนหอการค้า",
                "เมคเฟรนด์", "make friend", "รับเพื่อนใหม่", "ใครเรียน utcc",
                "ใครอยู่ utcc", "มีเพื่อนอยู่ utcc ไหม", "หากลุ่ม", "หาแก๊ง");

        if (friendCommunity) {
            forcePositive(res, 65, 80);
            return;
        }

        // -----------------------------
        // 4) สนใจชีวิตในมอ/กิจกรรม/คอนเสิร์ต
        // -----------------------------
        boolean lifeAndEvent = hasUtccWord && containsAny(t,
                "ชีวิตในมอ", "ชีวิตในมหาลัย", "ชีวิตเด็กมอ", "เด็กหอการค้า",
                "event", "อีเวนต์", "อีเว้นท์", "กิจกรรม", "คอนเสิร์ต",
                "งานคอนเสิร์ต", "งานมหาลัย", "งานมหาวิทยาลัย", "งาน open house",
                "open house utcc");

        if (lifeAndEvent) {
            forcePositive(res, 65, 80);
            return;
        }

        // -----------------------------
        // 5) คำถามเชิงบวก/อยากรู้เพิ่มเติม (ไม่ได้บ่น)
        // -----------------------------
        boolean positiveQuestion = hasUtccWord && containsAny(t,
                "ดีไหม", "ดีมั้ย", "โอเคไหม", "เป็นยังไงบ้าง", "น่าเรียนไหม", "น่าเรียนมั้ย")
                && !containsAny(t, "หรือเปล่าแย่", "กลัวไม่ดี", "กลัวโดน", "กลัวไม่มีงาน");

        if (positiveQuestion) {
            forcePositive(res, 60, 75);
        }
    }

    // ---------------- mapping TOPIC จากข้อความดิบ ----------------

    /**
     * จัด topic จากข้อความจริง ตามหมวดที่เราวิเคราะห์จากโพสดิบ
     * ให้ทุกโพสต์ต้องมี topic เสมอ (อย่างน้อย "อื่นๆ/Spam")
     */
    private String detectTopicFromRawText(String text) {
        if (text == null) return "อื่นๆ/Spam";
        String t = text.toLowerCase();

        // 18+
        if (containsAny(t, "นัดเย็ด", "หาเสี่ย", "คลิป18", "คลิป 18", "xnxx", "หาค่าขนม", "ons", "fwb", "หมอนวด")) {
            return "18+";
        }

        // รับจ้าง/จิตอาสา/ทำงานแทน
        if (containsAny(t, "รับทำการบ้าน", "รับทำรายงาน", "รับทำจิตอาสา",
                "รับทำกยศ", "set e-learning", "set e learning", "รับจ้างทำ")) {
            return "รับจ้างจิตอาสา";
        }

        // ค่าเทอม / กยศ
        if (containsAny(t, "กยศ", "ค่าเทอม", "ค่า เทอม", "ค่าแรกเข้า", "ผ่อนค่าเทอม")) {
            return "ค่าเทอม/กยศ";
        }

        // สมัครเรียน / TCAS
        if (containsAny(t, "สมัครเรียน", "tcas", "เปิดรับสมัคร", "ยื่นรอบ", "ยื่นพอร์ต", "พอร์ต")) {
            return "สมัครเรียน/TCAS";
        }

        // เปรียบเทียบมหาลัย
        if (containsAny(t, "utcc", "หอการค้า") &&
                containsAny(t, "dpu", "ธุรกิจบัณฑิตย์", "ไหนดีกว่า", "ดีกว่ากัน")) {
            return "เปรียบเทียบมหาวิทยาลัย";
        }

        // ชีวิตในมหาลัย / event / concert
        if (containsAny(t, "event", "กิจกรรม", "คอนเสิร์ต",
                "soft skills", "soft skill", "งานมหาลัย", "งานมหาวิทยาลัย", "ชีวิตเด็กหอการค้า", "ชีวิตในมอ",
                "ม.หอการค้าไทย", "มหาวิทยาลัยหอการค้าไทย")) {
            return "ชีวิตในมหาลัย/กิจกรรม";
        }

        // ระบบไอที
        if (containsAny(t, "อีเมล", "@utcc.ac.th", "login", "ล็อกอิน", "เข้าไม่ได้", "ระบบล่ม", "เข้าเว็บไม่ได้")) {
            return "ระบบ/ไอที";
        }

        // การเดินทาง
        if (containsAny(t, "ไป utcc", "ไปหอการค้า", "รถเมล์", "รถเมล", "bts", "mrt", "เดินทางไป")) {
            return "การเดินทาง";
        }

        // สังคม / หาเพื่อน / กลัวโดนเหยียด
        if (containsAny(t, "กลัวโดนเหยียด", "โดนเหยียด", "หาเพื่อน", "สังคมเป็นไง",
                "เพื่อนในมอ", "เมคเฟรนด์", "make friend")) {
            return "สังคม/หาเพื่อน";
        }

        // ข่าวเศรษฐกิจ / หอการค้าไทย
        if (containsAny(t, "sme", "หอการค้าไทย", "ประธานหอการค้า",
                "เศรษฐกิจ", "econutcc", "e-conutcc", "econ utcc")) {
            return "ข่าวเศรษฐกิจ/หอการค้า";
        }

        // สแปมหรือไม่เกี่ยว
        return "อื่นๆ/Spam";
    }

    // ---------------- sentiment helper เดิม (slang/nsfw/toxicity) ----------------

    // แปลงคำไทย/คำเพี้ยนของ sentiment ให้เข้ามาตรฐาน หรือคืน null ถ้าดูไม่ออก
    private String normalizeSimpleSentiment(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase();

        switch (s) {
            case "บวก":
            case "ดี":
            case "เชิงบวก":
            case "positive":
                return "positive";
            case "ลบ":
            case "แย่":
            case "เชิงลบ":
            case "negative":
                return "negative";
            case "กลาง":
            case "เฉยๆ":
            case "เฉย ๆ":
            case "เป็นกลาง":
            case "neutral":
                return "neutral";
            case "ไม่พบ":
            case "unknown":
            case "ยังไม่ชัดเจน":
            case "อื่น":
            case "อื่นๆ":
            case "other":
                return null; // ถือว่ายังไม่รู้ ให้ไปเดาต่อ
            default:
                // ถ้า LLM ดันตอบชื่อ emotion เช่น "worried", "fun"
                if ("worried".equals(s) || "sad".equals(s) || "angry".equals(s)) {
                    return "negative";
                }
                if ("happy".equals(s) || "proud".equals(s) || "fun".equals(s)) {
                    return "positive";
                }
                return null;
        }
    }

    // ให้คะแนนจากสแลง/คำวัยรุ่นในข้อความต้นฉบับ
    private int slangScore(String text) {
        if (text == null || text.isBlank()) return 0;
        String t = text.toLowerCase();

        int score = 0;
        for (var e : SLANG_SENTIMENT_BOOST.entrySet()) {
            if (t.contains(e.getKey())) {
                score += e.getValue();
            }
        }

        // ตรวจ pattern ชมเชิงประชด แบบ "สุดยอดไปเลยจ้า"
        if (t.contains("สุดยอดไปเลยจ้า")
                || t.contains("ดีมากเลยนะคะ")
                || (t.contains("ดีมากกก") && t.contains("🙃"))) {
            score -= 2; // ด้านลบ
        }

        return score;
    }

    // รวมทุกอย่างเป็นค่าดี/กลาง/แย่ ตามสแลง + nsfw/toxicity (เลเยอร์แรก)
    private void normalizeSentiment(AnalyzeResponse res, String originalText) {
        String s = normalizeSimpleSentiment(res.getSentiment());
        int slang = slangScore(originalText);

        // ถ้าโพสต์ 18+ หรือมี toxicity สูง → ต้องเป็นลบ
        String nsfw = res.getNsfw();
        String tox  = res.getToxicity();
        if (nsfw != null && nsfw.equalsIgnoreCase("nsfw")) {
            s = "negative";
        }
        if (tox != null && (tox.equalsIgnoreCase("high") || tox.equalsIgnoreCase("medium"))) {
            s = "negative";
        }

        // ถ้า LLM ตอบ neutral/ไม่ชัด แต่ slang ชี้บวกหรือลบแรง ⇒ ขยับตาม slang
        if ((s == null || "neutral".equals(s)) && slang != 0) {
            if (slang > 0) s = "positive";
            else if (slang < 0) s = "negative";
        }

        // ถ้ายังไม่รู้จริง ๆ ให้เป็น neutral
        if (s == null) s = "neutral";

        res.setSentiment(s);
    }

    // เลเยอร์สอง: ปรับ sentiment + คะแนนตาม topic (จากโพสดิบจริง)
    private void normalizeSentimentByTopic(AnalyzeResponse res, String text) {
        String topic = res.getTopic();
        if (topic == null) topic = "";
        String lower = (text == null) ? "" : text.toLowerCase();

        // 18+ ต้องลบเสมอ
        if ("18+".equals(topic)) {
            res.setSentiment("negative");
            res.setSentimentScore(20);
            return;
        }

        // รับจ้างทำงาน ต้องลบเสมอ
        if ("รับจ้างจิตอาสา".equals(topic)) {
            res.setSentiment("negative");
            res.setSentimentScore(30);
            return;
        }

        // ค่าเทอม / กยศ
        if ("ค่าเทอม/กยศ".equals(topic)) {
            if (containsAny(lower, "แพง", "กังวล", "ไม่มีเงิน", "หนัก", "ล้มละลาย")) {
                res.setSentiment("negative");
                res.setSentimentScore(35);
            } else {
                res.setSentiment("neutral");
                res.setSentimentScore(50);
            }
            return;
        }

        // สมัครเรียน / TCAS
        if ("สมัครเรียน/TCAS".equals(topic)) {
            res.setSentiment("neutral");
            if (containsAny(lower, "ดีใจ", "ตื่นเต้น", "ได้ที่นี่", "ติดหอการค้า")) {
                res.setSentiment("positive");
                res.setSentimentScore(70);
            } else {
                res.setSentimentScore(55);
            }
            return;
        }

        // ระบบ/ไอที
        if ("ระบบ/ไอที".equals(topic)) {
            if (containsAny(lower, "เข้าไม่ได้", "ล่ม", "error", "เสีย", "พัง")) {
                res.setSentiment("negative");
                res.setSentimentScore(25);
            } else {
                res.setSentiment("neutral");
                res.setSentimentScore(45);
            }
            return;
        }

        // ชีวิตในมหาลัย / event
        if ("ชีวิตในมหาลัย/กิจกรรม".equals(topic)) {
            if (containsAny(lower, "สนุก", "อร่อย", "ชอบ", "ดีมาก", "ฟิน", "ประทับใจ", "คือดี")) {
                res.setSentiment("positive");
                res.setSentimentScore(70);
            } else {
                res.setSentiment("neutral");
                res.setSentimentScore(55);
            }
            return;
        }

        // ข่าวเศรษฐกิจ / หอการค้า
        if ("ข่าวเศรษฐกิจ/หอการค้า".equals(topic)) {
            res.setSentiment("neutral");
            res.setSentimentScore(55);
            return;
        }

        // สังคม/หาเพื่อน
        if ("สังคม/หาเพื่อน".equals(topic)) {
            if (containsAny(lower, "กลัว", "กังวล", "เหงา", "โดนเหยียด", "ไม่กล้า")) {
                res.setSentiment("negative");
                res.setSentimentScore(35);
            } else {
                res.setSentiment("neutral");
                res.setSentimentScore(50);
            }
            return;
        }

        // การเดินทาง / เปรียบเทียบมอ / อื่นๆ → กลาง
        res.setSentiment("neutral");
        if (res.getSentimentScore() == null) {
            res.setSentimentScore(50);
        }
    }

    private String defaultFacultyGuessJson() {
        return "{\"faculty_code\":\"unknown\",\"faculty_name\":\"-\"," +
                "\"major_code\":\"unknown\",\"major_name\":\"-\"," +
                "\"level\":\"unknown\",\"reason\":\"-\"}";
    }

    // ---------------- main analyze ----------------

    public AnalyzeResponse analyze(AnalyzeRequest req) {

        // 1) สร้าง prompt
        String prompt = buildPrompt(req);

        // 2) เรียก LLM
        String answer = llm.ask(
                prompt,
                req.getModel(),
                req.getTemperature(),
                req.getMaxTokens()
        );

        AnalyzeResponse res = new AnalyzeResponse();

        try {
            JsonNode node = mapper.readTree(answer);

            // ---------- ฟิลด์หลัก ----------
            res.setSentiment(node.path("sentiment").asText(null));
            res.setEmotion(node.path("emotion").asText(null));
            res.setHiddenMeaning(node.path("hidden_meaning").asText("-"));

            // topic จาก LLM (เก็บไว้ก่อน เดี๋ยวเราจะ override จากข้อความดิบ)
            res.setTopic(node.path("topic").asText("unknown"));

            // คะแนน (0–100) รับได้ทั้งตัวเลขและ string
            JsonNode scoreNode = node.path("sentiment_score");
            if (scoreNode.isNumber()) {
                res.setSentimentScore(scoreNode.asInt());
            } else if (scoreNode.isTextual()) {
                try {
                    res.setSentimentScore(Integer.parseInt(scoreNode.asText().trim()));
                } catch (NumberFormatException e) {
                    res.setSentimentScore(null);
                }
            } else {
                res.setSentimentScore(null);
            }

            // ---------- เหตุผล ----------
            String rationaleSent = node.path("rationale_sentiment").asText("").trim();
            if (rationaleSent.isEmpty()) {
                // ถ้าไม่มี ให้ fallback ใช้ reason
                rationaleSent = node.path("reason").asText("").trim();
            }
            res.setRationaleSentiment(rationaleSent);

            String rationaleIntent = node.path("rationale_intent").asText("").trim();
            res.setRationaleIntent(rationaleIntent);

            // ---------- ฟิลด์อื่น ----------
            res.setIntent(node.path("intent").asText("unknown"));
            res.setUtccRelevance(node.path("utcc_relevance").asText("none"));
            res.setImpactLevel(node.path("impact_level").asText("impact_low"));
            res.setNsfw(node.path("nsfw").asText("safe"));
            res.setToxicity(node.path("toxicity").asText("none"));
            res.setActor(node.path("actor").asText("unknown"));

            JsonNode fg = node.path("faculty_guess");
            if (!fg.isMissingNode() && !fg.isNull()) {
                res.setFacultyGuessJson(fg.toString());
            } else {
                res.setFacultyGuessJson(defaultFacultyGuessJson());
            }

            res.setAnswerRaw(answer);

            // ------ ใช้ rule ของเราเองจากข้อความจริง ------
            // 1) จัด topic ใหม่จาก raw text
            String finalTopic = detectTopicFromRawText(req.getText());
            res.setTopic(finalTopic);

            // 2) normalize sentiment ตามสแลง/nsfw/toxicity
            normalizeSentiment(res, req.getText());

            // 3) ปรับ sentiment + คะแนนตาม topic ที่เรากำหนดจากโพสดิบ
            normalizeSentimentByTopic(res, req.getText());

            // 4) อัปเกรดให้เป็นบวกถ้าเป็นโพสสนใจ/ชมมอ/หาเพื่อน/กิจกรรม
            upgradePositiveIfInterest(res, req.getText());

        } catch (Exception e) {
            // ถ้า parse JSON ไม่ได้ ให้ใช้ค่า default ป้องกันระบบพัง
            res.setSentiment("neutral");
            res.setEmotion("neutral");
            res.setHiddenMeaning("-");
            res.setSentimentScore(null);
            res.setRationaleSentiment("");
            res.setRationaleIntent("");

            // topic จากข้อความดิบโดยตรง
            res.setTopic(detectTopicFromRawText(req.getText()));

            res.setIntent("unknown");
            res.setUtccRelevance("none");
            res.setImpactLevel("impact_low");
            res.setNsfw("safe");
            res.setToxicity("none");
            res.setActor("unknown");
            res.setFacultyGuessJson(defaultFacultyGuessJson());
            res.setAnswerRaw(answer);

            normalizeSentiment(res, req.getText());
            normalizeSentimentByTopic(res, req.getText());
            upgradePositiveIfInterest(res, req.getText());
        }

        return res;
    }

    // -------- prompt: อธิบาย rule ให้ LLM (เราใช้เป็น layer แรก) --------
    private String buildPrompt(AnalyzeRequest req) {
        return """
คุณคือนักวิเคราะห์ข้อความโซเชียลภาษาไทยสำหรับระบบ Social Listening ของมหาวิทยาลัยหอการค้าไทย (UTCC)

หน้าที่ของคุณ:
1) ตัดสินว่าโพสต์นี้มีโทน ดี / กลาง / แย่ ต่อ UTCC หรือประเด็นที่เกี่ยวข้องหรือไม่
2) ให้คะแนน sentiment_score (0–100)
3) จัดหมวดหัวข้อ (topic) ให้ตรงกับเนื้อหา
4) อธิบายเหตุผลแบบสั้น ๆ พร้อมอ้างประโยคจากข้อความจริงเป็นหลักฐาน

-------------------------
กติกาเรื่อง sentiment:
-------------------------
- positive = เนื้อหาชม, ถูกใจ, ภูมิใจ, มีโทนสนับสนุน UTCC, คณะ, สาขา, บริการ หรือประสบการณ์ที่เกี่ยวข้อง
- negative = บ่น, ไม่พอใจ, ผิดหวัง, โกรธ, กังวล, ด่า, ประชด, เหน็บ, กล่าวหา, พาดพิงเสียหาย, sexual ไม่เหมาะสม
- neutral  = แจ้งข่าว, ถามข้อมูล, แชร์ลิงก์, ตอบคำถามทั่วไป ที่ไม่แสดงอารมณ์ชัดเจนทั้งบวกหรือลบ

คะแนน sentiment_score:
- 0–20  = ลบมาก (Very negative)
- 21–40 = ลบ (Negative)
- 41–59 = กลาง ๆ (Neutral)
- 60–80 = บวก (Positive)
- 81–100 = บวกมาก (Very positive)

-------------------------
topic ที่อนุญาตให้ใช้ (เลือกที่ใกล้ที่สุด 1 ค่าเท่านั้น):
-------------------------
- "18+"
- "รับจ้างจิตอาสา"
- "สมัครเรียน/TCAS"
- "ค่าเทอม/กยศ"
- "เปรียบเทียบมหาวิทยาลัย"
- "ชีวิตในมหาลัย/กิจกรรม"
- "การเดินทาง"
- "สังคม/หาเพื่อน"
- "ระบบ/ไอที"
- "ข่าวเศรษฐกิจ/หอการค้า"
- "อื่นๆ/Spam"

-------------------------
สิ่งที่ต้องตอบกลับ (รูปแบบ JSON เท่านั้น):
-------------------------

{
  "sentiment": "positive|neutral|negative",
  "sentiment_score": 0-100,
  "topic": "<หนึ่งใน topic จากรายการด้านบน>",

  "rationale_sentiment": "<อธิบายชัด ๆ ว่าทำไมโพสต์นี้ถึงได้คะแนนนี้ โดยอ้างอิงใจความ/คำจากโพสต์>",
  "rationale_intent": "<ถ้าเห็นเจตนา เช่น บ่น, กล่าวหา, ขายของ, รับจ้างจิตอาสา, ถามข้อมูล ฯลฯ ให้เขียนสั้น ๆ ถ้าไม่ชัดให้ใช้ '-'>",

  "emotion": "curious|worried|hopeful|proud|angry|disappointed|fun|neutral",
  "hidden_meaning": "<ความหมายแฝงหรือน้ำเสียง เช่น กังวล, ประชด, เหน็บ, ถ้าไม่มีให้ใส่ '-' >",  

  "reason": "<อธิบายสรุปรวม (ภาษาไทย) ว่าทำไมโพสต์นี้เป็น positive หรือ neutral หรือ negative>",
  "evidence": [
    "<ประโยคจากข้อความต้นฉบับที่ใช้ตัดสิน>",
    "<จะมี 1–3 รายการก็ได้ แต่ต้องมาจากข้อความจริงเท่านั้น>"
  ],

  "utcc_relevance": "high|medium|low|none",
  "nsfw": "safe|borderline|nsfw",
  "toxicity": "none|low|medium|high",

  "faculty_guess": {
    "faculty_code": "unknown",
    "faculty_name": "-",
    "major_code": "unknown",
    "major_name": "-",
    "level": "unknown",
    "reason": "-"
  }
}

ข้อสำคัญ:
- ตอบเป็น JSON เพียงอย่างเดียว ห้ามมีข้อความอื่น และห้ามใส่ ``` หรือแท็กโค้ด
- ห้ามใช้ภาษาอังกฤษนอกจากค่าที่กำหนดใน field ต่าง ๆ
- ต้องมีทั้ง sentiment_score, topic, rationale_sentiment, reason และ evidence เสมอ ห้ามปล่อยว่าง

-------------------------
ข้อความที่ต้องวิเคราะห์:
-------------------------
%s
""".formatted(req.getText());
    }
}
