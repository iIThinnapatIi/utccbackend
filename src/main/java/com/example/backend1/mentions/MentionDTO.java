package com.example.backend1.mentions;

import java.time.Instant;
import java.util.List;

public record MentionDTO(
        String id,
        String text,
        String authorId,
        Instant createdAt,
        List<String> categories,  // เช่น ["UTCC","คณะบัญชี","18+","แง่ลบ"]
        String sentiment          // "positive" | "negative" | "neutral"
) {}
