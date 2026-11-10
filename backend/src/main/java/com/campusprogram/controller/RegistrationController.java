package com.campusprogram.controller;

import com.campusprogram.dto.MessageResponse;
import com.campusprogram.dto.MyRegistrationResponse;
import com.campusprogram.dto.RegistrationRequest;
import com.campusprogram.dto.RegistrationResponse;
import com.campusprogram.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// [2단계-⑥ Controller] 기능 2: 참가 신청 / 기능 3: 신청 취소 / 내 신청 내역
@RestController
@RequestMapping("/api")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    // POST /api/programs/1/registrations → 1번 프로그램 신청 (성공 시 201 Created)
    @PostMapping("/programs/{programId}/registrations")
    public ResponseEntity<RegistrationResponse> apply(@PathVariable Long programId,
                                                      @Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse response = registrationService.apply(programId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // DELETE /api/programs/1/registrations → 1번 프로그램 신청 취소
    @DeleteMapping("/programs/{programId}/registrations")
    public MessageResponse cancel(@PathVariable Long programId,
                                  @Valid @RequestBody RegistrationRequest request) {
        registrationService.cancel(programId, request);
        return new MessageResponse("신청을 취소했습니다.");
    }

    // GET /api/registrations?studentId=20260001 → 내 신청 내역
    @GetMapping("/registrations")
    public List<MyRegistrationResponse> getMyRegistrations(@RequestParam String studentId) {
        return registrationService.getMyRegistrations(studentId);
    }
}
