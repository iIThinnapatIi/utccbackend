package com.example.backend1.service;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.*;

@Component
@RequiredArgsConstructor
public class Classifier {
    private final KeywordConfig cfg;

    public Set<String> detectFaculty(String textNorm) {
        Set<String> out = new LinkedHashSet<>();
        for (var e : cfg.facultyMap.entrySet()) {
            if (textNorm.contains(e.getKey().toLowerCase())) out.add(e.getValue());
        }
        return out;
    }

    public Set<String> detectTopics(String textNorm) {
        Set<String> out = new LinkedHashSet<>();
        for (var e : cfg.topicMap.entrySet()) {
            if (textNorm.contains(e.getKey().toLowerCase())) out.add(e.getValue());
        }
        return out;
    }

    public Set<String> detectEntities(String textNorm) {
        Set<String> out = new LinkedHashSet<>();
        if (textNorm.contains("utcc") || textNorm.contains("มหาวิทยาลัยหอการค้าไทย") || textNorm.contains("หอการค้า"))
            out.add("university");
        if (!detectFaculty(textNorm).isEmpty()) out.add("faculty");
        if (textNorm.contains("วิภาวดี") || textNorm.contains("ดินแดง")) out.add("campus");
        if (textNorm.contains("open house") || textNorm.contains("รับปริญญา") || textNorm.contains("กิจกรรม")) out.add("event");
        return out;
    }
}