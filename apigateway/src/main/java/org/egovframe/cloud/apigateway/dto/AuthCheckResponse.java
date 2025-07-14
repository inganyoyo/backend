package org.egovframe.cloud.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egovframe.cloud.apigateway.domain.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthCheckResponse {
    private boolean isAuthorized;
    private User user;
}
