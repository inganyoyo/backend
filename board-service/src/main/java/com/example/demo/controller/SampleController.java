package com.example.demo.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.BusinessMessageException;
import com.example.demo.common.exception.dto.CommonErrorCode;
import com.example.demo.common.exception.dto.CustomErrorCode;
import com.example.demo.common.exception.dto.ErrorCode;
import com.example.demo.common.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "샘플 API", description = "API 응답 구조 테스트용 샘플 API")
public class SampleController {

    @Data
    @Schema(description = "사용자 생성 요청")
    public static class CreateUserRequest {
        @Schema(description = "사용자 이름", example = "홍길동", required = true)
        @NotBlank(message = "이름은 필수입니다")
        private String name;

        @Schema(description = "이메일 주소", example = "hong@example.com", required = true)
        @NotBlank(message = "이메일은 필수입니다")
        private String email;

        @Schema(description = "나이", example = "30", required = true)
        @NotNull(message = "나이는 필수입니다")
        private Integer age;
    }

    @Data
    @Schema(description = "사용자 응답")
    public static class UserResponse {
        @Schema(description = "사용자 ID", example = "1")
        private Long id;
        
        @Schema(description = "사용자 이름", example = "홍길동")
        private String name;
        
        @Schema(description = "이메일 주소", example = "hong@example.com")
        private String email;
        
        @Schema(description = "나이", example = "30")
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
    @Operation(
        summary = "사용자 목록 조회",
        description = "등록된 모든 사용자 목록을 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
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
    @Operation(
        summary = "사용자 상세 조회",
        description = "사용자 ID로 특정 사용자의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "조회 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "사용자를 찾을 수 없음"
        )
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @Parameter(description = "사용자 ID", example = "1") 
            @PathVariable Long id) {
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
    @Operation(
        summary = "사용자 생성",
        description = "새로운 사용자를 생성합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", 
            description = "생성 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "잘못된 입력값 또는 중복된 이메일"
        )
    })
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @org.springframework.web.bind.annotation.RequestBody
            @RequestBody(description = "사용자 생성 정보", required = true,
                        content = @Content(schema = @Schema(implementation = CreateUserRequest.class)))
            CreateUserRequest request) {
        log.info("사용자 생성: {}", request);

        // 중복 체크 예시
        if ("duplicate@test.com".equals(request.getEmail())) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_INPUT_INVALID);
        }

        UserResponse user = new UserResponse(3L, request.getName(), request.getEmail(), request.getAge());
        return ResponseUtil.created("사용자가 성공적으로 생성되었습니다.", user);
    }

    /**
     * 수정 성공 응답 예시
     */
    @Operation(
        summary = "사용자 정보 수정",
        description = "기존 사용자의 정보를 수정합니다."
    )
    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody
            @RequestBody(description = "수정할 사용자 정보")
            CreateUserRequest request) {
        log.info("사용자 수정: {} -> {}", id, request);

        UserResponse user = new UserResponse(id, request.getName(), request.getEmail(), request.getAge());
        return ResponseUtil.ok("사용자 정보가 성공적으로 수정되었습니다.", user);
    }

    /**
     * 삭제 성공 응답 예시
     */
    @Operation(
        summary = "사용자 삭제",
        description = "사용자를 삭제합니다."
    )
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "삭제할 사용자 ID", example = "1")
            @PathVariable Long id) {
        log.info("사용자 삭제: {}", id);

        if (id == 1L) {
            // 삭제 제약 조건 예외
            throw new BusinessException(CommonErrorCode.DB_CONSTRAINT_DELETE);
        }

        return ResponseUtil.ok("사용자가 성공적으로 삭제되었습니다.");
    }

    /**
     * 에러 테스트용 엔드포인트
     */
    @Operation(
        summary = "에러 테스트",
        description = "다양한 예외 상황을 테스트할 수 있는 API입니다."
    )
    @GetMapping("/error-test")
    public ResponseEntity<ApiResponse<String>> errorTest(
            @Parameter(description = "에러 타입", example = "business", 
                      schema = @Schema(allowableValues = {"business", "message", "runtime", "normal"}))
            @RequestParam String type) {
        log.info("에러 테스트: {}", type);

        switch (type) {
            case "business":
                throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND);
            case "message":
                throw new BusinessMessageException("사용자 정의 에러 메시지입니다.");
            case "runtime":
                throw new RuntimeException("런타임 예외 테스트");
            default:
                return ResponseUtil.ok("에러 테스트 완료", "정상 응답");
        }
    }
}
