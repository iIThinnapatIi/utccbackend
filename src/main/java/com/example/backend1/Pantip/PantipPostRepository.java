package com.example.backend1.Pantip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PantipPostRepository extends JpaRepository<PantipPost, Long> {

    // เชคว่ามีโพสซ้ำไหม
    boolean existsByUrl(String url);

    // ถ้าพวกมึงจะดึงโพสซ้ำใช้อันนี้
    PantipPost findByUrl(String url);
    List<PantipPost> findTop20ByOrderByIdDesc();
}