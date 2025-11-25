package com.example.backend1.Analysis;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class OnnxSentimentService {

    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;

    // WangchanBERTa-finetuned-sentiment: 0=neg, 1=neu, 2=pos
    private final String[] id2label = {"negative", "neutral", "positive"};

    // ========================= Faculty Rules =========================
// แมปคำที่มักโผล่ในโพสต์ → ชื่อคณะสำหรับแสดงผล
    private static final Map<String, String> FACULTY_KEYWORDS = Map.ofEntries(
            // บริหารธุรกิจ
            Map.entry("บริหารธุรกิจ", "คณะบริหารธุรกิจ"),
            Map.entry("บริหาร",      "คณะบริหารธุรกิจ"),
            Map.entry("การตลาด",     "คณะบริหารธุรกิจ"),
            Map.entry("การจัดการ",   "คณะบริหารธุรกิจ"),
            Map.entry("การเงิน",     "คณะบริหารธุรกิจ"),

            // บัญชี
            Map.entry("บัญชี",       "คณะบัญชี"),

            // นิเทศศาสตร์
            Map.entry("นิเทศศาสตร์",   "คณะนิเทศศาสตร์"),
            Map.entry("นิเทศ",         "คณะนิเทศศาสตร์"),
            Map.entry("สื่อสารการตลาด","คณะนิเทศศาสตร์"),
            Map.entry("มีเดีย",        "คณะนิเทศศาสตร์"),

            // เศรษฐศาสตร์
            Map.entry("เศรษฐศาสตร์", "คณะเศรษฐศาสตร์"),
            Map.entry("เศรษฐ",       "คณะเศรษฐศาสตร์"),

            // โลจิสติกส์และซัพพลายเชน
            Map.entry("โลจิสติกส์",      "คณะโลจิสติกส์และซัพพลายเชน"),
            Map.entry("โลจิส",           "คณะโลจิสติกส์และซัพพลายเชน"),
            Map.entry("ซัพพลายเชน",      "คณะโลจิสติกส์และซัพพลายเชน"),

            // การท่องเที่ยวและบริการ
            Map.entry("การท่องเที่ยว",   "คณะการท่องเที่ยวและบริการ"),
            Map.entry("ท่องเที่ยว",      "คณะการท่องเที่ยวและบริการ"),
            Map.entry("การโรงแรม",      "คณะการท่องเที่ยวและบริการ"),
            Map.entry("โรงแรม",         "คณะการท่องเที่ยวและบริการ"),

            // มนุษยศาสตร์
            Map.entry("มนุษยศาสตร์",   "คณะมนุษยศาสตร์"),
            Map.entry("มนุษย์",        "คณะมนุษยศาสตร์"),

            // นิติศาสตร์
            Map.entry("นิติศาสตร์",    "คณะนิติศาสตร์"),
            Map.entry("กฎหมาย",        "คณะนิติศาสตร์"),

            // วิทยาศาสตร์และเทคโนโลยี
            Map.entry("วิทยาศาสตร์",  "คณะวิทยาศาสตร์และเทคโนโลยี"),
            Map.entry("เทคโนโลยี",    "คณะวิทยาศาสตร์และเทคโนโลยี"),
            Map.entry("ไอที",         "คณะวิทยาศาสตร์และเทคโนโลยี"),
            Map.entry("คอมพิวเตอร์",  "คณะวิทยาศาสตร์และเทคโนโลยี")
    );


    /**
     * ตรวจจับคณะจากข้อความยาว ๆ 1 ชิ้น
     * ถ้าไม่เจอจะคืน null
     */
    private String detectFaculty(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase(Locale.ROOT);

        for (var e : FACULTY_KEYWORDS.entrySet()) {
            if (lower.contains(e.getKey().toLowerCase(Locale.ROOT))) {
                return e.getValue();
            }
        }
        return null;
    }

    @PostConstruct
    public void init() {
        try {
            env = OrtEnvironment.getEnvironment();

            Path modelPath = extractResource("/wangchan/model.onnx", "wangchan_model", ".onnx");
            long modelSize = Files.size(modelPath);
            System.out.println("Loaded model temp file = " + modelPath + " size=" + modelSize + " bytes");

            session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());

            Path tokPath = extractResource("/wangchan/tokenizer.json", "wangchan_tok", ".json");
            long tokSize = Files.size(tokPath);
            System.out.println("Loaded tokenizer temp file = " + tokPath + " size=" + tokSize + " bytes");

            tokenizer = HuggingFaceTokenizer.newInstance(tokPath);

            System.out.println("ONNX model loaded OK!");

        } catch (Exception e) {
            throw new RuntimeException("Failed to init ONNX model", e);
        }
    }

    /** วิเคราะห์ข้อความ 1 ชิ้น */
    public SentimentResult analyze(String text) {
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

                    // ตรวจจับคณะจากข้อความ แล้วใส่เข้าไปในผลลัพธ์
                    String faculty = detectFaculty(text);
                    out.setFaculty(faculty);

                    return out;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("ONNX sentiment inference failed", e);
        }
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
        private String faculty;   // คณะที่ตรวจเจอ (ถ้าเจอ)

        public String getLabel() {
            return label;
        }
        public void setLabel(String label) {
            this.label = label;
        }

        public double getScore() {
            return score;
        }
        public void setScore(double score) {
            this.score = score;
        }

        public String getFaculty() {
            return faculty;
        }
        public void setFaculty(String faculty) {
            this.faculty = faculty;
        }
    }
}
