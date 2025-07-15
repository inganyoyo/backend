package com.example.demo.board.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시판 도메인 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    
    private Long id;
    private BoardType boardType;
    private String title;
    private String content;
    private String author;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BoardFile> attachedFiles; // 첨부파일 리스트
    
    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    /**
     * 첨부파일 리스트 초기화
     */
    public List<BoardFile> getAttachedFiles() {
        if (this.attachedFiles == null) {
            this.attachedFiles = new ArrayList<>();
        }
        return this.attachedFiles;
    }
    
    /**
     * 첨부파일 개수 반환
     */
    public int getAttachedFileCount() {
        return getAttachedFiles().size();
    }
    
    /**
     * 첨부파일이 있는지 확인
     */
    public boolean hasAttachedFiles() {
        return getAttachedFileCount() > 0;
    }
}
