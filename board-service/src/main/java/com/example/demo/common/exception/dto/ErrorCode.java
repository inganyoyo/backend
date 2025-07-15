package com.example.demo.common.exception.dto;

public interface ErrorCode {
    int getStatus();
    String getCode();
    String getMessage();
}