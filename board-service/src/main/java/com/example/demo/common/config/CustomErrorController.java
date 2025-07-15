package com.example.demo.common.config;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.code.CommonErrorCode;
import com.example.demo.common.code.ErrorCode;
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
                errorCode = CommonErrorCode.NOT_FOUND;
                errorMessage = messageSource.getMessage(CommonErrorCode.NOT_FOUND.getMessageKey(), null,
                        LocaleContextHolder.getLocale());
                
                // 요청 정보를 메시지에 추가
                if (requestMethod != null && requestUri != null) {
                    errorMessage += " [" + requestMethod + " " + requestUri + "]";
                }
                break;
                
            case 405:
                errorCode = CommonErrorCode.METHOD_NOT_ALLOWED;
                errorMessage = messageSource.getMessage(CommonErrorCode.METHOD_NOT_ALLOWED.getMessageKey(), null,
                        LocaleContextHolder.getLocale());
                        
                // 요청 정보를 메시지에 추가
                if (requestMethod != null && requestUri != null) {
                    errorMessage += " [" + requestMethod + " " + requestUri + "]";
                }
                break;
                
            case 403:
                errorCode = CommonErrorCode.ACCESS_DENIED;
                errorMessage = messageSource.getMessage(CommonErrorCode.ACCESS_DENIED.getMessageKey(), null,
                        LocaleContextHolder.getLocale());
                break;
                
            case 401:
                errorCode = CommonErrorCode.UNAUTHORIZED;
                errorMessage = messageSource.getMessage(CommonErrorCode.UNAUTHORIZED.getMessageKey(), null,
                        LocaleContextHolder.getLocale());
                break;
                
            default:
                errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
                errorMessage = messageSource.getMessage(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessageKey(), null,
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
