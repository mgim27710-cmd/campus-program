package com.campusprogram.config;

import com.campusprogram.domain.Program;
import com.campusprogram.domain.ProgramCategory;
import com.campusprogram.repository.ProgramRepository;
import com.campusprogram.service.SeatCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// [2단계-⑦ 초기 데이터] 서버가 켜질 때 한 번 실행된다
// DB가 비어 있으면 시연용 프로그램 5개를 넣고, 모든 프로그램의 정원 정보를 Redis에 준비한다
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ProgramRepository programRepository;
    private final SeatCounter seatCounter;

    public DataInitializer(ProgramRepository programRepository, SeatCounter seatCounter) {
        this.programRepository = programRepository;
        this.seatCounter = seatCounter;
    }

    @Override
    public void run(String... args) {
        // 1단계: 프로그램이 하나도 없을 때만 시연용 데이터 추가
        if (programRepository.count() == 0) {
            insertSamplePrograms();
        }

        // 2단계: 모든 프로그램의 잔여석·신청자 정보를 Redis에 준비
        List<Program> programs = programRepository.findAll();
        for (Program program : programs) {
            seatCounter.ensureLoaded(program);
        }
        log.info("프로그램 {}개의 정원 정보를 Redis에 준비했습니다.", programs.size());
    }

    // 시각은 "서버를 처음 켠 시각" 기준으로 정해지므로 언제 실행해도 시연이 가능하다
    private void insertSamplePrograms() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        // 1번: 신청 가능 (일반 시연용)
        Program open = Program.create(
                "AI 시대의 개발자 커리어 특강",
                ProgramCategory.LECTURE,
                "현업 개발자가 말하는 신입 채용 흐름과 준비 방법",
                "공학관 301호",
                now.plusDays(10).withHour(14).withMinute(0),
                now.minusHours(1),
                now.plusDays(7),
                30
        );

        // 2번: 정원 3명 (정원 마감 시연용)
        Program small = Program.create(
                "취업 캠프: 이력서·면접 집중반",
                ProgramCategory.CAMP,
                "소수 정예로 진행하는 1:1 이력서 첨삭과 모의 면접",
                "학생회관 2층 세미나실",
                now.plusDays(14).withHour(10).withMinute(0),
                now.minusHours(1),
                now.plusDays(7),
                3
        );

        // 3번: 내일부터 신청 (신청 시작 전 시연용)
        Program upcoming = Program.create(
                "교내 SW 아이디어 공모전 설명회",
                ProgramCategory.CONTEST,
                "공모전 일정, 심사 기준, 팀 구성 방법 안내",
                "중앙도서관 강당",
                now.plusDays(20).withHour(16).withMinute(0),
                now.plusDays(1),
                now.plusDays(8),
                50
        );

        // 4번: 신청 기간 종료 (기간 종료 시연용)
        Program closed = Program.create(
                "클라우드 자격증 대비 특강",
                ProgramCategory.LECTURE,
                "AWS 자격증 시험 범위와 학습 순서 정리",
                "공학관 105호",
                now.plusDays(2).withHour(13).withMinute(0),
                now.minusDays(10),
                now.minusDays(1),
                20
        );

        // 5번: 동시성 시험용 (scripts/concurrency-test.sh 에서 사용)
        Program loadTest = Program.create(
                "[시험용] 선착순 동시 신청 테스트",
                ProgramCategory.LECTURE,
                "동시 요청 100건 중 정원 30건만 성공하는지 확인하는 프로그램",
                "온라인",
                now.plusDays(30).withHour(12).withMinute(0),
                now.minusHours(1),
                now.plusDays(30),
                30
        );

        // 넣는 순서대로 id가 1~5번이 된다
        programRepository.saveAll(List.of(open, small, upcoming, closed, loadTest));
        log.info("시연용 프로그램 5개를 추가했습니다.");
    }
}
