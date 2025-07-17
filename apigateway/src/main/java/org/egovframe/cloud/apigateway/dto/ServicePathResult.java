package org.egovframe.cloud.apigateway.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서비스명과 요청 경로를 담는 결과 클래스
 */
@Getter
@RequiredArgsConstructor
public class ServicePathResult {
    private final String serviceName;
    private final String requestPath;
}
