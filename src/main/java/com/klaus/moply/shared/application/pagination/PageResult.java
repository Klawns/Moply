package com.klaus.moply.shared.application.pagination;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/** Page content and count for the complete filtered result set. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
	public PageResult {
		content = List.copyOf(content);
		if (page < 0 || size < 1 || totalElements < 0 || totalPages < 0)
			throw new IllegalArgumentException("Metadados de paginação inválidos.");
	}

	public <R> PageResult<R> map(Function<? super T, R> mapper) {
		return new PageResult<R>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
	}

	public Stream<T> stream() {
		return content.stream();
	}

	public boolean isEmpty() {
		return content.isEmpty();
	}

	public T getFirst() {
		return content.getFirst();
	}
}
