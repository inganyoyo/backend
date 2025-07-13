package org.egovframe.cloud.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {

  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

  /** Jackson ObjectMapper 설정 (LocalDateTime 커스텀 형식 지원) */
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    return Jackson2ObjectMapperBuilder.json()
        .modules(new JavaTimeModule())
        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .serializers(new LocalDateTimeSerializer(DATE_TIME_FORMATTER))
        .deserializers(new LocalDateTimeDeserializer(DATE_TIME_FORMATTER))
        .build();
  }
}
