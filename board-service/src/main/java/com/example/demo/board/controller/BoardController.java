package com.example.demo.board.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.board.domain.BoardType;
import com.example.demo.board.dto.BoardDto;
import com.example.demo.board.dto.BoardSearchRequest;
import com.example.demo.board.dto.PagedResponse;
import com.example.demo.board.service.BoardService;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;

/**
 * 게시판 컨트롤러 (리소스 중심, boardType path variable 방식)
 */
@Slf4j
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Validated
public class BoardController {

    private final BoardService boardService;
    
    // 테스트용 상수
    private static final Long TEST_NOT_FOUND_ID = 9999L;
    private static final Long TEST_SERVICE_ERROR_ID = 999L;
    private static final String TEST_ERROR_KEYWORD = "error";

    /**
     * 게시판 목록 조회 (페이징, 검색, 정렬)
     * GET /api/boards/notice?page=1&size=10
     * GET /api/boards/free?page=1&size=10&keyword=검색어&searchType=title
     */
    @GetMapping("/{boardType}")
    public ResponseEntity<ApiResponse<PagedResponse<BoardDto>>> getBoards(
            @PathVariable String boardType,
            @ModelAttribute @Validated BoardSearchRequest searchRequest) {

        log.info("게시판 목록 조회 API 호출 - 타입: {}, 페이지: {}, 크기: {}, 검색어: {}",
                boardType, searchRequest.getPage(), searchRequest.getSize(), searchRequest.getKeyword());

        // boardType 검증
        BoardType type = validateAndParseBoardType(boardType);
        
        // 서비스 호출
        PagedResponse<BoardDto> response = boardService.getBoards(type, searchRequest);

        return ResponseEntity.ok(ApiResponse.success(getListSuccessMessage(type), response));
    }

    /**
     * 게시글 상세 조회
     * GET /api/boards/notice/{boardNo}
     * GET /api/boards/free/{boardNo}
     */
    @GetMapping("/{boardType}/{boardNo}")
    public ResponseEntity<ApiResponse<BoardDto>> getBoard(
            @PathVariable String boardType,
            @PathVariable @Positive(message = "게시글 번호는 양수여야 합니다.") Long boardNo) {

        log.info("게시글 상세 조회 API 호출 - 타입: {}, 번호: {}", boardType, boardNo);

        // boardType 검증
        BoardType type = validateAndParseBoardType(boardType);

        BoardDto response = boardService.getBoardById(boardNo, type);

        return ResponseEntity.ok(ApiResponse.success(getDetailSuccessMessage(type), response));
    }

    /**
     * 게시글 등록
     * POST /api/boards/notice
     * POST /api/boards/free
     */
    @PostMapping("/{boardType}")
    public ResponseEntity<ApiResponse<BoardDto>> createBoard(
            @PathVariable String boardType,
            @Validated(BoardDto.Create.class) @RequestBody BoardDto boardDto) {

        log.info("게시글 등록 API 호출 - 타입: {}, 제목: {}", boardType, boardDto.getTitle());

        // boardType 검증
        BoardType type = validateAndParseBoardType(boardType);

        boardDto.setBoardType(type);
        BoardDto response = boardService.createBoard(boardDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(getCreateSuccessMessage(type), response));
    }

    /**
     * 게시글 수정
     * PUT /api/boards/notice/{boardNo}
     * PUT /api/boards/free/{boardNo}
     */
    @PutMapping("/{boardType}/{boardNo}")
    public ResponseEntity<ApiResponse<BoardDto>> updateBoard(
            @PathVariable String boardType,
            @PathVariable @Positive(message = "게시글 번호는 양수여야 합니다.") Long boardNo,
            @Validated(BoardDto.Update.class) @RequestBody BoardDto boardDto) {

        log.info("게시글 수정 API 호출 - 타입: {}, 번호: {}, 제목: {}", boardType, boardNo, boardDto.getTitle());

        // boardType 검증
        BoardType type = validateAndParseBoardType(boardType);

        BoardDto response = boardService.updateBoard(boardNo, boardDto, type);

        return ResponseEntity.ok(ApiResponse.success(getUpdateSuccessMessage(type), response));
    }

