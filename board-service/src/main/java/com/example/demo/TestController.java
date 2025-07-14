package com.example.demo;

import com.example.demo.common.domain.UserContext;
import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.util.ResponseUtil;
import com.example.demo.common.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
public class TestController {

    @GetMapping(value = "/api/board/hello", produces = "application/json")
    public ResponseEntity<ApiResponse<Map<String, Object>>> helloGet(
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId
    ) {
        UserContext user = UserContextHolder.getContext();

        log.info("userId = {}, sessionId = {}", userId, sessionId);
        log.info("user = {} ",user);
        log.info("GET /hello 호출됨");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello World (GET) 안녕하세요! 한글 테스트입니다.");
        response.put("userId", userId);
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        response.put("userContext", user);

        return ResponseUtil.ok("요청이 성공적으로 처리되었습니다.", response);
    }

    @GetMapping(value = "/api/v1/board/hello2")
    public ResponseEntity<ApiResponse<String>> hello2Get() {
        log.info("GET /hello2 호출됨");
        String message = "Hello World (GET) 안녕하세요! hello2 한글 테스트입니다. 🚀";
        
        return ResponseUtil.ok("Hello2 요청 성공", message);
    }

    @PostMapping(value = "/api/v1/board/hello")
    public ResponseEntity<ApiResponse<String>> helloPost() {
        log.info("POST /hello 호출됨");
        String message = "Hello World (POST) 안녕하세요! 한글 POST 테스트입니다.";
        
        return ResponseUtil.ok("POST 요청 성공", message);
    }
}