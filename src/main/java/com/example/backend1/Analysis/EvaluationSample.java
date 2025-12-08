package com.example.backend1.Analysis;

import jakarta.persistence.*;

@Entity
@Table(name = "evaluation_samples")
public class EvaluationSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String text;


    @Column(name = "true_label", nullable = false)
    private String trueLabel;

    // getter / setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getTrueLabel() { return trueLabel; }
    public void setTrueLabel(String trueLabel) { this.trueLabel = trueLabel; }
}
