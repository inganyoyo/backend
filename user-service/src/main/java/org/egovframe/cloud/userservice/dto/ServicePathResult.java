package org.egovframe.cloud.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
public class ServicePathResult {
    private final String serviceName;
    private final String requestPath;

    @Override
    public String toString() {
        return "ServicePathResult{" +
                "serviceName='" + serviceName + '\'' +
                ", requestPath='" + requestPath + '\'' +
                '}';
    }
}
