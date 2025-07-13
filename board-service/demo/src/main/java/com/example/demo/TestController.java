package com.example.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class TestController {

    @GetMapping(value = "/api/v1/board/hello", produces = "text/plain;charset=UTF-8")
    public String helloGet() {
        log.info("GET /hello 호출됨");
        return "Hello World (GET) 안녕하세요! 한글 테스트입니다.";
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