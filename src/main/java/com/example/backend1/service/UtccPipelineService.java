package com.example.backend1.service;

import com.example.backend1.mentions.ClassificationService;
// ปรับ import ให้ตรงกับโปรเจกต์ของคุณเองด้านล่างนี้
// import com.example.backend1.model.*;
// import com.example.backend1.repository.*;

import com.example.backend1.model.*;
import com.example.backend1.repository.PostRepository;
import com.example.backend1.repository.PostTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UtccPipelineService {

    private final TextUtils textUtils;
    private final SentimentRules sentimentRules;
    private final RelevanceScorer relevanceScorer;
    private final ClassificationService classification;
    private final PostRepository postRepo;
    private final PostTagRepository tagRepo;

    // ✅ เขียน constructor เอง แทน Lombok @RequiredArgsConstructor
    public UtccPipelineService(
            TextUtils textUtils,
            SentimentRules sentimentRules,
            RelevanceScorer relevanceScorer,
            ClassificationService classification,
            PostRepository postRepo,
            PostTagRepository tagRepo
    ) {
        this.textUtils = textUtils;
        this.sentimentRules = sentimentRules;
        this.relevanceScorer = relevanceScorer;
        this.classification = classification;
        this.postRepo = postRepo;
        this.tagRepo = tagRepo;
    }

    @Transactional
    public Post processAndSave(
            Source source,
            String extId,
            String author,
            String textRaw,
            String url,
            LocalDateTime createdAt,
            boolean hasMedia
    ) {
        String norm = textUtils.normalize(textRaw);
        String lang = textUtils.detectLang(norm);
        double rel = relevanceScorer.score(norm);
        Sentiment senti = sentimentRules.infer(norm);

        Post post = postRepo.findBySourceAndExtId(source, extId).orElseGet(Post::new);
        post.setSource(source);
        post.setExtId(extId);
        post.setAuthor(author);
        post.setTextRaw(textRaw);
        post.setTextNorm(norm);
        post.setUrl(url);
        post.setCreatedAt(createdAt);
        post.setHasMedia(hasMedia);
        post.setSentiment(senti);
        post.setRelevance(BigDecimal.valueOf(rel));
        post.setLang(lang);
        post.setTextHash(textUtils.sha1(norm));

        Post saved = postRepo.save(post);

        // เขียนแท็กใหม่ทั้งหมด
        tagRepo.deleteAll(tagRepo.findByPostId(saved.getId()));

        // 1) คณะ/UTCC
        String faculty = classification.detectFacultyTagOrNull(norm);
        if (faculty != null && !faculty.isBlank()) {
            saveTags(saved, TagType.faculty, Set.of(faculty));
        }

        // 2) topic: categories ที่ไม่ใช่ "คณะ..." หรือ "UTCC"
        var cats = classification.detectCategories(norm);
        Set<String> topics = cats.stream()
                .filter(c -> !(c.startsWith("คณะ") || "UTCC".equals(c)))
                .collect(Collectors.toSet());
        if (!topics.isEmpty()) {
            saveTags(saved, TagType.topic, topics);
        }

        // 3) ถ้าต้องมี entity ภายหลัง ค่อยเพิ่มกฎ/เมธอดใน ClassificationService แล้วมาเรียกตรงนี้

        return saved;
    }

    private void saveTags(Post p, TagType type, Set<String> values) {
        if (values == null || values.isEmpty()) return;
        for (String v : values) {
            PostTag t = new PostTag();
            t.setPost(p);
            t.setTagType(type);
            t.setTagValue(v);
            tagRepo.save(t);
        }
    }
}
