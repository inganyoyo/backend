package org.egovframe.cloud.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egovframe.cloud.userservice.domain.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthCheckResponse {
    private boolean isAuthorized;
    private User user;
    int status;
}
