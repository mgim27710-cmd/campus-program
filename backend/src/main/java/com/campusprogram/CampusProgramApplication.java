package com.campusprogram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 프로그램 시작점: 이 main 메서드를 실행하면 스프링 서버가 8080 포트로 켜진다
@SpringBootApplication
public class CampusProgramApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusProgramApplication.class, args);
    }
}
