package com.example.backend1.service;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class TextUtils {
    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final Pattern MENTION = Pattern.compile("@\\w+");
    private static final Pattern HASHTAG = Pattern.compile("#(\\w+)");
    private static final Pattern EMOJI_CTRL = Pattern.compile("\\p{Cn}|\\p{So}");

    public String normalize(String s) {
        if (s == null) return "";
        String x = s;
        x = URL.matcher(x).replaceAll(" ");
        x = MENTION.matcher(x).replaceAll(" ");
        x = HASHTAG.matcher(x).replaceAll("$1");
        x = EMOJI_CTRL.matcher(x).replaceAll(" ");
        x = x.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        return x;
    }

    public String detectLang(String s) {
        long th = s.chars().filter(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.THAI).count();
        long en = s.chars().filter(c -> (c >= 'a' && c <= 'z')).count();
        return th >= en ? "th" : "en";
    }

    public String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return null; }
    }
}