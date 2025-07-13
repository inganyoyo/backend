package org.egovframe.cloud.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * org.egovframe.cloud.userservice.UserServiceApplication
 * <p>
 * 유저 서비스 어플리케이션 클래스
 * Redis 기반 세션 관리 시스템
 *
 * @version 1.0
 * @since 2025/07/14
 */
@ComponentScan({
  "org.egovframe.cloud.common",
  "org.egovframe.cloud.servlet",
  "org.egovframe.cloud.userservice"
}) // org.egovframe.cloud.common package 포함하기 위해
@EntityScan({"org.egovframe.cloud.servlet.domain", "org.egovframe.cloud.userservice.domain"})
@SpringBootApplication
public class UserServiceApplication {

  /**
   * 애플리케이션의 진입점
   *
   * @param args 명령행 인수
   */
  public static void main(String[] args) {
    SpringApplication.run(UserServiceApplication.class, args);
  }
}
