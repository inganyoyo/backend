package org.egovframe.cloud.apigateway.utils;

import org.egovframe.cloud.apigateway.config.GlobalConstant;
import org.egovframe.cloud.apigateway.dto.ServicePathResult;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 경로에서 서비스명과 실제 경로를 분리하는 유틸리티 클래스
 */
public class PathExtractor {

    /**
     * 전체 경로에서 서비스명과 실제 경로를 분리한다
     *
     * @param fullPath 전체 경로 (예: /user-service/api/users/profile)
     * @return ServicePathResult 서비스명과 요청 경로를 포함한 결과 객체
     */
    public static ServicePathResult extractServiceAndPath(String fullPath) {
        if (fullPath == null || fullPath.trim().isEmpty()) {
            return new ServicePathResult("unknown-service", "/");
        }

        // 경로 정규화
        String normalizedPath = fullPath.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        // 경로를 슬래시로 분리
        String[] pathParts = normalizedPath.split("/");

        // 빈 문자열 제거
        List<String> filteredParts = new ArrayList<>();
        for (String part : pathParts) {
            if (StringUtils.hasLength(part)) {
                filteredParts.add(part);
            }
        }

        if (filteredParts.isEmpty()) {
            return new ServicePathResult("unknown-service", normalizedPath);
        }

        // 첫 번째 부분이 서비스명인지 확인
        String firstPart = filteredParts.get(0);
        if (isKnownService(firstPart)) {
            // 서비스명 발견 - 나머지 경로 재구성
            String requestPath = "/";

            if (filteredParts.size() > 1) {
                // 서비스명 이후의 경로들 재조합
                List<String> pathSegments = filteredParts.subList(1, filteredParts.size());
                requestPath = "/" + String.join("/", pathSegments);
            }

            return new ServicePathResult(firstPart, requestPath);
        } else {
            // 서비스명 없음 - 전체 경로를 그대로 사용
            return new ServicePathResult("unknown-service", normalizedPath);
        }
    }

    /**
     * 주어진 서비스명이 알려진 서비스인지 확인한다
     *
     * @param serviceName 확인할 서비스명
     * @return boolean 알려진 서비스 여부
     */
    private static boolean isKnownService(String serviceName) {
        return GlobalConstant.isKnownService(serviceName);
    }
}
