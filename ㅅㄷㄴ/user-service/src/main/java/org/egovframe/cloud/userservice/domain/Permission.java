package org.egovframe.cloud.userservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 권한 정보 도메인 모델
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    private String service;
    private String method;
    private String path;
    private String description;
}
