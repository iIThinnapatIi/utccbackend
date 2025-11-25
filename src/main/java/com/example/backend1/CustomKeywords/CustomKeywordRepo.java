package com.example.backend1.CustomKeywords;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface CustomKeywordRepo extends JpaRepository<CustomKeyword, Long> {
}
