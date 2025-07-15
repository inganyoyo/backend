package com.example.demo.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private PageInfo pageInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        if (content == null) content = Collections.emptyList();
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (totalElements < 0) totalElements = 0;

        int totalPages = totalElements == 0 ? 1 : (int) Math.ceil((double) totalElements / size);

        return PagedResponse.<T>builder()
                .content(content)
                .pageInfo(PageInfo.builder()
                        .page(page)
                        .size(size)
                        .totalElements(totalElements)
                        .totalPages(totalPages)
                        .first(page == 1)
                        .last(page >= totalPages)
                        .hasNext(page < totalPages)
                        .hasPrevious(page > 1)
                        .build())
                .build();
    }
}
