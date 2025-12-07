package com.example.backend1.Analysis;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OnnxTensor;

import com.example.backend1.Faculty.Faculty;
import com.example.backend1.Faculty.FacultyService;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@Service
public class OnnxSentimentService {

    private final FacultyService facultyService;   // ⭐ ดึงคณะจาก DB

    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;

    // ใช้เช็คว่าโหลดโมเดลสำเร็จไหม
    private boolean modelLoaded = false;

    // WangchanBERTa-finetuned-sentiment: 0=neg, 1=neu, 2=pos
    private final String[] id2label = {"negative", "neutral", "positive"};

    public OnnxSentimentService(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @PostConstruct
    public void init() {
        try {
            env = OrtEnvironment.getEnvironment();

            // 👇❗ แก้ path ให้ตรงกับโครงไฟล์จริง
            Path modelPath = extractResource(
                    "/wangchan/model_out/model.onnx",   // เดิม /wangchan/model.onnx
                    "wangchan_model",
                    ".onnx"
            );
            long modelSize = Files.size(modelPath);
            System.out.println("Loaded model temp file = " + modelPath + " size=" + modelSize + " bytes");

            session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());

            Path tokPath = extractResource(
                    "/wangchan/tokenizer.json",
                    "wangchan_tok",
                    ".json"
            );
            long tokSize = Files.size(tokPath);
            System.out.println("Loaded tokenizer temp file = " + tokPath + " size=" + tokSize + " bytes");

            tokenizer = HuggingFaceTokenizer.newInstance(tokPath);

            modelLoaded = true;
            System.out.println("✅ ONNX model loaded OK!");

        } catch (Exception e) {
            // ❗ สำคัญ: ห้ามทำให้แอปล้ม ให้แค่เตือนแล้วใช้ fallback แทน
            modelLoaded = false;
            session = null;
            tokenizer = null;
            env = null;

            System.err.println(
                    "[WARN] ONNX model was NOT loaded. " +
                            "Sentiment will use fallback (neutral). Reason: " + e.getMessage()
            );
        }
    }

    /** วิเคราะห์ข้อความ 1 ชิ้น */
    public SentimentResult analyze(String text) {
        // ถ้าโมเดลไม่พร้อม → ใช้ fallback ทันที (neutral + faculty จาก FacultyService)
        if (!modelLoaded || env == null || session == null || tokenizer == null) {
            System.err.println("[INFO] analyze() using fallback sentiment (model not loaded)");
            return fallbackResult(text, "[ONNX] Model not loaded, using fallback");
        }

        try {
            // 1) tokenize ด้วย HuggingFace tokenizer
            Encoding enc = tokenizer.encode(text);

            long[] ids = enc.getIds();
            long[] mask = enc.getAttentionMask();

            long[][] ids2d = new long[1][ids.length];
            long[][] mask2d = new long[1][mask.length];
            ids2d[0] = ids;
            mask2d[0] = mask;

            try (OnnxTensor inputIds = OnnxTensor.createTensor(env, ids2d);
                 OnnxTensor attentionMask = OnnxTensor.createTensor(env, mask2d)) {

                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("input_ids", inputIds);
                inputs.put("attention_mask", attentionMask);

                try (OrtSession.Result result = session.run(inputs)) {
                    float[][] logits = (float[][]) result.get(0).getValue();
                    float[] probs = softmax(logits[0]);

                    int best = 0;
                    for (int i = 1; i < probs.length; i++) {
                        if (probs[i] > probs[best]) best = i;
                    }

                    SentimentResult out = new SentimentResult();
                    out.setLabel(id2label[best]);
                    out.setScore(probs[best]);

                    // ⭐ ใช้ FacultyService (ดึงจากฐานข้อมูล)
                    Faculty f = facultyService.detectFaculty(text);
                    if (f != null) {
                        out.setFacultyName(f.getName());
                        out.setFacultyId(f.getId());
                    } else {
                        out.setFacultyName(null);
                        out.setFacultyId(null);
                    }

                    return out;
                }
            }

        } catch (Exception e) {
            // ถ้าวิเคราะห์ไม่สำเร็จ → ไม่ให้แอปล้ม ใช้ fallback แทน
            System.err.println("[WARN] ONNX sentiment inference failed, using fallback. Reason: " + e.getMessage());
            return fallbackResult(text, "ONNX inference failed");
        }
    }

    /** สร้างผลลัพธ์แบบ fallback (neutral + faculty ถ้ามี) */
    private SentimentResult fallbackResult(String text, String reason) {
        SentimentResult out = new SentimentResult();
        out.setLabel("neutral");
        out.setScore(0.0);

        try {
            Faculty f = facultyService.detectFaculty(text);
            if (f != null) {
                out.setFacultyName(f.getName());
                out.setFacultyId(f.getId());
            }
        } catch (Exception e) {
            System.err.println("[WARN] Faculty detection failed in fallback: " + e.getMessage());
        }

        return out;
    }

    /** softmax ธรรมดา */
    private float[] softmax(float[] x) {
        double max = Double.NEGATIVE_INFINITY;
        for (float v : x) {
            if (v > max) max = v;
        }
        double sum = 0.0;
        double[] exps = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            exps[i] = Math.exp(x[i] - max);
            sum += exps[i];
        }
        float[] probs = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            probs[i] = (float) (exps[i] / sum);
        }
        return probs;
    }

    /** ดึงไฟล์จาก classpath ไปวางใน temp แล้วคืน Path */
    private Path extractResource(String resourcePath, String prefix, String suffix) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Path temp = Files.createTempFile(prefix, suffix);
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
    }

    // ใช้เป็น DTO ผลลัพธ์
    public static class SentimentResult {
        private String label;
        private double score;

        // ⭐ อัปเดต: เก็บทั้ง id และชื่อของคณะ
        private Long facultyId;
        private String facultyName;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }

        public Long getFacultyId() { return facultyId; }
        public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }

        public String getFacultyName() { return facultyName; }
        public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

        // ✅ backward compatible: ถ้าที่อื่นยังเรียก getFaculty()/setFaculty()
        // ให้ผูกกับ facultyName แทน
        public String getFaculty() { return facultyName; }
        public void setFaculty(String faculty) { this.facultyName = faculty; }
    }
}
