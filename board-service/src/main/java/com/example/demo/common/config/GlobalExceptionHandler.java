package com.example.demo.common.config;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.BusinessMessageException;
import com.example.demo.common.exception.dto.ErrorCode;
import javassist.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 클래스
 * 모든 컨트롤러에 적용되는 예외 처리를 담당
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  protected final MessageSource messageSource;

  /**
   * javax.validation.Valid or @Validated 으로 binding error 발생시 발생한다. HttpMessageConverter 에서 등록한
   * HttpMessageConverter binding 못할경우 발생 주로 @RequestBody, @RequestPart 어노테이션에서 발생
   *
   * @param e
   * @return ResponseEntity<ApiResponse<Void>>
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    log.error("handleMethodArgumentNotValidException", e);
    
    String message = messageSource.getMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage(), null, 
        LocaleContextHolder.getLocale());
    
    // 필드 에러 정보를 메시지에 추가
    String fieldErrors = e.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));
    
    if (!fieldErrors.isEmpty()) {
      message += " [" + fieldErrors + "]";
    }
    
    final ApiResponse<Void> response = ApiResponse.error(message, ErrorCode.INVALID_INPUT_VALUE.getCode());
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * 바인딩 객체 @ModelAttribute 으로 binding error 발생시 BindException 발생한다. ref
   * https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-modelattrib-method-args
   *
   * @param e
   * @return ResponseEntity<ApiResponse<Void>>
   */
  @ExceptionHandler(BindException.class)
  protected ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
    log.error("handleBindException", e);
    
    String message = messageSource.getMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage(), null,
        LocaleContextHolder.getLocale());
    
    // 필드 에러 정보를 메시지에 추가
    String fieldErrors = e.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));
    
    if (!fieldErrors.isEmpty()) {
      message += " [" + fieldErrors + "]";
    }
    
    final ApiResponse<Void> response = ApiResponse.error(message, ErrorCode.INVALID_INPUT_VALUE.getCode());
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * 요청은 잘 만들어졌지만, 문법 오류로 인하여 따를 수 없습니다
   *
   * @param e
   * @return ResponseEntity<ApiResponse<Void>>
   */
  @ExceptionHandler(HttpClientErrorException.UnprocessableEntity.class)
  protected ResponseEntity<ApiResponse<Void>> handleUnprocessableEntityException(
      HttpClientErrorException.UnprocessableEntity e) {
    log.error("handleUnprocessableEntityException", e);
    
    String message = messageSource.getMessage(ErrorCode.UNPROCESSABLE_ENTITY.getMessage(), null,
        LocaleContextHolder.getLocale());
    
    final ApiResponse<Void> response = ApiResponse.error(message, ErrorCode.UNPROCESSABLE_ENTITY.getCode());
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * 지원하지 않은 HTTP method 호출 할 경우 발생
   *
   * @param e
   * @return ResponseEntity<ApiResponse<Void>>
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException e) {
    log.error("handleHttpRequestMethodNotSupportedException", e);
    
    String message = messageSource.getMessage(ErrorCode.METHOD_NOT_ALLOWED.getMessage(), null,
        LocaleContextHolder.getLocale());
    
    final ApiResponse<Void> response = ApiResponse.error(message, ErrorCode.METHOD_NOT_ALLOWED.getCode());
    return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * enum type 일치하지 않아 binding 못할 경우 발생 주로 @RequestParam enum으로 binding 못했을 경우 발생
   *
   * @param e
   * @return ResponseEntity<ApiResponse<Void>>
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException e) {
    log.error("handleMethodArgumentTypeMismatchException", e);
    
    String message = messageSource.getMessage(ErrorCode.INVALID_TYPE_VALUE.getMessage(), null,
        LocaleContextHolder.getLocale());
    
    // 타입 미스매치 정보를 메시지에 추가
    String value = e.getValue() != null ? e.getValue().toString() : "";
    message += " [" + e.getName() + ": " + value + "]";
    
    final ApiResponse<Void> response = ApiResponse.error(message, ErrorCode.INVALID_TYPE_VALUE.getCode());
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * 요청한 페이지가 존재하지 않는 경우
   *
   * @param e
   * @return ResponseEntity<ApiResponse<Void>>
   */
  @ExceptionHandler(NotFoundException.class)
  protected ResponseEntity<ApiResponse<Void>> handleNotFoundException(NotFoundException e) {
    log.error("handleNotFoundException", e);
    
    String message = messageSource.getMessage(ErrorCode.NOT_FOUND.getMessage(), null,
        LocaleContextHolder.getLocale());
    
    final ApiResponse<Void> response = ApiResponse.error(message, ErrorCode.NOT_FOUND.getCode());
    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  /**
   * Authentication 객체가 필요한 권한을 보유하지 않은 경우 발생
   *
   * @param e
   * @return ResponseEntity<ApiResponse<Void>>
   */
  @ExceptionHandler(AccessDeniedException.class)
  protected ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
    log.error("handleAccessDeniedException", e);
    
    String message = messageSource.getMessage(ErrorCode.ACCESS_DENIED.getMessage(), null,
        LocaleContextHolder.getLocale());
    
    final ApiResponse<Void> response = ApiResponse.error(message, ErrorCode.ACCESS_DENIED.getCode());
    return new ResponseEntity<>(response, HttpStatus.valueOf(ErrorCode.ACCESS_DENIED.getStatus()));
  }

  /**
   * 사용자 인증되지 않은 경우 발생
   *
   * @param e
   * @return ResponseEntity<ApiResponse<Void>>
   */
  @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
  protected ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(
      HttpClientErrorException.Unauthorized e) {
    log.error("handleUnauthorizedException", e);
    
    String message = messageSource.getMessage(ErrorCode.UNAUTHORIZED.getMessage(), null,
        LocaleContextHolder.getLocale());
    
    final ApiResponse<Void> response = ApiResponse.error(message, ErrorCode.UNAUTHORIZED.getCode());
    return new ResponseEntity<>(response, HttpStatus.valueOf(ErrorCode.UNAUTHORIZED.getStatus()));
  }

  /**
   * 사용자에게 표시할 다양한 메시지를 직접 정의하여 처리하는 Business RuntimeException Handler 개발자가 만들어 던지는 런타임 오류를 처리
   *
   * @param e
   * @return ResponseEntity<ApiResponse<Void>>
   */
  @ExceptionHandler(BusinessMessageException.class)
  protected ResponseEntity<ApiResponse<Void>> handleBusinessMessageException(
      BusinessMessageException e) {
    log.error("handleBusinessMessageException", e);
    final ErrorCode errorCode = e.getErrorCode();
    final String customMessage = e.getCustomMessage();
    final ApiResponse<Void> response = ApiResponse.error(customMessage, errorCode.getCode());
    return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
  }

  /**
   * 개발자 정의 ErrorCode 를 처리하는 Business RuntimeException Handler 개발자가 만들어 던지는 런타임 오류를 처리
   *
   * @param e
   * @return ResponseEntity<ApiResponse<Void>>
   */
  @ExceptionHandler(BusinessException.class)
  protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
    log.error("handleBusinessException", e);
    final ErrorCode errorCode = e.getErrorCode();
    
    String message = messageSource.getMessage(errorCode.getMessage(), null,
        LocaleContextHolder.getLocale());
    
    final ApiResponse<Void> response = ApiResponse.error(message, errorCode.getCode());
    return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
  }

  /**
   * default exception
   *
   * @param e
   * @return ResponseEntity<ApiResponse<Void>>
   */
  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    log.error("handleException", e);
    
    String message = messageSource.getMessage(ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), null,
        LocaleContextHolder.getLocale());
    
    final ApiResponse<Void> response = ApiResponse.error(message, ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
