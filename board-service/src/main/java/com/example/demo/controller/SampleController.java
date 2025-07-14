package com.example.demo.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.BusinessMessageException;
import com.example.demo.common.exception.dto.ErrorCode;
import com.example.demo.common.util.ResponseUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * 공통 응답 구조 사용 예시를 보여주는 샘플 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/sample")
public class SampleController {

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "이름은 필수입니다")
        private String name;

        @NotBlank(message = "이메일은 필수입니다")
        private String email;

        @NotNull(message = "나이는 필수입니다")
        private Integer age;
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private Integer age;

        public UserResponse(Long id, String name, String email, Integer age) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.age = age;
        }
    }

    /**
     * 성공 응답 예시 - 데이터 리스트 조회
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers() {
        log.info("사용자 목록 조회");

        List<UserResponse> users = new ArrayList<>();
        users.add(new UserResponse(1L, "김철수", "kim@test.com", 30));
        users.add(new UserResponse(2L, "이영희", "lee@test.com", 25));

        return ResponseUtil.ok("사용자 목록 조회 성공", users);
    }

    /**
     * 성공 응답 예시 - 단일 데이터 조회
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        log.info("사용자 조회: {}", id);

        if (id > 100) {
            // 비즈니스 예외 발생 (커스텀 메시지)
            throw new BusinessMessageException("사용자를 찾을 수 없습니다. ID: " + id);
        }

        UserResponse user = new UserResponse(id, "김철수", "kim@test.com", 30);
        return ResponseUtil.ok(user);
    }

    /**
     * 생성 성공 응답 예시
     */
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("사용자 생성: {}", request);

        // 중복 체크 예시
        if ("duplicate@test.com".equals(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_INPUT_INVALID);
        }

        UserResponse user = new UserResponse(3L, request.getName(), request.getEmail(), request.getAge());
        return ResponseUtil.created("사용자가 성공적으로 생성되었습니다.", user);
    }

    /**
     * 수정 성공 응답 예시
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody CreateUserRequest request) {
        log.info("사용자 수정: {} -> {}", id, request);

        UserResponse user = new UserResponse(id, request.getName(), request.getEmail(), request.getAge());
        return ResponseUtil.ok("사용자 정보가 성공적으로 수정되었습니다.", user);
    }

    /**
     * 삭제 성공 응답 예시
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        log.info("사용자 삭제: {}", id);

        if (id == 1L) {
            // 삭제 제약 조건 예외
            throw new BusinessException(ErrorCode.DB_CONSTRAINT_DELETE);
        }

        return ResponseUtil.ok("사용자가 성공적으로 삭제되었습니다.");
    }

    /**
     * 에러 테스트용 엔드포인트
     */
    @GetMapping("/error-test")
    public ResponseEntity<ApiResponse<String>> errorTest(@RequestParam String type) {
        log.info("에러 테스트: {}", type);

        switch (type) {
            case "business":
                throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
            case "message":
                throw new BusinessMessageException("사용자 정의 에러 메시지입니다.");
            case "runtime":
                throw new RuntimeException("런타임 예외 테스트");
            default:
                return ResponseUtil.ok("에러 테스트 완료", "정상 응답");
        }
    }
}
