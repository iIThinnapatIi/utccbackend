package com.example.backend1.Pantip;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PantipCommentRepository extends JpaRepository<PantipComment, Long> {
    void deleteByPost(PantipPost post);
}