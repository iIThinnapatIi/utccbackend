package com.example.backend1.Faculty;

import jakarta.persistence.*;

@Entity
@Table(name = "faculty")
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ชื่อคณะเต็ม ๆ เช่น "คณะบริหารธุรกิจ"
    @Column(nullable = false)
    private String name;

    // ชื่อสั้น / ชื่อที่ใช้แสดงในกราฟ เช่น "บริหารธุรกิจ"
    private String shortName;

    // ====== getter / setter ======
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }
}
