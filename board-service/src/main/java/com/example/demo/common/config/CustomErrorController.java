package com.example.demo.common.config;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * Spring Boot 기본 에러 페이지를 처리하는 커스텀 에러 컨트롤러
 * GlobalExceptionHandler에서 처리되지 않은 에러들을 처리
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CustomErrorController implements ErrorController {

    private final MessageSource messageSource;

    /**
     * Spring Boot 기본 에러 페이지 처리
     * /error 경로로 들어오는 모든 에러를 처리
     */
    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request) {
        // 에러 정보 추출
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String requestMethod = request.getMethod(); // 직접 HttpServletRequest에서 가져오기
        Object exceptionType = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        // 로깅
        log.error("CustomErrorController - Status: {}, URI: {}, Method: {}, Exception: {}, Message: {}", 
                status, requestUri, requestMethod, exceptionType, message);

        // HTTP 상태 코드 확인
        int statusCode = status != null ? (Integer) status : 500;
        HttpStatus httpStatus = HttpStatus.valueOf(statusCode);

        // 에러 코드와 메시지 결정
        ErrorCode errorCode;
        String errorMessage;

        switch (statusCode) {
            case 404:
                errorCode = ErrorCode.NOT_FOUND;
                errorMessage = messageSource.getMessage(ErrorCode.NOT_FOUND.getMessage(), null,
                        LocaleContextHolder.getLocale());
                
                // 요청 정보를 메시지에 추가
                if (requestMethod != null && requestUri != null) {
                    errorMessage += " [" + requestMethod + " " + requestUri + "]";
                }
                break;
                
            case 405:
                errorCode = ErrorCode.METHOD_NOT_ALLOWED;
                errorMessage = messageSource.getMessage(ErrorCode.METHOD_NOT_ALLOWED.getMessage(), null,
                        LocaleContextHolder.getLocale());
                        
                // 요청 정보를 메시지에 추가
                if (requestMethod != null && requestUri != null) {
                    errorMessage += " [" + requestMethod + " " + requestUri + "]";
                }
                break;
                
            case 403:
                errorCode = ErrorCode.ACCESS_DENIED;
                errorMessage = messageSource.getMessage(ErrorCode.ACCESS_DENIED.getMessage(), null,
                        LocaleContextHolder.getLocale());
                break;
                
            case 401:
                errorCode = ErrorCode.UNAUTHORIZED;
                errorMessage = messageSource.getMessage(ErrorCode.UNAUTHORIZED.getMessage(), null,
                        LocaleContextHolder.getLocale());
                break;
                
            default:
                errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
                errorMessage = messageSource.getMessage(ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), null,
                        LocaleContextHolder.getLocale());
                
                // 예외 정보가 있으면 추가
                if (message != null) {
                    errorMessage += " [" + message + "]";
                }
                break;
        }

        final ApiResponse<Void> response = ApiResponse.error(errorMessage, errorCode.getCode());
        return new ResponseEntity<>(response, httpStatus);
    }

    /**
     * 에러 페이지 경로 반환 (Spring Boot 2.3+ 에서는 더 이상 필수가 아님)
     */
    public String getErrorPath() {
        return "/error";
    }
}
