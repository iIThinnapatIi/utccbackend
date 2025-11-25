package com.example.backend1.Analysis;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity นี้แมปกับตาราง social_analysis ใน MySQL
 * ใช้เก็บผลการวิเคราะห์แต่ละโพสต์/ทวีต
 */
@Entity
@Table(name = "social_analysis")
@Data // จาก Lombok: auto-gen getter/setter, toString(), equals(), hashCode()
public class Analysis {

    /**
     * ใช้เก็บไอดีของโพสต์/ทวีต
     * แมปกับคอลัมน์ id (PRIMARY KEY) ในตาราง social_analysis
     */
    @Id
    private String id;

    /**
     * เนื้อหาข้อความจริง ๆ ที่ดึงมาจากแพลตฟอร์ม (โพสต์ยาว)
     * - @Lob บอก Hibernate ให้เก็บแบบ Large Object (TEXT/LONGTEXT)
     * - columnDefinition = "TEXT" สั่งให้ MySQL ใช้ type TEXT (เก็บได้ ~65k ตัวอักษร)
     *   ถ้าอยากให้ยาวมากกว่านี้ เปลี่ยนเป็น "LONGTEXT" ได้
     *
     * ตรงนี้คือจุดที่เราแก้ เพื่อไม่ให้เกิด error
     * "Data too long for column 'text'" อีกแล้ว
     */
    @Lob
    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    /**
     * เวลาที่โพสต์ถูกสร้าง (string ที่ parse มาแล้ว)
     * แมปกับคอลัมน์ created_at
     */
    @Column(name = "created_at")
    private String createdAt;

    /**
     * แพลตฟอร์มที่มาของโพสต์ เช่น twitter, pantip
     * แมปกับคอลัมน์ platform
     */
    private String platform;

    /**
     * คณะที่โมเดล/กฎของเราตีความ เช่น การตลาด, นิเทศฯ, มนุษย์ศาสตร์ ฯลฯ
     * แมปกับคอลัมน์ faculty
     */
    private String faculty;

    /**
     * label อารมณ์รวมของโพสต์ เช่น positive / neutral / negative
     * แมปกับคอลัมน์ sentiment
     */
    private String sentiment;



    /**
     * label สุดท้ายที่เราใช้แสดงบน dashboard
     * เช่น อาจรวม sentiment + faculty หรือ rule อื่น ๆ
     * แมปกับคอลัมน์ final_label
     */
    @Column(name = "final_label")
    private String finalLabel;
}