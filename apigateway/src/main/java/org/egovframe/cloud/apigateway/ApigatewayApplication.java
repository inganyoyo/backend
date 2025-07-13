package org.egovframe.cloud.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * org.egovframe.cloud.apigateway.ApigatewayApplication
 * <p>
 * 게이트웨이 어플리케이션 클래스
 * 직접 URL 방식으로 마이크로서비스와 연동한다.
 *
 * @version 1.0
 * @since 2025/07/14
 */
@SpringBootApplication
public class ApigatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApigatewayApplication.class, args);
    }

}
