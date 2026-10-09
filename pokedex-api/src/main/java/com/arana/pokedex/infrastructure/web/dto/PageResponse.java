package com.arana.pokedex.infrastructure.web.dto;

import com.arana.pokedex.domain.model.PageResult;

import java.util.List;
import java.util.function.Function;

/**
 * The single page envelope every paginated endpoint returns.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages,
                              boolean first, boolean last) {

    public static <D, T> PageResponse<T> from(PageResult<D> result, Function<D, T> mapper) {
        int totalPages = result.totalPages();
        return new PageResponse<>(
                result.content().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                totalPages,
                result.page() == 0,
                result.page() >= totalPages - 1);
    }
}
