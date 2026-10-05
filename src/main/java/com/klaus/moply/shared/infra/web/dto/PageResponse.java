package com.klaus.moply.shared.infra.web.dto;

import java.util.List;

import com.klaus.moply.shared.application.pagination.PageResult;

/** Stable HTTP response shape for paginated resources. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
	public PageResponse {
		content = List.copyOf(content);
	}

	public static <S, T> PageResponse<T> from(PageResult<S> result, java.util.function.Function<? super S, T> mapper) {
		return new PageResponse<>(result.content().stream().map(mapper).toList(), result.page(), result.size(),
				result.totalElements(), result.totalPages());
	}
}
