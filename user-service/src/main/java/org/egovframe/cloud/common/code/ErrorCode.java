package org.egovframe.cloud.common.code;

public interface ErrorCode {
    int getStatus();
    String getCode();
    String getMessageKey();
}
