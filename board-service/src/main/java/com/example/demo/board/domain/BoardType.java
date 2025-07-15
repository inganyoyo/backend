package com.example.demo.board.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게시판 타입 enum
 */
@Getter
@RequiredArgsConstructor
public enum BoardType {
    NOTICE("NOTICE", "공지사항"),
    FREE("FREE", "자유게시판");
    
    private final String code;
    private final String description;
    
    /**
     * 코드로 BoardType 찾기 (대소문자 무시)
     */
    public static BoardType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        
        for (BoardType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null; // 예외 대신 null 반환으로 변경
    }
}
