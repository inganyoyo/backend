package org.egovframe.cloud.apigateway.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Locale;

/**
 * org.egovframe.cloud.common.util.MessageUtil
 * <p>
 * MessageSource 값을 읽을 수 있는 메소드를 제공한다.
 * <pre>

 * </pre>
 */
@Component
public class MessageUtil {

    @Resource(name = "messageSource")
    private MessageSource messageSource;

    /**
     * messageSource 에 코드값을 넘겨 메시지를 찾아 리턴한다.
     *
     * @param code
     * @return
     */
    public String getMessage(String code) {
        return this.getMessage(code, new Object[]{});
    }

    /**
     * messageSource 에 코드값과 인자를 넘겨 메시지를 찾아 리턴한다.
     *
     * @param code
     * @param args
     * @return
     */
    public String getMessage(String code, Object[] args) {
        return this.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * messageSource 에 코드값, 인자, 지역정보를 넘겨 메시지를 찾아 리턴한다.
     *
     * @param code
     * @param args
     * @param locale
     * @return
     */
    public String getMessage(String code, Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, locale);
    }

}
