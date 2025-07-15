package com.example.demo.board.service;

import com.example.demo.board.domain.Board;
import com.example.demo.board.domain.BoardFile;
import com.example.demo.board.domain.BoardType;
import com.example.demo.board.domain.SearchType;
import com.example.demo.board.dto.BoardDto;
import com.example.demo.board.dto.BoardSearchRequest;
import com.example.demo.board.dto.PagedResponse;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.code.CommonErrorCode;
import com.example.demo.common.code.CustomErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


/**
 * 게시판 서비스
 */
@Slf4j
@Service
public class BoardService {
    
    // 상수 정의
    private static final int INITIAL_NOTICE_COUNT = 3;
    private static final int INITIAL_FREE_COUNT = 5;
    private static final int NOTICE_VIEW_COUNT_MULTIPLIER = 10;
    private static final int FREE_VIEW_COUNT_MULTIPLIER = 5;
    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final Long TEST_SERVICE_ERROR_ID = 999L;
    private static final Long UNDELETABLE_BOARD_ID = 1L;
    private static final String TEST_ERROR_KEYWORD = "error";
    private static final String TEST_ADMIN_AUTHOR = "admin";
    private static final String TEST_FORBIDDEN_CONTENT = "forbidden";
    
    // 메모리상에 데이터 저장 (DB 대신 사용)
    private final Map<Long, Board> boardStorage = new ConcurrentHashMap<>();
    private final Map<Long, BoardFile> fileStorage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final AtomicLong fileIdGenerator = new AtomicLong(1);
    
    public BoardService() {
        initializeData();
    }
    
    /**
     * 초기 데이터 생성
     */
    private void initializeData() {
        createInitialNoticeBoards();
        createInitialFreeBoards();
        
        log.info("초기 데이터 {} 개 생성 완료 (공지사항: {}개, 자유게시판: {}개)", 
                boardStorage.size(), INITIAL_NOTICE_COUNT, INITIAL_FREE_COUNT);
        log.info("첨부파일 {} 개 생성 완료", fileStorage.size());
    }
    
    /**
     * 공지사항 초기 데이터 생성
     */
    private void createInitialNoticeBoards() {
        for (int i = 1; i <= INITIAL_NOTICE_COUNT; i++) {
            Board board = createBoard(BoardType.NOTICE, i, "공지사항 " + i, 
                    "이것은 공지사항 " + i + "의 내용입니다.", "관리자" + i, 
                    i * NOTICE_VIEW_COUNT_MULTIPLIER);
            
            // 첫 번째 공지사항에만 첨부파일 추가
            if (i == 1) {
                addSampleFilesToBoard(board, 2);
            }
            
            boardStorage.put(board.getId(), board);
        }
    }
    
    /**
     * 자유게시판 초기 데이터 생성
     */
    private void createInitialFreeBoards() {
        for (int i = 1; i <= INITIAL_FREE_COUNT; i++) {
            Board board = createBoard(BoardType.FREE, i, "자유게시판 글 " + i,
                    "이것은 자유게시판 " + i + "의 내용입니다.", "사용자" + i,
                    i * FREE_VIEW_COUNT_MULTIPLIER);
            
            // 홀수 번째 글에만 첨부파일 추가
            if (i % 2 == 1) {
                addSampleFilesToBoard(board, 1);
            }
            
            boardStorage.put(board.getId(), board);
        }
    }
    
    /**
     * 게시글 생성 (공통 로직)
     */
    private Board createBoard(BoardType boardType, int sequence, String title, 
                             String content, String author, int viewCount) {
        return Board.builder()
                .id(idGenerator.getAndIncrement())
                .boardType(boardType)
                .title(title)
                .content(content)
                .author(author)
                .viewCount(viewCount)
                .createdAt(LocalDateTime.now().minusDays(sequence))
                .updatedAt(LocalDateTime.now().minusDays(sequence))
                .attachedFiles(new ArrayList<>())
                .build();
    }
    
