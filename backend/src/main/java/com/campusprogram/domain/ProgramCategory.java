package com.campusprogram.domain;

// [2단계-① 도메인] 프로그램 종류
// DB에는 영어 이름(LECTURE 등)으로 저장하고, 화면에는 label(한글)을 보여준다
public enum ProgramCategory {

    LECTURE("특강"),
    CAMP("취업 캠프"),
    CONTEST("공모전");

    private final String label;

    ProgramCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
