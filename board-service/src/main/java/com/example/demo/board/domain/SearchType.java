package com.example.demo.board.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 검색 타입 enum
 */
@Getter
@RequiredArgsConstructor
public enum SearchType {
    ALL("all", "전체"),
    TITLE("title", "제목"),
    CONTENT("content", "내용"),
    AUTHOR("author", "작성자");
    
    private final String code;
    private final String description;
    
    /**
     * 코드로 SearchType 찾기
     */
    public static SearchType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return ALL;
        }
        
        for (SearchType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return ALL; // 기본값
    }
}
