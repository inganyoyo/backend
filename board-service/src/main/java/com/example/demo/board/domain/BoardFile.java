package com.example.demo.board.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시판 첨부파일 도메인 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardFile {
    
    // 파일 크기 상수 정의
    private static final long BYTE = 1L;
    private static final long KILOBYTE = 1024L;
    private static final long MEGABYTE = KILOBYTE * 1024L;
    private static final long GIGABYTE = MEGABYTE * 1024L;
    
    private Long id;
    private Long boardId;                // 게시글 ID
    private String originalFileName;     // 원본 파일명
    private String storedFileName;       // 저장된 파일명
    private String filePath;             // 파일 경로
    private Long fileSize;               // 파일 크기 (byte)
    private String contentType;          // MIME 타입
    private LocalDateTime uploadedAt;    // 업로드 시간
    
    /**
     * 파일 크기를 사람이 읽기 쉬운 형태로 반환
     */
    public String getFormattedFileSize() {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }
        
        if (fileSize < KILOBYTE) {
            return fileSize + " B";
        } else if (fileSize < MEGABYTE) {
            return String.format("%.1f KB", fileSize / (double) KILOBYTE);
        } else if (fileSize < GIGABYTE) {
            return String.format("%.1f MB", fileSize / (double) MEGABYTE);
        } else {
            return String.format("%.1f GB", fileSize / (double) GIGABYTE);
        }
    }
    
    /**
     * 이미지 파일 여부 판단
     */
    public boolean isImageFile() {
        return contentType != null && contentType.startsWith("image/");
    }
}
