package com.example.backend1.mentions;

import java.util.List;
import java.util.Map;

public record SearchResponse(
        List<MentionDTO> items,
        Map<String, Long> facetSentiment,
        Map<String, Long> facetCategory,
        Map<String, Long> facetFaculty
) {}
