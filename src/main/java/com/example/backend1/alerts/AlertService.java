package com.example.backend1.alerts;

import com.example.backend1.settings.UserSettings;
import com.example.backend1.settings.UserSettingsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final EntityManager em;
    private final UserSettingsRepository settingsRepo;
    private final MailService mail;

    public AlertService(EntityManager em, UserSettingsRepository settingsRepo, MailService mail) {
        this.em = em;
        this.settingsRepo = settingsRepo;
        this.mail = mail;
    }

    /** ตรวจอัตรา negative ทั้งหมด แล้วแจ้งเตือนถ้าเกิน threshold */
    @Transactional(readOnly = true)
    public String evaluateAndNotify() {
        UserSettings s = settingsRepo.findAll().stream().findFirst().orElse(null);
        if (s == null) return "no-settings";
        if (!s.isNotificationsEnabled()) return "notifications-disabled";

        long total = countAll();
        long neg   = countNeg();
        if (total == 0) return "no-data";

        double ratio = (neg * 100.0) / total;
        if (ratio >= s.getNegativeThreshold()) {
            String subject = "UTCC Social Alert: Negative high";
            String body = "Negative ratio = " + String.format("%.2f", ratio) + "% (" + neg + "/" + total + ")";
            mail.send("marketing@utcc.local", subject, body);
            log.info("Sent alert email: {}", body);
            return "sent:" + ratio;
        }
        return "ok:" + ratio;
    }

    private long countAll() {
        Query q = em.createNativeQuery("select count(*) from tweet_analysis");
        Number n = (Number) q.getSingleResult();
        return n.longValue();
    }

    private long countNeg() {
        Query q = em.createNativeQuery(
                "select count(*) from tweet_analysis where lower(sentiment_label) in ('neg','negative')"
        );
        Number n = (Number) q.getSingleResult();
        return n.longValue();
    }
}
