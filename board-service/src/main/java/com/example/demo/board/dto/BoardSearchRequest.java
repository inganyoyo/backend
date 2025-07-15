package com.example.demo.board.dto;

import com.example.demo.board.domain.SearchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 게시판 검색 및 페이징 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardSearchRequest {
    
    @Min(value = 1, message = "{validation.page.min}")
    @Builder.Default
    private Integer page = 1;
    
    @Min(value = 1, message = "{validation.size.min}")
    @Builder.Default
    private Integer size = 10;
    
    @Pattern(regexp = "^(regDate|title|viewCount)$", 
             message = "{validation.sort.pattern}")
    @Builder.Default
    private String sort = "regDate";
    
    @Pattern(regexp = "^(asc|desc)$", 
             message = "{validation.order.pattern}")
    @Builder.Default
    private String order = "desc";
    
    @Size(max = 100, message = "{validation.keyword.size}")
    private String keyword;
    
    @Builder.Default
    private SearchType searchType = SearchType.ALL;
    
    /**
     * searchType을 문자열로 설정 (URL 파라미터용)
     */
    public void setSearchType(String searchType) {
        this.searchType = SearchType.fromCode(searchType);
    }
    
    /**
     * 검색 조건이 있는지 확인
     */
    public boolean hasSearchCondition() {
        return keyword != null && !keyword.trim().isEmpty();
    }
    
    /**
     * 페이징 시작 인덱스 계산 (0부터 시작)
     */
    public int getOffset() {
        return (page - 1) * size;
    }
}
