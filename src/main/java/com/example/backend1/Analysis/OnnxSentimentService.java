package com.example.backend1.Analysis;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.SessionOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Service สำหรับเรียกใช้โมเดล ONNX (WangchanBERTa-finetuned-sentiment)
 *
 * - โหลด model.onnx + tokenizer.json จาก classpath: src/main/resources/wangchan/...
 * - วิเคราะห์ Sentiment (0=neg, 1=neu, 2=pos)
 * - ตอนนี้ faculty ยังให้ค่า "ไม่ระบุ" ไว้ก่อน (ถ้าจะต่อกับ Faculty จริงค่อยมาเติมทีหลัง)
 */
@Service
public class OnnxSentimentService {

    // ------------------------------------------------------------
    // ฟิลด์หลักของ ONNX
    // ------------------------------------------------------------
    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;

    // ใช้เช็คว่าโหลดโมเดลสำเร็จไหม
    private boolean modelLoaded = false;

    // WangchanBERTa-finetuned-sentiment: 0=neg, 1=neu, 2=pos
    private static final String[] LABELS = new String[]{"negative", "neutral", "positive"};

    // ============================================================
    // helper: โครงผลลัพธ์ที่ส่งกลับให้ฝั่งอื่นใช้
    // ============================================================
    public static class SentimentResult {
        private String label;      // label สุดท้าย (neg/neu/pos)
        private float score;       // prob ที่โมเดลมั่นใจ
        private String faculty;    // ชื่อคณะ (ตอนนี้ใช้ "ไม่ระบุ")
        private Long facultyId;    // id คณะ (เผื่อใช้ทีหลัง)
        private String facultyName;

        public String getLabel() {
            return label;
        }

        public float getScore() {
            return score;
        }

        public String getFaculty() {
            return faculty;
        }

        public Long getFacultyId() {
            return facultyId;
        }

        public String getFacultyName() {
            return facultyName;
        }

        public SentimentResult setLabel(String label) {
            this.label = label;
            return this;
        }

        public SentimentResult setScore(float score) {
            this.score = score;
            return this;
        }

        public SentimentResult setFaculty(String faculty) {
            this.faculty = faculty;
            return this;
        }

        public SentimentResult setFacultyId(Long facultyId) {
            this.facultyId = facultyId;
            return this;
        }

        public SentimentResult setFacultyName(String facultyName) {
            this.facultyName = facultyName;
            return this;
        }
    }

