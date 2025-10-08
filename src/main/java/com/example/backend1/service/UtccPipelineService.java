package com.example.backend1.service;

import com.example.backend1.model.Post;
import com.example.backend1.model.PostTag;
import com.example.backend1.model.Source;
import com.example.backend1.model.Sentiment;
import com.example.backend1.model.TagType;
import com.example.backend1.repository.PostRepository;
import com.example.backend1.repository.PostTagRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UtccPipelineService {
    private final TextUtils textUtils;
    private final SentimentRules sentimentRules;
    private final RelevanceScorer relevanceScorer;
    private final Classifier classifier;
    private final PostRepository postRepo;
    private final PostTagRepository tagRepo;

    @Transactional
    public Post processAndSave(Source source, String extId, String author, String textRaw,
                               String url, LocalDateTime createdAt, boolean hasMedia) {
        String norm = textUtils.normalize(textRaw);
        String lang = textUtils.detectLang(norm);
        double rel = relevanceScorer.score(norm);
        Sentiment senti = sentimentRules.infer(norm);

        // ถ้าคะแนนเกี่ยวข้องต่ำมาก สามารถเลือก drop ได้
        // if (rel < 0.2) return null;

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

        // rewrite tags อย่างง่าย
        tagRepo.deleteAll(tagRepo.findByPostId(saved.getId()));
        saveTags(saved, TagType.faculty, classifier.detectFaculty(norm));
        saveTags(saved, TagType.topic, classifier.detectTopics(norm));
        saveTags(saved, TagType.entity, classifier.detectEntities(norm));
        return saved;
    }

    private void saveTags(Post p, TagType type, Set<String> values) {
        for (String v : values) {
            PostTag t = new PostTag();
            t.setPost(p);
            t.setTagType(type);
            t.setTagValue(v);
            tagRepo.save(t);
        }
    }
}