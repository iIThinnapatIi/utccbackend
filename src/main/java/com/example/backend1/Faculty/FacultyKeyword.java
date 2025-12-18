package com.example.backend1.Faculty;

import jakarta.persistence.*;

@Entity
@Table(name = "faculty_keyword")
public class FacultyKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // คำที่ใช้จับ เช่น "การตลาด", "นิเทศ", "บริหาร"
    @Column(nullable = false)
    private String keyword;

    // FK → faculty.id
    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    //getter / setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }
}
