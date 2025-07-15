package com.example.demo.board.dto;

import com.example.demo.board.domain.Board;
import com.example.demo.board.domain.BoardType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 게시판 통합 DTO
 * 생성, 수정, 응답에 모두 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoardDto {
    
    /**
     * Validation Groups
     */
    public interface Create {}
    public interface Update {}
    
    private Long id;
    
    private BoardType boardType;
    
    @NotBlank(message = "{validation.title.required}", groups = {Create.class, Update.class})
    @Size(min = 1, max = 100, message = "{validation.title.size}", groups = {Create.class, Update.class})
    private String title;
    
    @NotBlank(message = "{validation.content.required}", groups = {Create.class, Update.class})
    @Size(min = 1, max = 1000, message = "{validation.content.size}", groups = {Create.class, Update.class})
    private String content;
    
    @NotBlank(message = "{validation.author.required}", groups = {Create.class})
    @Size(min = 1, max = 50, message = "{validation.author.size}", groups = {Create.class})
    private String author;
    
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BoardFileDto> attachedFiles; // 첨부파일 리스트
    private Integer attachedFileCount; // 첨부파일 개수 (목록 조회시 편의용)
    
    /**
     * Board 엔티티를 BoardDto로 변환
     */
    public static BoardDto from(Board board) {
        if (board == null) {
            return null;
        }
        
        List<BoardFileDto> fileDtos = board.getAttachedFiles() != null ? 
                board.getAttachedFiles().stream()
                        .map(BoardFileDto::from)
                        .collect(Collectors.toList()) : 
                null;
        
        return BoardDto.builder()
                .id(board.getId())
                .boardType(board.getBoardType())
                .title(board.getTitle())
                .content(board.getContent())
                .author(board.getAuthor())
                .viewCount(board.getViewCount())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .attachedFiles(fileDtos)
                .attachedFileCount(board.getAttachedFileCount())
                .build();
    }
    
    /**
     * 생성용 Board 엔티티로 변환
     */
    public Board toEntity() {
        return Board.builder()
                .boardType(this.boardType)
                .title(this.title)
                .content(this.content)
                .author(this.author)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .attachedFiles(new ArrayList<>()) // 빈 리스트로 초기화
                .build();
    }
}
