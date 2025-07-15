package com.example.demo.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.BusinessMessageException;
import com.example.demo.common.code.CommonErrorCode;
import com.example.demo.common.code.SuccessCode;
import com.example.demo.common.util.SuccessResponseUtil;
import com.example.demo.common.util.MessageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 공통 응답 구조 사용 예시를 보여주는 샘플 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/sample")
@Tag(name = "샘플 API", description = "API 응답 구조 테스트용 샘플 API")
public class SampleController {

    private final SuccessResponseUtil successResponseUtil;
    private final MessageUtil messageUtil;
    
    public SampleController(SuccessResponseUtil successResponseUtil, MessageUtil messageUtil) {
        this.successResponseUtil = successResponseUtil;
        this.messageUtil = messageUtil;
    }

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
                throw BusinessException.builder(CommonErrorCode.ENTITY_NOT_FOUND).args("테스트 항목").build();
            case "message":
                throw new BusinessMessageException("사용자 정의 에러 메시지입니다.");
            case "runtime":
                throw new RuntimeException("런타임 예외 테스트");
            default:
                return successResponseUtil.success(SuccessCode.OPERATION_COMPLETED)
                        .data("정상 응답")
                        .args(messageUtil.getMessage("domain.test"), 
                              messageUtil.getMessage("action.complete"))
                        .build();
        }
    }
}
