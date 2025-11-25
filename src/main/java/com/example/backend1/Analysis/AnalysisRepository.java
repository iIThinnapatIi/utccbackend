package com.example.backend1.Analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, String> {

    /**
     * ดึงข้อมูลสรุปจำนวนโพสต์ตามคณะ แยก positive, neutral, negative
     * ใช้สำหรับแสดง Top Faculties บน Dashboard
     */
    @Query("""
        select a.faculty as faculty,
               count(a) as total
        from Analysis a
        where a.faculty is not null
          and a.faculty <> 'ไม่ระบุ'
        group by a.faculty
        order by count(a) desc
        """)
    List<Object[]> getFacultySummary();

}
