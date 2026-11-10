package com.campusprogram.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// [2단계-① 도메인] 비교과 프로그램 1개 = programs 테이블의 한 줄
@Entity
@Table(name = "programs")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgramCategory category;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String location;

    // 프로그램이 실제로 열리는 날짜·시각
    @Column(nullable = false)
    private LocalDateTime eventAt;

    // 신청 시작 시각 (이 순간에 접속이 몰린다)
    @Column(nullable = false)
    private LocalDateTime applyStartAt;

    // 신청 마감 시각
    @Column(nullable = false)
    private LocalDateTime applyEndAt;

    // 정원
    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // JPA가 내부적으로 사용하는 기본 생성자 (직접 호출하지 않음)
    protected Program() {
    }

    // 새 프로그램을 만들 때는 이 메서드를 사용한다
    public static Program create(String title,
                                 ProgramCategory category,
                                 String description,
                                 String location,
                                 LocalDateTime eventAt,
                                 LocalDateTime applyStartAt,
                                 LocalDateTime applyEndAt,
                                 int capacity) {
        Program program = new Program();
        program.title = title;
        program.category = category;
        program.description = description;
        program.location = location;
        program.eventAt = eventAt;
        program.applyStartAt = applyStartAt;
        program.applyEndAt = applyEndAt;
        program.capacity = capacity;
        return program;
    }

    // DB에 처음 저장되기 직전에 생성 시각을 자동으로 넣는다
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 지금이 신청 시작 전인가?
    public boolean isBeforeApplyPeriod(LocalDateTime now) {
        return now.isBefore(applyStartAt);
    }

    // 지금이 신청 마감 후인가?
    public boolean isAfterApplyPeriod(LocalDateTime now) {
        return now.isAfter(applyEndAt);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public ProgramCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getEventAt() {
        return eventAt;
    }

    public LocalDateTime getApplyStartAt() {
        return applyStartAt;
    }

    public LocalDateTime getApplyEndAt() {
        return applyEndAt;
    }

    public int getCapacity() {
        return capacity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