    // ============================================================
    // 1) helper โหลด resource จาก classpath (ไม่ต้องลบโฟลเดอร์)
    // ============================================================
    /**
     * ใช้โหลดไฟล์จาก classpath เช่น "wangchan/model.onnx"
     * path ที่ส่งเข้ามา **ห้ามมี /** นำหน้า
     */
    private InputStream loadResource(String path) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IOException("Resource not found on classpath: " + path);
        }
        return in;
    }

    /**
     * helper: copy ไฟล์จาก resources ไปเป็น temp file แล้วคืน Path กลับมา
     * ใช้ตอนสร้าง Session / Tokenizer ที่ต้องการ path จริง ๆ
     */
    private Path copyResourceToTemp(String resourcePath, String prefix, String suffix) throws IOException {
        try (InputStream in = loadResource(resourcePath)) {
            Path tmp = Files.createTempFile(prefix, suffix);
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            return tmp;
        }
    }

    // ============================================================
    // 2) init: โหลด ONNX model + tokenizer ตอนสตาร์ทแอป
    // ============================================================
    @PostConstruct
    public void init() {
        try {
            env = OrtEnvironment.getEnvironment("utcc-wangchan");

            // ---------- 2.1 โหลด model.onnx จาก resources/wangchan ----------
            Path modelPath = copyResourceToTemp(
                    "wangchan/model.onnx",
                    "wangchan-model",
                    ".onnx"
            );
            SessionOptions opts = new SessionOptions();
            session = env.createSession(modelPath.toString(), opts);

            // ---------- 2.2 โหลด tokenizer.json จาก resources/wangchan ----------
            Path tokPath = copyResourceToTemp(
                    "wangchan/tokenizer.json",
                    "wangchan-tokenizer",
                    ".json"
            );
            tokenizer = HuggingFaceTokenizer.builder()
                    .optTokenizerPath(tokPath)   // ✅ ใช้ Path (ไม่ใช่ InputStream)
                    .build();

            modelLoaded = true;
            System.out.println("[INFO] ONNX model loaded successfully ✅");
        } catch (Exception ex) {
            modelLoaded = false;
            System.err.println(
                    "[WARN] ONNX model was NOT loaded. Sentiment will use fallback (neutral). Reason: "
                            + ex.getMessage()
            );
        }
    }

    // ============================================================
    // 3) public method ให้คนอื่นเรียกวิเคราะห์
    // ============================================================
    /**
     * วิเคราะห์ข้อความเดียวแล้วคืนผลลัพธ์เป็น SentimentResult
     * ถ้าโมเดลโหลดไม่สำเร็จ -> คืน neutral กลับไป (กันระบบล้ม)
     */
    public SentimentResult analyze(String text) {
        // กัน null / ว่าง
        if (text == null || text.isBlank() || !modelLoaded || session == null || tokenizer == null) {
            // fallback แบบปลอดภัย
            return new SentimentResult()
                    .setLabel("neutral")
                    .setScore(0.0f)
                    .setFaculty("ไม่ระบุ");
        }

        try {
            // ---------- 3.1 tokenize ด้วย HuggingFaceTokenizer ----------
            Encoding encoding = tokenizer.encode(text);

            // DJL Encoding ให้เป็น long[] อยู่แล้ว -> ไม่ต้อง mapToLong
            long[] inputIds = encoding.getIds();
            long[] attentionMask = encoding.getAttentionMask();

            long[][] ids2d = new long[][]{ inputIds };
            long[][] mask2d = new long[][]{ attentionMask };

            // ---------- 3.2 สร้างเทนเซอร์ให้ ONNX ----------
            Map<String, OnnxTensor> inputs = new HashMap<>();
            OnnxTensor idsTensor = OnnxTensor.createTensor(env, ids2d);
            OnnxTensor maskTensor = OnnxTensor.createTensor(env, mask2d);

            inputs.put("input_ids", idsTensor);
            inputs.put("attention_mask", maskTensor);

            // ---------- 3.3 เรียก session.run ----------
            float[] probs;
            try (var result = session.run(inputs)) {
                // สมมติ output แรกคือ logits: [1, 3]
                float[][] logits = (float[][]) result.get(0).getValue();
                probs = softmax(logits[0]);
            } finally {
                idsTensor.close();
                maskTensor.close();
            }

            // ---------- 3.4 หา label ที่โอกาสสูงสุด ----------
            int maxIdx = 0;
            float maxVal = probs[0];
            for (int i = 1; i < probs.length; i++) {
                if (probs[i] > maxVal) {
                    maxVal = probs[i];
                    maxIdx = i;
                }
            }

            String label = (maxIdx >= 0 && maxIdx < LABELS.length)
                    ? LABELS[maxIdx]
                    : "neutral";

            // ถ้าจะ map faculty จากโมเดล ต้องเติม logic ตรงนี้ทีหลัง
            return new SentimentResult()
                    .setLabel(label)
                    .setScore(maxVal)
                    .setFaculty("ไม่ระบุ");   // ตอนนี้ fix เป็น "ไม่ระบุ" ไว้ก่อน

        } catch (OrtException ex) {   // ✅ เอา IOException ออก
            // ถ้ามี error ระหว่างรันโมเดล -> ไม่ให้ระบบล้ม, คืน neutral กลับไป
            System.err.println("[ERROR] ONNX analyze failed: " + ex.getMessage());
            return new SentimentResult()
                    .setLabel("neutral")
                    .setScore(0.0f)
                    .setFaculty("ไม่ระบุ");
        }
    }


    // ============================================================
    // 4) helper: softmax จาก logits -> prob
    // ============================================================
    private float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) {
            if (v > max) max = v;
        }

        double sum = 0.0;
        double[] exps = new double[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exps[i] = Math.exp(logits[i] - max);
            sum += exps[i];
        }

        float[] probs = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            probs[i] = (float) (exps[i] / sum);
        }
        return probs;
    }
}