    /**
     * 게시글에 샘플 첨부파일 추가
     */
    private void addSampleFilesToBoard(Board board, int fileCount) {
        // PDF 파일들 추가
        for (int j = 1; j <= fileCount; j++) {
            BoardFile pdfFile = createSampleFile(board, j, "pdf", "샘플파일" + j + ".pdf", 
                    "application/pdf", MB * j);
            fileStorage.put(pdfFile.getId(), pdfFile);
            board.getAttachedFiles().add(pdfFile);
        }
        
        // 이미지 파일 추가
        BoardFile imageFile = createSampleFile(board, 0, "image", "이미지" + board.getId() + ".jpg",
                "image/jpeg", 512L * KB);
        fileStorage.put(imageFile.getId(), imageFile);
        board.getAttachedFiles().add(imageFile);
    }
    
    /**
     * 샘플 파일 생성 (공통 로직)
     */
    private BoardFile createSampleFile(Board board, int sequence, String type, 
                                      String originalFileName, String contentType, long fileSize) {
        String storedFileName = sequence == 0 ? 
                "stored_" + board.getId() + "_" + type + getFileExtension(originalFileName) :
                "stored_" + board.getId() + "_" + sequence + getFileExtension(originalFileName);
                
        return BoardFile.builder()
                .id(fileIdGenerator.getAndIncrement())
                .boardId(board.getId())
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath("/uploads/" + board.getId() + "/")
                .fileSize(fileSize)
                .contentType(contentType)
                .uploadedAt(board.getCreatedAt())
                .build();
    }
    
    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
    
    /**
     * 게시판 목록 조회 (페이징, 검색, 정렬)
     */
    public PagedResponse<BoardDto> getBoards(BoardType boardType, BoardSearchRequest searchRequest) {
        log.info("게시판 목록 조회 요청 - 타입: {}, 페이지: {}, 크기: {}, 검색어: {}", 
                boardType, searchRequest.getPage(), searchRequest.getSize(), searchRequest.getKeyword());
        
        validateSearchKeyword(searchRequest.getKeyword());
        
        List<Board> boards = getBoardsByType(boardType);
        List<Board> filteredBoards = applySearchCondition(boards, searchRequest);
        List<Board> sortedBoards = applySorting(filteredBoards, searchRequest);
        List<Board> pagedBoards = applyPaging(sortedBoards, searchRequest);
        
        List<BoardDto> boardDtos = pagedBoards.stream()
                .map(BoardDto::from)
                .collect(Collectors.toList());
        
        return PagedResponse.of(boardDtos, searchRequest.getPage(), searchRequest.getSize(), filteredBoards.size());
    }
    
    /**
     * 검색 키워드 검증 (테스트용)
     */
    private void validateSearchKeyword(String keyword) {
        if (TEST_ERROR_KEYWORD.equalsIgnoreCase(keyword)) {
            throw BusinessException.builder(CustomErrorCode.BOARD_LIST_ERROR).build();
        }
    }
    
    /**
     * 타입별 게시글 조회
     */
    private List<Board> getBoardsByType(BoardType boardType) {
        return boardStorage.values().stream()
                .filter(board -> board.getBoardType() == boardType)
                .collect(Collectors.toList());
    }
    
    /**
     * 검색 조건 적용
     */
    private List<Board> applySearchCondition(List<Board> boards, BoardSearchRequest searchRequest) {
        if (!searchRequest.hasSearchCondition()) {
            return boards;
        }
        
        String keyword = searchRequest.getKeyword().toLowerCase();
        SearchType searchType = searchRequest.getSearchType();
        
        return boards.stream()
                .filter(board -> matchesSearchCondition(board, keyword, searchType))
                .collect(Collectors.toList());
    }
    
    /**
     * 게시글이 검색 조건에 맞는지 확인
     */
    private boolean matchesSearchCondition(Board board, String keyword, SearchType searchType) {
        switch (searchType) {
            case TITLE:
                return board.getTitle().toLowerCase().contains(keyword);
            case CONTENT:
                return board.getContent().toLowerCase().contains(keyword);
            case AUTHOR:
                return board.getAuthor().toLowerCase().contains(keyword);
            case ALL:
            default:
                return board.getTitle().toLowerCase().contains(keyword) ||
                       board.getContent().toLowerCase().contains(keyword) ||
                       board.getAuthor().toLowerCase().contains(keyword);
        }
    }
    
