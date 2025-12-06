// package: com.example.backend1.CustomKeywords

package com.example.backend1.CustomKeywords;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_keyword_history")
public class CustomKeywordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // keyword ที่ถูกแก้
    @ManyToOne
    @JoinColumn(name = "keyword_id", nullable = false)
    private CustomKeyword keyword;

    // ใครเป็นคนแก้ (ตอนนี้ถ้ายังไม่มีระบบ user จริง ๆ อาจใส่เป็น 1 ไปก่อนก็ได้)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "old_sentiment")
    private String oldSentiment;

    @Column(name = "new_sentiment")
    private String newSentiment;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    // ====== getter / setter ======
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomKeyword getKeyword() { return keyword; }
    public void setKeyword(CustomKeyword keyword) { this.keyword = keyword; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOldSentiment() { return oldSentiment; }
    public void setOldSentiment(String oldSentiment) { this.oldSentiment = oldSentiment; }

    public String getNewSentiment() { return newSentiment; }
    public void setNewSentiment(String newSentiment) { this.newSentiment = newSentiment; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
