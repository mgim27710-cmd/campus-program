package com.campusprogram.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

// [2단계-① 도메인] 신청 기록 1건 = registrations 테이블의 한 줄
// (program_id, student_id) 조합을 유일하게 만들어서 DB 차원에서도 중복 신청을 막는다
@Entity
@Table(
        name = "registrations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_program_student",
                columnNames = {"program_id", "student_id"}
        )
)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 프로그램에 신청했는지 (여러 신청 기록 → 하나의 프로그램)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    // 학번
    @Column(name = "student_id", nullable = false, length = 20)
    private String studentId;

    // 이름
    @Column(nullable = false, length = 20)
    private String studentName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Registration() {
    }

    public static Registration create(Program program, String studentId, String studentName) {
        Registration registration = new Registration();
        registration.program = program;
        registration.studentId = studentId;
        registration.studentName = studentName;
        return registration;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 취소할 때 입력한 이름이 신청 때 이름과 같은지 확인
    public boolean hasSameName(String name) {
        return this.studentName.equals(name);
    }

    public Long getId() {
        return id;
    }

    public Program getProgram() {
        return program;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
