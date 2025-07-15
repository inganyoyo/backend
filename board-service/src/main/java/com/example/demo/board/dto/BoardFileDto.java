package com.example.demo.board.dto;

import com.example.demo.board.domain.BoardFile;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시판 첨부파일 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoardFileDto {
    
    private Long id;
    private String originalFileName;     // 원본 파일명
    private String storedFileName;       // 저장된 파일명 (다운로드 링크 생성용)
    private Long fileSize;               // 파일 크기 (byte)
    private String formattedFileSize;    // 사람이 읽기 쉬운 파일 크기
    private String contentType;          // MIME 타입
    private boolean isImageFile;         // 이미지 파일 여부
    private LocalDateTime uploadedAt;    // 업로드 시간
    
    /**
     * BoardFile 엔티티를 BoardFileDto로 변환
     */
    public static BoardFileDto from(BoardFile boardFile) {
        if (boardFile == null) {
            return null;
        }
        
        return BoardFileDto.builder()
                .id(boardFile.getId())
                .originalFileName(boardFile.getOriginalFileName())
                .storedFileName(boardFile.getStoredFileName())
                .fileSize(boardFile.getFileSize())
                .formattedFileSize(boardFile.getFormattedFileSize())
                .contentType(boardFile.getContentType())
                .isImageFile(boardFile.isImageFile())
                .uploadedAt(boardFile.getUploadedAt())
                .build();
    }
}
