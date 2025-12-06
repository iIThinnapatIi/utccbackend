package com.example.backend1.Faculty;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class FacultyService {

    private final FacultyKeywordRepository keywordRepo;

    public FacultyService(FacultyKeywordRepository keywordRepo) {
        this.keywordRepo = keywordRepo;
    }

    /**
     * รับข้อความยาว ๆ 1 ชิ้น แล้วลองหา “คณะ” จาก keyword ในฐานข้อมูล
     * ถ้าเจอ → คืน Faculty
     * ถ้าไม่เจอ → คืน null
     */
    @Transactional(readOnly = true)
    public Faculty detectFaculty(String text) {
        if (text == null || text.isBlank()) return null;

        String lower = text.toLowerCase(Locale.ROOT);

        // ถ้า keyword เยอะมาก ๆ แล้วช้า ค่อยทำ cache ทีหลังได้
        List<FacultyKeyword> all = keywordRepo.findAll();

        for (FacultyKeyword fk : all) {
            if (fk.getKeyword() == null || fk.getKeyword().isBlank()) continue;

            String kw = fk.getKeyword().toLowerCase(Locale.ROOT);
            if (lower.contains(kw)) {
                return fk.getFaculty();   // เจอ keyword → คืนคณะเลย
            }
        }
        return null;
    }
}
