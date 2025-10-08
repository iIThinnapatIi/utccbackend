package com.example.backend1.repository;

import com.example.backend1.model.Post;
import com.example.backend1.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySourceAndExtId(Source source, String extId);
}