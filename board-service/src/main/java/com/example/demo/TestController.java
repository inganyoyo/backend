package com.example.demo;

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
    public ResponseEntity<Map<String, Object>> helloGet(
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

        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/api/v1/board/hello2", produces = "text/plain;charset=UTF-8")
    public String hello2Get() {
        log.info("GET /hello2 호출됨");
        return "Hello World (GET) 안녕하세요! hello2 한글 테스트입니다. 🚀";
    }

    @PostMapping(value = "/api/v1/board/hello", produces = "text/plain;charset=UTF-8")
    public String helloPost() {
        log.info("POST /hello 호출됨");
        return "Hello World (POST) 안녕하세요! 한글 POST 테스트입니다.";
    }
}