    /**
     * 정렬 적용
     */
    private List<Board> applySorting(List<Board> boards, BoardSearchRequest searchRequest) {
        Comparator<Board> comparator = createComparator(searchRequest.getSort());
        
        if ("desc".equalsIgnoreCase(searchRequest.getOrder())) {
            comparator = comparator.reversed();
        }
        
        return boards.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
    
    /**
     * 정렬 기준에 따른 Comparator 생성
     */
    private Comparator<Board> createComparator(String sort) {
        switch (sort) {
            case "title":
                return Comparator.comparing(Board::getTitle);
            case "viewCount":
                return Comparator.comparing(Board::getViewCount);
            case "regDate":
            default:
                return Comparator.comparing(Board::getCreatedAt);
        }
    }
    
    /**
     * 페이징 적용
     */
    private List<Board> applyPaging(List<Board> boards, BoardSearchRequest searchRequest) {
        int startIndex = searchRequest.getOffset();
        int endIndex = Math.min(startIndex + searchRequest.getSize(), boards.size());
        
        if (startIndex >= boards.size()) {
            return new ArrayList<>();
        }
        
        return boards.subList(startIndex, endIndex);
    }
    
    /**
     * 게시글 단건 조회
     */
    public BoardDto getBoardById(Long id, BoardType boardType) {
        log.info("게시글 조회 요청 - ID: {}, 타입: {}", id, boardType);
        
        validateServiceErrorTest(id);
        
        Board board = findBoardById(id);
        validateBoardType(board, boardType);
        
        // 조회수 증가
        board.incrementViewCount();
        
        return BoardDto.from(board);
    }
    
    /**
     * 서비스 에러 테스트 검증
     */
    private void validateServiceErrorTest(Long id) {
        if (TEST_SERVICE_ERROR_ID.equals(id)) {
            log.info("validateServiceErrorTest not found");
            throw BusinessException.builder(CommonErrorCode.SYSTEM_MAINTENANCE).build();
        }
    }
    
    /**
     * ID로 게시글 조회
     */
    private Board findBoardById(Long id) {
        Board board = boardStorage.get(id);
        if (board == null) {
            // 아규먼트를 사용하여 메시지 템플릿에 "게시글" 전달
            throw BusinessException.builder(CommonErrorCode.ENTITY_NOT_FOUND).args("게시글").build();
        }
        return board;
    }
    
    /**
     * 특정 ID의 게시글을 특정 작성자가 작성했는지 확인
     */
    public boolean isBoardAuthor(Long boardId, String authorName) {
        log.info("게시글 작성자 확인 - ID: {}, 작성자: {}", boardId, authorName);
        
        Board board = boardStorage.get(boardId);
        if (board == null) {
            // ID와 함께 더 자세한 정보 제공
            throw BusinessException.builder(CommonErrorCode.ENTITY_NOT_FOUND_WITH_ID).args("게시글", boardId).build();
        }
        
        return board.getAuthor().equals(authorName);
    }
    
    /**
     * 특정 작성자의 게시글 개수 조회
     */
    public long getPostCountByAuthor(String authorName) {
        log.info("작성자별 게시글 수 조회 - 작성자: {}", authorName);
        
        if (authorName == null || authorName.trim().isEmpty()) {
            throw BusinessException.builder(CommonErrorCode.VALIDATION_REQUIRED).args("작성자명").build();
        }
        
        long count = boardStorage.values().stream()
                .filter(board -> board.getAuthor().equals(authorName))
                .count();
                
        log.info("작성자 '{}' 의 게시글 개수: {}", authorName, count);
        return count;
    }
    
    /**
     * 첨부파일 조회
     */
    public BoardFile getAttachedFile(Long boardId, Long fileId) {
        log.info("첨부파일 조회 - 게시글ID: {}, 파일ID: {}", boardId, fileId);
        
        // 게시글 존재 확인
        Board board = findBoardById(boardId);
        
        // 파일 존재 확인
        BoardFile file = fileStorage.get(fileId);
        if (file == null) {

        }
        
        // 해당 게시글의 파일인지 확인
        if (!file.getBoardId().equals(boardId)) {
            throw BusinessException.builder(CommonErrorCode.ACCESS_DENIED).build();
        }
        
        return file;
    }

    /**
     * 게시글 타입 검증
     */
    private void validateBoardType(Board board, BoardType expectedType) {
        if (board.getBoardType() != expectedType) {

        }
    }
    
    /**
     * 게시글 생성
     */
    public BoardDto createBoard(BoardDto boardDto) {
        log.info("게시글 생성 요청 - 제목: {}, 타입: {}", boardDto.getTitle(), boardDto.getBoardType());
        
        validateCreateRequest(boardDto);
        
        Board board = Board.builder()
                .id(idGenerator.getAndIncrement())
                .boardType(boardDto.getBoardType())
                .title(boardDto.getTitle())
                .content(boardDto.getContent())
                .author(boardDto.getAuthor())
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .attachedFiles(new ArrayList<>())
                .build();
        
        boardStorage.put(board.getId(), board);
        log.info("게시글 생성 완료 - ID: {}", board.getId());
        
        return BoardDto.from(board);
    }
    
    /**
     * 게시글 생성 요청 검증
     */
    private void validateCreateRequest(BoardDto boardDto) {
        if (boardDto.getTitle().toLowerCase().contains(TEST_ERROR_KEYWORD)) {
            throw BusinessException.builder(CustomErrorCode.INVALID_TITLE_CONTENT).build();
        }
        
        if (TEST_ADMIN_AUTHOR.equalsIgnoreCase(boardDto.getAuthor())) {
            throw BusinessException.builder(CustomErrorCode.ADMIN_WRITE_FORBIDDEN).build();
        }
    }
    
    /**
     * 게시글 수정
     */
    public BoardDto updateBoard(Long id, BoardDto boardDto, BoardType boardType) {
        log.info("게시글 수정 요청 - ID: {}, 제목: {}, 타입: {}", id, boardDto.getTitle(), boardType);
        
        Board board = findBoardById(id);
        validateBoardType(board, boardType);
        validateUpdateRequest(boardDto);
        
        Board updatedBoard = Board.builder()
                .id(board.getId())
                .boardType(board.getBoardType())
                .title(boardDto.getTitle())
                .content(boardDto.getContent())
                .author(board.getAuthor()) // 작성자는 변경하지 않음
                .viewCount(board.getViewCount())
                .createdAt(board.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .attachedFiles(board.getAttachedFiles()) // 기존 첨부파일 유지
                .build();
        
        boardStorage.put(id, updatedBoard);
        log.info("게시글 수정 완료 - ID: {}", id);
        
        return BoardDto.from(updatedBoard);
    }
    
    /**
     * 게시글 수정 요청 검증
     */
    private void validateUpdateRequest(BoardDto boardDto) {
        if (boardDto.getContent().toLowerCase().contains(TEST_FORBIDDEN_CONTENT)) {
            throw BusinessException.builder(CustomErrorCode.FORBIDDEN_CONTENT).build();
        }
    }
    
    /**
     * 게시글 삭제
     */
    public void deleteBoard(Long id, BoardType boardType) {
        log.info("게시글 삭제 요청 - ID: {}, 타입: {}", id, boardType);
        
        Board board = findBoardById(id);
        validateBoardType(board, boardType);
        validateDeleteRequest(id);
        
        // 첨부파일도 함께 삭제
        deleteAttachedFiles(board);
        
        boardStorage.remove(id);
        log.info("게시글 삭제 완료 - ID: {}", id);
    }
    
    /**
     * 게시글 삭제 요청 검증
     */
    private void validateDeleteRequest(Long id) {
        if (UNDELETABLE_BOARD_ID.equals(id)) {
            throw BusinessException.builder(CustomErrorCode.DELETE_FORBIDDEN).build();
        }
    }
    
    /**
     * 첨부파일 삭제
     */
    private void deleteAttachedFiles(Board board) {
        if (board.hasAttachedFiles()) {
            board.getAttachedFiles().forEach(file -> fileStorage.remove(file.getId()));
            log.info("첨부파일 {} 개 삭제 완료", board.getAttachedFileCount());
        }
    }
}