    /**
     * 게시글 삭제
     * DELETE /api/boards/notice/{boardNo}
     * DELETE /api/boards/free/{boardNo}
     */
    @DeleteMapping("/{boardType}/{boardNo}")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(
            @PathVariable String boardType,
            @PathVariable @Positive(message = "게시글 번호는 양수여야 합니다.") Long boardNo) {

        log.info("게시글 삭제 API 호출 - 타입: {}, 번호: {}", boardType, boardNo);

        // boardType 검증
        BoardType type = validateAndParseBoardType(boardType);

        boardService.deleteBoard(boardNo, type);

        return ResponseEntity.ok(ApiResponse.success(getDeleteSuccessMessage(type)));
    }

    /**
     * 예외 테스트용 엔드포인트
     * GET /api/boards/notice/test/error/{type}
     * GET /api/boards/free/test/error/{type}
     */
    @GetMapping("/{boardType}/test/error/{type}")
    public ResponseEntity<ApiResponse<Void>> testException(
            @PathVariable String boardType,
            @PathVariable String type) {

        log.info("예외 테스트 API 호출 - 게시판타입: {}, 테스트타입: {}", boardType, type);

        // boardType 검증
        BoardType boardTypeEnum = validateAndParseBoardType(boardType);

        switch (type.toLowerCase()) {
            case "notfound":
                boardService.getBoardById(TEST_NOT_FOUND_ID, boardTypeEnum);
                break;
            case "service":
                boardService.getBoardById(TEST_SERVICE_ERROR_ID, boardTypeEnum);
                break;
            case "search":
                BoardSearchRequest searchRequest = BoardSearchRequest.builder()
                        .keyword(TEST_ERROR_KEYWORD)
                        .build();
                boardService.getBoards(boardTypeEnum, searchRequest);
                break;
            default:
                throw new RuntimeException("알 수 없는 예외가 발생했습니다.");
        }

        return ResponseEntity.ok(ApiResponse.success("테스트 완료"));
    }

    // === Private 헬퍼 메서드들 ===

    /**
     * boardType 문자열을 BoardType enum으로 변환 (검증 포함)
     */
    private BoardType validateAndParseBoardType(String boardType) {
        BoardType type = BoardType.fromCode(boardType);
        if (type == null) {
            throw new BusinessException(ErrorCode.INVALID_BOARD_TYPE);
        }
        return type;
    }

    /**
     * 성공 메시지 생성
     */
    private String getSuccessMessage(BoardType boardType, String action) {
        String prefix = boardType == BoardType.NOTICE ? "공지사항" : "자유게시판 글";
        return prefix + "이 성공적으로 " + action + "되었습니다.";
    }

    /**
     * 목록 조회 성공 메시지
     */
    private String getListSuccessMessage(BoardType boardType) {
        String prefix = boardType == BoardType.NOTICE ? "공지사항" : "자유게시판";
        return prefix + " 목록을 성공적으로 조회했습니다.";
    }

    /**
     * 상세 조회 성공 메시지
     */
    private String getDetailSuccessMessage(BoardType boardType) {
        String prefix = boardType == BoardType.NOTICE ? "공지사항" : "자유게시판 글";
        return prefix + "을 성공적으로 조회했습니다.";
    }

    /**
     * 생성 성공 메시지
     */
    private String getCreateSuccessMessage(BoardType boardType) {
        return getSuccessMessage(boardType, "등록");
    }

    /**
     * 수정 성공 메시지
     */
    private String getUpdateSuccessMessage(BoardType boardType) {
        return getSuccessMessage(boardType, "수정");
    }

    /**
     * 삭제 성공 메시지
     */
    private String getDeleteSuccessMessage(BoardType boardType) {
        return getSuccessMessage(boardType, "삭제");
    }
}
